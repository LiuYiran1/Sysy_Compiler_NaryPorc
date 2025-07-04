package com.compiler.ll.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BasicBlock extends User {
    private final List<Instruction> instructions = new ArrayList<>();

    public BasicBlock(String name) {
        super(null, name); // 没有类型
    }

    public void addInstruction(Instruction inst) {
        instructions.add(inst);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public String toIR() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (Instruction inst : instructions) {
            sb.append("  ").append(inst.toIR()).append("\n");
        }
        return sb.toString();
    }
}
