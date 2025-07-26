package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIRImmediate;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.List;

public class MIRLiOp extends MIRInstruction {
    public enum Op {
        LI,  // Load Immediate li
    }

    private final Op op;
    private final MIRImmediate source;

    public MIRLiOp(Op op, MIRVirtualReg result, MIRImmediate source) {
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
        return op.name() + " " + result + ", " + source;
    }
}
