package com.compiler.ll;

import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.ReturnInst;
import com.compiler.ll.Values.Instructions.ReturnVoidInst;
import com.compiler.ll.Values.Value;
import com.compiler.ll.utils.NameManager;

public class IRBuilder {
    private BasicBlock currentBlock;
    private final Context context;
    private final NameManager nameManager;
    private Function currentFunction;

    // 构造器示例
    public IRBuilder(Context context, NameManager nameManager) {
        this.context = context;
        this.nameManager = nameManager;
    }

    public void positionAfter(BasicBlock basicBlock) {
        currentBlock = basicBlock;
    }

    public ReturnInst buildReturn(Value val) {
        ReturnInst inst = new ReturnInst(val);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public void buildReturnVoid() {
        ReturnVoidInst inst = new ReturnVoidInst(context.getVoidType());
        currentBlock.addInstruction(inst);
    }


}
