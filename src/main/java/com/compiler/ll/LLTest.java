package com.compiler.ll;

import com.compiler.ll.Types.*;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.FloatPredicate;
import com.compiler.ll.Values.Instructions.IntPredicate;
import com.compiler.ll.Values.Value;

import java.util.ArrayList;

public class LLTest {
    static final Context context = new Context();
    static final IntegerType i32 = context.getIntType(32);
    static final FloatType f32 = context.getFloatType();
    static final IRBuilder builder = context.getIRBuilder();
    static final Module mod = context.getModule("module");

    public static void main(String[] args) {
        Function function = new Function(context.getFunctionType(context.getInt32Type(), new ArrayList<>()), "main", mod);
        BasicBlock bb = context.newBasicBlock("mainEntry");
        function.addBasicBlock(bb);
        builder.positionAfter(bb);

        // testNamesAndReturn();
        // testTypeAndAlloc();
        // testCast();
        // testStore();
        // testCompareAndBranch();
        testFunction();



        mod.dump();
    }

    static void testFunction(){

    }

    static void testCompareAndBranch() {
        // 常量折叠
        Value c = builder.buildFloatCompare(FloatPredicate.OEQ, f32.getConstantFloat(2.0f), f32.getConstantFloat(2.01f), "fcon");
        builder.buildReturn(c);
        Value c1 = builder.buildIntCompare(IntPredicate.NE, i32.getConstantInt(10), i32.getConstantInt(20), "icon");
        builder.buildReturn(c1);
        Value c3 = builder.buildIntCompare(IntPredicate.SGE, c1, c, "tt");
        builder.buildReturn(c3);
        Value a = builder.buildAlloca(f32, "a");
        Value b = builder.buildAlloca(i32, "b");
        Value c4 = builder.buildFloatCompare(FloatPredicate.OEQ, a, b, "c4");
        builder.buildReturn(c4);

        BasicBlock trueBlock = context.newBasicBlock("trueBlock");
        BasicBlock falseBlock = context.newBasicBlock("falseBlock");
        builder.buildConditionalBranch(c4, trueBlock, falseBlock);
        builder.positionAfter(trueBlock);
        builder.buildBranch(falseBlock);
    }

    static void testStore(){
        Value a = builder.buildAlloca(f32, "a");
        builder.buildStore(a, new ConstantFloat(f32, 99.9f));
        Value b = builder.buildAlloca(i32, "b");
        Value c = builder.buildFloatToSigned(a, i32, "c");
        builder.buildStore(b, c);
    }

    static void testCast(){
        Value a = builder.buildAlloca(i32, "a");
        Value b = builder.buildAlloca(f32, "b");
        Value a1 = builder.buildSignedToFloat(a, f32, "a");
        Value b1 = builder.buildFloatToSigned(b, i32, "b");
    }

    static void testTypeAndAlloc(){
        Type arrTy1 = context.getArrayType(i32, 9);
        Type arrTy2 = context.getArrayType(arrTy1, 10);
        Type arrTy3 = context.getArrayType(arrTy2, 11);
        Value a = builder.buildAlloca(arrTy1, "a");
        Value a1 = builder.buildAlloca(arrTy2, "a");
        Value a2 = builder.buildAlloca(arrTy3, "a");
    }

    static void testNamesAndReturn() {
        builder.buildReturnVoid();
        Value a = builder.buildAlloca(i32, "a");
        Value a1 = builder.buildAlloca(i32, "a");
        Value a2 = builder.buildAlloca(i32, "a.1");
        builder.buildReturn(a);
        builder.buildReturn(a1);
        builder.buildReturn(a2);
        builder.buildReturn(new ConstantInt(i32, 99));
        builder.buildReturn(new ConstantFloat(f32, 99.9f));
    }
}
//llc -relocation-model=static -march=riscv64 -mattr=+d -filetype=asm main.ll -o main.s