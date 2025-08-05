package com.compiler.mir;

import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;
import com.compiler.mir.operand.MIROperand;
import com.compiler.mir.operand.MIRVirtualReg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MIRFunction {
    private final String name;
    private final List<MIROperand> params = new ArrayList<>();
    private final List<MIRBasicBlock> blocks = new ArrayList<>();
    private final Map<MIRBasicBlock, List<Instruction>> phiNodes = new LinkedHashMap<>();
    private int nextRegId = 0;

    public MIRFunction(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<MIROperand> getParams() {
        return params;
    }

    public List<MIRBasicBlock> getBlocks() {
        return blocks;
    }

    public Map<MIRBasicBlock, List<Instruction>> getPhiNodes() {
        return phiNodes;
    }

    public MIRVirtualReg newVirtualReg(MIRType type) {
        return new MIRVirtualReg(nextRegId++, type);
    }

    public void addParam(MIROperand reg) {
        params.add(reg);
    }

    public void addBlock(MIRBasicBlock block) {
        blocks.add(block);
    }

    public void addPhiNode(MIRBasicBlock block, Instruction phi) {
        phiNodes.putIfAbsent(block, new ArrayList<>());
        phiNodes.get(block).add(phi);
    }

}

