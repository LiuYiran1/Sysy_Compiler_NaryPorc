package com.compiler.ll.Values;

import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.BranchInst;
import com.compiler.ll.Values.Instructions.CondBranchInst;
import com.compiler.ll.Values.Instructions.Opcode;
import com.compiler.ll.Values.Instructions.PhiInst;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BasicBlock extends User {
    Function func;
    private final List<Instruction> instructions = new ArrayList<>();

    private final List<PhiInst> phiInsts = new ArrayList<>();

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
    }


    public void replaceButNotDeleteInstruction(Instruction oldInst, Value newValue) {
        for (User user : oldInst.getUsers()) {
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

    public void removeInst(Instruction inst) {
        // 这里要注意删除时还要消除分支指令生成时的控制流图的边
        if (inst.getOpcode() == Opcode.BR){
            BranchInst branchInst = (BranchInst) inst;
            BasicBlock target = branchInst.getTarget();
            // 删除控制流边
            this.successors.remove(target);
            target.getPredecessors().remove(this);
            instructions.remove(inst);

        } else if (inst.getOpcode() == Opcode.CBR){
            CondBranchInst condBranchInst = (CondBranchInst) inst;
            BasicBlock trueBlock = condBranchInst.getTrueBlock();
            BasicBlock falseBlock = condBranchInst.getFalseBlock();
            // 删除控制流边
            this.successors.remove(trueBlock);
            this.successors.remove(falseBlock);
            trueBlock.getPredecessors().remove(this);
            falseBlock.getPredecessors().remove(this);
            instructions.remove(inst);
        } else {
            instructions.remove(inst);
        }
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
