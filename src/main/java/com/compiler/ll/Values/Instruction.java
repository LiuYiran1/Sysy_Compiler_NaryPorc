package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instructions.Opcode;

import java.util.ArrayList;
import java.util.List;

public abstract class Instruction extends Value {
    protected final Opcode opcode;
    protected final List<Value> operands = new ArrayList<>();

    public Instruction(Type type, String name, Opcode opcode) {
        super(type, name);
        this.opcode = opcode;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public void addOperand(Value val) {
        operands.add(val);
    }

    public List<Value> getOperands() {
        return operands;
    }

    public abstract String toIR();
}
