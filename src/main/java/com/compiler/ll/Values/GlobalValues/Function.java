package com.compiler.ll.Values.GlobalValues;

import com.compiler.ll.Module;
import com.compiler.ll.Types.FunctionType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Argument;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValue;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Instructions.PhiInst;

import java.util.*;
import java.util.stream.Collectors;

public class Function extends GlobalValue {
    private final Module mod;
    private final List<Argument> arguments = new ArrayList<>();
    private final List<BasicBlock> blocks = new ArrayList<>();

    // --------------------------------------pass-----------------------------------------
    private BasicBlock entryBlock;
    // block -> set of dominators
    private Map<BasicBlock, Set<BasicBlock>> domMap = new HashMap<>();
    private Map<BasicBlock, BasicBlock> idomMap = new LinkedHashMap<>(); // 每个块的立即支配着
    private Map<BasicBlock, List<BasicBlock>> domTree = new HashMap<>();
    private Map<BasicBlock, Set<BasicBlock>> domFrontier = new HashMap<>(); // 支配边界


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

    public Argument getArgument(int index) {
        return arguments.get(index);
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public void addBasicBlock(BasicBlock block) {
        if (blocks.isEmpty()) this.entryBlock = block;
        blocks.add(block);
    }

    public void removeBasicBlock(BasicBlock block) {
        // 从当前 Function 的基本块列表中删除
        blocks.remove(block);

        // 断开 CFG 中的连接：让其前驱与后继都解除引用
        for (BasicBlock pred : new ArrayList<>(block.getPredecessors())) {
            pred.getSuccessors().remove(block);
        }
        for (BasicBlock succ : new ArrayList<>(block.getSuccessors())) {
            succ.getPredecessors().remove(block);

            // 如果后继块中有 Phi，需要把该 block 作为 incoming 的信息删掉
            for (PhiInst phi : succ.getPhiInsts()) {
                phi.removeIncomingFrom(block);
            }
        }

        // 清理指令等内容
        block.delete();
    }


    public List<BasicBlock> getBlocks() {
        return blocks;
    }


    // --------------------------------------pass--------------------------------------------
    public boolean isDef(){
        return entryBlock != null;
    }

    public BasicBlock getEntryBlock(){ // 这里应该没问题
        return entryBlock;
    }

    public List<BasicBlock> dfsTraversal() {
        List<BasicBlock> visited = new LinkedList<>();
        Set<BasicBlock> seen = new HashSet<>();
        Deque<BasicBlock> stack = new ArrayDeque<>(); // 官方文档建议使用 Deque 来代替 Stack

        BasicBlock entry = getEntryBlock();
        if (entry == null) return visited;

        stack.push(entry);
        while (!stack.isEmpty()) {
            BasicBlock bb = stack.pop();
            if (seen.contains(bb)) continue;
            seen.add(bb);
            visited.add(bb);

            // 添加所有后继
            List<BasicBlock> successors = bb.getSuccessors();
            for (int i = successors.size() - 1; i >= 0; i--) {
                stack.push(successors.get(i));
            }
        }
        return visited;
    }

    public void buildDomTreeFromIdom() {
        domTree.clear();
        for (BasicBlock bb : getBasicBlocks()) {
            BasicBlock idom = getIdom(bb);
            if (idom != null) {
                domTree.computeIfAbsent(idom, k -> new ArrayList<>()).add(bb);
            }
        }
    }

    public void removeInstruction(Instruction inst) {
        for (BasicBlock bb : getBasicBlocks()) {
            bb.removeInstruction(inst);
        }
    }

    public List<BasicBlock> getDomChildren(BasicBlock bb) {
        return domTree.getOrDefault(bb, List.of());
    }


    public void setDomMap(Map<BasicBlock, Set<BasicBlock>> map) {
        this.domMap = map;
    }

    public Map<BasicBlock, Set<BasicBlock>> getDomMap() {
        return domMap;
    }

    public Set<BasicBlock> getDominators(BasicBlock bb) {
        return domMap.getOrDefault(bb, Set.of());
    }

    public void setIdomMap(Map<BasicBlock, BasicBlock> map) {
        this.idomMap = map;
    }

    public BasicBlock getIdom(BasicBlock bb) {
        return idomMap.get(bb);
    }

    public Map<BasicBlock, Set<BasicBlock>> getDomFrontier() {
        return domFrontier;
    }

    public Map<BasicBlock, List<BasicBlock>> getDomTree() {
        return domTree;
    }
    // --------------------------------------pass--------------------------------------------


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
