package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.List;

public class MIRAllocOp extends MIRInstruction {

    private final int size;
    private final int alignment;

    public MIRAllocOp(MIRVirtualReg result, int size, int alignment) {
        super(result);
        this.size = size;
        this.alignment = alignment;
    }

    @Override
    public List<MIROperand> getOperands() {
        return List.of();
    }

    @Override
    public String toString() {
        return "ALLOC " + result + ", size=" + size + ", align=" + alignment;
    }
}