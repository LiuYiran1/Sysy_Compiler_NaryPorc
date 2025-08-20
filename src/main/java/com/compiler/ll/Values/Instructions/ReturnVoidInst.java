package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Types.VoidType;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class ReturnVoidInst extends TerminatorInst {
    public ReturnVoidInst(VoidType voidType, BasicBlock block) {
        super(voidType, "", Opcode.RET, block);
    }

    @Override
    public String toIR() {
        return "ret void";
    }

    @Override
    public Instruction clone() {
        throw new RuntimeException("Not implemented");
    }
}
