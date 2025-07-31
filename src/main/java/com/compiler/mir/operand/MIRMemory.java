package com.compiler.mir.operand;

import com.compiler.mir.MIRType;

public class MIRMemory extends MIROperand {
    private final MIROperand base;
    private final MIROperand offset;
    private final MIRType type;

    public MIRMemory(MIROperand base, MIROperand offset, MIRType type) {
        this.base = base;
        this.offset = offset;
        this.type = type;
    }

    public MIROperand getBase() {
        return base;
    }
    public MIROperand getOffset() {
        return offset;
    }
    @Override
    public MIRType getType() {
        return type;
    }

    // offset(base)
    @Override
    public String toString() {
        return "[" + base + " + " + offset + "]";
    }
}