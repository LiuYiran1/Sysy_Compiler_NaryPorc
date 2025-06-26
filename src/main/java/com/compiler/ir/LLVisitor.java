package com.compiler.ir;

import com.compiler.frontend.SysYParserBaseVisitor;
import com.sun.jdi.FloatType;
import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Module;
import com.compiler.frontend.SysYParser;
import org.llvm4j.optional.Option;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.LLVMGetBasicBlockTerminator;

public class LLVisitor extends SysYParserBaseVisitor<Value> {
    private static final Context context = new Context();

    private static final IRBuilder builder = context.newIRBuilder();

    private static final Module mod = context.newModule("module");

    private static final IntegerType i32 = context.getInt32Type();

    private static final FloatingPointType f32 = context.getFloatType();

    private static final VoidType vo = context.getVoidType();

    private static final ConstantInt intZero = i32.getConstant(0, false);

    private static final ConstantFP floatZero = f32.getConstant(0.0);

    private final SymbolTable symbolTable = new SymbolTable();
    private Function curFunc;
    private final Map<String, Type> retTypes = new LinkedHashMap<>();

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
        return super.visitConstDecl(ctx);
    }

    @Override
    public Value visitBType(SysYParser.BTypeContext ctx) {
        return super.visitBType(ctx);
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
        return super.visitVarDecl(ctx);
    }

    @Override
    public Value visitVarDef(SysYParser.VarDefContext ctx) {
        return super.visitVarDef(ctx);
    }

    @Override
    public Value visitInitVal(SysYParser.InitValContext ctx) {
        return super.visitInitVal(ctx);
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
        Type[] params = null;
        if(ctx.funcFParams() != null) {
            params = new Type[ctx.funcFParams().funcFParam().size()];
        }else {
            params = new Type[]{};
        }

        // ！处理数组参数
        for (int i = 0; i < params.length; i++) {

        }

        String funcName = ctx.IDENT().getText();

        Function function = mod.addFunction(funcName, context.getFunctionType(retType, params, false));

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
            if(ctx.exp() != null) {
                Value retVal = visit(ctx.exp());
                Type retType = retVal.getType();
                
                Type curFuncRetType = retTypes.get(curFunc.getName());
                
                if(retType.isIntegerType() && curFuncRetType.isFloatingPointType()){
                    retVal = builder.buildSignedToFloat(retVal, f32, Option.of("fRet"));
                } else if (retType.isFloatingPointType() && curFuncRetType.isIntegerType()) {
                    retVal = builder.buildFloatToSigned(retVal, i32, Option.of("iRet"));
                }

                // 构建ret指令（若基本块有终结指令则不构建）
                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
                if(LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
                    builder.buildReturn(Option.of(retVal));
                }


            } else { // return void
                BasicBlock curBlock = builder.getInsertionBlock().unwrap();
                if(LLVMGetBasicBlockTerminator(curBlock.getRef()) == null) {
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

            if (leftType.isIntegerType() && rightType.isFloatingPointType()){
                left = builder.buildSignedToFloat(left, f32, Option.of("lIToF"));
                leftType = left.getType();
            } else if (leftType.isFloatingPointType() && rightType.isIntegerType()) {
                right = builder.buildSignedToFloat(right, f32, Option.of("rIToF"));
                rightType = right.getType();
            }

            if (leftType.isIntegerType() && rightType.isIntegerType()){
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
            } else{
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
