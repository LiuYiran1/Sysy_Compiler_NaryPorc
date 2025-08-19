package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIRImmediate;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRShiftOp extends MIRInstruction {

    public enum Op {
        SLL,  // 逻辑左移 (Shift Left Logical)
        SRL,  // 逻辑右移 (Shift Right Logical)
        SRA   // 算术右移 (Shift Right Arithmetic)
    }

    private final Op op;
    private final MIRVirtualReg source;
    private final MIRImmediate shiftAmount;


    public MIRShiftOp(Op op, MIRVirtualReg result,
                      MIRVirtualReg source, MIRImmediate shiftAmount) {
        super(result);
        this.op = op;
        this.source = source;
        this.shiftAmount = shiftAmount;
    }

    public Op getOp() {
        return op;
    }

    public MIRVirtualReg getSource() {
        return source;
    }

    public MIRImmediate getShiftAmount() {
        return shiftAmount;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(source, shiftAmount);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(op.name()).append(" ");

        // 添加结果寄存器
        if (resultVirtualReg != null) {
            sb.append(resultVirtualReg);
        } else {
            sb.append(resultPhysicalReg);
        }

        sb.append(", ").append(source).append(", ").append(shiftAmount);
        return sb.toString();
    }
}