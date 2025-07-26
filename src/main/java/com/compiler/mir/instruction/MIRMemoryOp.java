package com.compiler.mir.instruction;

import com.compiler.mir.operand.MIRMemory;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.Arrays;
import java.util.List;

public class MIRMemoryOp extends MIRInstruction {
    public enum Type {
        INTEGER, // 整数类型lw,sw
        FLOAT,    // 浮点类型flw,fsw
        POINTER,  // 指针类型ld,sd
    }

    public enum Op { LOAD, STORE }

    private final Op op;
    private final Type type;
    private final MIRMemory address;
    private final MIROperand value; // STORE时使用

    public MIRMemoryOp(Op op, Type type, MIRMemory address, MIRVirtualReg result) {
        super(result);
        this.op = op;
        this.address = address;
        this.type = type;
        this.value = null;
    }

    public MIRMemoryOp(Op op, Type type, MIRMemory address, MIROperand value) {
        super(null); // STORE没有结果
        this.op = op;
        this.type = type;
        this.address = address;
        this.value = value;
    }

    @Override
    public List<MIROperand> getOperands() {
        if (op == Op.LOAD) {
            return Arrays.asList(address);
        } else {
            return Arrays.asList(address, value);
        }
    }

    @Override
    public String toString() {
        if (op == Op.LOAD) {
            return type.name() + " LOAD " + result + ", " + address;
        } else {
            return type.name() + " STORE " + value + ", " + address;
        }
    }
}