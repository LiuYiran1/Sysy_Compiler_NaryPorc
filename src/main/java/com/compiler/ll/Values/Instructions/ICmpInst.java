package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class ICmpInst extends Instruction {
    private final IntPredicate predicate;

    public ICmpInst(Type resultType, String name, IntPredicate predicate, Value lhs, Value rhs) {
        super(resultType, name, Opcode.ICMP);
        this.predicate = predicate;
        addOperand(lhs);
        addOperand(rhs);
    }

    public IntPredicate getPredicate() {
        return predicate;
    }

    @Override
    public String toIR() {
        return "%" + name + " = icmp " + predicate.getIRName() + " "
                + operands.get(0).getType().toIR() + " "
                + operands.get(0).getName() + ", " + operands.get(1).getName();
    }
}
