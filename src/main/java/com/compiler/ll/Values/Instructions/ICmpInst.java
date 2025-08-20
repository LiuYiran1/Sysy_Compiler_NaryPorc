package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class ICmpInst extends Instruction {
    private final IntPredicate predicate;

    public ICmpInst(Type resultType, String name, IntPredicate predicate, Value lhs, Value rhs, BasicBlock block) {
        super(resultType, name, Opcode.ICMP, block);
        this.predicate = predicate;
        addOperand(lhs);
        addOperand(rhs);
    }

    public IntPredicate getPredicate() {
        return predicate;
    }

    @Override
    public boolean isICmpInst(){
        return true;
    }

    @Override
    public Instruction clone() {
        return new ICmpInst(type, nameManager.getUniqueName(name), predicate, operands.get(0), operands.get(1), block);
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        Value op2 = operands.get(1);
        String op1Str = getOpStr(op1);
        String op2Str = getOpStr(op2);
        return "%" + name + " = icmp " + predicate.getIRName() + " "
                + operands.get(0).getType().toIR() + " "
                + op1Str + ", " + op2Str;
    }
}
