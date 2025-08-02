package com.compiler.backend.allocator;

import com.compiler.backend.PhysicalRegister;
import com.compiler.mir.MIRBasicBlock;
import com.compiler.mir.MIRFunction;
import com.compiler.mir.MIRType;
import com.compiler.mir.instruction.*;
import com.compiler.mir.operand.*;
import java.util.*;

public class LinearScanRegisterAllocator {
    private final List<LiveInterval> intervals = new ArrayList<>();
    private final Map<LiveInterval, PhysicalRegister> registerAssignment = new LinkedHashMap<>();
    private final Map<LiveInterval, Integer> spillLocations = new LinkedHashMap<>();

    // 分开整数和浮点可用寄存器
    private final List<PhysicalRegister> availableIntRegs = new ArrayList<>();
    private final List<PhysicalRegister> availableFloatRegs = new ArrayList<>();

    // 预留的临时寄存器（用于spill加载/存储）

    private final PhysicalRegister intTempReg1 = PhysicalRegister.T0;
    private final PhysicalRegister intTempReg2 = PhysicalRegister.T1;
    int tempIntRegIndex = 0;
    private final List<PhysicalRegister> tempIntRegs = Arrays.asList(intTempReg1, intTempReg2);

    private final PhysicalRegister floatTempReg1 = PhysicalRegister.FT0;
    private final PhysicalRegister floatTempReg2 = PhysicalRegister.FT1;
    int tempFloatRegIndex = 0;
    private final List<PhysicalRegister> tempFloatRegs = Arrays.asList(floatTempReg1, floatTempReg2);


    private int stackOffset = 0;
    private final MIRFunction function;
    private final Map<MIRBasicBlock, Set<MIRVirtualReg>> liveInSets = new LinkedHashMap<>();
    private final Map<MIRBasicBlock, Set<MIRVirtualReg>> liveOutSets = new LinkedHashMap<>();

    private final Map<MIRInstruction, Integer> instructionPositions = new LinkedHashMap<>();
    private final Map<MIRVirtualReg, LiveInterval> intervalMap = new LinkedHashMap<>();

    // 记录使用的寄存器集合
    private final Set<PhysicalRegister> usedCalleeSaved = new LinkedHashSet<>();
    private final Set<PhysicalRegister> usedCallerSaved = new LinkedHashSet<>();


    public LinearScanRegisterAllocator(MIRFunction function) {
        this.function = function;
        System.out.println("starting linear scan register allocation for function: " + function.getName());
        initializeAvailableRegisters();
        System.out.println(availableIntRegs);
        System.out.println(availableFloatRegs);
        assignInstructionPositions();
        buildControlFlowGraph();
        computeLiveness();
        System.out.println("liveness initialized");
        buildLiveIntervals();
        System.out.println("live intervals initialized");
    }

    private void initializeAvailableRegisters() {
        // 初始化整数寄存器
        availableIntRegs.addAll(PhysicalRegister.CALLER_SAVED_INT);
        availableIntRegs.addAll(PhysicalRegister.CALLEE_SAVED_INT);

        // 初始化浮点寄存器
        availableFloatRegs.addAll(PhysicalRegister.CALLER_SAVED_FLOAT);
        availableFloatRegs.addAll(PhysicalRegister.CALLEE_SAVED_FLOAT);

        // 移除特殊寄存器
//        availableIntRegs.remove(PhysicalRegister.SP);
//        availableIntRegs.remove(PhysicalRegister.RA);
        availableIntRegs.remove(PhysicalRegister.A0); // 返回值寄存器
        availableFloatRegs.remove(PhysicalRegister.FA0); // 浮点返回值寄存器

        // 预留临时寄存器（不参与分配）
        availableIntRegs.remove(intTempReg1);
        availableIntRegs.remove(intTempReg2);
        availableFloatRegs.remove(floatTempReg1);
        availableFloatRegs.remove(floatTempReg2);
    }

    // 给每个指令分配序号，从entry到return
    private void assignInstructionPositions() {
        int counter = 0;
        for (MIRBasicBlock block : function.getBlocks()) {
            for (MIRInstruction inst : block.getInstructions()) {
                instructionPositions.put(inst, counter++);
            }
        }
    }

    public void allocate() {

        // 分别处理整数和浮点寄存器分配
        List<LiveInterval> intIntervals = new ArrayList<>();
        List<LiveInterval> floatIntervals = new ArrayList<>();

        // 按类型分离生存区间
        for (LiveInterval interval : intervals) {
            if (MIRType.isFloat(interval.vreg.getType())) {
                floatIntervals.add(interval);
            } else {
                intIntervals.add(interval);
            }
        }

        // 分别排序
        intIntervals.sort(Comparator.comparingInt(i -> i.start));
        floatIntervals.sort(Comparator.comparingInt(i -> i.start));

        // 按起始点排序生存区间
        intervals.sort(Comparator.comparingInt(i -> i.start));
        for(var interval: intIntervals) {
            System.err.println(interval.vreg.toString() + ": " + interval.start + " " + interval.end);
        }
        System.out.println("starting linear scan register allocation for function: " + function.getName());

        // 分别处理整数和浮点寄存器分配
        allocateRegisters(intIntervals, false); // 分配整数寄存器
        allocateRegisters(floatIntervals, true);  // 分配浮点寄存器

        // 打印出分配情况
        System.out.println("Register allocation completed for function: " + function.getName());
        for(var entry : registerAssignment.entrySet()) {
            LiveInterval interval = entry.getKey();
            PhysicalRegister reg = entry.getValue();
            System.out.println("VReg: " + interval.vreg + " assigned to " + reg);
        }
        for(var entry : spillLocations.entrySet()) {
            LiveInterval interval = entry.getKey();
            Integer location = entry.getValue();
            System.out.println("VReg: " + interval.vreg + " spilled at location: " + location);
        }
    }

    private void allocateRegisters(List<LiveInterval> intervals, boolean forFloat) {
//        TreeSet<LiveInterval> active = new TreeSet<>(Comparator.comparingInt(i -> i.end));
        TreeSet<LiveInterval> active = new TreeSet<>((i1, i2) -> {
            int endCompare = Integer.compare(i1.end, i2.end);
            if (endCompare != 0) {
                return endCompare;
            }
            // end 相同时，按 start 排序
            int startCompare = Integer.compare(i1.start, i2.start);
            if (startCompare != 0) {
                return startCompare;
            }
            // start 也相同时，按虚拟寄存器的唯一标识排序
            return i1.vreg.toString().compareTo(i2.vreg.toString());
        });

        List<PhysicalRegister> availableRegs = forFloat ?
                new ArrayList<>(availableFloatRegs) :
                new ArrayList<>(availableIntRegs);
        int R = availableRegs.size();
        System.out.println(intervals.size());
        System.out.println("Allocating " + R + " registers for function: " + function.getName());

        for (LiveInterval current : intervals) {

            if (MIRType.isFloat(current.vreg.getType()) != forFloat){
                throw new IllegalArgumentException("Interval type mismatch: " + current.vreg.getType());
                //continue;
            }


            expireOldIntervals(current.start, active, availableRegs);

            if (active.size() == R) {
//                throw new RuntimeException("never access");
                spillAtInterval(current, active, availableRegs, forFloat);
            } else {
                // 分配物理寄存器

                PhysicalRegister reg = availableRegs.remove(0);
                registerAssignment.put(current, reg);
                active.add(current);
                System.err.println("active = " + active.size());
                recordUsedRegister(reg);
            }
        }
    }

    private void recordUsedRegister(PhysicalRegister reg) {
        if (PhysicalRegister.CALLEE_SAVED_INT.contains(reg) ||
                PhysicalRegister.CALLEE_SAVED_FLOAT.contains(reg)) {
            usedCalleeSaved.add(reg);
        } else if (PhysicalRegister.CALLER_SAVED_INT.contains(reg) ||
                PhysicalRegister.CALLER_SAVED_FLOAT.contains(reg)) {
            usedCallerSaved.add(reg);
        }
    }

    private void expireOldIntervals(int currentPoint, TreeSet<LiveInterval> active,
                                    List<PhysicalRegister> availableRegs) {
        Iterator<LiveInterval> it = active.iterator();
        int j = 0;
        while (it.hasNext()) {
//            System.err.println("j =" + j++);
            LiveInterval interval = it.next();
            if (interval.end <= currentPoint) {
//                System.err.println(registerAssignment.get(interval));
                System.err.println("available = " + availableRegs.size());
                availableRegs.add(registerAssignment.get(interval));
                System.err.println("available = " + availableRegs.size());
                it.remove();
            } else {
                break;
            }
        }
    }

    private void spillAtInterval(LiveInterval current, TreeSet<LiveInterval> active, List<PhysicalRegister> availableRegs, boolean forFloat) {
        LiveInterval spill = active.last();
        if (spill.end > current.end) {
            // 溢出spill区间
            spillLocations.put(spill, allocateSpillSlot(forFloat));

            // 将spill的寄存器分配给当前区间
            PhysicalRegister reg = registerAssignment.get(spill);
            registerAssignment.put(current, reg);
            registerAssignment.remove(spill);

            active.remove(spill);
            active.add(current);
        } else {
            // 溢出当前区间
            spillLocations.put(current, allocateSpillSlot(MIRType.isFloat(current.vreg.getType())));
        }
    }

//    private int allocateSpillSlot() {
//        stackOffset -= 8; // 每个溢出槽8个字节
//        return stackOffset;
//    }
    private int allocateSpillSlot(boolean isFloat) {
        stackOffset -= 8; // 整数和浮点都占用8个字节
        return stackOffset;
    }

    private void buildControlFlowGraph() {
        // 初始化CFG
        for (MIRBasicBlock block : function.getBlocks()) {
            liveInSets.put(block, new LinkedHashSet<>());
            liveOutSets.put(block, new LinkedHashSet<>());
        }
    }

    private void computeLiveness() {
        boolean changed;
        int iteration = 0;
        int maxIterations = function.getBlocks().size() * 20000; // 防止无限循环

        do {
            changed = false;
            iteration++;

            // 反向遍历基本块
            List<MIRBasicBlock> blocks = new ArrayList<>(function.getBlocks());
            Collections.reverse(blocks);

            for(MIRBasicBlock block : blocks) {
                System.out.println(block.getLabel().toString());
            }


            for (MIRBasicBlock block : blocks) {
                // 计算use[B]和def[B]
                Set<MIRVirtualReg> use = new LinkedHashSet<>();
                Set<MIRVirtualReg> def = new LinkedHashSet<>();

                // 遍历块中的指令
                for (MIRInstruction inst : block.getInstructions()) {
                    // 处理uses
                    for (MIRVirtualReg reg : getUsedRegisters(inst)) {
                        // 如果该寄存器还没有被定义，则添加到use集合
                        if (!def.contains(reg)) {
                            use.add(reg);
                        }
                    }

                    // 处理defs
                    MIRVirtualReg defReg = getDefinedRegister(inst);
                    if (defReg != null) {
                        def.add(defReg);
                    }
                }

                // 计算新的liveOut
                Set<MIRVirtualReg> newLiveOut = new LinkedHashSet<>();
                for (MIRBasicBlock successor : getSuccessors(block)) {
                    newLiveOut.addAll(liveInSets.get(successor));
                }

                // 计算新的liveIn: use[B] ∪ (liveOut[B] - def[B])
                Set<MIRVirtualReg> newLiveIn = new LinkedHashSet<>(use);
                Set<MIRVirtualReg> liveOutMinusDef = new LinkedHashSet<>(newLiveOut);
                liveOutMinusDef.removeAll(def);
                newLiveIn.addAll(liveOutMinusDef);

                // 检查是否有变化
                if (!newLiveIn.equals(liveInSets.get(block)) ||
                        !newLiveOut.equals(liveOutSets.get(block))) {

                    liveInSets.put(block, newLiveIn);
                    liveOutSets.put(block, newLiveOut);
                    changed = true;
                }
            }
        } while (changed && iteration < maxIterations);
    }

    private Set<MIRBasicBlock> getSuccessors(MIRBasicBlock block) {
        Set<MIRBasicBlock> successors = new LinkedHashSet<>();
        List<MIRInstruction> instructions = block.getInstructions();

        if (!instructions.isEmpty()) {
            if(instructions.size() > 2){
                MIRInstruction secondLastInst = instructions.get(instructions.size() - 2);
                if(secondLastInst instanceof MIRControlFlowOp){
                    MIRControlFlowOp cf = (MIRControlFlowOp) secondLastInst;
                    if (cf.getType() == MIRControlFlowOp.Type.COND_JMP) {
                        // 条件跳转
                        MIRBasicBlock trueTarget = findBlockByName(cf.getTarget().toString());
                        if (trueTarget != null) successors.add(trueTarget);
                    } else {
                        throw new IllegalStateException("Unexpected control flow operation: " + secondLastInst);
                    }
                }
            }

            MIRInstruction lastInst = instructions.get(instructions.size() - 1);

            if (lastInst instanceof MIRControlFlowOp) {
                MIRControlFlowOp cfOp = (MIRControlFlowOp) lastInst;

                if (cfOp.getType() == MIRControlFlowOp.Type.JMP) {
                    // 无条件跳转
                    MIRBasicBlock target = findBlockByName(cfOp.getTarget().toString());
                    if (target != null) successors.add(target);
                } else if (cfOp.getType() == MIRControlFlowOp.Type.RET) {
                    // 返回语句，无后继
                }
            } else {
                // 没有控制流指令，顺序执行下一个块
                System.err.println("Unexpected control flow operation: " + lastInst);

                int currentIndex = function.getBlocks().indexOf(block);
                if (currentIndex < function.getBlocks().size() - 1) {
                    successors.add(function.getBlocks().get(currentIndex + 1));
                }
                throw new IllegalStateException("Unexpected control flow operation: " + lastInst);
            }
        }

        return successors;
    }

    private MIRBasicBlock findBlockByName(String name) {
        for (MIRBasicBlock block : function.getBlocks()) {
            if (block.getLabel().toString().equals(name)) {
                return block;
            }
        }
        return null;
    }

    private void buildLiveIntervals() {
        // 初始化所有虚拟寄存器的生存区间
//        for (MIRVirtualReg reg : getAllVirtualRegisters()) {
//            intervalMap.put(reg, new LiveInterval(reg, Integer.MAX_VALUE, Integer.MIN_VALUE));
//        }

        // 如果有一个变量之后未被使用怎么办？
        // 直接删除这条指令？

        for (MIRVirtualReg reg : getAllVirtualRegisters()) {
            // 自动检测寄存器类型
            intervalMap.put(reg, new LiveInterval(reg, Integer.MAX_VALUE, Integer.MIN_VALUE));
        }

        // 第一遍：处理指令级别的定义和使用
        for (MIRBasicBlock block : function.getBlocks()) {
            for (MIRInstruction inst : block.getInstructions()) {
                int pos = instructionPositions.get(inst);

                // 处理定义
                MIRVirtualReg def = getDefinedRegister(inst);
                if (def != null) {
                    LiveInterval interval = intervalMap.get(def);
                    interval.start = Math.min(interval.start, pos);
                }

                // 处理使用
                for (MIRVirtualReg reg : getUsedRegisters(inst)) {
                    LiveInterval interval = intervalMap.get(reg);
                    interval.end = Math.max(interval.end, pos);
                }
            }
        }

        // 第二遍：处理基本块边界
        for (MIRBasicBlock block : function.getBlocks()) {
            int firstPos = instructionPositions.get(block.getInstructions().get(0));
            int lastPos = instructionPositions.get(block.getInstructions().get(block.getInstructions().size() - 1));

            // 处理live-in
            for (MIRVirtualReg reg : liveInSets.get(block)) {
                LiveInterval interval = intervalMap.get(reg);
                interval.start = Math.min(interval.start, firstPos);
                interval.end = Math.max(interval.end, firstPos);
            }

            // 处理live-out
            for (MIRVirtualReg reg : liveOutSets.get(block)) {
                LiveInterval interval = intervalMap.get(reg);
                interval.end = Math.max(interval.end, lastPos);
            }
        }

        // 收集所有生存区间
        intervals.addAll(intervalMap.values());
    }


    // 获取函数中所有虚拟寄存器
    private Set<MIRVirtualReg> getAllVirtualRegisters() {
        Set<MIRVirtualReg> registers = new LinkedHashSet<>();

        // 添加指令中定义的寄存器
        for (MIRBasicBlock block : function.getBlocks()) {
            for (MIRInstruction inst : block.getInstructions()) {
                MIRVirtualReg def = getDefinedRegister(inst);
                if (def != null) {
                    registers.add(def);
                }
                registers.addAll(getUsedRegisters(inst));
            }
        }

        return registers;
    }

    // 获取指令定义的虚拟寄存器（def）
    private MIRVirtualReg getDefinedRegister(MIRInstruction inst) {
        if (inst instanceof MIRArithOp) {
            return ((MIRArithOp) inst).getResult();
        } else if (inst instanceof MIRMoveOp) {
            return ((MIRMoveOp) inst).getResult();
        } else if (inst instanceof MIRMemoryOp) {
            return ((MIRMemoryOp) inst).getResult();
        } else if (inst instanceof MIRConvertOp) {
            return ((MIRConvertOp) inst).getResult();
        } else if (inst instanceof MIRCmpOp) {
            return ((MIRCmpOp) inst).getResult();
        } else if (inst instanceof MIRLiOp) {
            return ((MIRLiOp) inst).getResult();
        } else if (inst instanceof MIRLuiOp) {
            return ((MIRLuiOp) inst).getResult();
        } else if (inst instanceof MIRLaOp) {
            return ((MIRLaOp) inst).getResult();
        } else if (inst instanceof MIRAllocOp) {
            return ((MIRAllocOp) inst).getResult();
        }
        //System.err.println("Unexpected instruction: " + inst.toString());
        return null;
    }

    // 获取指令使用的虚拟寄存器（use）
    private Set<MIRVirtualReg> getUsedRegisters(MIRInstruction inst) {
        Set<MIRVirtualReg> uses = new LinkedHashSet<>();

        if (inst instanceof MIRArithOp) {
            MIRArithOp arithOp = (MIRArithOp) inst;
            addIfVirtualReg(uses, arithOp.getOperands().get(0));
            addIfVirtualReg(uses, arithOp.getOperands().get(1));
        } else if (inst instanceof MIRMoveOp) {
            addIfVirtualReg(uses, ((MIRMoveOp) inst).getOperands().get(0));
        } else if (inst instanceof MIRMemoryOp) {
            MIRMemoryOp memOp = (MIRMemoryOp) inst;
            addIfVirtualReg(uses, ((MIRMemory)memOp.getOperands().get(0)).getBase());
            addIfVirtualReg(uses, ((MIRMemory)memOp.getOperands().get(0)).getOffset());
            if(memOp.getOperands().size() > 1) {
                // store
                addIfVirtualReg(uses, memOp.getOperands().get(1));
            }
        } else if (inst instanceof MIRConvertOp) {
            addIfVirtualReg(uses, ((MIRConvertOp) inst).getOperands().get(0));
        } else if (inst instanceof MIRCmpOp) {
            MIRCmpOp cmpOp = (MIRCmpOp) inst;
            addIfVirtualReg(uses, cmpOp.getOperands().get(0));
            addIfVirtualReg(uses, cmpOp.getOperands().get(1));
        } else if (inst instanceof MIRControlFlowOp) {
            MIRControlFlowOp cfOp = (MIRControlFlowOp) inst;
            for(MIROperand operand : cfOp.getOperands()) {
                addIfVirtualReg(uses, operand);
            }
        }

        return uses;
    }

    private void addIfVirtualReg(Set<MIRVirtualReg> set, MIROperand operand) {
        if (operand instanceof MIRVirtualReg) {
            set.add((MIRVirtualReg) operand);
        }
    }

    public PhysicalRegister getRegisterFor(MIRVirtualReg vreg) {
        LiveInterval interval = intervalMap.get(vreg);
        if (interval != null) {
            return registerAssignment.get(interval);
        }
        return null;
    }

    public Integer getSpillLocation(MIRVirtualReg vreg) {
        LiveInterval interval = intervalMap.get(vreg);
        if (interval != null) {
            return spillLocations.get(interval);
        }
        return null;
    }

    public int getFrameSize() {
        return Math.abs(stackOffset);
    }

    public Map<MIRVirtualReg, PhysicalRegister> getAllocationMap() {
        Map<MIRVirtualReg, PhysicalRegister> result = new LinkedHashMap<>();
        for (Map.Entry<LiveInterval, PhysicalRegister> entry : registerAssignment.entrySet()) {
            result.put(entry.getKey().vreg, entry.getValue());
        }
        return result;
    }

    public Map<MIRVirtualReg, Integer> getSpillMap() {
        Map<MIRVirtualReg, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<LiveInterval, Integer> entry : spillLocations.entrySet()) {
            result.put(entry.getKey().vreg, entry.getValue());
        }
        return result;
    }

    public Set<PhysicalRegister> getUsedCalleeSaved() {
        return usedCalleeSaved;
    }

    public Set<PhysicalRegister> getUsedCallerSaved() {
        return usedCallerSaved;
    }
    public PhysicalRegister getIntTempReg1() { return intTempReg1; }
    public PhysicalRegister getIntTempReg2() { return intTempReg2; }
    public PhysicalRegister getIntTempReg() {
        return tempIntRegs.get(tempIntRegIndex++ % tempIntRegs.size());
    }
    public PhysicalRegister getFloatTempReg1() { return floatTempReg1; }
    public PhysicalRegister getFloatTempReg2() { return floatTempReg2; }
    public PhysicalRegister getFloatTempReg() {
        return tempFloatRegs.get(tempFloatRegIndex++ % tempFloatRegs.size());
    }



    private static class LiveInterval {
        final MIRVirtualReg vreg;
        int start;
        int end;

        LiveInterval(MIRVirtualReg vreg, int start, int end) {
            this.vreg = vreg;
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LiveInterval that = (LiveInterval) o;
            return vreg.equals(that.vreg);
        }

        @Override
        public int hashCode() {
            return vreg.hashCode();
        }
    }
}