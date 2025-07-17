package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class FpToSiInst extends Instruction {
    public FpToSiInst(Value operand, Type destType, String name) {
        super(destType, name, Opcode.FPTOSI);
        addOperand(operand);
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        String op1Str = getOpStr(op1);
        return "%" + name + " = fptosi " + operands.get(0).getType().toIR() + " " + op1Str
                + " to " + type.toIR();
    }
}