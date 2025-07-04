package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class FAddInst extends Instruction {
    public FAddInst(Type type, String name, Value lhs, Value rhs) {
        super(type, name, Opcode.FADD);
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        return "%" + name + " = fadd " + type.toIR() + " " + operands.get(0).getName()
                + ", " + operands.get(1).getName();
    }
}