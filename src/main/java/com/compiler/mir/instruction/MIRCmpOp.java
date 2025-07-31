package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRCmpOp extends MIRInstruction {

    public enum Type { INT, FLOAT }
    public enum Op { EQ, NE, LT, LE, GT, GE }

    private final Type cmpType;
    private final Op op;
    private final MIROperand left;
    private final MIROperand right;

    public MIRCmpOp(Type cmpType, Op op, MIRVirtualReg result, MIROperand left, MIROperand right) {
        super(result);
        this.cmpType = cmpType;
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public Type getCmpType() {
        return cmpType;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(left, right);
    }

    @Override
    public String toString() {
        return cmpType.name() + "_CMP " + op.name() + " " + resultVirtualReg + ", " + left + ", " + right;
    }
}

