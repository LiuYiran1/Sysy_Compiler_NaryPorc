package com.compiler.ll.Values;

import com.compiler.ll.Values.GlobalValues.Function;
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
