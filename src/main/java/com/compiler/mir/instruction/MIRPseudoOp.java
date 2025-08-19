package com.compiler.mir.instruction;

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

    public MIRPseudoOp(Type type) {
        super((MIRVirtualReg) null);
        this.type = type;
    }
    public Type getType() {
        return type;
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
