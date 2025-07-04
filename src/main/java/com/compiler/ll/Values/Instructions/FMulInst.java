package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class FMulInst extends Instruction {
    public FMulInst(Type type, String name, Value lhs, Value rhs) {
        super(type, name, Opcode.FMUL);
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        return "%" + name + " = fmul " + type.toIR() + " " + operands.get(0).getName()
                + ", " + operands.get(1).getName();
    }
}