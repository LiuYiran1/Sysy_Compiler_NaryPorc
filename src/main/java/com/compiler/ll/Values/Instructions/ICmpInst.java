package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class ICmpInst extends Instruction {
    private final String cond;

    public ICmpInst(String cond, Value lhs, Value rhs, String name) {
        super(lhs.getType(), name, Opcode.ICMP);
        this.cond = cond;
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        return "%" + name + " = icmp " + cond + " " + operands.get(0).getType().toIR()
                + " " + operands.get(0).getName() + ", " + operands.get(1).getName();
    }
}