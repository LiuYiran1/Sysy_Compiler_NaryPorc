package com.compiler.ll;

import com.compiler.ll.Types.*;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.*;
import com.compiler.ll.Values.Value;
import com.compiler.ll.exceptions.CondException;
import com.compiler.ll.exceptions.GEPException;
import com.compiler.ll.exceptions.LoadException;
import com.compiler.ll.exceptions.ZeroExtException;
import com.compiler.ll.utils.NameManager;

import java.util.List;

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

    public Value buildLoad(Value pointer, String varName) {
        if (!pointer.getType().isPointerType()) {
            throw new LoadException("load expects a pointer type");
        }

        Type pointeeType = ((PointerType) pointer.getType()).getPointeeType();
        String name = nameManager.getUniqueName(varName);

        LoadInst inst = new LoadInst(pointeeType, name, pointer);
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

    public StoreInst buildStore(Value pointer, Value value) { // 注意顺序
        StoreInst inst = new StoreInst(value, pointer); // 注意：StoreInst 返回值为 null（void）
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildIntCompare(IntPredicate cond, Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            int a = ((ConstantInt) lhs).getValue();
            int b = ((ConstantInt) rhs).getValue();
            boolean result = switch (cond) {
                case EQ -> a == b;
                case NE -> a != b;
                case SLT -> a < b;
                case SGT -> a > b;
                case SLE -> a <= b;
                case SGE -> a >= b;
            };
            return new ConstantInt(context.getInt1Type(), result ? 1 : 0);
        }

        String name = nameManager.getUniqueName(varName);
        ICmpInst inst = new ICmpInst(context.getInt1Type(), name, cond, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildFloatCompare(FloatPredicate cond, Value lhs, Value rhs, String varName) {
        if (lhs instanceof ConstantFloat && rhs instanceof ConstantFloat) {
            float a = ((ConstantFloat) lhs).getValue();
            float b = ((ConstantFloat) rhs).getValue();
            boolean result = switch (cond) {
                case OEQ -> a == b;
                case ONE -> a != b;
                case OLT -> a < b;
                case OGT -> a > b;
                case OLE -> a <= b;
                case OGE -> a >= b;
            };
            return new ConstantInt(context.getInt1Type(), result ? 1 : 0);
        }


        String name = nameManager.getUniqueName(varName);
        FCmpInst inst = new FCmpInst(context.getInt1Type(), name, cond, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public BranchInst buildBranch(BasicBlock target){
        BranchInst inst = new BranchInst(target);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public CondBranchInst buildConditionalBranch(Value condition, BasicBlock trueBlock, BasicBlock falseBlock) {
        // 类型检查：condition 应为 i1 类型
        Type condType = condition.getType();
        if (condType.isIntegerType()){
            if (((IntegerType)condType).getBitWidth() != 1){
                throw new CondException("cond is not i1!!!");
            }
        } else {
            throw new CondException("cond is not integer!!!");
        }
        CondBranchInst inst = new CondBranchInst(condition, trueBlock, falseBlock);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildZeroExt(Value from, Type toType, String name){
        if (!from.getType().isIntegerType()){
            throw new ZeroExtException("from is not integer!!!");
        }
        if (!toType.isIntegerType()){
            throw new ZeroExtException("toType is not integer!!!");
        }
        // 常量折叠
        if (from.isConstant()){
            int value = ((ConstantInt)from).getValue();
            return new ConstantInt((IntegerType) toType, value);
        }

        ZExtInst inst = new ZExtInst(from, toType, name);
        currentBlock.addInstruction(inst);
        return inst;
    }


    public Value buildCall(Function function, List<Value> params) {
        Type retType = ((FunctionType)(function.getType())).getReturnType();

        String retName = retType.isVoidType() ? null : nameManager.getUniqueName("call");
        CallInst inst = new CallInst(retType, retName, function.getName(), params.toArray(new Value[0]));
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildIntMul(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            int res = ((ConstantInt) lhs).getValue() * ((ConstantInt) rhs).getValue();
            return new ConstantInt((IntegerType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        MulInst inst = new MulInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildSignedDiv(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            int divisor = ((ConstantInt) rhs).getValue();
            if (divisor == 0) throw new ArithmeticException("division by zero");
            int res = ((ConstantInt) lhs).getValue() / divisor;
            return new ConstantInt((IntegerType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        SDivInst inst = new SDivInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildSignedRem(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            int divisor = ((ConstantInt) rhs).getValue();
            if (divisor == 0) throw new ArithmeticException("remainder by zero");
            int res = ((ConstantInt) lhs).getValue() % divisor;
            return new ConstantInt((IntegerType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        SRemInst inst = new SRemInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildIntAdd(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            int res = ((ConstantInt) lhs).getValue() + ((ConstantInt) rhs).getValue();
            return new ConstantInt((IntegerType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        AddInst inst = new AddInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildIntSub(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            int res = ((ConstantInt) lhs).getValue() - ((ConstantInt) rhs).getValue();
            return new ConstantInt((IntegerType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        SubInst inst = new SubInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildFloatMul(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            float res = ((ConstantFloat) lhs).getValue() * ((ConstantFloat) rhs).getValue();
            return new ConstantFloat((FloatType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        FMulInst inst = new FMulInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildFloatDiv(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            float divisor = ((ConstantFloat) rhs).getValue();
            float res = ((ConstantFloat) lhs).getValue() / divisor;
            return new ConstantFloat((FloatType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        FDivInst inst = new FDivInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildFloatRem(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            float res = ((ConstantFloat) lhs).getValue() % ((ConstantFloat) rhs).getValue();
            return new ConstantFloat((FloatType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        FRemInst inst = new FRemInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildFloatAdd(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            float res = ((ConstantFloat) lhs).getValue() + ((ConstantFloat) rhs).getValue();
            return new ConstantFloat((FloatType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        FAddInst inst = new FAddInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildFloatSub(Value lhs, Value rhs, String varName) {
        if (lhs.isConstant() && rhs.isConstant()) {
            float res = ((ConstantFloat) lhs).getValue() - ((ConstantFloat) rhs).getValue();
            return new ConstantFloat((FloatType) lhs.getType(), res);
        }
        String name = nameManager.getUniqueName(varName);
        FSubInst inst = new FSubInst(lhs.getType(), name, lhs, rhs);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public PhiInst buildPhi(Type type, String varName) {
        String name = nameManager.getUniqueName(varName);
        PhiInst inst = new PhiInst(type, name);
        currentBlock.addInstruction(inst);
        return inst;
    }

    public Value buildGetElementPtr(Value basePointer, List<Value> indices, String varName) {
        if (!basePointer.getType().isPointerType()) {
            throw new GEPException("GEP base must be a pointer");
        }

        Type elementType = ((PointerType) basePointer.getType()).getPointeeType();
        Type resultType = new PointerType(context, elementType); // GEP 返回的仍然是指针类型

        String name = nameManager.getUniqueName(varName);
        GetElementPtrInst inst = new GetElementPtrInst(resultType, name, basePointer, indices);
        currentBlock.addInstruction(inst);
        return inst;
    }















    public void setCurFunc(Function func) {
        this.currentFunction = func;
    }

    public void addBlock(BasicBlock block) {
        this.currentFunction.addBasicBlock(block);
    }
}
