package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class CondBranchInst extends Instruction {
    private final String trueLabel;
    private final String falseLabel;

    public CondBranchInst(Value condition, String trueLabel, String falseLabel) {
        super(null, "", Opcode.CBR);
        addOperand(condition);
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
    }

    @Override
    public String toIR() {
        return "br i1 " + operands.get(0).getName() + ", label %" + trueLabel + ", label %" + falseLabel;
    }
}