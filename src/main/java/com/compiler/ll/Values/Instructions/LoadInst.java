package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class LoadInst extends Instruction {
    public LoadInst(Type type, String name, Value pointer, BasicBlock block) {
        super(type, name, Opcode.LOAD, block);
        addOperand(pointer);
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        String op1Str = getOpStr(op1);
        return "%" + name + " = load " + type.toIR() + ", " + operands.get(0).getType().toIR()
                + " " + op1Str;
    }

    @Override
    public Instruction clone() {
        return new LoadInst(type, nameManager.getUniqueName(name), operands.get(0), block);
    }
}