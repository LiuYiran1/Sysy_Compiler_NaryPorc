package com.compiler.mir.operand;

import com.compiler.mir.MIRType;

public class MIRVirtualReg extends MIROperand {
    private final int id;
    private final MIRType type;

    public MIRVirtualReg(int id, MIRType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public MIRType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "%v" + id;
    }
}
