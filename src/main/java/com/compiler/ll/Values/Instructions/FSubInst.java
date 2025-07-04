package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class FSubInst extends Instruction {
    public FSubInst(Type type, String name, Value lhs, Value rhs) {
        super(type, name, Opcode.FSUB);
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        return "%" + name + " = fsub " + type.toIR() + " " + operands.get(0).getName()
                + ", " + operands.get(1).getName();
    }
}