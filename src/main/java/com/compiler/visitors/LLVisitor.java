package com.compiler.visitors;
import com.compiler.frontend.SysYParserBaseVisitor;
import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Module;
import com.compiler.frontend.SysYParser;

public class LLVisitor extends SysYParserBaseVisitor<Value> {
    private static final Context context = new Context();

    private static final IRBuilder builder = context.newIRBuilder();

    private static final Module mod = context.newModule("module");

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
        return super.visitFuncDef(ctx);
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
        return super.visitStmt(ctx);
    }

    @Override
    public Value visitExp(SysYParser.ExpContext ctx) {
        return super.visitExp(ctx);
    }

    @Override
    public Value visitCond(SysYParser.CondContext ctx) {
        return super.visitCond(ctx);
    }

    @Override
    public Value visitLVal(SysYParser.LValContext ctx) {
        return super.visitLVal(ctx);
    }

    @Override
    public Value visitNumber(SysYParser.NumberContext ctx) {
        return super.visitNumber(ctx);
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
