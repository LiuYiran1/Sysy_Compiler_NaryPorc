package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIRGlobalVariable;
import com.compiler.mir.operand.MIRImmediate;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRLuiOp extends MIRInstruction {

    private final MIRImmediate immediate;
    private final MIRGlobalVariable globalVariable;

    public MIRLuiOp(MIRVirtualReg result, MIRImmediate immediate) {
        super(result);
        this.immediate = immediate;
        this.globalVariable = null; // 如果没有全局变量，则为null
    }

    public MIRLuiOp(MIRVirtualReg result, MIRGlobalVariable globalVariable) {
        super(result);
        this.globalVariable = globalVariable;
        this.immediate = null; // 如果没有立即数，则为null
    }

    @Override
    public List<MIROperand> getOperands() {
        return Arrays.asList(immediate, globalVariable);
    }

    @Override
    public String toString(){
        return "LUI " + result +
               (immediate != null ? ", " + immediate.toString() : "") +
               (globalVariable != null ? ", " + globalVariable.toString() : "");
    }
}
