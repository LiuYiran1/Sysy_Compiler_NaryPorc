package com.compiler.ir2;



import com.compiler.frontend.SysYParser;
import com.compiler.frontend.SysYParserBaseVisitor;
import com.compiler.ll.Context;
import com.compiler.ll.IRBuilder;
import com.compiler.ll.Module;
import com.compiler.ll.Types.*;
import com.compiler.ll.Values.*;
import com.compiler.ll.Values.Constants.ConstantArray;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instructions.FloatPredicate;
import com.compiler.ll.Values.Instructions.IntPredicate;
import com.compiler.ll.Values.Instructions.Opcode;
import com.compiler.pass.*;
import org.antlr.v4.runtime.ParserRuleContext;


import java.io.File;
import java.util.*;

public class LLVisitor extends SysYParserBaseVisitor<Value> {
    private final Context context = new Context();

    private final IRBuilder builder = context.getIRBuilder();

    private final Module mod = context.getModule("module");

    private final IntegerType i1 = context.getInt1Type();

    private final IntegerType i32 = context.getInt32Type();

    private final IntegerType i64 = context.getInt64Type();

    private final FloatType f32 = context.getFloatType();

    private final VoidType vo = context.getVoidType();

    private final ConstantInt zero = i1.getConstantInt(0);

    private final ConstantInt one = i1.getConstantInt(1);

    private final ConstantInt intZero = i32.getConstantInt(0);

    private final ConstantInt intOne = i32.getConstantInt(1);

    private final ConstantInt intZero64 = context.getInt64Type().getConstantInt(0);
    /// ///

    private final ConstantFloat floatZero = f32.getConstantFloat(0.0f);

    private final ConstantFloat floatOne = f32.getConstantFloat(1.0f);

    private final SymbolTable symbolTable = new SymbolTable();

    private Function curFunc;

    private BasicBlock inBT;

    private final Stack<BasicBlock> breakBlocks = new Stack<>();

    private final Stack<BasicBlock> continueBlocks = new Stack<>();

    private final Map<String, Type> retTypes = new LinkedHashMap<>();

    private final Map<Value, Constant> globalValues = new LinkedHashMap<>();


    private boolean arrDefaultZero = false;

    public LLVisitor() {
        initRunTimeLibrary();
    }

    private void addFunction(String name, Type returnType, Type... params) {
        FunctionType funcType = context.getFunctionType(returnType, List.of(params));
        Function func = mod.addFunction(name, funcType);
        symbolTable.addSymbol(name, func);
    }

    private void initRunTimeLibrary() {
        // int getint();
        addFunction("getint", i32);

        // int getch();
        addFunction("getch", i32);

        // float getfloat();
        addFunction("getfloat", f32);

        // int getarray(int[])
        addFunction("getarray", i32, context.getPointerType(i32));

        // int getfarray(float[])
        addFunction("getfarray", i32, context.getPointerType(f32));

        // void putint(int)
        addFunction("putint", vo, i32);

        // void putch(int)
        addFunction("putch", vo, i32);

        // void putfloat(float)
        addFunction("putfloat", vo, f32);

        // void putarray(int, int[])
        addFunction("putarray", vo, i32, context.getPointerType(i32));

        // void putfarray(int, float[])
        addFunction("putfarray", vo, i32, context.getPointerType(i32));

        // void starttime()
        addFunction("starttime", vo);

        // void stoptime()
        addFunction("stoptime", vo);
    }

    public void LOG(String msg) {
        //System.out.println(msg);
    }

    public void dump(File file) {
        for (Function func = mod.getFirstFunction(); func != null; func = func.getNextFunction()) {
            for (BasicBlock bb = func.getFirstBasicBlock(); bb != null; bb = bb.getNextBasicBlock()) {
                if (bb.getTerminator() == null) {
                    builder.positionAfter(bb);
                    String funcName = func.getName();
                    Type retType = retTypes.get(funcName);
                    if (retType.isIntegerType()) {
                        builder.buildReturn(intZero);
                    } else if (retType.isVoidType()) {
                        builder.buildReturnVoid();
                    } else if (retType.isFloatType()) {
                        builder.buildReturn(floatZero);
                    } else {
                        throw new RuntimeException("Unknown return type: " + retType);
                    }

                }
            }
        }

        // 遍历所有指令,消除终止指令后的冗余指令
        List<Instruction> DCE = new ArrayList<>(); // 死代码
        List<BasicBlock> DBE = new ArrayList<>(); // 空块
        for (Function func = mod.getFirstFunction(); func != null; func = func.getNextFunction()) {
            for (BasicBlock bb = func.getFirstBasicBlock(); bb != null; bb = bb.getNextBasicBlock()) {
                boolean terminatorFlag = false;
                if (bb.getFirstInstruction() == null) {
                    DBE.add(bb);
                    continue;
                }
                for (Instruction inst = bb.getFirstInstruction(); inst != null; inst = inst.getNextInstruction()) {
                    int opcode = inst.getOpcode().ordinal();
                    if (terminatorFlag) {
                        DCE.add(inst);
                    }
                    if ((opcode == Opcode.RET.ordinal() || opcode == Opcode.BR.ordinal()) && !terminatorFlag) {
                        terminatorFlag = true;
                    }
                }
            }
        }
        for (Instruction inst : DCE) {
            inst.eraseFromParent();
        }
        for (BasicBlock bb : DBE) {
            bb.delete();
        }

        Pass DFGPass = new DFGPass();
        Pass deadCodeElimPass = new DeadCodeElimPass();
        Pass domPass = new DominateAnalPass();
        Pass mem2RegPass = new Mem2RegPass(context);
        Pass unUsedVarElimPass = new UnusedVarElimPass();

        DFGPass.run(mod);
        deadCodeElimPass.run(mod);
        domPass.run(mod);
        mem2RegPass.run(mod);
        unUsedVarElimPass.run(mod);


        mod.dump(file);
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
                    var globalVar = mod.addGlobalVariable(varName, i32);

                    globalVar.setInitializer(calConstInt(init));
                    symbolTable.addSymbol(varName, globalVar);
                    globalValues.put(globalVar, calConstInt(init));
                } else if (type.isFloatType()) {
                    var globalVar = mod.addGlobalVariable(varName, f32);

                    globalVar.setInitializer(calConstFloat(init));
                    symbolTable.addSymbol(varName, globalVar);
                    globalValues.put(globalVar, calConstFloat(init));
                } else {
                    throw new RuntimeException("Unknown type: " + type);
                }

            } else { // 局部变量
                if (type.isIntegerType()) {
                    Value alloc = builder.buildAlloca(i32, varName);

                    if (init.getType().isFloatType()) {
                        init = builder.buildFloatToSigned(init, i32, "iInit");
                    }
                    builder.buildStore(alloc, init);

                    symbolTable.addSymbol(varName, alloc);
                } else if (type.isFloatType()) {
                    Value alloc = builder.buildAlloca(f32, varName);

                    if (init.getType().isIntegerType()) {
                        init = builder.buildSignedToFloat(init, f32, "fInit");
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
                    long dim = calConstInt(visitConstExp(constExp)).getSExtValue(); // 这里是否应该是getZExtValue
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i));
                }
                // 处理数组赋值
                var globalVar = mod.addGlobalVariable(varName, arrayType);
                if (ctx.ASSIGN() != null && (ctx.constInitVal() != null && !ctx.constInitVal().constInitVal().isEmpty())) {
                    int memSize = 1;
                    for (int dim : dimensions) {
                        memSize *= dim;
                    }
                    Value[] mem = new Value[memSize + 5];
                    for (int i = 0; i < mem.length; i++) {
                        mem[i] = type.isIntegerType() ? intZero : floatZero;
                    }
                    myVisitConstInitVal(ctx.constInitVal(), mem, 0, dimensions, memSize);

                    Constant[] newMem = new Constant[mem.length];
                    for (int i = 0; i < mem.length; i++) {
                        newMem[i] = type.isIntegerType() ? calConstInt(mem[i]) : calConstFloat(mem[i]);
                    }

                    Constant initializer = buildNestedArray(newMem, dimensions, type);
                    globalVar.setInitializer(initializer);

                } else {
                    // globalVar.setInitializer(arrayType.getConstantArray()); /////
                    // 这里没调用 setInitializer 会自动处理
                }

                // 存符号表
                symbolTable.addSymbol(varName, globalVar);
            } else {
                // 获取所有维数信息
                List<Integer> dimensions = new LinkedList<>();
                for (var constExp : ctx.constExp()) {
                    long dim = calConstInt(visitConstExp(constExp)).getSExtValue(); // 这里是否应该是getZExtValue
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i));
                }
                Value ptr = builder.buildAlloca(arrayType, varName + "Arr");
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
                            if (mem[i].getType().isFloatType()) {
                                mem[i] = builder.buildFloatToSigned(mem[i], i32, "iArr");
                            }
                        } else if (type.isFloatType()) {
                            if (mem[i].getType().isIntegerType()) {
                                mem[i] = builder.buildSignedToFloat(mem[i], f32, "fArr");
                            }
                        }
                    }

                    // 以线性的方式生成数组
                    Type linearPtrType = context.getPointerType(type); // LLVMPointerType(type.getRef(), 0);  // address space 0
                    // 将多维数组压成一维
                    Value linearPtr = builder.buildBitCast(ptr, linearPtrType, varName + "FlatPtr"); // LLVMValueRef linearPtr = LLVMBuildBitCast(builder.getRef(), ptr.getRef(), linearPtrType, varName + "FlatPtr");
                    for (int i = 0; i < memSize; i++) {
                        Value val = mem[i];
                        // 优化：如果是 Constant 且为 0，跳过
                        if (arrDefaultZero) {
                            if (val.getType().isIntegerType() && val.isConstant() && ((ConstantInt) val).isZero()) continue;
                            if (val.getType().isFloatType() && val.isConstant() && ((ConstantFloat) val).isZero()) continue;
                        }

                        Value idx = i64.getConstantInt(i); // i64 index
                        List<Value> indices = new ArrayList<Value>();
                        indices.add(idx);
                        Value gep = builder.buildGetElementPtr(linearPtr, indices, varName + "Elem" + i);
                        builder.buildStore(gep, val);
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

    private ConstantInt calConstInt(Value init) {
        if (init.getType().isFloatType()) {
            init = builder.buildFloatToSigned(init, i32, "iInit");
        }
        return new ConstantInt(init, i32);
    }

    private ConstantFloat calConstFloat(Value init) {
        if (init.getType().isIntegerType()) {
            init = builder.buildSignedToFloat(init, f32, "fInit");
        }
        return new ConstantFloat(init, f32);
    }

    public void myVisitVarDef(SysYParser.VarDefContext ctx, Type type) {
        LOG("myVisitVarDef");
        if (ctx.L_BRACKT().isEmpty()) { // 普通变量
            String varName = ctx.IDENT().getText();

            if (symbolTable.isBottom()) { // 全局变量
                if (type.isIntegerType()) {
                    var globalVar = mod.addGlobalVariable(varName, i32);
                    if (ctx.ASSIGN() != null) {
                        Value init = visitInitVal(ctx.initVal());

                        globalVar.setInitializer(calConstInt(init));
                    } else {
                        globalVar.setInitializer(intZero);
                    }
                    symbolTable.addSymbol(varName, globalVar);
                } else if (type.isFloatType()) {
                    var globalVar = mod.addGlobalVariable(varName, f32);
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
                    Value alloc = builder.buildAlloca(i32, varName);
                    if (ctx.ASSIGN() != null) {
                        Value init = visitInitVal(ctx.initVal());
                        if (init.getType().isFloatType()) {
                            init = builder.buildFloatToSigned(init, i32, "iInit");
                        }
                        builder.buildStore(alloc, init);
                    } else {
                        builder.buildStore(alloc, intZero);
                    }
                    symbolTable.addSymbol(varName, alloc);
                } else if (type.isFloatType()) {
                    Value alloc = builder.buildAlloca(f32, varName);
                    if (ctx.ASSIGN() != null) {
                        Value init = visitInitVal(ctx.initVal());
                        if (init.getType().isIntegerType()) {
                            init = builder.buildSignedToFloat(init, f32, "fInit");
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
                    long dim = calConstInt(visitConstExp(constExp)).getSExtValue();
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i));
                }
                // 处理数组赋值
                var globalVar = mod.addGlobalVariable(varName, arrayType);
                if (ctx.ASSIGN() != null && (ctx.initVal() != null && !ctx.initVal().initVal().isEmpty())) {
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

                    Constant[] newMem = new Constant[mem.length];
                    for (int i = 0; i < mem.length; i++) {
                        newMem[i] = type.isIntegerType() ? calConstInt(mem[i]) : calConstFloat(mem[i]);
                    }

                    Constant initializer = buildNestedArray(newMem, dimensions, type);
                    globalVar.setInitializer(initializer);

                } else {
                    // globalVar.setInitializer(arrayType.getConstantArray()); /////
                }

                // 存符号表
                symbolTable.addSymbol(varName, globalVar);
            } else {
                // 获取所有维数信息
                List<Integer> dimensions = new LinkedList<>();
                for (var constExp : ctx.constExp()) {
                    long dim = calConstInt(visitConstExp(constExp)).getSExtValue();
                    dimensions.add((int) dim); // 这里的强制转换是否会有问题？
                }
                // 构建数组类型
                Type arrayType = type;
                for (int i = dimensions.size() - 1; i >= 0; i--) {
                    arrayType = context.getArrayType(arrayType, dimensions.get(i));
                }
                Value ptr = builder.buildAlloca(arrayType, varName + "Arr");
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
                            if (mem[i].getType().isFloatType()) {
                                mem[i] = builder.buildFloatToSigned(mem[i], i32, "iArr");
                            }
                        } else if (type.isFloatType()) {
                            if (mem[i].getType().isIntegerType()) {
                                mem[i] = builder.buildSignedToFloat(mem[i], f32, "fArr");
                            }
                        }
                    }

                    // 以线性的方式生成数组
                    PointerType linearPtrType = context.getPointerType(type); // LLVMPointerType(type.getRef(), 0);  // address space 0
                    // 将多维数组压成一维
                    Value linearPtr = builder.buildBitCast(ptr, linearPtrType, varName + "FlatPtr"); // LLVMValueRef linearPtr = LLVMBuildBitCast(builder.getRef(), ptr.getRef(), linearPtrType, varName + "FlatPtr");
                    for (int i = 0; i < memSize; i++) {
                        Value val = mem[i];
                        // 优化：如果是 Constant 且为 0，跳过
                        if (arrDefaultZero) {
                            if (val.getType().isIntegerType() && val.isConstant() && ((ConstantInt) val).isZero()) continue;
                            if (val.getType().isFloatType() && val.isConstant() && ((ConstantFloat) val).isZero()) continue;
                        }

                        Value idx = i64.getConstantInt(i); // i64 index
                        List<Value> indices = new ArrayList<Value>();
                        indices.add(idx);
                        Value gep = builder.buildGetElementPtr(linearPtr, indices, varName + "Elem" + i);
                        builder.buildStore(gep, val);
                    }



                }

            }

        }
    }

    private Constant buildNestedArray(Constant[] flat, List<Integer> dims, Type elemType) {
        return buildArrayRecursive(flat, dims, 0, elemType).result;
    }

    private static class ArrayBuildResult {
        Constant result;
        int nextIndex;

        ArrayBuildResult(Constant result, int nextIndex) {
            this.result = result;
            this.nextIndex = nextIndex;
        }
    }

    private ArrayBuildResult buildArrayRecursive(Constant[] flat, List<Integer> dims, int startIndex, Type elemType) {
        int dim = dims.get(0);

        if (dims.size() == 1) {
            // 最后一维，直接构造常量数组
            Constant[] slice = new Constant[dim];
            for (int i = 0; i < dim; i++) {
                slice[i] = flat[startIndex + i];
            }
            ArrayType arrayType = context.getArrayType(elemType, dim);
            Constant constArray = new ConstantArray(context, elemType, List.of(slice));// LLVMConstArray(elemType, new PointerPointer<>(slice), dim);
            return new ArrayBuildResult(constArray, startIndex + dim);
        } else {
            // 多维，递归构造
            List<Integer> subDims = dims.subList(1, dims.size());

            Type subType = elemType;
            for (int i = subDims.size() - 1; i >= 0; i--) {
                subType = context.getArrayType(subType, subDims.get(i));
            }

            Constant[] subArrays = new Constant[dim];
            int currentIndex = startIndex;
            for (int i = 0; i < dim; i++) {
                ArrayBuildResult sub = buildArrayRecursive(flat, subDims, currentIndex, elemType);
                subArrays[i] = sub.result;
                currentIndex = sub.nextIndex;
            }

            ArrayType arrayType = context.getArrayType(subArrays[0].getType(), dim);
            Constant constArray = new ConstantArray(context, subArrays[0].getType(), List.of(subArrays));
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
            Type llvmType = myVisitFuncFParam(ctx.funcFParams().funcFParam().get(i));
            paramTypes[i] = llvmType;
            paramNames[i] = ctx.funcFParams().funcFParam().get(i).IDENT().getText();
        }

        String funcName = ctx.IDENT().getText();

        Function function = mod.addFunction(funcName, context.getFunctionType(retType, List.of(paramTypes)));

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
        Value[] params = function.getArguments().toArray(new Argument[0]);
        for (int i = 0; i < paramTypes.length; i++) {
            Value alloc = builder.buildAlloca(paramTypes[i], paramNames[i]);
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

    public Type myVisitFuncFParam(SysYParser.FuncFParamContext ctx) {
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
                    long dim = calConstInt(visitExp(exps.get(i))).getSExtValue();
                    curType = context.getArrayType(curType, (int) dim);
                }
            }

            return context.getPointerType(curType);
        }

        return baseType;
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
                if (retType.isIntegerType() && curFuncRetType.isFloatType()) {
                    retVal = builder.buildSignedToFloat(retVal, f32, "fRet");
                } else if (retType.isFloatType() && curFuncRetType.isIntegerType()) {
                    retVal = builder.buildFloatToSigned(retVal, i32, "iRet");
                }

//                // 构建ret指令（若基本块有终结指令则不构建）
//                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
//                if (LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
//                    builder.buildReturn(Option.of(retVal));
//                }
                builder.buildReturn(retVal);
            } else { // return void
//                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
//                if (LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
//                    builder.buildReturn(Option.empty());
//                }
                builder.buildReturnVoid();
            }
        } else if (ctx.lVal() != null) {
            Value rVal = visitExp(ctx.exp());
            String lValName = ctx.lVal().IDENT().getText();
            Value lValAddr = symbolTable.getSymbol(lValName);
            PointerType pVarType = (PointerType) lValAddr.getType();
            Type varType = pVarType.getPointeeType();
            if (varType.isArrayType()) { // 修改
                ArrayType arrayType = (ArrayType) varType;
                if (rVal.getType().isFloatType() && arrayType.getElementType().isIntegerType()) {
                    rVal = builder.buildFloatToSigned(rVal, i32, "iLVar");
                } else if (rVal.getType().isIntegerType() && arrayType.getElementType().isFloatType()) {
                    rVal = builder.buildSignedToFloat(rVal, f32, "fLVar");
                }
                Value gep = visitLVal(ctx.lVal());
                builder.buildStore(gep, rVal);
            } else if(varType.isPointerType()){
                PointerType pointerType = (PointerType) varType;
                if (rVal.getType().isFloatType() && pointerType.getPointeeType().isIntegerType()) {
                    rVal = builder.buildFloatToSigned(rVal, i32, "iLVar");
                } else if (rVal.getType().isIntegerType() && pointerType.getPointeeType().isFloatType()) {
                    rVal = builder.buildSignedToFloat(rVal, f32, "fLVar");
                }
                Value gep = visitLVal(ctx.lVal());
                builder.buildStore(gep, rVal);
            } else {
                if (rVal.getType().isFloatType() && varType.isIntegerType()) {
                    rVal = builder.buildFloatToSigned(rVal, i32, "iLVar");
                } else if (rVal.getType().isIntegerType() && varType.isFloatType()) {
                    rVal = builder.buildSignedToFloat(rVal, f32, "fLVar");
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
                cond = builder.buildIntCompare(IntPredicate.NE, cond, intZero, "cond");
            } else if (cond.getType().isFloatType()) {
                cond = builder.buildFloatCompare(FloatPredicate.ONE, cond, floatZero, "cond");
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
                cond = builder.buildIntCompare(IntPredicate.NE, cond, intZero, "cond");
            } else if (cond.getType().isFloatType()) {
                cond = builder.buildFloatCompare(FloatPredicate.ONE, cond, floatZero, "cond");
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
            Value[] params = new Value[function.getArgumentCount()];

            if (ctx.funcRParams() != null) {
                List<Argument> arguments = function.getArguments();
                for (int i = 0; i < params.length; i++) {
                    Value argVal = visit(ctx.funcRParams().param(i));
                    Type argType = argVal.getType();
                    Type expectedType = arguments.get(i).getType();

                    // 函数参数隐式转换
                    if (argType.isFloatType() && expectedType.isIntegerType()) {
                        argVal = builder.buildFloatToSigned(argVal, expectedType, "funcFToI");
                    } else if (argType.isIntegerType() && expectedType.isFloatType()) {
                        argVal = builder.buildSignedToFloat(argVal, expectedType, "funcIToF");
                    }
//                    else if (argType.isPointerType()) {
//                        List<Value> indices = new ArrayList<>();
//                        indices.add(i64.getConstantInt(0));
//                        // indices.add(i64.getConstantInt(0));
//                        argVal = builder.buildGetElementPtr(argVal, indices, "paramArr");
//                    }

                    params[i] = argVal;
                }
            }

            Value funcRet = builder.buildCall(function, List.of(params));

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
                        return builder.buildIntSub(intZero, value, "neg_int"); // 注意 WrapSemantics 对应的参数是什么意思
                    } else if (type.isFloatType()) {
                        return builder.buildFloatSub(floatZero, value, "neg_float");
                    }
                    break;
                case "!":
                    if (type.isIntegerType()) {
                        Value cmp = builder.buildIntCompare(IntPredicate.EQ, value, intZero, "cmpI1");
                        Value cmp32 = builder.buildZeroExt(cmp, i32, "cmpI32");
                        return cmp32;
                    } else if (type.isFloatType()) {
                        Value cmp = builder.buildFloatCompare(FloatPredicate.OEQ, value, floatZero, "cmpI1");
                        Value cmp32 = builder.buildZeroExt(cmp, i32, "cmpI32");
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

            if (leftType.isIntegerType() && rightType.isFloatType()) {
                left = builder.buildSignedToFloat(left, f32, "lIToF");
                leftType = left.getType();
            } else if (leftType.isFloatType() && rightType.isIntegerType()) {
                right = builder.buildSignedToFloat(right, f32, "rIToF");
                rightType = right.getType();
            }

            if (leftType.isIntegerType() && rightType.isIntegerType()) {
                if (ctx.MUL() != null) {
                    return builder.buildIntMul(left, right, "iMul");
                } else if (ctx.DIV() != null) {
                    return builder.buildSignedDiv(left, right, "iDiv"); // 第三个参数表示除法是否要精确，注意
                } else if (ctx.MOD() != null) {
                    return builder.buildSignedRem(left, right, "iRem"); // 取余要signed吗???
                } else if (ctx.PLUS() != null) {
                    return builder.buildIntAdd(left, right, "iAdd");
                } else if (ctx.MINUS() != null) {
                    return builder.buildIntSub(left, right, "iSub");
                } else {
                    throw new RuntimeException("exp binaryOp error");
                }
            } else if (leftType.isFloatType() && rightType.isFloatType()) {
                if (ctx.MUL() != null) {
                    return builder.buildFloatMul(left, right, "fMul");
                } else if (ctx.DIV() != null) {
                    return builder.buildFloatDiv(left, right, "fDiv");
                } else if (ctx.MOD() != null) {
                    return builder.buildFloatRem(left, right, "fRem");
                } else if (ctx.PLUS() != null) {
                    return builder.buildFloatAdd(left, right, "fAdd");
                } else if (ctx.MINUS() != null) {
                    return builder.buildFloatSub(left, right, "fSub");
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
                left = builder.buildIntCompare(IntPredicate.NE, left, intZero, "cond");
            } else if (left.getType().isFloatType()) {
                left = builder.buildFloatCompare(FloatPredicate.ONE, left, floatZero, "cond");
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
                var phi = builder.buildPhi(i32, "andPhi");

                phi.addIncoming(andFalse, intZero);
                phi.addIncoming(andTrue, right);
                return phi;
            } else if (right.getType().isFloatType()) {
                var phi = builder.buildPhi(f32, "andPhi");

                phi.addIncoming(andFalse, floatZero);
                phi.addIncoming(andTrue, right);
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
                left = builder.buildIntCompare(IntPredicate.NE, left, intZero, "cond");
            } else if (left.getType().isFloatType()) {
                left = builder.buildFloatCompare(FloatPredicate.ONE, left, floatZero, "cond");
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
                var phi = builder.buildPhi(i32, "orPhi");

                phi.addIncoming(orTrue, intOne);
                if (inBT != null) {
                    phi.addIncoming(inBT, right);
                    inBT = null;
                } else {
                    phi.addIncoming(orFalse, right);
                }
                return phi;
            } else if (right.getType().isFloatType()) {
                var phi = builder.buildPhi(f32, "orPhi");

                phi.addIncoming(orTrue, floatOne);
                if (inBT != null) {
                    phi.addIncoming(inBT, right);
                    inBT = null;
                } else {
                    phi.addIncoming(orFalse, right);
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

            if (leftType.isIntegerType() && rightType.isFloatType()) {
                left = builder.buildSignedToFloat(left, f32, "lIToF");
                leftType = left.getType();
            } else if (leftType.isFloatType() && rightType.isIntegerType()) {
                right = builder.buildSignedToFloat(right, f32, "rIToF");
                rightType = right.getType();
            }

            if (leftType.isIntegerType() && rightType.isIntegerType()) {
                if (ctx.LT() != null) {
                    var lt = builder.buildIntCompare(IntPredicate.SLT, left, right, "lt");
                    return builder.buildZeroExt(lt, i32, "zextForLt");
                } else if (ctx.LE() != null) {
                    var le = builder.buildIntCompare(IntPredicate.SLE, left, right, "le");
                    return builder.buildZeroExt(le, i32, "zextForLe");
                } else if (ctx.GT() != null) {
                    var gt = builder.buildIntCompare(IntPredicate.SGT, left, right, "gt");
                    return builder.buildZeroExt(gt, i32, "zextForGt");
                } else if (ctx.GE() != null) {
                    var ge = builder.buildIntCompare(IntPredicate.SGE, left, right, "ge");
                    return builder.buildZeroExt(ge, i32, "zextForGe");
                } else if (ctx.EQ() != null) {
                    var eq = builder.buildIntCompare(IntPredicate.EQ, left, right, "eq");
                    return builder.buildZeroExt(eq, i32, "zextForEq");
                } else if (ctx.NEQ() != null) {
                    var neq = builder.buildIntCompare(IntPredicate.NE, left, right, "neq");
                    return builder.buildZeroExt(neq, i32, "zextForNeq");
                }
            } else if (leftType.isFloatType() && rightType.isFloatType()) {
                if (ctx.LT() != null) {
                    var lt = builder.buildFloatCompare(FloatPredicate.OLT, left, right, "olt");
                    return builder.buildZeroExt(lt, i32, "zextForOLt");
                } else if (ctx.LE() != null) {
                    var le = builder.buildFloatCompare(FloatPredicate.OLE, left, right, "ole");
                    return builder.buildZeroExt(le, i32, "zextForOLe");
                } else if (ctx.GT() != null) {
                    var gt = builder.buildFloatCompare(FloatPredicate.OGT, left, right, "ogt");
                    return builder.buildZeroExt(gt, i32, "zextForOGt");
                } else if (ctx.GE() != null) {
                    var ge = builder.buildFloatCompare(FloatPredicate.OGE, left, right, "oge");
                    return builder.buildZeroExt(ge, i32, "zextForOGe");
                } else if (ctx.EQ() != null) {
                    var eq = builder.buildFloatCompare(FloatPredicate.OEQ, left, right, "oeq");
                    return builder.buildZeroExt(eq, i32, "zextForOEq");
                } else if (ctx.NEQ() != null) {
                    var neq = builder.buildFloatCompare(FloatPredicate.ONE, left, right, "oneq");
                    return builder.buildZeroExt(neq, i32, "zextForONeq");
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
                return constGlobal;
            } else {
                return constGlobal;
            }
        }
        if (varAddr == null) {
            throw new RuntimeException("Variable '" + varName + "' not found");
        }

        PointerType pVarType = (PointerType) varAddr.getType();
        Type varType = pVarType.getPointeeType();

        //System.out.println("aaaaaaaaaaaaaaaa" + varType.getAsString());

        //Value var = builder.buildLoad(varAddr, Option.of("val"));

        if (varType.isArrayType() || varType.isPointerType()) {
            // 数组访问
            List<Value> indices = new ArrayList<>();
            for (SysYParser.ExpContext expCtx : ctx.exp()) {
                Value idx = visitExp(expCtx);
                // LLVM数组中的索引类型默认为i64
                //idx = builder.buildSignExt(idx, context.getInt64Type(), Option.of("idx64"));
                idx = builder.buildZeroExt(idx, context.getInt64Type(), "idx64");
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

            Value tem = buildArrayAccess(varAddr, indices, isFunctionArg, needLoad);
            return tem;

        } else {
            // 普通变量访问
            return builder.buildLoad(varAddr, "val");
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
//            if(ans.getType().isIntegerType() || ans.getType().isFloatType()){
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
            type = ((PointerType)type).getPointeeType();
        }

        // 为函数参数数组再做一次判断
        if (type.isPointerType()) {
            dim++;
            type = ((PointerType)type).getPointeeType();
        }

        // 递归统计 ArrayType 层数
        while (type.isArrayType()) {
            dim++;
            type = ((ArrayType)type).getElementType(); // 这里赋临时初值 1
        }

        return dim;
    }


    public Value buildArrayAccess(Value var, List<Value> indices, boolean isFunctionArg, boolean needLoad) {
        // 如果是函数参数，先 load 一次（它本质上是一个 ptr-to-array 的参数）
        // 这里的isFunctionArg表示该数组的来源是不是函数参数
        // Value currentPtr = isFunctionArg ? builder.buildLoad(var, "load_array_param") : var;
        Value currentPtr = isFunctionArg ? builder.buildLoad(var, "load_array_param") : var;
        //System.out.println("currentPtr typeAAAAAAA"+currentPtr.getType().getAsString());
        ArrayType varType = context.getArrayType(var.getType());
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
                    List.of(gepIndices),
                    "arrayidx" + level
            );


        }

        // 根据是否需要 load 值来决定是否加载
        //Value ans = needLoad ? builder.buildLoad(currentPtr, Option.of("array_element")) : currentPtr;


        // 如果是右值，需要检查类型：基本类型（int/float）才 load；否则仍返回 GEP 结果
        // 貌似如果不是基本类型也要load
        if (needLoad) {
            if (indices.size() == dim) {
                return builder.buildLoad(currentPtr, "array_element");
            } else { // 那么如下一定是函数参数，那么就需要再gep一次
//                currentPtr = builder.buildGetElementPtr(
//                        currentPtr,
//                        List.of(new Value[]{intZero64, intZero64}),
//                        "arrayidxparam"
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

            return i32.getConstantInt((int) value);
        } else if (ctx.FLOAT_CONST() != null) {
            String text = ctx.FLOAT_CONST().getText();
            float value = Float.parseFloat(text);

            return f32.getConstantFloat(value);
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
