package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.BranchInst;

import java.util.*;

class Loop {
    BasicBlock header;
    Set<BasicBlock> bodyBlocks;
    BasicBlock preHeader;
    Function parentFunc;

    public Loop(BasicBlock header, Function parentFunc) {
        this.header = header;
        this.parentFunc = parentFunc;
        this.bodyBlocks = new LinkedHashSet<>();
    }

    public boolean contains(BasicBlock bb) {
        return bodyBlocks.contains(bb);
    }
}

public class LoopAnalPass implements Pass {
    DominateAnalPass domPass;

    public LoopAnalPass(DominateAnalPass domPass) {
        this.domPass = domPass;
    }

    @Override
    public boolean run(Module mod) {
        for (Function func : mod.getFunctionDefs()) {
            List<Loop> loops = findLoops(func);

            // 按循环体大小排序：先处理内层循环
            loops.sort((l1, l2) -> l2.bodyBlocks.size() - l1.bodyBlocks.size());

            for (Loop loop : loops) {
                System.out.println("Loop header: " + loop.header.getName());
                System.out.println("Loop body blocks: ");
                for (BasicBlock bb : loop.bodyBlocks) {
                    System.out.print(bb.getName() + " ");
                }
                System.out.println("Loop preHeader: " + loop.preHeader.getName());
                System.out.println("\n---");
            }
        }
        return true;
    }


    List<Loop> findLoops(Function func) {
        List<Loop> loops = new ArrayList<>();

        // 遍历每条基本块及其前驱，寻找 back edge
        for (BasicBlock bb : new ArrayList<>(func.getBasicBlocks())) {
            for (BasicBlock pred : new ArrayList<>(bb.getPredecessors())) {
                // 如果 bb 支配 pred -> back edge
                if (func.getDomMap().get(pred).contains(bb)) {
                    // bb 是 Header, pred 是循环尾块
                    BasicBlock header = bb;
                    BasicBlock backEdgeFrom = pred;

                    // 收集循环体块
                    Set<BasicBlock> loopBody = collectLoopBlocks(backEdgeFrom, header);

                    // 创建 Loop 对象
                    Loop loop = findLoopByHeader(header, loops);
                    if (loop == null) {
                        loop = new Loop(header, func);
                        loops.add(loop);
                    }
                    loop.bodyBlocks.addAll(loopBody);

                    // 找 Preheader
                    loop.preHeader = getOrCreatePreheader(loop);

                }
            }
        }

        return loops;
    }

    private BasicBlock getOrCreatePreheader(Loop loop) {
        BasicBlock header = loop.header;
        Function func = loop.parentFunc;

        // 找出所有循环外前驱
        List<BasicBlock> nonLoopPreds = new ArrayList<>();
        for (BasicBlock pred : header.getPredecessors()) {
            if (!loop.bodyBlocks.contains(pred)) {
                nonLoopPreds.add(pred);
            }
        }

        if (nonLoopPreds.isEmpty()) {
            // 没有循环外前驱，说明 Header 入口只能从循环内来（扯蛋）
            throw new RuntimeException("No predecessors found for " + header.getName());
        } else if (nonLoopPreds.size() == 1) {
            // 只有一个前驱，可以直接用它作为 Preheader
            return nonLoopPreds.get(0);
        } else {
            // 多个前驱，需要创建新的 Preheader
            BasicBlock newPreheader = func.createBasicBlock("preheader_for_" + header.getName());

            // 修改所有非循环体前驱的跳转，指向 newPreheader
            for (BasicBlock pred : nonLoopPreds) {
                pred.replaceSuccessor(header, newPreheader);
            }

            // Preheader 跳转到 Header
            newPreheader.setTerminator(new BranchInst(header, newPreheader));
            newPreheader.addSuccessor(header);
            header.addPredecessor(newPreheader);

            // 需要重新建立支配关系
            domPass.run(func);

            return newPreheader;
        }
    }



    private Loop findLoopByHeader(BasicBlock header, List<Loop> loops) {
        for (Loop loop : loops) {
            if (loop.header == header) return loop;
        }
        return null;
    }

    private Set<BasicBlock> collectLoopBlocks(BasicBlock backEdgeFrom, BasicBlock header) {
        Set<BasicBlock> loopBlocks = new LinkedHashSet<>();
        Stack<BasicBlock> stack = new Stack<>();
        stack.push(backEdgeFrom);
        loopBlocks.add(header); ////

        while (!stack.isEmpty()) {
            BasicBlock bb = stack.pop();
            if (!loopBlocks.contains(bb)) {
                loopBlocks.add(bb);
                for (BasicBlock pred : bb.getPredecessors()) {
                    if (pred != header) { // 阻止 DFS 超出循环头
                        stack.push(pred);
                    }
                }
            }
        }

        return loopBlocks;
    }
}
