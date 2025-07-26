package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class CondBranchInst extends TerminatorInst {
    private final BasicBlock trueBlock;
    private final BasicBlock falseBlock;

    public CondBranchInst(Value condition, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock block) {
        super(null, "", Opcode.CBR, block);
        addOperand(condition);
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    @Override
    public String toIR() {
        return "br i1 " + getOpStr(operands.get(0)) + ", label %" + trueBlock.getName() + ", label %" + falseBlock.getName();
    }
}