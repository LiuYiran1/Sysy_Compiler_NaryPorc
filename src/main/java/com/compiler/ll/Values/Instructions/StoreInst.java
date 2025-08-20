package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;
import com.compiler.ll.exceptions.StoreException;

public class StoreInst extends Instruction {
    public StoreInst(Value value, Value ptr, BasicBlock block) {
        super(null, "", Opcode.STORE, block);
        addOperand(value);
        addOperand(ptr);
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        Value op2 = operands.get(1);
        String op1Str = getOpStr(op1);
        String op2Str = getOpStr(op2);

        return "store " + operands.get(0).getType().toIR() + " " + op1Str
                + ", " + operands.get(1).getType().toIR() + " " + op2Str;
    }

    @Override
    public Instruction clone() {
        return new StoreInst(operands.get(0), operands.get(1), block);
    }


}