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

    @Override
    public void run(Module module) {
        this.nameManager = module.getBuilder().getNameManager();
        for (Function function : module.getFunctionDefs()) {
            insertPhi(function);
        }
    }

    // 一、插入 phi
    public void insertPhi(Function function) {
        // Step 1: 找出所有局部 alloca（栈变量）
        List<AllocaInst> allocas = new ArrayList<>();
        for (Instruction inst : function.getEntryBlock().getInstructions()) {
            // 只考虑 i32 类型
            if (inst.getOpcode() == Opcode.ALLOCA && ((PointerType)inst.getType()).getPointeeType().isIntegerType()) {
                allocas.add((AllocaInst) inst);
            }
        }

        // Step 2: 构建 defBlocks: 每个变量在哪些块中 store 过
        Map<AllocaInst, Set<BasicBlock>> defBlocks = new HashMap<>();
        for (AllocaInst alloca : allocas) {
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
        for (AllocaInst alloca : allocas) {
            Set<BasicBlock> hasAlready = new HashSet<>(); // 已经插过 phi 的 block
            Queue<BasicBlock> workList = new LinkedList<>(defBlocks.get(alloca));

            while (!workList.isEmpty()) {
                BasicBlock def = workList.poll();
                for (BasicBlock df : function.getDomFrontier().get(def)) {
                    if (hasAlready.contains(df)) continue;
                    hasAlready.add(df);

                    // 插入 phi
                    String varName = nameManager.getUniqueName("Mem2RegPhi");
                    PhiInst phi = new PhiInst(alloca.getType(),varName, df);
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

    private void rename(Function function,
                        Map<AllocaInst, Deque<Value>> stacks,
                        Map<AllocaInst, List<PhiInst>> phiMap,
                        Map<BasicBlock, List<Instruction>> allocLoads,
                        BasicBlock bb,
                        Set<BasicBlock> visited) {

        visited.add(bb);

        // === 处理 phi（提前压栈） ===
        for (PhiInst phi : bb.getPhiInsts()) {
            AllocaInst var = phi.getVariable();
            stacks.computeIfAbsent(var, k -> new ArrayDeque<>()).push(phi);
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
                        //bb.replaceInstruction(load, val); // 用栈顶替换 load
                        iter.remove(); // 删除 load 指令
                    }
                }
            } else if (inst.getOpcode() == Opcode.STORE) {
                Value ptr = inst.getOperand(1);
                if (ptr instanceof Instruction && ((Instruction) ptr).getOpcode() == Opcode.ALLOCA && stacks.containsKey(ptr)) {
                    Value val = inst.getOperand(0);
                    stacks.get(ptr).push(val); // 新定义压栈
                    iter.remove(); // 删除 store 指令
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
                rename(function, stacks, phiMap, allocLoads, child, visited);
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

