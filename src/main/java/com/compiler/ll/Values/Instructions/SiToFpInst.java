package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class SiToFpInst extends Instruction {
    public SiToFpInst(Value operand, Type destType, String name, BasicBlock block) {
        super(destType, name, Opcode.SITOFP, block);
        addOperand(operand);
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        String op1Str = getOpStr(op1);
        return "%" + name + " = sitofp " + operands.get(0).getType().toIR() + " " + op1Str
                + " to " + type.toIR();
    }
}