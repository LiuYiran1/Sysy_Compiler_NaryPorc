package com.compiler.mir.instruction;

import com.compiler.backend.PhysicalRegister;
import com.compiler.ll.Values.Value;
import com.compiler.mir.operand.MIRLabel;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.*;


public class MIRControlFlowOp extends MIRInstruction {

    public enum Type {
        RET,       // 返回  ret
        JMP,       // 无条件跳转 j
        COND_JMP,  // 条件跳转 j + bnez
        CALL       // 函数调用 call
    }

    private final Type type;
    private final MIROperand target;
    private final MIROperand condition; // COND_JMP时使用
    private final List<MIROperand> args; // CALL时使用


    // 返回指令
    public MIRControlFlowOp() {
        super((MIRVirtualReg) null);
        this.type = Type.RET;
        this.target = null;
        this.condition = null;
        this.args = null;
    }

    // 无条件跳转
    public MIRControlFlowOp(MIRLabel target) {
        super((MIRVirtualReg) null);
        this.type = Type.JMP;
        this.target = target;
        this.condition = null;
        this.args = null;
    }

    // 条件跳转
    public MIRControlFlowOp(MIROperand condition, MIRLabel trueTarget) {
        super((MIRVirtualReg) null);
        this.type = Type.COND_JMP;
        this.target = trueTarget;
        this.condition = condition;
        this.args = null;
    }

    // 函数调用
    public MIRControlFlowOp(MIRLabel func, MIRVirtualReg result, List<MIROperand> args) {
        super(result);
        this.type = Type.CALL;
        this.target = func;
        this.condition = null;
        this.args = args;
    }

    public MIRControlFlowOp(MIRLabel func, List<MIROperand> args) {
        super((MIRVirtualReg) null);
        this.type = Type.CALL;
        this.target = func;
        this.condition = null;
        this.args = args;
    }

    public MIROperand getTarget() {
        return target;
    }
    public MIROperand getCondition() {
        return condition;
    }
    public List<MIROperand> getArgs() {
        return args != null ? args : new ArrayList<>();
    }

    public Type getType() {
        return type;
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
            case RET -> "RET ";
            case JMP -> "JMP " + target;
            case COND_JMP -> "COND_JMP " + condition + ", " + target;
            case CALL -> "CALL " + target + " -> " + ((resultVirtualReg != null) ? resultVirtualReg.toString() : "" ) + ", args: " + args;
            default -> "UNKNOWN";
        };
    }
}
