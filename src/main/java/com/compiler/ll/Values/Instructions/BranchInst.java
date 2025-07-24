package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;

public class BranchInst extends TerminatorInst {
    private final BasicBlock target;

    public BranchInst(BasicBlock target, BasicBlock block) {
        super(null, "", Opcode.BR, block);
        this.target = target;
    }

    public BasicBlock getTarget() {
        return target;
    }

    @Override
    public String toIR() {
        return "br label %" + target.getName();
    }
}