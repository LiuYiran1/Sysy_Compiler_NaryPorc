package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class FCmpInst extends Instruction {
    private final FloatPredicate predicate;

    public FCmpInst(Type resultType, String name, FloatPredicate predicate, Value lhs, Value rhs, BasicBlock block) {
        super(resultType, name, Opcode.FCMP, block);
        this.predicate = predicate;
        addOperand(lhs);
        addOperand(rhs);
    }

    public FloatPredicate getPredicate() {
        return predicate;
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        Value op2 = operands.get(1);
        String op1Str = getOpStr(op1);
        String op2Str = getOpStr(op2);
        return "%" + name + " = fcmp " + predicate.getIRName() + " "
                + operands.get(0).getType().toIR() + " "
                + op1Str + ", " + op2Str;
    }
}
