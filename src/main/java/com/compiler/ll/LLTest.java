package com.compiler.ll;

import com.compiler.ll.Types.FunctionType;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;

import java.util.ArrayList;

public class LLTest {
    public static void main(String[] args) {
        Context context = new Context();
        IRBuilder builder = context.getIRBuilder();
        Module mod = context.getModule("module");

        // funcType
        Function function = new Function(context.getFunctionType(context.getInt32Type(), new ArrayList<>()), "main");
        mod.addFunction(function);
        BasicBlock bb = new BasicBlock("mainEntry");
        function.addBasicBlock(bb);
        builder.positionAfter(bb);
        builder.buildReturnVoid();
        mod.dump();
    }
}
