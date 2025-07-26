package com.compiler.mir.operand;

import com.compiler.mir.MIRType;

public class MIRFloatConstant extends MIROperand{
    MIRType type;
    double value;

    public MIRFloatConstant(MIRType type,double value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public MIRType getType() {
        return type;
    }
}
