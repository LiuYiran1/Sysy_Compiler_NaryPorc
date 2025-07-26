package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class BitCastInst extends Instruction {
    private final Type fromType;
    private final Type toType;
    private final Value operand;

    public BitCastInst(Type fromType, Type toType, Value operand, String name, BasicBlock block) {
        super(toType, name, Opcode.BC, block);
        this.fromType = fromType;
        this.toType = toType;
        this.operand = operand;
        addOperand(operand);
    }

    @Override
    public String toIR() {
        Value op1 = operands.get(0);
        String op1Str = getOpStr(op1);
        return "%" + name + " = bitcast " + fromType.toIR() + " " +
                op1Str + " to " + toType.toIR();
    }
}
