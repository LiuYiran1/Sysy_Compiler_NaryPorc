package com.compiler.ll;

import com.compiler.ll.Types.FloatType;
import com.compiler.ll.Types.IntegerType;
import com.compiler.ll.Types.PointerType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.*;
import com.compiler.ll.Values.Value;
import com.compiler.ll.utils.NameManager;

public class IRBuilder {
    private BasicBlock currentBlock;
    private final Context context;
    private final NameManager nameManager;
    private Function currentFunction;

    private IntegerType i32;
    private FloatType f32;

    // 构造器示例
    public IRBuilder(Context context, NameManager nameManager) {
        this.context = context;
        this.nameManager = nameManager;
        this.i32 = context.getInt32Type();
        this.f32 = context.getFloatType();
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

    public AllocaInst buildAlloca(Type allocType, String varName) {
        String name = nameManager.getUniqueName(varName);
        AllocaInst inst = new AllocaInst(context.getPointerType(allocType), name);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildFloatToSigned(Value value, Type type, String varName) {
        if (value.isConstant()){
            float raw = ((ConstantFloat)value).getValue();
            int casted = (int) raw;
            return i32.getConstantInt(casted);
        }

        FpToSiInst inst = new FpToSiInst(value, type, varName);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildSignedToFloat(Value value, Type type, String varName) {
        if (value.isConstant()){
            int raw = ((ConstantInt)value).getValue();
            float casted = (float) raw;
            return f32.getConstantFloat(casted);
        }

        SiToFpInst inst = new SiToFpInst(value, type, varName);
        currentBlock.addInstruction(inst);
        return inst;
    }

    // 7

}
