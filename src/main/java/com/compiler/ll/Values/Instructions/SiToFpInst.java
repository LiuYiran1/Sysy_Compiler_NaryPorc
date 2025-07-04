package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class SiToFpInst extends Instruction {
    public SiToFpInst(Value operand, Type destType, String name) {
        super(destType, name, Opcode.SITOFP);
        addOperand(operand);
    }

    @Override
    public String toIR() {
        return "%" + name + " = sitofp " + operands.get(0).getType().toIR() + " " + operands.get(0).getName()
                + " to " + type.toIR();
    }
}