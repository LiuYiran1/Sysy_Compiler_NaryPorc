package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class SDivInst extends Instruction {
    public SDivInst(Type type, String name, Value lhs, Value rhs) {
        super(type, name, Opcode.SDIV);
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        return "%" + name + " = sdiv " + type.toIR() + " " + operands.get(0).getName()
                + ", " + operands.get(1).getName();
    }
}