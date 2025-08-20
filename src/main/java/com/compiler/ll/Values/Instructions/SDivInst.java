package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class SDivInst extends Instruction {
    public SDivInst(Type type, String name, Value lhs, Value rhs, BasicBlock block) {
        super(type, name, Opcode.SDIV, block);
        addOperand(lhs);
        addOperand(rhs);
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        Value op2 = operands.get(1);
        String op1Str = getOpStr(op1);
        String op2Str = getOpStr(op2);
        return "%" + name + " = sdiv " + type.toIR() + " " + op1Str
                + ", " + op2Str;
    }

    @Override
    public Instruction clone() {
        return new SDivInst(type, nameManager.getUniqueName(name), operands.get(0), operands.get(1), block);
    }
}