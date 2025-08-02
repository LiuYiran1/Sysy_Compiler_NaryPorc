package com.compiler.mir.operand;

import com.compiler.mir.MIRType;

public class MIRImmediate extends MIROperand {
    private final long value;
    private final MIRType type;

    public MIRImmediate(long value, MIRType type) {
        this.value = value;
        this.type = type;
    }

    public long getValue() {
        return value;
    }

    @Override
    public MIRType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
