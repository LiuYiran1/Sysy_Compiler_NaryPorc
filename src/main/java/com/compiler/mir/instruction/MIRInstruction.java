package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.List;

public abstract class MIRInstruction {
    protected final MIRVirtualReg result;

    public MIRInstruction(MIRVirtualReg result) {
        this.result = result;
    }

    public MIRVirtualReg getResult() {
        return result;
    }

    public abstract List<MIROperand> getOperands();

    public abstract String toString();
}
