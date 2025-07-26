package com.compiler.ll.Values.GlobalValues;

import com.compiler.ll.Module;
import com.compiler.ll.Types.FunctionType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Argument;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Function extends GlobalValue {
    private final Module mod;
    private final List<Argument> arguments = new ArrayList<>();
    private final List<BasicBlock> blocks = new ArrayList<>();

    public Function(FunctionType type, String name, Module mod) {
        super(type, name);
        this.mod = mod;
        for (int i = 0; i < type.getParamTypes().size(); i++) {
            Type argType = type.getParamTypes().get(i);
            arguments.add(new Argument(argType, "arg" + i));
        }
    }

    public Function getNextFunction() {
        if (mod == null) return null;
        List<Function> funcs = mod.getFunctions();
        int idx = funcs.indexOf(this);
        if (idx == -1 || idx + 1 >= funcs.size()) return null;
        return funcs.get(idx + 1);
    }

    public BasicBlock getFirstBasicBlock() {
        return blocks.isEmpty() ? null : blocks.get(0);
    }

    public List<BasicBlock> getBasicBlocks() {
        return blocks;
    }


    public List<Argument> getArguments() {
        return arguments;
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public void addBasicBlock(BasicBlock block) {
        blocks.add(block);
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    // 这里要区分声明和定义
    @Override
    public String toIR() {
        FunctionType ft = (FunctionType) type;
        String argsIR = arguments.stream()
                .map(Argument::toIR)
                .collect(Collectors.joining(", "));

        if (blocks.isEmpty()) {
            // 没有基本块 → 是声明
            return "declare " + ft.getReturnType().toIR() +
                    " @" + name + "(" + argsIR + ")";
        }

        // 否则是定义
        StringBuilder sb = new StringBuilder();
        sb.append("define ").append(ft.getReturnType().toIR())
                .append(" @").append(name).append("(").append(argsIR).append(") {\n");

        for (BasicBlock bb : blocks) {
            sb.append(bb.toIR()).append("\n");
        }

        sb.append("}");
        return sb.toString();
    }

}
