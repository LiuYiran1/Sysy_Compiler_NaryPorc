package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRMoveOp extends MIRInstruction {
    public enum MoveType {
        INTEGER,    // mv (整数移动)
        FLOAT,      // fmv.s (浮点移动)
        INT_TO_FLOAT // fmv.w.x (整数到浮点)
    }

    private final MIROperand source;
    private final MoveType moveType;

    public MIRMoveOp(MIRVirtualReg dest, MIROperand source, MoveType moveType) {
        super(dest);
        this.source = source;
        this.moveType = moveType;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(source);
    }

    @Override
    public String toString() {
        return switch (moveType) {
            case INTEGER -> "MV " + result + ", " + source;
            case FLOAT -> "FMV_S " + result + ", " + source;
            case INT_TO_FLOAT -> "FMV_W_X " + result + ", " + source;
            default -> "MOVE " + result + ", " + source;
        };
    }
}
