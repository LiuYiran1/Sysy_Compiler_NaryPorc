package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;

import java.util.*;

public class DominateAnalPass implements Pass {

    @Override
    public void run(Module module) {
        List<Function> functions = module.getFunctionDefs();
        for (Function function : functions) {
            computeDomSet(function);
            computeIdom(function, function.getDomMap());
            function.buildDomTreeFromIdom();
            computeDomFrontier(function);
            debug(function);
        }
    }

    public void computeDomSet(Function function) {
        List<BasicBlock> blocks = function.dfsTraversal();
        if (blocks.isEmpty()) return;

        BasicBlock entry = function.getEntryBlock();
        Map<BasicBlock, Set<BasicBlock>> dom = new HashMap<>();

        // 初始化: 每个 block 的支配者集合为所有 block，除了 entry 是只支配自己
        for (BasicBlock bb : blocks) {
            if (bb == entry) {
                dom.put(bb, new HashSet<>(List.of(entry)));
            } else {
                dom.put(bb, new HashSet<>(blocks)); // 初始化为全集
            }
        }

        // 固定点算法（迭代直到收敛）
        boolean changed;
        do {
            changed = false;

            for (BasicBlock bb : blocks) {
                if (bb == entry) continue;

                List<BasicBlock> preds = bb.getPredecessors();
                if (preds.isEmpty()) continue;

                // 初始为第一个前驱的dom集合
                Set<BasicBlock> newDom = new HashSet<>(dom.get(preds.get(0)));
                // 和所有前驱做交集
                for (int i = 1; i < preds.size(); i++) {
                    newDom.retainAll(dom.get(preds.get(i)));
                }
                newDom.add(bb); // 自己支配自己

                if (!newDom.equals(dom.get(bb))) {
                    dom.put(bb, newDom);
                    changed = true;
                }
            }
        } while (changed);

        // 存到 function 中
        function.setDomMap(dom);
    }

    private void computeIdom(Function function, Map<BasicBlock, Set<BasicBlock>> dom) {
        Map<BasicBlock, BasicBlock> idom = new HashMap<>();
        List<BasicBlock> blocks = function.dfsTraversal();
        BasicBlock entry = function.getEntryBlock();

        for (BasicBlock bb : blocks) {
            if (bb == entry) continue;

            Set<BasicBlock> doms = new HashSet<>(dom.get(bb));
            doms.remove(bb); // 除去自己

            BasicBlock idomCand = null;
            for (BasicBlock d : doms) {
                boolean isIdom = true;
                for (BasicBlock other : doms) {
                    if (other == d) continue;
                    if (dom.get(other).contains(d)) {
                        isIdom = false;
                        break;
                    }
                }
                if (isIdom) {
                    idomCand = d;
                    break;
                }
            }

            if (idomCand != null) {
                idom.put(bb, idomCand);
            }
        }

        function.setIdomMap(idom);
    }

    private void computeDomFrontier(Function function) {
        Map<BasicBlock, Set<BasicBlock>> df = new HashMap<>();
        for (BasicBlock bb : function.getBasicBlocks()) {
            df.put(bb, new HashSet<>());
        }

        // 每个 block 遍历它的前驱
        for (BasicBlock bb : function.getBasicBlocks()) {
            List<BasicBlock> preds = bb.getPredecessors();
            if (preds.size() >= 2) {
                for (BasicBlock p : preds) {
                    BasicBlock runner = p;
                    while (runner != null && runner != function.getIdom(bb)) {
                        df.get(runner).add(bb);
                        runner = function.getIdom(runner);
                    }
                }
            }
        }

        function.getDomFrontier().clear();
        function.getDomFrontier().putAll(df);
    }

    private void debug(Function function) {
        System.out.println("**********  DomFrontier  **********\n" + "Function: " + function.getName());
        for (var entry : function.getDomFrontier().entrySet()) {
            System.out.print("DF(" + entry.getKey().getName() + ") = { ");
            for (BasicBlock bb : entry.getValue()) {
                System.out.print(bb.getName() + " ");
            }
            System.out.println("}");
        }

    }


}
