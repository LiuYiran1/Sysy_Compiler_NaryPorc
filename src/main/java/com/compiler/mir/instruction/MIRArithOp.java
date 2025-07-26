package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRArithOp extends MIRInstruction {

    public enum Type { INT, FLOAT }
    public enum Op { ADD, SUB, MUL, DIV, REM, XOR }

    private final Type type;
    private final Op op;
    private final MIROperand left;
    private final MIROperand right;

    public MIRArithOp(Op op, MIRVirtualReg result, Type type, MIROperand left, MIROperand right) {
        super(result);
        this.op = op;
        this.type = type;
        this.left = left;
        this.right = right;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(left, right);
    }

    @Override
    public String toString() {
        return type.name() + op.name() + " " + result + ", " + left + ", " + right;
    }
}