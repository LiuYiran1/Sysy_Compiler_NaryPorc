package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRPhysicalReg;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.List;

public abstract class MIRInstruction {
    protected final MIRVirtualReg resultVirtualReg;
    protected final MIRPhysicalReg resultPhysicalReg;

    public MIRInstruction(MIRVirtualReg result) {
        this.resultVirtualReg = result;
        this.resultPhysicalReg = null;
    }

    public MIRInstruction(MIRPhysicalReg resultPhysicalReg) {
        this.resultVirtualReg = null;
        this.resultPhysicalReg = resultPhysicalReg;
    }

    public MIRVirtualReg getResult() {
        return resultVirtualReg;
    }

    public MIRPhysicalReg getResultPhysicalReg() {
        return resultPhysicalReg;
    }

    public abstract List<MIROperand> getOperands();

    public abstract String toString();
}
