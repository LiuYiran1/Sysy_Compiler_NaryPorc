package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;

public class BranchInst extends Instruction {
    private final BasicBlock target;

    public BranchInst(BasicBlock target) {
        super(null, "", Opcode.BR);
        this.target = target;
    }

    @Override
    public String toIR() {
        return "br label %" + target.getName();
    }
}