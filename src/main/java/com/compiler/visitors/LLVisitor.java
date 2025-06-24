package com.compiler.visitors;
import com.compiler.frontend.SysYParserBaseVisitor;
import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Module;

public class LLVisitor extends SysYParserBaseVisitor<Void> {
    private static final Context context = new Context();

    private static final IRBuilder builder = context.newIRBuilder();

    private static final Module mod = context.newModule("module");

    private static final IntegerType i32 = context.getInt32Type();

    private static final VoidType vo = context.getVoidType();

    private static final ConstantInt zero = i32.getConstant(0, false);
}
