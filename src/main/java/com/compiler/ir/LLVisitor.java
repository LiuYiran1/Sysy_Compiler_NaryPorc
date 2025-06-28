package com.compiler.ir;

import com.compiler.frontend.SysYParserBaseVisitor;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Module;
import com.compiler.frontend.SysYParser;
import org.llvm4j.optional.Option;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMConstIntGetSExtValue;

public class LLVisitor extends SysYParserBaseVisitor<Value> {
    private final Context context = new Context();

    private final IRBuilder builder = context.newIRBuilder();

    private final Module mod = context.newModule("module");

    private final IntegerType i32 = context.getInt32Type();

    private final FloatingPointType f32 = context.getFloatType();

    private final VoidType vo = context.getVoidType();

    private final ConstantInt intZero = i32.getConstant(0, false);

    private final ConstantFP floatZero = f32.getConstant(0.0);

    private final SymbolTable symbolTable = new SymbolTable();
    private Function curFunc;
    private final Map<String, Type> retTypes = new LinkedHashMap<>();

    private final Map<Value, Constant> globalValues = new LinkedHashMap<>();

    public void dump(Option<File> of) {
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

        } else {

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

    private Constant calConstInt(Value init){
        if (init.getType().isFloatingPointType()) {
            init = builder.buildFloatToSigned(init, i32, Option.of("iInit"));
        }
        return new ConstantInt(LLVMConstInt(LLVMInt32Type(), LLVMConstIntGetSExtValue(init.getRef()), 0));
    }

    private Constant calConstFloat(Value init){
        if (init.getType().isIntegerType()) {
            init = builder.buildSignedToFloat(init, f32, Option.of("fInit"));
        }
        return new ConstantFP(LLVMConstReal(LLVMFloatType(), LLVMConstRealGetDouble(init.getRef(), new IntPointer(0))));
    }

    public void myVisitVarDef(SysYParser.VarDefContext ctx, Type type) {
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
                    for(var m : mem){
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
                    LLVMTypeRef linearPtrType = LLVMPointerType(type.getRef(),0);  // address space 0
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
        if(ctx.exp() != null) {
            return visitExp(ctx.exp());
        } else {
            return null;
        }
    }

    public void myVisitInitVal(SysYParser.InitValContext ctx, Value[] mem, int index, List<Integer> dimensions, int h) {
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
        for(var m : mem){
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
        for(int i = 0;i < paramTypes.length;i++){
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
                for (int i = 1; i < exps.size(); i++) {
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
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                Value retVal = visit(ctx.exp());
                Type retType = retVal.getType();

                Type curFuncRetType = retTypes.get(curFunc.getName());

                if (retType.isIntegerType() && curFuncRetType.isFloatingPointType()) {
                    retVal = builder.buildSignedToFloat(retVal, f32, Option.of("fRet"));
                } else if (retType.isFloatingPointType() && curFuncRetType.isIntegerType()) {
                    retVal = builder.buildFloatToSigned(retVal, i32, Option.of("iRet"));
                }

                // 构建ret指令（若基本块有终结指令则不构建）
                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
                if (LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
                    builder.buildReturn(Option.of(retVal));
                }


            } else { // return void
                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
                if (LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
                    builder.buildReturn(Option.empty());
                }
            }
        }
        return null;
    }


    @Override
    public Value visitExp(SysYParser.ExpContext ctx) {
        if (ctx.lVal() != null) {
            return visitLVal(ctx.lVal());

        } else if (ctx.number() != null) {
            return visitNumber(ctx.number());
        } else if (ctx.IDENT() != null) { // 函数调用

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

    @Override
    public Value visitCond(SysYParser.CondContext ctx) {
        return super.visitCond(ctx);
    }

    @Override
    public Value visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Value var = symbolTable.getSymbol(varName);
        Constant constGlobal = globalValues.get(var);
        if (constGlobal != null) {
            return constGlobal;
        }
        if (var == null) {
            throw new RuntimeException("Variable '" + varName + "' not found");
        }

        if (!ctx.exp().isEmpty()) {
            // 处理数组
            return null;
        } else {
            Value loaded = builder.buildLoad(var, Option.of("lVar"));
            return loaded;
        }
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

            return context.getInt32Type().getConstant(value, false);
        } else if (ctx.FLOAT_CONST() != null) {
            String text = ctx.FLOAT_CONST().getText();
            double value = Double.parseDouble(text);

            return context.getFloatType().getConstant((float) value);
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
