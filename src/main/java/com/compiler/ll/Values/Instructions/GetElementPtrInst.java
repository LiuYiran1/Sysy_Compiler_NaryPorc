package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;
import java.util.*;

public class GetElementPtrInst extends Instruction {
    private final Value basePointer;
    private final List<Value> indices;

    public GetElementPtrInst(Type type, String name, Value base, List<Value> indices) {
        super(type, name, Opcode.GEP);
        this.basePointer = base;
        this.indices = indices;
        addOperand(base);
        indices.forEach(this::addOperand);
    }

    @Override
    public String toIR() {
        StringBuilder sb = new StringBuilder("%" + name + " = getelementptr " + type.toIR()
                + ", " + operands.get(0).getType().toIR() + " " + operands.get(0).getName());
        for (int i = 1; i < operands.size(); i++) {
            sb.append(", " + operands.get(i).getType().toIR() + " " + operands.get(i).getName());
        }
        return sb.toString();
    }

    public List<Value> getIndices() {
        return indices;
    }

    public Value getBasePointer() {
        return basePointer;
    }
}