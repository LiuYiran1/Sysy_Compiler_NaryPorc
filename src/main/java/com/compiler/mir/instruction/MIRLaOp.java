package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIRGlobalVariable;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.List;

public class MIRLaOp extends MIRInstruction {
    public enum Op {
        La,  // Load symbol address la
    }

    private final Op op;
    private final MIRGlobalVariable source;

    public MIRLaOp(Op op, MIRVirtualReg result, MIRGlobalVariable source) {
        super(result);
        this.op = op;
        this.source = source;
    }

    @Override
    public List<MIROperand> getOperands() {
        return List.of(source);
    }

    @Override
    public String toString() {
        return op.name() + " " + resultVirtualReg + ", " + source;
    }
}
