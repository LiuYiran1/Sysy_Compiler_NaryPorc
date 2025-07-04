package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Values.Instruction;

public class BranchInst extends Instruction {
    private final String targetLabel;

    public BranchInst(String targetLabel) {
        super(null, "", Opcode.BR);
        this.targetLabel = targetLabel;
    }

    @Override
    public String toIR() {
        return "br label %" + targetLabel;
    }
}