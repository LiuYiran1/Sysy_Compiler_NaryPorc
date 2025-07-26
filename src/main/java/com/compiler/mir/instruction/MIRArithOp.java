package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRArith extends MIRInstruction {

    public enum Op { ADD, SUB, MUL, DIV, REM, XOR }

    private final Op op;
    private final MIROperand left;
    private final MIROperand right;

    public MIRArith(Op op, MIRVirtualReg result, MIROperand left, MIROperand right) {
        super(result);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(left, right);
    }

    @Override
    public String toString() {
        return op.name() + " " + result + ", " + left + ", " + right;
    }
}