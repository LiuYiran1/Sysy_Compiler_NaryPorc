package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRConvertOp extends MIRInstruction {
    public enum Op {
        ZEXT,     // 零扩展
        FPTOI,    // 浮点到整数
        ITOFP,    // 整数到浮点
        BITCAST   // 位转换
    }

    private final Op op;
    private final MIROperand source;

    public MIRConvertOp(Op op, MIRVirtualReg result, MIROperand source) {
        super(result);
        this.op = op;
        this.source = source;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(source);
    }

    @Override
    public String toString() {
        return op.name() + " " + resultVirtualReg + ", " + source;
    }
}
