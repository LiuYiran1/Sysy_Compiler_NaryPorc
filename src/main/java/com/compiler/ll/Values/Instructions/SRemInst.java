package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class SRemInst extends Instruction {
    public SRemInst(Type type, String name, Value lhs, Value rhs) {
        super(type, name, Opcode.SREM);
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        return "%" + name + " = srem " + type.toIR() + " " + operands.get(0).getName()
                + ", " + operands.get(1).getName();
    }
}