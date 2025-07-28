package com.compiler.mir;

import com.compiler.mir.instruction.MIRInstruction;
import com.compiler.mir.operand.MIRGlobalVariable;

import java.io.PrintWriter;

public class MIRPrinter {
    private final MIRModule module;
    private final PrintWriter writer;
    private int indentLevel = 0;

    public MIRPrinter(MIRModule module, PrintWriter writer) {
        this.module = module;
        this.writer = writer;
    }

    public void printModule() {
        // 打印全局变量
        printSectionHeader("Global Variables");
        for (MIRGlobalVariable global : module.getGlobalVariables()) {
            printGlobalVariable(global);
        }
        writer.println();

        // 打印函数
        printSectionHeader("Functions");

        for (MIRFunction function : module.getFunctions()) {
            printFunction(function);
            writer.println();
        }
    }

    private void printGlobalVariable(MIRGlobalVariable global) {
        writer.printf("%s\t%s \n", global.getInitializerType().name(), global.getName());
    }

    private void printFunction(MIRFunction function) {
        // 函数头
        writer.printf("func %s(", function.getName());
        for (int i = 0; i < function.getParams().size(); i++) {
            if (i > 0) writer.print(", ");
            writer.printf("%s", function.getParams().get(i));
        }
        writer.printf(")");
        writer.flush();
        indentLevel++;

        // 打印基本块
        for (MIRBasicBlock block : function.getBlocks()) {
            printBasicBlock(block);
        }

        indentLevel--;
    }

    private void printBasicBlock(MIRBasicBlock block) {
        printIndent();
        writer.printf("%s:\n", block.getLabel().toString());
        indentLevel++;
        for (MIRInstruction inst : block.getInstructions()) {
            printInstruction(inst);
        }
        indentLevel--;
    }

    private void printInstruction(MIRInstruction inst) {
        printIndent();
        writer.printf("%s " ,inst.toString());
        writer.println();
        writer.flush();
    }


    // 辅助方法
    private void printIndent() {
        for (int i = 0; i < indentLevel; i++) {
            writer.print("  ");
        }
    }

    private void printSectionHeader(String title) {
        writer.println(";========================================");
        writer.println("; " + title);
        writer.println(";========================================");
    }

}

