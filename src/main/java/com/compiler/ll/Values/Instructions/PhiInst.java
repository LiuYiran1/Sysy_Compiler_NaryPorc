package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

import java.util.ArrayList;
import java.util.List;

public class PhiInst extends Instruction {
    private final List<String> incomingBlocks = new ArrayList<>();

    public PhiInst(Type type, String name) {
        super(type, name, Opcode.PHI);
    }

    public void addIncoming(Value value, String blockName) {
        addOperand(value);
        incomingBlocks.add(blockName);
    }

    @Override
    public String toIR() {
        StringBuilder sb = new StringBuilder("%" + name + " = phi " + type.toIR() + " ");
        for (int i = 0; i < operands.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("[ " + operands.get(i).getName() + ", %" + incomingBlocks.get(i) + " ]");
        }
        return sb.toString();
    }
}