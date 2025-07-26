package com.compiler.mir.operand;

import com.compiler.mir.MIRType;

public class MIRLabel extends MIROperand {
    private final String name;

    public MIRLabel(String name) {
        this.name = name;
    }


    @Override
    public MIRType getType() {
        return null; // 标签没有类型
    }

    @Override
    public String toString() {
        return name;
    }
}