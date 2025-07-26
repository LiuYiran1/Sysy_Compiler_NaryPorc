package com.compiler.mir;

import com.compiler.mir.instruction.MIRInstruction;
import com.compiler.mir.operand.MIRLabel;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MIRBasicBlock {
    private final MIRLabel label;
    private final List<MIRInstruction> instructions = new ArrayList<>();
    private final List<MIRBasicBlock> predecessors = new ArrayList<>();
    private final List<MIRBasicBlock> successors = new ArrayList<>();

    public MIRBasicBlock(String name) {
        this.label = new MIRLabel(name);
    }

    public MIRLabel getLabel() {
        return label;
    }

    public void addInstruction(MIRInstruction inst) {
        instructions.add(inst);
    }

    public void addPredecessor(MIRBasicBlock pred) {
        predecessors.add(pred);
    }

    public void addSuccessor(MIRBasicBlock succ) {
        successors.add(succ);
    }

    public List<MIRInstruction> getInstructions() {
        return instructions;
    }

    public List<MIRBasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<MIRBasicBlock> getSuccessors() {
        return successors;
    }

    // 获取所有使用的虚拟寄存器
    public Set<MIRVirtualReg> getUsedRegs() {
        Set<MIRVirtualReg> regs = new HashSet<>();
        for (MIRInstruction inst : instructions) {
            if (inst.getResult() != null) {
                regs.add(inst.getResult());
            }
            for (MIROperand op : inst.getOperands()) {
                if (op instanceof MIRVirtualReg) {
                    regs.add((MIRVirtualReg) op);
                }
            }
        }
        return regs;
    }
}