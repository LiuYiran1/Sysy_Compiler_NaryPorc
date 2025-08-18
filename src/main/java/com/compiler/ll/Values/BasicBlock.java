package com.compiler.ll.Values;

import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BasicBlock extends User {
    Function func;
    private final List<Instruction> instructions = new ArrayList<>();

    private final List<PhiInst> phiInsts = new ArrayList<>();
    private final List<PhiInst> allPhiInsts = new ArrayList<>();

    private final List<BasicBlock> predecessors = new ArrayList<>();
    private final List<BasicBlock> successors = new ArrayList<>();

    public BasicBlock(String name, Function function) {
        super(null, name); // 没有类型
        func = function;
    }

    public BasicBlock getNextBasicBlock() {
        if (func == null) return null;
        List<BasicBlock> blocks = func.getBasicBlocks();
        int idx = blocks.indexOf(this);
        if (idx == -1 || idx + 1 >= blocks.size()) return null;
        return blocks.get(idx + 1);
    }

    public void insertPhi(PhiInst phiInst) {
        instructions.add(0, phiInst);
        phiInsts.add(phiInst);
        allPhiInsts.add(phiInst);
    }


    public void replaceAllUse(Instruction oldInst, Value newValue) {
        List<User> users = new ArrayList<>(oldInst.getUsers());
        for (User user : users) {
            user.replaceOperand(oldInst, newValue);
        }
    }


    public void addInstruction(Instruction inst) {
        instructions.add(inst);
    }

    public Instruction getFirstInstruction() {
        return instructions.get(0);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Instruction getTerminator() {
        if (instructions.isEmpty()) return null;
        Instruction last = instructions.get(instructions.size() - 1);
        return last.isTerminator() ? last : null;
    }

    public List<PhiInst> getPhiInsts() {
        return phiInsts;
    }

    public void addToAllPhiInsts(PhiInst phiInst) {
        allPhiInsts.add(phiInst);
    }

    public List<PhiInst> getAllPhiInsts() {
        return allPhiInsts;
    }

    public void delete() {
        if (func != null) {
            func.getBlocks().remove(this);
            func = null; // 解除引用
        }
        // 清空指令
        instructions.clear();
    }

    public void removeInstruction(Instruction inst){
        instructions.remove(inst);
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<BasicBlock> getSuccessors() {
        return successors;
    }

    public void addPredecessor(BasicBlock bb) {
        if (!predecessors.contains(bb)) {
            predecessors.add(bb);
        }
    }

    public void addSuccessor(BasicBlock bb) {
        if (!successors.contains(bb)) {
            successors.add(bb);
        }
    }

    public void setTerminator(Instruction inst) {
        instructions.set(instructions.size() - 1, inst);
    }

    public void removePredecessor(BasicBlock pred) {
        if (predecessors.remove(pred)) {
            // 同步更新自己内部的 PHI 节点
            for (PhiInst phi : new ArrayList<>(allPhiInsts)) {
                phi.removeIncomingFrom(pred);
            }

            // 对称删除 pred 的 successors
            pred.getSuccessors().remove(this);
        }
    }

    public void removeSuccessor(BasicBlock succ) {
        if (successors.remove(succ)) {
            // 同步删除 succ 的 predecessors
            succ.getPredecessors().remove(this);
            // 同时删掉 succ 的 PHI 参数
            for (PhiInst phi : succ.getAllPhiInsts()) {
                phi.removeIncomingFrom(this);
            }
        }
    }

    /**
     * 用 newPred 替换旧的前驱 oldPred
     */
    public void replacePredecessor(BasicBlock oldPred, BasicBlock newPred) {
        int idx = predecessors.indexOf(oldPred);
        if (idx == -1) return; // oldPred 不存在，直接返回

        // 1. 替换 predecessors 列表中的 oldPred
        predecessors.set(idx, newPred);

        // 2. 更新 oldPred 的 successors 列表
        oldPred.getSuccessors().remove(this);

        // 3. 确保 newPred 的 successors 包含当前块
        if (!newPred.getSuccessors().contains(this)) {
            newPred.getSuccessors().add(this);
        }

        // 4. 更新所有 PHI 节点，将 oldPred 对应的 incoming 替换为 newPred
        for (PhiInst phi : allPhiInsts) {
            phi.replaceIncomingBlock(oldPred, newPred);
        }
    }



    public void removeInst(Instruction inst) {
        instructions.remove(inst);
    }

    @Override
    public String toIR() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (Instruction inst : instructions) {
            sb.append("  ").append(inst.toIR()).append("\n");
        }
        return sb.toString();
    }
}
