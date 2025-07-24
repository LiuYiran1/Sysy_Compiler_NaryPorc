package com.compiler.ir;

import com.compiler.frontend.SysYParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import kotlin.Pair;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Module;
import com.compiler.frontend.SysYParser;
import org.llvm4j.optional.Option;

import java.io.File;
import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMConstIntGetSExtValue;

public class LLVisitor extends SysYParserBaseVisitor<Value> {
    private final Context context = new Context();

    private final IRBuilder builder = context.newIRBuilder();

    private final Module mod = context.newModule("module");

    private final IntegerType i1 = context.getInt1Type();

    private final IntegerType i32 = context.getInt32Type();

    private final FloatingPointType f32 = context.getFloatType();

    private final VoidType vo = context.getVoidType();

    private final ConstantInt zero = i1.getConstant(0, false);

    private final ConstantInt one = i1.getConstant(1, false);

    private final ConstantInt intZero = i32.getConstant(0, false);

    private final ConstantInt intOne = i32.getConstant(1, false);

    private final ConstantInt intZero64 = context.getInt64Type().getConstant(0, true);
    /// ///

    private final ConstantFP floatZero = f32.getConstant(0.0);

    private final ConstantFP floatOne = f32.getConstant(1.0);

    private final SymbolTable symbolTable = new SymbolTable();

    private Function curFunc;

    private BasicBlock inBT;

    private final Stack<BasicBlock> breakBlocks = new Stack<>();

    private final Stack<BasicBlock> continueBlocks = new Stack<>();

    private final Map<String, Type> retTypes = new LinkedHashMap<>();

    private final Map<Value, Constant> globalValues = new LinkedHashMap<>();

    public LLVisitor() {
        initRunTimeLibrary();
    }

    private void addFunction(String name, LLVMTypeRef returnType, LLVMTypeRef... params) {
        LLVMTypeRef funcType = LLVMFunctionType(returnType, new PointerPointer<>(params), params.length, 0);
        LLVMValueRef func = LLVMAddFunction(mod.getRef(), name, funcType);
        symbolTable.addSymbol(name, new Function(func));
    }

    private void initRunTimeLibrary() {
        LLVMTypeRef i32 = LLVMInt32TypeInContext(context.getRef());
        LLVMTypeRef f32 = LLVMFloatTypeInContext(context.getRef());
        LLVMTypeRef voidTy = LLVMVoidTypeInContext(context.getRef());

        // int getint();
        addFunction("getint", i32);

        // int getch();
        addFunction("getch", i32);

        // float getfloat();
        addFunction("getfloat", f32);

        // int getarray(int[])
        addFunction("getarray", i32, LLVMPointerType(i32, 0));

        // int getfarray(float[])
        addFunction("getfarray", i32, LLVMPointerType(f32, 0));

        // void putint(int)
        addFunction("putint", voidTy, i32);

        // void putch(int)
        addFunction("putch", voidTy, i32);

        // void putfloat(float)
        addFunction("putfloat", voidTy, f32);

        // void putarray(int, int[])
        addFunction("putarray", voidTy, i32, LLVMPointerType(i32, 0));

        // void putfarray(int, float[])
        addFunction("putfarray", voidTy, i32, LLVMPointerType(f32, 0));

        // void starttime()
        addFunction("starttime", voidTy);

        // void stoptime()
        addFunction("stoptime", voidTy);
    }

    public void LOG(String msg) {
        //System.out.println(msg);
    }

    public void dump(Option<File> of) {

        for (LLVMValueRef func = LLVMGetFirstFunction(mod.getRef()); func != null; func = LLVMGetNextFunction(func)) {
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                if (LLVMGetBasicBlockTerminator(bb) == null) {
                    builder.positionAfter(new BasicBlock(bb));
                    String funcName = LLVMGetValueName(func).getString();
                    Type retType = retTypes.get(funcName);
                    if (retType.isIntegerType()) {
                        builder.buildReturn(Option.of(intZero));
                    } else if (retType.isVoidType()) {
                        builder.buildReturn(Option.empty());
                    } else if (retType.isFloatingPointType()) {
                        builder.buildReturn(Option.of(floatZero));
                    } else {
                        throw new RuntimeException("Unknown return type: " + retType);
                    }

                }
            }
        }

        // 遍历所有指令,消除终止指令后的冗余指令
        List<LLVMValueRef> DCE = new ArrayList<>();
        List<LLVMBasicBlockRef> DBE = new ArrayList<>();
        for (LLVMValueRef func = LLVMGetFirstFunction(mod.getRef()); func != null; func = LLVMGetNextFunction(func)) {
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                boolean terminatorFlag = false;
                if (LLVMGetFirstInstruction(bb) == null) {
                    DBE.add(bb);
                    continue;
                }
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                    int opcode = LLVMGetInstructionOpcode(inst);
                    if (terminatorFlag) {
                        DCE.add(inst);
                    }
                    if ((opcode == LLVMRet || opcode == LLVMBr) && !terminatorFlag) {
                        terminatorFlag = true;
                    }
                }
            }
        }
        for (var code : DCE) {
            LLVMInstructionEraseFromParent(code);
        }
        for (var bb : DBE) {
            LLVMDeleteBasicBlock(bb);
        }

//        // ============= mem2reg优化 =============
//
//        String targetTriple = "riscv64-unknown-elf";
//        String dataLayout = "e-m:e-p:64:64-i64:64-i128:128-n64-S128";
//        LLVMSetTarget(mod.getRef(), targetTriple);
//        LLVMSetDataLayout(mod.getRef(), dataLayout);
//
//        LLVMPassManagerRef passManager = LLVMCreateFunctionPassManagerForModule(mod.getRef());
//
//        LLVMAddBasicAliasAnalysisPass(passManager);
//        LLVMAddTypeBasedAliasAnalysisPass(passManager);
//
//        LLVMAddPromoteMemoryToRegisterPass(passManager);  // mem2reg - 提升栈变量到寄存器
//        LLVMAddDCEPass(passManager);                      // 死代码消除 - 移除无用指令
//        LLVMAddCFGSimplificationPass(passManager);         // 控制流图简化 - 消除不可达块
//
//        LLVMInitializeFunctionPassManager(passManager);
//
//        for (LLVMValueRef func = LLVMGetFirstFunction(mod.getRef()); func != null; func = LLVMGetNextFunction(func)) {
//            if (LLVMCountBasicBlocks(func) > 0) {
//                LLVMRunFunctionPassManager(passManager, func);
//                System.out.println(LLVMGetValueName(func).getString());
//            }
//        }
//        LLVMFinalizeFunctionPassManager(passManager);
//        LLVMDisposePassManager(passManager);


        mod.dump(of);
    }

    @Override
    public Value visitProgram(SysYParser.ProgramContext ctx) {
        return super.visitProgram(ctx);
    }

    @Override
    public Value visitCompUnit(SysYParser.CompUnitContext ctx) {
        return super.visitCompUnit(ctx);
    }

    @Override
    public Value visitDecl(SysYParser.DeclContext ctx) {
        return super.visitDecl(ctx);
    }

    @Override
    public Value visitConstDecl(SysYParser.ConstDeclContext ctx) {
        LOG("visitConstDecl");
        String typeStr = ctx.bType().getText();
        Type type = switch (typeStr) {
            case "int" -> context.getInt32Type();
            case "float" -> context.getFloatType();
            default -> throw new RuntimeException("Unknown type: " + typeStr);
        };
        for (var constDef : ctx.constDef()) {
            myVisitConstDef(constDef, type);
        }
        return null;
    }

    public void myVisitConstDef(SysYParser.ConstDefContext ctx, Type type) {
        LOG("myVisitConstDef");
        if (ctx.L_BRACKT().isEmpty()) { // 普通变量
            String varName = ctx.IDENT().getText();
            Value init = visitConstInitVal(ctx.constInitVal());

            if (symbolTable.isBottom()) { // 全局变量
                if (type.isIntegerType()) {
                    var globalVar = mod.addGlobalVariable(varName, i32, Option.empty()).unwrap();

                    globalVar.setInitializer(calConstInt(init));
                    symbolTable.addSymbol(varName, globalVar);
                    globalValues.put(globalVar, calConstInt(init));
                } else if (type.isFloatingPointType()) {
                    var globalVar = mod.addGlobalVariable(varName, f32, Option.empty()).unwrap();

                    globalVar.setInitializer(calConstFloat(init));
                    symbolTable.addSymbol(varName, globalVar);
                    globalValues.put(globalVar, calConstFloat(init));
                } else {
                    throw new RuntimeException("Unknown type: " + type);
                }

            } else { // 局部变量
                if (type.isIntegerType()) {
                    Value alloc = builder.buildAlloca(i32, Option.of(varName));

                    if (init.getType().isFloatingPointType()) {
                        init = builder.buildFloatToSigned(init, i32, Option.of("iInit"));
                    }
                    builder.buildStore(alloc, init);

                    symbolTable.addSymbol(varName, alloc);
                } else if (type.isFloatingPointType()) {
                    Value alloc = builder.buildAlloca(f32, Option.of(varName));

                    if (init.getType().isIntegerType()) {
                        init = builder.buildSignedToFloat(init, f32, Option.of("fInit"));
                    }
                    builder.buildStore(alloc, init);

                    symbolTable.addSymbol(varName, alloc);
                } else {
                    throw new RuntimeException("Unknown type: " + type);
                }

            }

        } else { // 数组

            String varName = ctx.IDENT().getText();

            if (symbolTable.isBottom()) {
                // 获取所有维数信息
                List<Integer> dimensions = new LinkedList<>();
                for (var constExp : ctx.constExp()) {
                    long dim = LLVMConstIntGetSExtValue(calConstInt(visitConstExp(constExp)).getRef());
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i)).unwrap();
                }
                // 处理数组赋值
                var globalVar = mod.addGlobalVariable(varName, arrayType, Option.empty()).unwrap();
                if (ctx.ASSIGN() != null) {
                    int memSize = 1;
                    for (int dim : dimensions) {
                        memSize *= dim;
                    }
                    Value[] mem = new Value[memSize + 5];
                    for (int i = 0; i < mem.length; i++) {
                        mem[i] = type.isIntegerType() ? intZero : floatZero;
                    }
                    myVisitConstInitVal(ctx.constInitVal(), mem, 0, dimensions, memSize);
                    for (var m : mem) {
                        //System.out.println(m.getAsString());
                    }

                    LLVMValueRef[] newMem = new LLVMValueRef[mem.length];
                    for (int i = 0; i < mem.length; i++) {
                        newMem[i] = type.isIntegerType() ? calConstInt(mem[i]).getRef() : calConstFloat(mem[i]).getRef();
                    }

                    LLVMValueRef initializer = buildNestedArray(context.getRef(), newMem, dimensions, type.getRef());
                    LLVMSetInitializer(globalVar.getRef(), initializer);

                } else {
                    globalVar.setInitializer(arrayType.getConstantArray()); /////
                }

                // 存符号表
                symbolTable.addSymbol(varName, globalVar);
            } else {
                // 获取所有维数信息
                List<Integer> dimensions = new LinkedList<>();
                for (var constExp : ctx.constExp()) {
                    long dim = LLVMConstIntGetSExtValue(calConstInt(visitConstExp(constExp)).getRef());
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i)).unwrap();
                }
                Value ptr = builder.buildAlloca(arrayType, Option.of(varName + "Arr"));
                symbolTable.addSymbol(varName, ptr);
                if (ctx.ASSIGN() != null) {
                    int memSize = 1;
                    for (int dim : dimensions) {
                        memSize *= dim;
                    }
                    Value[] mem = new Value[memSize + 5];
                    for (int i = 0; i < mem.length; i++) {
                        mem[i] = type.isIntegerType() ? intZero : floatZero;
                    }
                    myVisitConstInitVal(ctx.constInitVal(), mem, 0, dimensions, memSize);
                    for (int i = 0; i < mem.length; i++) {
                        if (type.isIntegerType()) {
                            if (mem[i].getType().isFloatingPointType()) {
                                mem[i] = builder.buildFloatToSigned(mem[i], i32, Option.of("iArr"));
                            }
                        } else if (type.isFloatingPointType()) {
                            if (mem[i].getType().isIntegerType()) {
                                mem[i] = builder.buildSignedToFloat(mem[i], f32, Option.of("fArr"));
                            }
                        }
                    }
                    // 以线性的方式生成数组
                    LLVMTypeRef linearPtrType = LLVMPointerType(type.getRef(), 0);  // address space 0
                    // 将多维数组压成一维
                    LLVMValueRef linearPtr = LLVMBuildBitCast(builder.getRef(), ptr.getRef(), linearPtrType, varName + "FlatPtr");
                    for (int i = 0; i < memSize; i++) {
                        LLVMValueRef idx = LLVMConstInt(LLVMInt64Type(), i, 0); // i64 index
                        PointerPointer<Pointer> indices = new PointerPointer<>(1).put(0, idx);
                        LLVMValueRef gep = LLVMBuildGEP(builder.getRef(), linearPtr, indices, 1, varName + "Elem" + i);
                        LLVMBuildStore(builder.getRef(), mem[i].getRef(), gep);
                    }
                }

            }

        }
    }

    private void myVisitConstInitVal(SysYParser.ConstInitValContext cxt, Value[] mem, int index, List<Integer> dimensions, int h) {
        LOG("myVisitInitVal");
        if (cxt.constExp() != null) {
            mem[index] = visitConstExp(cxt.constExp());
            return;
        }
        h = h / dimensions.get(0);

        List<Integer> subDims = new LinkedList<>(dimensions);
        subDims.remove(0);
        for (int i = 0; i < cxt.constInitVal().size(); i++) {

            myVisitConstInitVal(cxt.constInitVal(i), mem, index, subDims, h);
            index = cxt.constInitVal(i).constExp() == null ? index + h : index + 1;
        }
    }

    @Override
    public Value visitConstDef(SysYParser.ConstDefContext ctx) {
        return super.visitConstDef(ctx);
    }

    @Override
    public Value visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        return super.visitConstInitVal(ctx);
    }

    @Override
    public Value visitVarDecl(SysYParser.VarDeclContext ctx) {
        LOG("visitVarDecl");
        String typeStr = ctx.bType().getText();
        Type type = switch (typeStr) {
            case "int" -> context.getInt32Type();
            case "float" -> context.getFloatType();
            default -> throw new RuntimeException("Unknown type: " + typeStr);
        };
        for (var varDef : ctx.varDef()) {
            myVisitVarDef(varDef, type);
        }
        return null;
    }

    private Constant calConstInt(Value init) {
        if (init.getType().isFloatingPointType()) {
            init = builder.buildFloatToSigned(init, i32, Option.of("iInit"));
        }
        return new ConstantInt(LLVMConstInt(LLVMInt32Type(), LLVMConstIntGetSExtValue(init.getRef()), 0));
    }

    private Constant calConstFloat(Value init) {
        if (init.getType().isIntegerType()) {
            init = builder.buildSignedToFloat(init, f32, Option.of("fInit"));
        }
        return new ConstantFP(LLVMConstReal(LLVMFloatType(), LLVMConstRealGetDouble(init.getRef(), new IntPointer(0))));
    }

    public void myVisitVarDef(SysYParser.VarDefContext ctx, Type type) {
        LOG("myVisitVarDef");
        if (ctx.L_BRACKT().isEmpty()) { // 普通变量
            String varName = ctx.IDENT().getText();

            if (symbolTable.isBottom()) { // 全局变量
                if (type.isIntegerType()) {
                    var globalVar = mod.addGlobalVariable(varName, i32, Option.empty()).unwrap();
                    if (ctx.ASSIGN() != null) {
                        Value init = visitInitVal(ctx.initVal());

                        globalVar.setInitializer(calConstInt(init));
                    } else {
                        globalVar.setInitializer(intZero);
                    }
                    symbolTable.addSymbol(varName, globalVar);
                } else if (type.isFloatingPointType()) {
                    var globalVar = mod.addGlobalVariable(varName, f32, Option.empty()).unwrap();
                    if (ctx.ASSIGN() != null) {
                        Value init = visitInitVal(ctx.initVal());

                        globalVar.setInitializer(calConstFloat(init));
                    } else {
                        globalVar.setInitializer(floatZero);
                    }
                    symbolTable.addSymbol(varName, globalVar);
                } else {
                    throw new RuntimeException("Unknown type: " + type);
                }

            } else { // 局部变量
                if (type.isIntegerType()) {
                    Value alloc = builder.buildAlloca(i32, Option.of(varName));
                    if (ctx.ASSIGN() != null) {
                        Value init = visitInitVal(ctx.initVal());
                        if (init.getType().isFloatingPointType()) {
                            init = builder.buildFloatToSigned(init, i32, Option.of("iInit"));
                        }
                        builder.buildStore(alloc, init);
                    } else {
                        builder.buildStore(alloc, intZero);
                    }
                    symbolTable.addSymbol(varName, alloc);
                } else if (type.isFloatingPointType()) {
                    Value alloc = builder.buildAlloca(f32, Option.of(varName));
                    if (ctx.ASSIGN() != null) {
                        Value init = visitInitVal(ctx.initVal());
                        if (init.getType().isIntegerType()) {
                            init = builder.buildSignedToFloat(init, f32, Option.of("fInit"));
                        }
                        builder.buildStore(alloc, init);
                    } else {
                        builder.buildStore(alloc, floatZero);
                    }
                    symbolTable.addSymbol(varName, alloc);
                } else {
                    throw new RuntimeException("Unknown type: " + type);
                }

            }

        } else { // 数组
            String varName = ctx.IDENT().getText();

            if (symbolTable.isBottom()) {
                // 获取所有维数信息
                List<Integer> dimensions = new LinkedList<>();
                for (var constExp : ctx.constExp()) {
                    long dim = LLVMConstIntGetSExtValue(calConstInt(visitConstExp(constExp)).getRef());
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i)).unwrap();
                }
                // 处理数组赋值
                var globalVar = mod.addGlobalVariable(varName, arrayType, Option.empty()).unwrap();
                if (ctx.ASSIGN() != null) {
                    int memSize = 1;
                    for (int dim : dimensions) {
                        memSize *= dim;
                    }
                    Value[] mem = new Value[memSize + 5];
                    for (int i = 0; i < mem.length; i++) {
                        mem[i] = type.isIntegerType() ? intZero : floatZero;
                    }
                    myVisitInitVal(ctx.initVal(), mem, 0, dimensions, memSize);
                    for (var m : mem) {
                        //System.out.println(m.getAsString());
                    }

                    LLVMValueRef[] newMem = new LLVMValueRef[mem.length];
                    for (int i = 0; i < mem.length; i++) {
                        newMem[i] = type.isIntegerType() ? calConstInt(mem[i]).getRef() : calConstFloat(mem[i]).getRef();
                    }

                    LLVMValueRef initializer = buildNestedArray(context.getRef(), newMem, dimensions, type.getRef());
                    LLVMSetInitializer(globalVar.getRef(), initializer);

                } else {
                    globalVar.setInitializer(arrayType.getConstantArray()); /////
                }

                // 存符号表
                symbolTable.addSymbol(varName, globalVar);
            } else {
                // 获取所有维数信息
                List<Integer> dimensions = new LinkedList<>();
                for (var constExp : ctx.constExp()) {
                    long dim = LLVMConstIntGetSExtValue(calConstInt(visitConstExp(constExp)).getRef());
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i)).unwrap();
                }
                Value ptr = builder.buildAlloca(arrayType, Option.of(varName + "Arr"));
                symbolTable.addSymbol(varName, ptr);
                if (ctx.ASSIGN() != null) {
                    int memSize = 1;
                    for (int dim : dimensions) {
                        memSize *= dim;
                    }
                    Value[] mem = new Value[memSize + 5];
                    for (int i = 0; i < mem.length; i++) {
                        mem[i] = type.isIntegerType() ? intZero : floatZero;
                    }
                    myVisitInitVal(ctx.initVal(), mem, 0, dimensions, memSize);
                    for (int i = 0; i < mem.length; i++) {
                        if (type.isIntegerType()) {
                            if (mem[i].getType().isFloatingPointType()) {
                                mem[i] = builder.buildFloatToSigned(mem[i], i32, Option.of("iArr"));
                            }
                        } else if (type.isFloatingPointType()) {
                            if (mem[i].getType().isIntegerType()) {
                                mem[i] = builder.buildSignedToFloat(mem[i], f32, Option.of("fArr"));
                            }
                        }
                    }
                    // 以线性的方式生成数组
                    LLVMTypeRef linearPtrType = LLVMPointerType(type.getRef(), 0);  // address space 0
                    // 将多维数组压成一维
                    LLVMValueRef linearPtr = LLVMBuildBitCast(builder.getRef(), ptr.getRef(), linearPtrType, varName + "FlatPtr");
                    for (int i = 0; i < memSize; i++) {
                        LLVMValueRef idx = LLVMConstInt(LLVMInt64Type(), i, 0); // i64 index
                        PointerPointer<Pointer> indices = new PointerPointer<>(1).put(0, idx);
                        LLVMValueRef gep = LLVMBuildGEP(builder.getRef(), linearPtr, indices, 1, varName + "Elem" + i);
                        LLVMBuildStore(builder.getRef(), mem[i].getRef(), gep);
                    }
                }

            }

        }
    }

    private LLVMValueRef buildNestedArray(LLVMContextRef context, LLVMValueRef[] flat, List<Integer> dims, LLVMTypeRef elemType) {
        return buildArrayRecursive(context, flat, dims, 0, elemType).result;
    }

    private static class ArrayBuildResult {
        LLVMValueRef result;
        int nextIndex;

        ArrayBuildResult(LLVMValueRef result, int nextIndex) {
            this.result = result;
            this.nextIndex = nextIndex;
        }
    }

    private ArrayBuildResult buildArrayRecursive(LLVMContextRef context, LLVMValueRef[] flat, List<Integer> dims, int startIndex, LLVMTypeRef elemType) {
        int dim = dims.get(0);

        if (dims.size() == 1) {
            // 最后一维，直接构造常量数组
            LLVMValueRef[] slice = new LLVMValueRef[dim];
            for (int i = 0; i < dim; i++) {
                slice[i] = flat[startIndex + i];
            }
            LLVMTypeRef arrayType = LLVMArrayType(elemType, dim);
            LLVMValueRef constArray = LLVMConstArray(elemType, new PointerPointer<>(slice), dim);
            return new ArrayBuildResult(constArray, startIndex + dim);
        } else {
            // 多维，递归构造
            List<Integer> subDims = dims.subList(1, dims.size());

            LLVMTypeRef subType = elemType;
            for (int i = subDims.size() - 1; i >= 0; i--) {
                subType = LLVMArrayType(subType, subDims.get(i));
            }

            LLVMValueRef[] subArrays = new LLVMValueRef[dim];
            int currentIndex = startIndex;
            for (int i = 0; i < dim; i++) {
                ArrayBuildResult sub = buildArrayRecursive(context, flat, subDims, currentIndex, elemType);
                subArrays[i] = sub.result;
                currentIndex = sub.nextIndex;
            }

            LLVMTypeRef arrayType = LLVMArrayType(LLVMTypeOf(subArrays[0]), dim);
            LLVMValueRef constArray = LLVMConstArray(LLVMTypeOf(subArrays[0]), new PointerPointer<>(subArrays), dim);
            return new ArrayBuildResult(constArray, currentIndex);
        }
    }


//    @Override
//    public Value visitVarDef(SysYParser.VarDefContext ctx) {
//        return super.visitVarDef(ctx);
//    }

    @Override
    public Value visitInitVal(SysYParser.InitValContext ctx) {
        LOG("visitInitVal");
        if (ctx.exp() != null) {
            return visitExp(ctx.exp());
        } else {
            return null;
        }
    }

    public void myVisitInitVal(SysYParser.InitValContext ctx, Value[] mem, int index, List<Integer> dimensions, int h) {
        LOG("myVisitInitVal");
        if (ctx.exp() != null) {
            mem[index] = visitExp(ctx.exp());
            return;
        }
        h = h / dimensions.get(0);
        ///System.out.println("h = " + h);
        List<Integer> subDims = new LinkedList<>(dimensions);
        subDims.remove(0);
        for (int i = 0; i < ctx.initVal().size(); i++) {

            myVisitInitVal(ctx.initVal(i), mem, index, subDims, h);
            index = ctx.initVal(i).exp() == null ? index + h : index + 1;
        }
        for (var m : mem) {
            //System.out.println(m.getAsString() + "h: " + h);
        }
    }

    @Override
    public Value visitFuncDef(SysYParser.FuncDefContext ctx) {
        // 函数返回值
        Type retType = null;
        if (ctx.funcType().getText().equals("void")) {
            retType = context.getVoidType();
        } else if (ctx.funcType().getText().equals("float")) {
            retType = context.getFloatType();
        } else if (ctx.funcType().getText().equals("int")) {
            retType = context.getInt32Type();
        }
        // 函数参数解析
        Type[] paramTypes = null;
        String[] paramNames = null;
        if (ctx.funcFParams() != null) {
            paramTypes = new Type[ctx.funcFParams().funcFParam().size()];
            paramNames = new String[paramTypes.length];
        } else {
            paramTypes = new Type[]{};
        }

        // ！处理普通/数组参数
        for (int i = 0; i < paramTypes.length; i++) {
            LLVMTypeRef llvmType = myVisitFuncFParam(ctx.funcFParams().funcFParam().get(i));
            paramTypes[i] = new Type(llvmType);
            paramNames[i] = ctx.funcFParams().funcFParam().get(i).IDENT().getText();
        }

        String funcName = ctx.IDENT().getText();

        Function function = mod.addFunction(funcName, context.getFunctionType(retType, paramTypes, false));

        symbolTable.addSymbol(funcName, function);
        retTypes.put(funcName, retType);
        curFunc = function;

        BasicBlock funcEntry = context.newBasicBlock(funcName + "Entry");
        function.addBasicBlock(funcEntry);
        builder.positionAfter(funcEntry);

        // 进入作用域
        symbolTable.enterScope();
        // 将参数加入到符号表中
        // 数组
        Value[] params = function.getParameters();
        for (int i = 0; i < paramTypes.length; i++) {
            Value alloc = builder.buildAlloca(paramTypes[i], Option.of(paramNames[i]));
            builder.buildStore(alloc, params[i]);
            symbolTable.addSymbol(paramNames[i], alloc);
        }

        visitBlock(ctx.block());

        symbolTable.exitScope();

        return null;
    }

    @Override
    public Value visitFuncType(SysYParser.FuncTypeContext ctx) {
        return super.visitFuncType(ctx);
    }

    @Override
    public Value visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        return super.visitFuncFParams(ctx);
    }

    public LLVMTypeRef myVisitFuncFParam(SysYParser.FuncFParamContext ctx) {
        LOG("myVisitFuncFParam");
        Type baseType;
        if (ctx.bType().getText().equals("int")) {
            baseType = context.getInt32Type();
        } else {
            baseType = context.getFloatType();
        }

        // 是数组参数
        if (!ctx.L_BRACKT().isEmpty()) {
            // 跳过第一个 []，其维度在函数参数里可以省略
            Type curType = baseType;
            List<SysYParser.ExpContext> exps = ctx.exp();
            if (exps != null && !exps.isEmpty()) {
                for (int i = 0; i < exps.size(); i++) {
                    long dim = LLVMConstIntGetSExtValue(calConstInt(visitExp(exps.get(i))).getRef());
                    curType = context.getArrayType(curType, (int) dim).unwrap();
                }
            }

            return LLVMPointerType(curType.getRef(), 0);
        }

        return baseType.getRef();
    }

    @Override
    public Value visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        return super.visitFuncFParam(ctx);
    }

    @Override
    public Value visitBlock(SysYParser.BlockContext ctx) {
        return super.visitBlock(ctx);
    }

    @Override
    public Value visitBlockItem(SysYParser.BlockItemContext ctx) {
        return super.visitBlockItem(ctx);
    }

    @Override
    public Value visitStmt(SysYParser.StmtContext ctx) {
        LOG("visitStmt");
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                Value retVal = visit(ctx.exp());
                Type retType = retVal.getType();

                Type curFuncRetType = retTypes.get(curFunc.getName());

                // 处理隐式转换
                if (retType.isIntegerType() && curFuncRetType.isFloatingPointType()) {
                    retVal = builder.buildSignedToFloat(retVal, f32, Option.of("fRet"));
                } else if (retType.isFloatingPointType() && curFuncRetType.isIntegerType()) {
                    retVal = builder.buildFloatToSigned(retVal, i32, Option.of("iRet"));
                }

//                // 构建ret指令（若基本块有终结指令则不构建）
//                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
//                if (LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
//                    builder.buildReturn(Option.of(retVal));
//                }
                builder.buildReturn(Option.of(retVal));
            } else { // return void
//                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
//                if (LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
//                    builder.buildReturn(Option.empty());
//                }
                builder.buildReturn(Option.empty());
            }
        } else if (ctx.lVal() != null) {
            Value rVal = visitExp(ctx.exp());
            String lValName = ctx.lVal().IDENT().getText();
            Value lValAddr = symbolTable.getSymbol(lValName);
            PointerType pVarType = new PointerType(lValAddr.getType().getRef());
            Type varType = pVarType.getElementType();
            if (varType.isPointerType() || varType.isArrayType()) {
                ArrayType arrayType = new ArrayType(varType.getRef());
                if (rVal.getType().isFloatingPointType() && arrayType.getElementType().isIntegerType()) {
                    rVal = builder.buildFloatToSigned(rVal, i32, Option.of("iLVar"));
                } else if (rVal.getType().isIntegerType() && arrayType.getElementType().isFloatingPointType()) {
                    rVal = builder.buildSignedToFloat(rVal, f32, Option.of("fLVar"));
                }
                Value gep = visitLVal(ctx.lVal());
                builder.buildStore(gep, rVal);
            } else {
                if (rVal.getType().isFloatingPointType() && varType.isIntegerType()) {
                    rVal = builder.buildFloatToSigned(rVal, i32, Option.of("iLVar"));
                } else if (rVal.getType().isIntegerType() && varType.isFloatingPointType()) {
                    rVal = builder.buildSignedToFloat(rVal, f32, Option.of("fLVar"));
                }
                builder.buildStore(lValAddr, rVal);
            }
        } else if (ctx.block() != null) {
            symbolTable.enterScope();
            visitBlock(ctx.block());
            symbolTable.exitScope();
        } else if (ctx.IF() != null) {
            BasicBlock ifTrue = context.newBasicBlock("ifTrue");
            BasicBlock ifFalse = context.newBasicBlock("ifFalse");
            BasicBlock ifNext = context.newBasicBlock("ifNext");

            curFunc.addBasicBlock(ifTrue);
            curFunc.addBasicBlock(ifFalse);
            curFunc.addBasicBlock(ifNext);

            var cond = visitCond(ctx.cond());

            if (cond.getType().isIntegerType()) {
                cond = builder.buildIntCompare(IntPredicate.NotEqual, cond, intZero, Option.of("cond"));
            } else if (cond.getType().isFloatingPointType()) {
                cond = builder.buildFloatCompare(FloatPredicate.OrderedNotEqual, cond, floatZero, Option.of("cond"));
            } else {
                throw new RuntimeException("type not supported");
            }

            builder.buildConditionalBranch(cond, ifTrue, ifFalse);

            builder.positionAfter(ifTrue);
            visitStmt(ctx.stmt(0));
            builder.buildBranch(ifNext);

            builder.positionAfter(ifFalse);
            if (ctx.ELSE() != null) {
                visitStmt(ctx.stmt(1));
            }
            builder.buildBranch(ifNext);

            builder.positionAfter(ifNext);
        } else if (ctx.WHILE() != null) {

            BasicBlock whileCond = context.newBasicBlock("whileCond");
            BasicBlock whileBody = context.newBasicBlock("whileBody");
            BasicBlock whileNext = context.newBasicBlock("whileNext");

            curFunc.addBasicBlock(whileCond);
            curFunc.addBasicBlock(whileBody);
            curFunc.addBasicBlock(whileNext);

            breakBlocks.push(whileNext);
            continueBlocks.push(whileCond);

            builder.buildBranch(whileCond);
            builder.positionAfter(whileCond);

            // 保证visitCond返回的一定是i32的0或1,哎，保证不了，还得判断类型

            var cond = visitCond(ctx.cond());

            if (cond.getType().isIntegerType()) {
                cond = builder.buildIntCompare(IntPredicate.NotEqual, cond, intZero, Option.of("cond"));
            } else if (cond.getType().isFloatingPointType()) {
                cond = builder.buildFloatCompare(FloatPredicate.OrderedNotEqual, cond, floatZero, Option.of("cond"));
            } else {
                throw new RuntimeException("type not supported");
            }
            builder.buildConditionalBranch(cond, whileBody, whileNext);

            builder.positionAfter(whileBody);
            visitStmt(ctx.stmt(0));
            builder.buildBranch(whileCond);

            builder.positionAfter(whileNext);

            breakBlocks.pop();
            continueBlocks.pop();
        } else if (ctx.BREAK() != null) {
            BasicBlock basicBlock = breakBlocks.peek();
            builder.buildBranch(basicBlock);
        } else if (ctx.CONTINUE() != null) {
            BasicBlock basicBlock = continueBlocks.peek();
            builder.buildBranch(basicBlock);
        } else {
            //处理 1 + 2 + 3；
            if (ctx.exp() != null) {
                visitExp(ctx.exp());
            }
        }
        return null;
    }


    @Override
    public Value visitExp(SysYParser.ExpContext ctx) {
        LOG("visitExp");
        if (ctx.lVal() != null) {
            return visitLVal(ctx.lVal());

        } else if (ctx.number() != null) {
            return visitNumber(ctx.number());
        } else if (ctx.IDENT() != null) { // 函数调用
            String funcName = ctx.IDENT().getText();
            if (!((symbolTable.getSymbol(funcName)) instanceof Function)) {
                throw new RuntimeException("Function not found: " + funcName);
            }

            Function function = (Function) symbolTable.getSymbol(funcName);
            Value[] params = new Value[function.getParameterCount()];

            if (ctx.funcRParams() != null) {
                for (int i = 0; i < params.length; i++) {
                    params[i] = visit(ctx.funcRParams().param(i));
                }
            }

            Value funcRet = builder.buildCall(function, params, Option.empty());

            return funcRet;
        } else if (ctx.unaryOp() != null) {
            String op = ctx.unaryOp().getText();
            Value value = visit(ctx.exp(0));
            Type type = value.getType();

            switch (op) {
                case "+":
                    return value;
                case "-":
                    if (type.isIntegerType()) {
                        return builder.buildIntSub(intZero, value, WrapSemantics.Unspecified, Option.of("neg_int")); // 注意 WrapSemantics 对应的参数是什么意思
                    } else if (type.isFloatingPointType()) {
                        return builder.buildFloatSub(floatZero, value, Option.of("neg_float"));
                    }
                    break;
                case "!":
                    if (type.isIntegerType()) {
                        Value cmp = builder.buildIntCompare(IntPredicate.Equal, value, intZero, Option.of("cmpI1"));
                        Value cmp32 = builder.buildZeroExt(cmp, i32, Option.of("cmpI32"));
                        return cmp32;
                    } else if (type.isFloatingPointType()) {
                        Value cmp = builder.buildFloatCompare(FloatPredicate.OrderedEqual, value, floatZero, Option.of("cmpI1"));
                        Value cmp32 = builder.buildZeroExt(cmp, i32, Option.of("cmpI32"));
                        return cmp32;
                    }
                    break;
                default:
                    throw new RuntimeException("unaryOp error");
            }

        } else if (ctx.L_PAREN() != null) {
            return visitExp(ctx.exp(0));
        } else if (ctx.exp().size() == 2) {
            Value left = visit(ctx.exp(0));
            Value right = visit(ctx.exp(1));

            Type leftType = left.getType();
            Type rightType = right.getType();

            if (leftType.isIntegerType() && rightType.isFloatingPointType()) {
                left = builder.buildSignedToFloat(left, f32, Option.of("lIToF"));
                leftType = left.getType();
            } else if (leftType.isFloatingPointType() && rightType.isIntegerType()) {
                right = builder.buildSignedToFloat(right, f32, Option.of("rIToF"));
                rightType = right.getType();
            }

            if (leftType.isIntegerType() && rightType.isIntegerType()) {
                if (ctx.MUL() != null) {
                    return builder.buildIntMul(left, right, WrapSemantics.Unspecified, Option.of("iMul"));
                } else if (ctx.DIV() != null) {
                    return builder.buildSignedDiv(left, right, false, Option.of("iDiv")); // 第三个参数表示除法是否要精确，注意
                } else if (ctx.MOD() != null) {
                    return builder.buildSignedRem(left, right, Option.of("iRem")); // 取余要signed吗???
                } else if (ctx.PLUS() != null) {
                    return builder.buildIntAdd(left, right, WrapSemantics.Unspecified, Option.of("iAdd"));
                } else if (ctx.MINUS() != null) {
                    return builder.buildIntSub(left, right, WrapSemantics.Unspecified, Option.of("iSub"));
                } else {
                    throw new RuntimeException("exp binaryOp error");
                }
            } else if (leftType.isFloatingPointType() && rightType.isFloatingPointType()) {
                if (ctx.MUL() != null) {
                    return builder.buildFloatMul(left, right, Option.of("fMul"));
                } else if (ctx.DIV() != null) {
                    return builder.buildFloatDiv(left, right, Option.of("fDiv"));
                } else if (ctx.MOD() != null) {
                    return builder.buildFloatRem(left, right, Option.of("fRem"));
                } else if (ctx.PLUS() != null) {
                    return builder.buildFloatAdd(left, right, Option.of("fAdd"));
                } else if (ctx.MINUS() != null) {
                    return builder.buildFloatSub(left, right, Option.of("fSub"));
                } else {
                    throw new RuntimeException("exp binaryOp error");
                }
            } else {
                throw new RuntimeException("exp binary type error");
            }

        } else {
            throw new RuntimeException("visitExp error");
        }
        return null;
    }

    /*
    下面需要做特殊处理的原因是，||优先级低，当出现a||b&&c的情况，会在进入到||的计算时先计算（b&&c)导致
     */
    @Override
    public Value visitCond(SysYParser.CondContext ctx) {
        LOG("visitCond");
        if (ctx.exp() != null) {
            return visit(ctx.exp());
        } else if (ctx.AND() != null) {
            Value left = visitCond(ctx.cond(0));
            BasicBlock andTrue = context.newBasicBlock("andTrue");
            BasicBlock andFalse = context.newBasicBlock("andFalse");
            BasicBlock andNext = context.newBasicBlock("andNext");

            curFunc.addBasicBlock(andTrue);
            curFunc.addBasicBlock(andFalse);
            curFunc.addBasicBlock(andNext);

            inBT = andNext; //

            if (left.getType().isIntegerType()) {
                left = builder.buildIntCompare(IntPredicate.NotEqual, left, intZero, Option.of("cond"));
            } else if (left.getType().isFloatingPointType()) {
                left = builder.buildFloatCompare(FloatPredicate.OrderedNotEqual, left, floatZero, Option.of("cond"));
            } else {
                throw new RuntimeException("type error");
            }

            builder.buildConditionalBranch(left, andTrue, andFalse);

            builder.positionAfter(andTrue);
            Value right = visitCond(ctx.cond(1));
            builder.buildBranch(andNext);

            builder.positionAfter(andFalse);
            builder.buildBranch(andNext);

            builder.positionAfter(andNext);

            if (right.getType().isIntegerType()) {
                var phi = builder.buildPhi(i32, Option.of("andPhi"));

                phi.addIncoming(new Pair<>(andFalse, intZero));
                phi.addIncoming(new Pair<>(andTrue, right));
                return phi;
            } else if (right.getType().isFloatingPointType()) {
                var phi = builder.buildPhi(f32, Option.of("andPhi"));

                phi.addIncoming(new Pair<>(andFalse, floatZero));
                phi.addIncoming(new Pair<>(andTrue, right));
                return phi;
            } else {
                throw new RuntimeException("type error");
            }

        } else if (ctx.OR() != null) {
            Value left = visitCond(ctx.cond(0));
            inBT = null;
            BasicBlock orTrue = context.newBasicBlock("orTrue");
            BasicBlock orFalse = context.newBasicBlock("orFalse");
            BasicBlock orNext = context.newBasicBlock("orNext");

            curFunc.addBasicBlock(orTrue);
            curFunc.addBasicBlock(orFalse);
            curFunc.addBasicBlock(orNext);

            if (left.getType().isIntegerType()) {
                left = builder.buildIntCompare(IntPredicate.NotEqual, left, intZero, Option.of("cond"));
            } else if (left.getType().isFloatingPointType()) {
                left = builder.buildFloatCompare(FloatPredicate.OrderedNotEqual, left, floatZero, Option.of("cond"));
            } else {
                throw new RuntimeException("type error");
            }

            builder.buildConditionalBranch(left, orTrue, orFalse);

            builder.positionAfter(orTrue);
            builder.buildBranch(orNext);

            builder.positionAfter(orFalse);
            Value right = visitCond(ctx.cond(1));

            builder.buildBranch(orNext);
            builder.positionAfter(orNext);

            if (right.getType().isIntegerType()) {
                var phi = builder.buildPhi(i32, Option.of("orPhi"));

                phi.addIncoming(new Pair<>(orTrue, intOne));
                if (inBT != null) {
                    phi.addIncoming(new Pair<>(inBT, right));
                    inBT = null;
                } else {
                    phi.addIncoming(new Pair<>(orFalse, right));
                }
                return phi;
            } else if (right.getType().isFloatingPointType()) {
                var phi = builder.buildPhi(f32, Option.of("orPhi"));

                phi.addIncoming(new Pair<>(orTrue, floatOne));
                if (inBT != null) {
                    phi.addIncoming(new Pair<>(inBT, right));
                    inBT = null;
                } else {
                    phi.addIncoming(new Pair<>(orFalse, right));
                }
                return phi;
            } else {
                throw new RuntimeException("type error");
            }

        } else {
            Value left = visitCond(ctx.cond(0));
            Value right = visitCond(ctx.cond(1));

            Type leftType = left.getType();
            Type rightType = right.getType();

            if (leftType.isIntegerType() && rightType.isFloatingPointType()) {
                left = builder.buildSignedToFloat(left, f32, Option.of("lIToF"));
                leftType = left.getType();
            } else if (leftType.isFloatingPointType() && rightType.isIntegerType()) {
                right = builder.buildSignedToFloat(right, f32, Option.of("rIToF"));
                rightType = right.getType();
            }

            if (leftType.isIntegerType() && rightType.isIntegerType()) {
                if (ctx.LT() != null) {
                    var lt = builder.buildIntCompare(IntPredicate.SignedLessThan, left, right, Option.of("lt"));
                    return builder.buildZeroExt(lt, i32, Option.of("zextForLt"));
                } else if (ctx.LE() != null) {
                    var le = builder.buildIntCompare(IntPredicate.SignedLessEqual, left, right, Option.of("le"));
                    return builder.buildZeroExt(le, i32, Option.of("zextForLe"));
                } else if (ctx.GT() != null) {
                    var gt = builder.buildIntCompare(IntPredicate.SignedGreaterThan, left, right, Option.of("gt"));
                    return builder.buildZeroExt(gt, i32, Option.of("zextForGt"));
                } else if (ctx.GE() != null) {
                    var ge = builder.buildIntCompare(IntPredicate.SignedGreaterEqual, left, right, Option.of("ge"));
                    return builder.buildZeroExt(ge, i32, Option.of("zextForGe"));
                } else if (ctx.EQ() != null) {
                    var eq = builder.buildIntCompare(IntPredicate.Equal, left, right, Option.of("eq"));
                    return builder.buildZeroExt(eq, i32, Option.of("zextForEq"));
                } else if (ctx.NEQ() != null) {
                    var neq = builder.buildIntCompare(IntPredicate.NotEqual, left, right, Option.of("neq"));
                    return builder.buildZeroExt(neq, i32, Option.of("zextForNeq"));
                }
            } else if (leftType.isFloatingPointType() && rightType.isFloatingPointType()) {
                if (ctx.LT() != null) {
                    var lt = builder.buildFloatCompare(FloatPredicate.OrderedLessThan, left, right, Option.of("olt"));
                    return builder.buildZeroExt(lt, i32, Option.of("zextForOLt"));
                } else if (ctx.LE() != null) {
                    var le = builder.buildFloatCompare(FloatPredicate.OrderedLessEqual, left, right, Option.of("ole"));
                    return builder.buildZeroExt(le, i32, Option.of("zextForOLe"));
                } else if (ctx.GT() != null) {
                    var gt = builder.buildFloatCompare(FloatPredicate.OrderedGreaterThan, left, right, Option.of("ogt"));
                    return builder.buildZeroExt(gt, i32, Option.of("zextForOGt"));
                } else if (ctx.GE() != null) {
                    var ge = builder.buildFloatCompare(FloatPredicate.OrderedGreaterEqual, left, right, Option.of("oge"));
                    return builder.buildZeroExt(ge, i32, Option.of("zextForOGe"));
                } else if (ctx.EQ() != null) {
                    var eq = builder.buildFloatCompare(FloatPredicate.OrderedEqual, left, right, Option.of("oeq"));
                    return builder.buildZeroExt(eq, i32, Option.of("zextForOEq"));
                } else if (ctx.NEQ() != null) {
                    var neq = builder.buildFloatCompare(FloatPredicate.OrderedNotEqual, left, right, Option.of("oneq"));
                    return builder.buildZeroExt(neq, i32, Option.of("zextForONeq"));
                }
            } else {
                throw new RuntimeException("cond binary type error");
            }
        }
        return null;
    }


    @Override
    public Value visitLVal(SysYParser.LValContext ctx) {
        LOG("visitLVal");
        String varName = ctx.IDENT().getText();
        Value varAddr = symbolTable.getSymbol(varName);
        //System.out.println(varName);
        //System.out.println(varName + "  Lval" + varAddr.getType().getAsString());
        Constant constGlobal = globalValues.get(varAddr);
        if (constGlobal != null) {
            if (constGlobal.getType().isIntegerType()) {
                return i32.getConstant(LLVMConstIntGetSExtValue(constGlobal.getRef()), true);
            } else {
                return f32.getConstant(LLVMConstRealGetDouble(constGlobal.getRef(), new IntPointer(0)));
            }
        }
        if (varAddr == null) {
            throw new RuntimeException("Variable '" + varName + "' not found");
        }

        PointerType pVarType = new PointerType(varAddr.getType().getRef());
        Type varType = pVarType.getElementType();

        //System.out.println("aaaaaaaaaaaaaaaa" + varType.getAsString());

        //Value var = builder.buildLoad(varAddr, Option.of("val"));

        if (varType.isArrayType() || varType.isPointerType()) {
            // 数组访问
            List<Value> indices = new ArrayList<>();
            for (SysYParser.ExpContext expCtx : ctx.exp()) {
                Value idx = visitExp(expCtx);
                // LLVM数组中的索引类型默认为i64
                //idx = builder.buildSignExt(idx, context.getInt64Type(), Option.of("idx64"));
                idx = builder.buildZeroExt(idx, context.getInt64Type(), Option.of("idx64"));
                indices.add(idx);
            }

            // 函数参数得先 load
            boolean isFunctionArg = varType.isPointerType();

            boolean needLoad = false;
            ParserRuleContext parent = ctx.getParent();

            if (parent instanceof SysYParser.ExpContext) { // a[1] = b[1];中 a[1] 不需要 load
                needLoad = true;
            } else if (parent instanceof SysYParser.StmtContext) {
                needLoad = false;
            }

            return buildArrayAccess(varAddr, indices, isFunctionArg, needLoad);

        } else {
            // 普通变量访问
            return builder.buildLoad(varAddr, Option.of("val"));
        }
    }

//    public Value buildArrayAccess(Value var, List<Value> indices, boolean isFunctionArg, boolean needLoad) {
//        // 如果是函数参数，先 load 一次
//        Value ptr = isFunctionArg ? builder.buildLoad(var, Option.of("load_array_param")) : var;
//
//        List<Value> gepIndices = new ArrayList<>();
//
//        if (!isFunctionArg) {
//            // 本地数组（alloca），第一个 GEP index 是 0，访问数组首地址！！！！！！！
//            gepIndices.add(intZero);
//        }
//
//        // 遍历表达式索引，例如 a[i][j][k] 就是 i, j, k
//        gepIndices.addAll(indices);
//
//        // 构建 GEP 指令
//        Value gep = builder.buildGetElementPtr(ptr, gepIndices.toArray(new Value[0]), Option.of("array_element_ptr"), true); // 最后一个是true，表示数组不能越界
//
//        // load 出值
//        Value ans =  needLoad ? builder.buildLoad(gep, Option.of("array_element")): gep;
//        /*
//            对于 exp 中的 lVal 有两种情况：
//            1. 在函数参数（其中也有两种情况，一种是传入 int 一种是传入 array ，前者是传值，后者是传指针）
//            2. 是右值（这里与参数中传入 int 是一样的处理）
//         */
//        if (needLoad) {
//            if(ans.getType().isIntegerType() || ans.getType().isFloatingPointType()){
//                return ans;
//            } else {
//                return gep;
//            }
//        }
//        return ans;
//    }


    public int getArrayDimension(Type type) {
        int dim = 0;

        // 先转换地址
        if (type.isPointerType()) {
            type = new PointerType(type.getRef()).getElementType();
        }

        // 为函数参数数组再做一次判断
        if (type.isPointerType()) {
            dim++;
            type = new PointerType(type.getRef()).getElementType();
        }

        // 递归统计 ArrayType 层数
        while (type.isArrayType()) {
            dim++;
            type = new ArrayType(type.getRef()).getElementType();
        }

        return dim;
    }


    public Value buildArrayAccess(Value var, List<Value> indices, boolean isFunctionArg, boolean needLoad) {
        // 如果是函数参数，先 load 一次（它本质上是一个 ptr-to-array 的参数）
        // 这里的isFunctionArg表示该数组的来源是不是函数参数
        Value currentPtr = isFunctionArg ? builder.buildLoad(var, Option.of("load_array_param")) : var;
        //System.out.println("currentPtr typeAAAAAAA"+currentPtr.getType().getAsString());
        ArrayType varType = new ArrayType(var.getType().getRef());
        int dim = getArrayDimension(var.getType());


        for (int level = 0; level < indices.size(); level++) {
            Value index = indices.get(level);

            Value[] gepIndices;


            if (level == 0 && isFunctionArg) {
                // 第一层：ptr -> [N x T]，GEP 的 index 只有一个
                gepIndices = new Value[]{index};

            } else {
                // 后续层：需要两个 index（先进入数组元素，再进入子数组/元素）
                gepIndices = new Value[]{intZero64, index};
            }

            currentPtr = builder.buildGetElementPtr(
                    currentPtr,
                    gepIndices,
                    Option.of("arrayidx" + level),
                    true
            );

        }

        // 根据是否需要 load 值来决定是否加载
        //Value ans = needLoad ? builder.buildLoad(currentPtr, Option.of("array_element")) : currentPtr;


        // 如果是右值，需要检查类型：基本类型（int/float）才 load；否则仍返回 GEP 结果
        if (needLoad) {
            if (indices.size() == dim) {
                return builder.buildLoad(currentPtr, Option.of("array_element"));
            } else { // 那么如下一定是函数参数，那么就需要再gep一次
//                currentPtr = builder.buildGetElementPtr(
//                        currentPtr,
//                        new Value[]{intZero64, intZero64},
//                        Option.of("arrayidx" + "param"),
//                        true
//                );
                return currentPtr; // 对于数组或结构体，返回指针
            }
        }

        return currentPtr;
    }


    @Override
    public Value visitNumber(SysYParser.NumberContext ctx) {
        if (ctx.INTEGER_CONST() != null) {
            String text = ctx.INTEGER_CONST().getText();
            long value;

            if (text.startsWith("0x") || text.startsWith("0X")) {
                value = Long.parseLong(text.substring(2), 16);  // 十六进制
            } else if (text.startsWith("0") && text.length() > 1) {
                value = Long.parseLong(text.substring(1), 8);   // 八进制
            } else { // 十进制
                value = Long.parseLong(text);
            }

            return i32.getConstant((int) value, false);
        } else if (ctx.FLOAT_CONST() != null) {
            String text = ctx.FLOAT_CONST().getText();
            double value = Double.parseDouble(text);

            return f32.getConstant((float) value);
        } else {
            throw new RuntimeException("visit number error");
        }
    }

    @Override
    public Value visitUnaryOp(SysYParser.UnaryOpContext ctx) {
        return super.visitUnaryOp(ctx);
    }

    @Override
    public Value visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
        return super.visitFuncRParams(ctx);
    }

    @Override
    public Value visitParam(SysYParser.ParamContext ctx) {
        return super.visitParam(ctx);
    }

    @Override
    public Value visitConstExp(SysYParser.ConstExpContext ctx) {
        return super.visitConstExp(ctx);
    }
}
