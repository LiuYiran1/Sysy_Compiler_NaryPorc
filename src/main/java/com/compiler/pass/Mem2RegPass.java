package com.compiler.pass;

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
    NameManager nameManager;

    List<AllocaInst> rmAllocas = new ArrayList<>();
    List<StoreInst> rmStores = new ArrayList<>();
    List<LoadInst> rmLoads = new ArrayList<>();


    @Override
    public void run(Module module) {
        this.nameManager = module.getBuilder().getNameManager();
        for (Function func : module.getFunctionDefs()) {

            insertPhi(func);
            Map<AllocaInst, Deque<Value>> stacks = getVarStacks(func);
            Set<BasicBlock> visited = new HashSet<>();

            rename(func, stacks, func.getEntryBlock(), visited);

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

    // 一、插入 phi
    public void insertPhi(Function function) {
        // Step 1: 找出所有局部 alloca（栈变量）
        rmAllocas.clear();
        for (Instruction inst : function.getEntryBlock().getInstructions()) {
            // 只考虑 i32 类型
            if (inst.getOpcode() == Opcode.ALLOCA && ((PointerType)inst.getType()).getPointeeType().isIntegerType()) {
                rmAllocas.add((AllocaInst) inst);
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

        // Step 3: 为每个变量插入 phi 函数
        for (AllocaInst alloca : rmAllocas) {
            Set<BasicBlock> hasAlready = new HashSet<>(); // 已经插过 phi 的 block
            Queue<BasicBlock> workList = new LinkedList<>(defBlocks.get(alloca));

            while (!workList.isEmpty()) {
                BasicBlock def = workList.poll();
                for (BasicBlock df : function.getDomFrontier().get(def)) {
                    if (hasAlready.contains(df)) continue;
                    hasAlready.add(df);

                    // 插入 phi
                    String varName = nameManager.getUniqueName("Mem2RegPhi");
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

    private Map<AllocaInst, Deque<Value>> getVarStacks(Function func) {
        Map<AllocaInst, Deque<Value>> stacks = new HashMap<>();


        for (AllocaInst alloca : rmAllocas) {
            Deque<Value> stack = new ArrayDeque<>();
            // 如果变量没有phi，就用 undef 作为初始值，或用alloca自己作为初始值
            //stack.push(getInitialValueFor(alloca));
            stacks.put(alloca, stack);
        }

        // 对有phi的变量
        List<BasicBlock> blocks = func.getBasicBlocks();
        for (BasicBlock bb : blocks) {
            for (PhiInst phi : bb.getPhiInsts()) {
                AllocaInst var = phi.getVariable();
                stacks.computeIfAbsent(var, k -> new ArrayDeque<>()).push(phi);
            }
        }
        return stacks;
    }

    private void rename(Function function,
                        Map<AllocaInst, Deque<Value>> stacks,
                        BasicBlock bb,
                        Set<BasicBlock> visited) {

        visited.add(bb);

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
                        bb.replaceButNotDeleteInstruction(load, val); // 用栈顶替换 load

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

        // === 递归处理子节点 ===
        for (BasicBlock succ : bb.getSuccessors()) {
            for (PhiInst phi : succ.getPhiInsts()) {
                AllocaInst var = phi.getVariable();
                if (stacks.containsKey(var)) {
                    Value val = stacks.get(var).peek();
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


}

