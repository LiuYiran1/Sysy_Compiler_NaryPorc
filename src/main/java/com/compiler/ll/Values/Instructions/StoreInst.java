package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class StoreInst extends Instruction {
    public StoreInst(Value value, Value ptr) {
        super(null, "", Opcode.STORE);
        addOperand(value);
        addOperand(ptr);
    }

    @Override
    public String toIR() {
        return "store " + operands.get(0).getType().toIR() + " " + operands.get(0).getName()
                + ", " + operands.get(1).getType().toIR() + "* " + operands.get(1).getName();
    }
}