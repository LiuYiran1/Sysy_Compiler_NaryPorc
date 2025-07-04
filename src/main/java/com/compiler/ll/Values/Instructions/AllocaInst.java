package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;

public class AllocaInst extends Instruction {
    public AllocaInst(Type pointeeType, String name) {
        super(pointeeType, name, Opcode.ALLOCA);
    }

    @Override
    public String toIR() {
        return name + " = alloca " + type.toIR();
    }
}