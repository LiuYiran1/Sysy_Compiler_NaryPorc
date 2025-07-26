package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIRLabel;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MIRControlFlow extends MIRInstruction {
    public enum Type {
        RET,       // 返回
        JMP,       // 无条件跳转
        COND_JMP,  // 条件跳转
        CALL       // 函数调用
    }

    private final Type type;
    private final MIROperand target;
    private final MIROperand condition; // COND_JMP时使用
    private final List<MIROperand> args; // CALL时使用

    // 返回指令
    public MIRControlFlow(MIROperand retValue) {
        super(null);
        this.type = Type.RET;
        this.target = retValue;
        this.condition = null;
        this.args = null;
    }

    // 无条件跳转
    public MIRControlFlow(MIRLabel target) {
        super(null);
        this.type = Type.JMP;
        this.target = target;
        this.condition = null;
        this.args = null;
    }

    // 条件跳转
    public MIRControlFlow(MIROperand condition, MIRLabel trueTarget, MIRLabel falseTarget) {
        super(null);
        this.type = Type.COND_JMP;
        this.target = trueTarget;
        this.condition = condition;
        this.args = Arrays.asList(falseTarget);
    }

    // 函数调用
    public MIRControlFlow(MIRLabel func, MIRVirtualReg result, List<MIROperand> args) {
        super(result);
        this.type = Type.CALL;
        this.target = func;
        this.condition = null;
        this.args = args;
    }

    @Override
    public List<MIROperand> getOperands() {
        List<MIROperand> operands = new ArrayList<>();
        if (condition != null) operands.add(condition);
        if (args != null) operands.addAll(args);
        if (target != null) operands.add(target);
        return operands;
    }

    @Override
    public String toString() {
        return switch (type) {
            case RET -> "RET " + target;
            case JMP -> "JMP " + target;
            case COND_JMP -> "COND_JMP " + condition + ", " + target + ", " + args.get(0);
            case CALL -> "CALL " + target + " -> " + result + ", args: " + args;
            default -> "UNKNOWN";
        };
    }
}
