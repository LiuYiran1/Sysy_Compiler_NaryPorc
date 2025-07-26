package com.compiler.pass;

import com.compiler.ll.Context;
import com.compiler.ll.Module;
import com.compiler.ll.Types.PointerType;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Instructions.*;
import com.compiler.ll.Values.Value;
import com.compiler.ll.utils.NameManager;

import java.util.*;

public class Mem2RegPass implements Pass {
    Context context;
    NameManager nameManager;

    List<AllocaInst> rmAllocas = new ArrayList<>();
    List<StoreInst> rmStores = new ArrayList<>();
    List<LoadInst> rmLoads = new ArrayList<>();

    public Mem2RegPass(Context context) {
        this.context = context;
    }


    @Override
    public void run(Module module) {
        this.nameManager = module.getBuilder().getNameManager();
        for (Function func : module.getFunctionDefs()) {

            insertPhi(func);

            rename(func, stacks, func.getEntryBlock(), new HashSet<>());

            delete(func);
        }
    }

    // 一、插入 phi
    public void insertPhi(Function function) {
        // Step 1: 找出所有局部 alloca（栈变量）
        rmAllocas.clear();
        for(BasicBlock bb = function.getFirstBasicBlock(); bb != null; bb = bb.getNextBasicBlock()){
            for (Instruction inst : bb.getInstructions()) {
                // 只考虑 i32 和 f32 类型
                if (inst.getOpcode() == Opcode.ALLOCA && (((PointerType)inst.getType()).getPointeeType().isIntegerType() || ((PointerType)inst.getType()).getPointeeType().isFloatType())) {
                    rmAllocas.add((AllocaInst) inst);
                }
            }
        }

        // Step 2: 构建 defBlocks: 每个变量在哪些块中 store 过
        Map<AllocaInst, Set<BasicBlock>> defBlocks = new HashMap<>();
        for (AllocaInst alloca : rmAllocas) {
            defBlocks.put(alloca, new HashSet<>());
        }

        for (BasicBlock bb : function.getBasicBlocks()) {
            for (Instruction inst : bb.getInstructions()) {
                if (inst.getOpcode() == Opcode.STORE) {
                    Value ptr = inst.getOperand(1);
                    if (ptr instanceof Instruction && ((Instruction) ptr).getOpcode() == Opcode.ALLOCA && defBlocks.containsKey(ptr)) {
                        defBlocks.get(ptr).add(bb);
                    }
                }
            }
        }

        // Step 3: 构建 defBlocks: 每个变量在哪些块中 load 过
        Map<AllocaInst, Set<BasicBlock>> useBlocks = new HashMap<>();
        for (AllocaInst alloca : rmAllocas) {
            useBlocks.put(alloca, new HashSet<>());
        }
        for (BasicBlock bb : function.getBasicBlocks()) {
            for (Instruction inst : bb.getInstructions()) {
                if (inst.getOpcode() == Opcode.LOAD) {
                    Value ptr = inst.getOperand(0);
                    if (ptr instanceof Instruction && ((Instruction) ptr).getOpcode() == Opcode.ALLOCA && useBlocks.containsKey(ptr)) {
                        useBlocks.get(ptr).add(bb);
                    }
                }
            }
        }


        // Step 4: 为每个变量插入 phi 函数
        for (AllocaInst alloca : rmAllocas) {
            Set<BasicBlock> hasAlready = new HashSet<>(); // 已经插过 phi 的 block
            Queue<BasicBlock> workList = new LinkedList<>(defBlocks.get(alloca));

            Set<BasicBlock> loadBlocks = useBlocks.get(alloca);

            while (!workList.isEmpty()) {
                BasicBlock def = workList.poll(); // 定义变量块
                for (BasicBlock df : function.getDomFrontier().get(def)) {
                    if (hasAlready.contains(df)) continue;
                    hasAlready.add(df);

                    //if (!isLiveIn(df, loadBlocks, function)) continue; 这不对

                    // 插入 phi
                    String varName = nameManager.getUniqueName("Mem2RegPhi_" + alloca.getName());
                    PhiInst phi = new PhiInst(((PointerType)alloca.getType()).getPointeeType(),varName, df);
                    df.insertPhi(phi);
                    phi.setVariable(alloca); // 记录这个 phi 是哪个变量的

                    // 如果这个 df 块中没有定义过，则也加到 worklist
                    if (!defBlocks.get(alloca).contains(df)) {
                        workList.add(df);
                    }
                }
            }
        }
    }

    private boolean isLiveIn(BasicBlock block, Set<BasicBlock> useBlocks, Function function) {
        for (BasicBlock use : useBlocks) {
            BasicBlock runner = use;
            while (runner != null) {
                if (runner == block) return true;
                runner = function.getIdom(runner);
            }
        }
        return false;
    }


    Map<AllocaInst, Deque<Value>> stacks = new HashMap<>(); // 到达定值栈
    private void rename(Function function,
                        Map<AllocaInst, Deque<Value>> stacks,
                        BasicBlock bb,
                        Set<BasicBlock> visited) {

        visited.add(bb);

        for (PhiInst phi : bb.getPhiInsts()) {
            AllocaInst var = phi.getVariable();
            stacks.computeIfAbsent(var, k -> new ArrayDeque<>()).push(phi);
        }
        for (AllocaInst alloca : rmAllocas) {
            if (!stacks.containsKey(alloca)) {
                stacks.put(alloca, new ArrayDeque<>());
            }
        }

        List<Instruction> instrs = bb.getInstructions();
        ListIterator<Instruction> iter = instrs.listIterator();

        // === 重命名 load/store，替换/压栈 ===
        while (iter.hasNext()) {
            Instruction inst = iter.next();
            if (inst.getOpcode() == Opcode.LOAD) {
                LoadInst load = (LoadInst) inst;
                Value ptr = load.getOperand(0);
                if (ptr instanceof Instruction && ((Instruction) ptr).getOpcode() == Opcode.ALLOCA && stacks.containsKey(ptr)) {
                    Value val = stacks.get(ptr).peek();
                    if (val != null) {
                        bb.replaceAllUse(load, val); // 用栈顶替换 load
                        rmLoads.add(load);
                    }
                }
            } else if (inst.getOpcode() == Opcode.STORE) {
                StoreInst store = (StoreInst) inst;
                Value ptr = store.getOperand(1);
                if (ptr instanceof Instruction && ((Instruction) ptr).getOpcode() == Opcode.ALLOCA && stacks.containsKey(ptr)) {
                    Value val = store.getOperand(0);
                    stacks.get(ptr).push(val); // 新定义压栈

                    rmStores.add(store);
                }
            }
        }

        // 为后继填入phi
        for (BasicBlock succ : bb.getSuccessors()) {
            for (PhiInst phi : succ.getPhiInsts()) {
                AllocaInst var = phi.getVariable();
                if (stacks.containsKey(var)) {
                    Value val = stacks.get(var).peek();
                    if (val == null){
                        // phi.addIncoming(bb, AllocaInst.getUndef());
                        phi.addIncoming(bb, context.getInt32Type().getConstantInt(0));
                        continue;
                    }

                    phi.addIncoming(bb, val); // phi 填补来源
                }
            }
        }

        // 递归子块（按支配树）
        for (BasicBlock child : function.getDomTree().getOrDefault(bb, List.of())) {
            if (!visited.contains(child)) {
                rename(function, stacks, child, visited);
            }
        }

        // === 弹出本 block 定义的变量 ===
        for (Instruction inst : instrs) {
            if (inst.getOpcode() == Opcode.STORE) {
                Value ptr = inst.getOperand(1);
                if (ptr instanceof Instruction && ((Instruction) ptr).getOpcode() == Opcode.ALLOCA && stacks.containsKey(ptr)) {
                    stacks.get(ptr).pop();
                }
            }
        }
        for (PhiInst phi : bb.getPhiInsts()) {
            AllocaInst var = phi.getVariable();
            stacks.get(var).pop();
        }
    }

    private void delete(Function func) {
        // 最后移除
        for (AllocaInst alloc : rmAllocas) {
            func.removeInstruction(alloc);
        }
        for (StoreInst store : rmStores) {
            func.removeInstruction(store);
        }
        for (LoadInst load : rmLoads) {
            func.removeInstruction(load);
        }
    }


}

