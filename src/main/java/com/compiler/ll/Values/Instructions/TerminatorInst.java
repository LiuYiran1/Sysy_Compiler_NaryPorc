package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;

public abstract class TerminatorInst extends Instruction {

    public TerminatorInst(Type type, String name, Opcode opcode, BasicBlock block) {
        super(type, name, opcode, block);
    }

    @Override
    public boolean isTerminator() {
        return true;
    }
}
