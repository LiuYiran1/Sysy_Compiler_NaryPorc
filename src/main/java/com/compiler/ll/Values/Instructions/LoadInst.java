package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class LoadInst extends Instruction {
    public LoadInst(Type type, String name, Value pointer) {
        super(type, name, Opcode.LOAD);
        addOperand(pointer);
    }

    @Override
    public String toIR() {
        return "%" + name + " = load " + type.toIR() + ", " + operands.get(0).getType().toIR()
                + " " + operands.get(0).getName();
    }
}