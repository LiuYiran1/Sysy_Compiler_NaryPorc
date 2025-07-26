package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIRVirtualReg;

public class MIRAlloca extends MIRInstruction {

    private final int size;
    private final int alignment;

    public MIRAlloca(MIRVirtualReg result, int size, int alignment) {
        super(result);
        this.size = size;
        this.alignment = alignment;
    }

    @Override
    public String toString() {
        return "ALLOCA " + result + ", size=" + size + ", align=" + alignment;
    }
}