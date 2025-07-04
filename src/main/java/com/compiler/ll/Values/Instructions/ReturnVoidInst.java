package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Types.VoidType;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class ReturnVoidInst extends Instruction {
    public ReturnVoidInst(VoidType voidType) {
        super(voidType, "", Opcode.RET);
        addOperand(null);
    }

    @Override
    public String toIR() {
        return "ret void";
    }
}
