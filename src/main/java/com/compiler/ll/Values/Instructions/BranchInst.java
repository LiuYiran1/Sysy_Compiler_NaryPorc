package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;

public class BranchInst extends TerminatorInst {
    private BasicBlock target;

    public BranchInst(BasicBlock target, BasicBlock block) {
        super(null, "", Opcode.BR, block);
        this.target = target;
    }

    public BasicBlock getTarget() {
        return target;
    }

    public void replaceTarget(BasicBlock target) {
        this.target = target;
    }

    @Override
    public String toIR() {
        return "br label %" + target.getName();
    }
}