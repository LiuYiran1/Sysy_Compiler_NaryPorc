package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class FCmpInst extends Instruction {
    private final FloatPredicate predicate;

    public FCmpInst(Type resultType, String name, FloatPredicate predicate, Value lhs, Value rhs) {
        super(resultType, name, Opcode.FCMP);
        this.predicate = predicate;
        addOperand(lhs);
        addOperand(rhs);
    }

    public FloatPredicate getPredicate() {
        return predicate;
    }

    @Override
    public String toIR() {
        return "%" + name + " = fcmp " + predicate.getIRName() + " "
                + operands.get(0).getType().toIR() + " "
                + operands.get(0).getName() + ", " + operands.get(1).getName();
    }
}
