package com.compiler.mir.instruction;

import com.compiler.ll.Values.Instruction;
import com.compiler.mir.operand.MIRLabel;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Collections;
import java.util.List;

public class MIRPseudoOp extends MIRInstruction {
    public enum Type {
        PROLOGUE,   // 函数入口
        EPILOGUE,   // 函数出口
        CALLER_SAVE_REG,   // 保存寄存器
        CALLEE_SAVE_REG,   // 保存寄存器
        CALLER_RESTORE_REG, // 恢复寄存器
        CALLEE_RESTORE_REG, // 恢复寄存器
        SELECT // 选择指令
    }

    private final Type type;
    private final MIRLabel call;

    public MIRPseudoOp(Type type) {
        super((MIRVirtualReg) null);
        this.type = type;
        this.call = null;
    }

    public MIRPseudoOp(Type type, MIRLabel instruction) {
        super((MIRVirtualReg) null);
        this.type = type;
        this.call = instruction;
    }
    public Type getType() {
        return type;
    }
    public MIRLabel getInstName() {
        return call;
    }

    @Override
    public List<MIROperand> getOperands() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return type.name();
    }
}
