package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.PointerType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;
import java.util.*;

public class GetElementPtrInst extends Instruction {
    private final Value basePointer;
    private final List<Value> indices;
    private final Type elementType;

    public GetElementPtrInst(PointerType type, Type elementType,String name, Value base, List<Value> indices) {
        super(type, name, Opcode.GEP);
        this.basePointer = base;
        this.indices = indices;
        this.elementType = elementType;
        addOperand(base);
        indices.forEach(this::addOperand);
    }

    @Override
    public String toIR() {
        StringBuilder sb = new StringBuilder("%" + name + " = getelementptr " + elementType.toIR()
                + ", " + operands.get(0).getType().toIR() + " " + getOpStr(operands.get(0)));
        for (int i = 1; i < operands.size(); i++) {
            sb.append(", " + operands.get(i).getType().toIR() + " " + getOpStr(operands.get(i)));
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