package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class FCmpInst extends Instruction {
    private final String cond;

    public FCmpInst(String cond, Value lhs, Value rhs, String name) {
        super(lhs.getType(), name, Opcode.FCMP);
        this.cond = cond;
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        return "%" + name + " = fcmp " + cond + " " + operands.get(0).getType().toIR()
                + " " + operands.get(0).getName() + ", " + operands.get(1).getName();
    }
}