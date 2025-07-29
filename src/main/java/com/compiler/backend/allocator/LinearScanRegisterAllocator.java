//package com.compiler.backend.allocator;
//
//import com.compiler.backend.PhysicalRegister;
//import com.compiler.mir.MIRBasicBlock;
//import com.compiler.mir.MIRFunction;
//import com.compiler.mir.instruction.MIRControlFlowOp;
//import com.compiler.mir.instruction.MIRInstruction;
//import com.compiler.mir.operand.MIROperand;
//import com.compiler.mir.operand.MIRVirtualReg;
//
//import java.util.*;
//
//public class LinearScanRegisterAllocator {
//
//    private final List<LiveInterval> intervals = new ArrayList<>();
//    private final Map<LiveInterval, PhysicalRegister> registerAssignment = new LinkedHashMap<>();
//    private final Map<LiveInterval, Integer> spillLocations = new LinkedHashMap<>();
//    private final TreeSet<LiveInterval> active = new TreeSet<>(Comparator.comparingInt(i -> i.end));
//    private final List<PhysicalRegister> availableRegisters = new ArrayList<>();
//    private int stackOffset = 0;
//    private final MIRFunction function;
//    private final Map<MIRBasicBlock, Set<LiveInterval>> liveInSets = new HashMap<>();
//    private final Map<MIRBasicBlock, Set<LiveInterval>> liveOutSets = new HashMap<>();
//    private int instructionCounter = 0;
//
//    public LinearScanRegisterAllocator(MIRFunction function) {
//        this.function = function;
//        initializeAvailableRegisters();
//        buildLiveIntervals();
//    }
//
//    private void initializeAvailableRegisters() {
//        // 添加所有可用的物理寄存器
//        availableRegisters.addAll(PhysicalRegister.CALLER_SAVED_INT);
//        availableRegisters.addAll(PhysicalRegister.CALLEE_SAVED_INT);
//    }
//
//    public void allocate() {
//        // 按起始点排序生存区间
//        intervals.sort(Comparator.comparingInt(i -> i.start));
//
//        // 主分配循环
//        for (LiveInterval current : intervals) {
//            expireOldIntervals(current.start);
//
//            if (active.size() == availableRegisters.size()) {
//                spillAtInterval(current);
//            } else {
//                // 分配物理寄存器
//                PhysicalRegister reg = availableRegisters.remove(0);
//                registerAssignment.put(current, reg);
//
//                // 添加到活跃集合（按结束点排序）
//                active.add(current);
//            }
//        }
//    }
//
//    private void expireOldIntervals(int currentPoint) {
//        Iterator<LiveInterval> it = active.iterator();
//        while (it.hasNext()) {
//            LiveInterval interval = it.next();
//            if (interval.end < currentPoint) {
//                // 释放寄存器
//                availableRegisters.add(registerAssignment.get(interval));
//                it.remove();
//            } else {
//                // 活跃区间按结束点排序，后续区间不会过期
//                break;
//            }
//        }
//    }
//
//    private void spillAtInterval(LiveInterval current) {
//        // 找到结束点最大的活跃区间
//        LiveInterval spill = active.last();
//
//        if (spill.end > current.end) {
//            // 溢出spill区间
//            spillLocations.put(spill, allocateSpillSlot());
//
//            // 将spill的寄存器分配给当前区间
//            PhysicalRegister reg = registerAssignment.get(spill);
//            registerAssignment.put(current, reg);
//            registerAssignment.remove(spill);
//
//            // 从活跃集合中移除spill，添加当前区间
//            active.remove(spill);
//            active.add(current);
//        } else {
//            // 溢出当前区间
//            spillLocations.put(current, allocateSpillSlot());
//        }
//    }
//
//    private int allocateSpillSlot() {
//        stackOffset -= 4; // 每个溢出槽4字节
//        return stackOffset;
//    }
//
//    private void buildLiveIntervals() {
//        // 第一步：构建控制流图并计算生存变量
//        buildControlFlowGraph();
//        computeLiveness();
//
//        // 第二步：为每个虚拟寄存器创建生存区间
//        Map<MIRVirtualReg, LiveInterval> intervalMap = new HashMap<>();
//
//        // 遍历所有基本块和指令
//        for (MIRBasicBlock block : function.getBlocks()) {
//            Set<LiveInterval> live = new HashSet<>(liveOutSets.get(block));
//
//            // 反向遍历指令
//            List<MIRInstruction> instructions = block.getInstructions();
//            for (int i = instructions.size() - 1; i >= 0; i--) {
//                MIRInstruction inst = instructions.get(i);
//                int position = instructionCounter + i;
//
//                // 处理定义
//                if (inst.getDef() != null) {
//                    MIRVirtualReg def = inst.getDef();
//                    LiveInterval interval = intervalMap.computeIfAbsent(
//                            def, k -> new LiveInterval(def, Integer.MAX_VALUE, Integer.MIN_VALUE)
//                    );
//
//                    // 更新起始点
//                    interval.start = Math.min(interval.start, position);
//
//                    // 从生存集合中移除
//                    live.remove(interval);
//                }
//
//                // 处理使用
//                for (MIROperand use : inst.getUses()) {
//                    if (use instanceof MIRVirtualReg) {
//                        MIRVirtualReg reg = (MIRVirtualReg) use;
//                        LiveInterval interval = intervalMap.computeIfAbsent(
//                                reg, k -> new LiveInterval(reg, Integer.MAX_VALUE, Integer.MIN_VALUE)
//                        );
//
//                        // 更新结束点
//                        interval.end = Math.max(interval.end, position);
//
//                        // 添加到生存集合
//                        live.add(interval);
//                    }
//                }
//            }
//
//            instructionCounter += instructions.size();
//        }
//
//        // 收集所有生存区间
//        intervals.addAll(intervalMap.values());
//    }
//
//    private void buildControlFlowGraph() {
//        // 初始化CFG
//        for (MIRBasicBlock block : function.getBlocks()) {
//            liveInSets.put(block, new HashSet<>());
//            liveOutSets.put(block, new HashSet<>());
//        }
//
//        // 构建基本块之间的边
//        Map<MIRBasicBlock, Set<MIRBasicBlock>> successors = new HashMap<>();
//        for (MIRBasicBlock block : function.getBlocks()) {
//            Set<MIRBasicBlock> blockSuccessors = new HashSet<>();
//
//            // 查找跳转目标
//            for (MIRInstruction inst : block.getInstructions()) {
//                if (inst instanceof MIRControlFlowOp) {
//                    MIRControlFlowOp cfOp = (MIRControlFlowOp) inst;
//                    if (cfOp.getTarget() != null) {
//                        blockSuccessors.add(findBlockByName(cfOp.getTarget().getName()));
//                    }
//                    if (cfOp.getFalseTarget() != null) {
//                        blockSuccessors.add(findBlockByName(cfOp.getFalseTarget().getName()));
//                    }
//                }
//            }
//
//            successors.put(block, blockSuccessors);
//        }
//    }
//
//    private void computeLiveness() {
//        boolean changed;
//        do {
//            changed = false;
//            for (MIRBasicBlock block : function.getBlocks()) {
//                Set<LiveInterval> liveOut = liveOutSets.get(block);
//                Set<LiveInterval> liveIn = liveInSets.get(block);
//
//                // liveIn = use[B] ∪ (liveOut[B] - def[B])
//                Set<LiveInterval> newLiveIn = new HashSet<>(computeUses(block));
//                Set<LiveInterval> liveOutMinusDef = new HashSet<>(liveOut);
//                liveOutMinusDef.removeAll(computeDefs(block));
//                newLiveIn.addAll(liveOutMinusDef);
//
//                // liveOut[B] = ∪ liveIn[S] for S in successors(B)
//                Set<LiveInterval> newLiveOut = new HashSet<>();
//                for (MIRBasicBlock succ : getSuccessors(block)) {
//                    newLiveOut.addAll(liveInSets.get(succ));
//                }
//
//                if (!newLiveIn.equals(liveIn) || !newLiveOut.equals(liveOut)) {
//                    liveInSets.put(block, newLiveIn);
//                    liveOutSets.put(block, newLiveOut);
//                    changed = true;
//                }
//            }
//        } while (changed);
//    }
//
//    private Set<LiveInterval> computeUses(MIRBasicBlock block) {
//        Set<LiveInterval> uses = new HashSet<>();
//        for (MIRInstruction inst : block.getInstructions()) {
//            for (MIROperand operand : inst.getUses()) {
//                if (operand instanceof MIRVirtualReg) {
//                    // 这里简化处理，实际需要创建或获取LiveInterval
//                    uses.add(new LiveInterval((MIRVirtualReg) operand, 0, 0));
//                }
//            }
//        }
//        return uses;
//    }
//
//    private Set<LiveInterval> computeDefs(MIRBasicBlock block) {
//        Set<LiveInterval> defs = new HashSet<>();
//        for (MIRInstruction inst : block.getInstructions()) {
//            if (inst.getDef() != null) {
//                defs.add(new LiveInterval(inst.getDef(), 0, 0));
//            }
//        }
//        return defs;
//    }
//
//    private Set<MIRBasicBlock> getSuccessors(MIRBasicBlock block) {
//        // 实际实现中需要根据控制流指令确定后继块
//        return new HashSet<>();
//    }
//
//    private MIRBasicBlock findBlockByName(String name) {
//        return function.getBlocks().stream()
//                .filter(b -> b.getLabel().toString().equals(name))
//                .findFirst()
//                .orElse(null);
//    }
//
//    public PhysicalRegister getRegisterFor(MIRVirtualReg vreg) {
//        return intervals.stream()
//                .filter(i -> i.vreg.equals(vreg))
//                .findFirst()
//                .map(registerAssignment::get)
//                .orElse(null);
//    }
//
//    public Integer getSpillLocation(MIRVirtualReg vreg) {
//        return intervals.stream()
//                .filter(i -> i.vreg.equals(vreg))
//                .findFirst()
//                .map(spillLocations::get)
//                .orElse(null);
//    }
//
//    public int getFrameSize() {
//        return Math.abs(stackOffset);
//    }
//
//    private static class LiveInterval {
//        final MIRVirtualReg vreg;
//        int start;
//        int end;
//
//        LiveInterval(MIRVirtualReg vreg, int start, int end) {
//            this.vreg = vreg;
//            this.start = start;
//            this.end = end;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            LiveInterval that = (LiveInterval) o;
//            return vreg.equals(that.vreg);
//        }
//
//        @Override
//        public int hashCode() {
//            return vreg.hashCode();
//        }
//    }
//}
