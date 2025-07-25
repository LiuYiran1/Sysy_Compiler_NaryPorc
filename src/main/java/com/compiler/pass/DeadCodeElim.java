package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.GlobalValues.Function;

public class DeadCodeElim implements Pass {
    @Override
    public void run(Module module) {
        for(Function function : module.getFunctions()) {
            deleteSuspendBlocks(function);
        }
    }

    private void deleteSuspendBlocks(Function function) {

    }
}
