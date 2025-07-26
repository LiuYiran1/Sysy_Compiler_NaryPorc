package com.compiler.mir;

import com.compiler.mir.operand.MIRVirtualReg;

import java.util.ArrayList;
import java.util.List;

public class MIRFunction {
    private final String name;
    private final List<MIRVirtualReg> params = new ArrayList<>();
    private final List<MIRBasicBlock> blocks = new ArrayList<>();
    private int nextRegId = 0;

    public MIRFunction(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<MIRVirtualReg> getParams() {
        return params;
    }

    public List<MIRBasicBlock> getBlocks() {
        return blocks;
    }

    public MIRVirtualReg newVirtualReg(MIRType type) {
        return new MIRVirtualReg(nextRegId++, type);
    }

    public void addParam(MIRType type) {
        params.add(newVirtualReg(type));
    }

    public void addBlock(MIRBasicBlock block) {
        blocks.add(block);
    }
}

