package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRMoveOp extends MIRInstruction {
    private final MIROperand source;

    public MIRMoveOp(MIRVirtualReg dest, MIROperand source) {
        super(dest);
        this.source = source;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(source);
    }

    @Override
    public String toString() {
        return "MOVE " + result + ", " + source;
    }
}
