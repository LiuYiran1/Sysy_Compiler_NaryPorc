package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeadCodeElimPass implements Pass {
    @Override
    public void run(Module module) {
        for(Function function : module.getFunctions()) {
            deleteSuspendBlocks(function);
        }
    }

    private void deleteSuspendBlocks(Function function) {
        List<BasicBlock> blocks = function.getBasicBlocks();
        BasicBlock entry = function.getEntryBlock();

        boolean changed;
        do {
            changed = false;

            for (BasicBlock block : new ArrayList<>(blocks)) {
                // 不删 entry，自然入口
                if (block == entry) continue;

                // 如果没有前驱（说明不可达了）
                if (block.getPredecessors().isEmpty()) {
                    function.removeBasicBlock(block);  // 会更新 CFG 和 phi
                    changed = true;
                }
            }

        } while (changed);
    }


}
