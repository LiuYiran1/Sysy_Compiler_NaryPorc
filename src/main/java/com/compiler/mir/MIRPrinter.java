package com.compiler.mir;

import com.compiler.mir.instruction.MIRInstruction;
import com.compiler.mir.operand.MIRGlobalVariable;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

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
        writer.printf("%s %s: ", global.getType(), global.getName());
    }

    private void printFunction(MIRFunction function) {
        // 函数头
        writer.printf("func %s(", function.getName());
        for (int i = 0; i < function.getParams().size(); i++) {
            if (i > 0) writer.print(", ");
            writer.printf("%s", function.getParams().get(i));
        }

        indentLevel++;

        // 打印基本块
        for (MIRBasicBlock block : function.getBlocks()) {
            printBasicBlock(block);
        }

        indentLevel--;
    }

    private void printBasicBlock(MIRBasicBlock block) {
        printIndent();
        writer.printf("%s:\n", block.getLabel());

        indentLevel++;
        for (MIRInstruction inst : block.getInstructions()) {
            printInstruction(inst);
        }
        indentLevel--;
    }

    private void printInstruction(MIRInstruction inst) {
        printIndent();
        writer.printf("%s " ,inst.toString());

//        if (inst instanceof MIRPseudoOp) {
//            printPseudoOp((MIRPseudoOp) inst);
//        } else if (inst instanceof MIRControlFlowOp) {
//            printControlFlowOp((MIRControlFlowOp) inst);
//        } else if (inst instanceof MIRArithOp) {
//            printArithOp((MIRArithOp) inst);
//        } else if (inst instanceof MIRCmpOp) {
//            printCmpOp((MIRCmpOp) inst);
//        } else if (inst instanceof MIRMemoryOp) {
//            printMemoryOp((MIRMemoryOp) inst);
//        } else if (inst instanceof MIRMoveOp) {
//            printMoveOp((MIRMoveOp) inst);
//        } else if (inst instanceof MIRConvertOp) {
//            printConvertOp((MIRConvertOp) inst);
//        } else if (inst instanceof MIRLiOp) {
//            printLiOp((MIRLiOp) inst);
//        } else if (inst instanceof MIRLuiOp) {
//            printLuiOp((MIRLuiOp) inst);
//        } else if (inst instanceof MIRAllocOp) {
//            printAllocOp((MIRAllocOp) inst);
//        } else {
//            writer.printf("; UNKNOWN INSTRUCTION: %s\n", inst.getClass().getSimpleName());
//        }
    }

//    // 具体指令打印方法
//    private void printPseudoOp(MIRPseudoOp op) {
//        switch (op.getType()) {
//            case PROLOGUE:
//                writer.printf("PROLOGUE frame_size=%d\n", op.getImmediate());
//                break;
//            case EPILOGUE:
//                writer.println("EPILOGUE");
//                break;
//            case CALLEE_SAVE_REG:
//                writer.println("SAVE_CALLEE_SAVED");
//                break;
//            case CALLEE_RESTORE_REG:
//                writer.println("RESTORE_CALLEE_SAVED");
//                break;
//            case CALLER_SAVE_REG:
//                writer.println("SAVE_CALLER_SAVED");
//                break;
//            case CALLER_RESTORE_REG:
//                writer.println("RESTORE_CALLER_SAVED");
//                break;
//            default:
//                writer.println("; UNKNOWN PSEUDO OP");
//        }
//    }
//
//    private void printControlFlowOp(MIRControlFlowOp op) {
//        if (op.isReturn()) {
//            writer.println("ret");
//        } else if (op.isUnconditionalJump()) {
//            writer.printf("j %s\n", op.getLabel());
//        } else if (op.isConditionalJump()) {
//            writer.printf("bnez %s, %s\n", op.getCondition(), op.getLabel());
//        } else if (op.isCall()) {
//            writer.printf("call %s -> %s", op.getFuncLabel(), op.getDest());
//            for (MIROperand arg : op.getArgs()) {
//                writer.printf(", %s", arg);
//            }
//            writer.println();
//        } else {
//            writer.println("; UNKNOWN CONTROL FLOW");
//        }
//    }
//
//    private void printArithOp(MIRArithOp op) {
//        String opStr = "";
//        switch (op.getOp()) {
//            case ADD: opStr = "add"; break;
//            case SUB: opStr = "sub"; break;
//            case MUL: opStr = "mul"; break;
//            case DIV: opStr = "div"; break;
//            case REM: opStr = "rem"; break;
//            case XOR: opStr = "xor"; break;
//        }
//
//        String typeSuffix = (op.getType() == MIRArithOp.Type.FLOAT) ? ".f" : "";
//        writer.printf("%s%s %s, %s, %s\n", opStr, typeSuffix, op.getDest(), op.getSrc1(), op.getSrc2());
//    }
//
//    private void printCmpOp(MIRCmpOp op) {
//        String typePrefix = (op.getType() == MIRCmpOp.Type.FLOAT) ? "f" : "i";
//        String opStr = "";
//        switch (op.getCmpOp()) {
//            case EQ: opStr = "eq"; break;
//            case NE: opStr = "ne"; break;
//            case GT: opStr = "gt"; break;
//            case LT: opStr = "lt"; break;
//            case GE: opStr = "ge"; break;
//            case LE: opStr = "le"; break;
//        }
//
//        writer.printf("%s%s.%s %s, %s, %s\n",
//                typePrefix, opStr,
//                op.getResult(), op.getSrc1(), op.getSrc2());
//    }
//
//    private void printMemoryOp(MIRMemoryOp op) {
//        String opStr = (op.getMemOp() == MIRMemoryOp.Op.LOAD) ? "load" : "store";
//        String typeSuffix = (op.getMemType() == MIRMemoryOp.Type.FLOAT) ? ".f" : "";
//
//        if (op.getMemOp() == MIRMemoryOp.Op.LOAD) {
//            writer.printf("%s%s %s, [%s]\n", opStr, typeSuffix, op.getValue(), op.getMemory());
//        } else {
//            writer.printf("%s%s [%s], %s\n", opStr, typeSuffix, op.getMemory(), op.getValue());
//        }
//    }
//
//    private void printMoveOp(MIRMoveOp op) {
//        String typeSuffix = "";
//        switch (op.getMoveType()) {
//            case INTEGER: typeSuffix = ".i"; break;
//            case FLOAT: typeSuffix = ".f"; break;
//            case INT_TO_FLOAT: typeSuffix = ".i2f"; break;
//            case FLOAT_TO_INT: typeSuffix = ".f2i"; break;
//        }
//        writer.printf("mv%s %s, %s\n", typeSuffix, op.getDest(), op.getSrc());
//    }
//
//    private void printConvertOp(MIRConvertOp op) {
//        String opStr = "";
//        switch (op.getOp()) {
//            case ITOFP: opStr = "itofp"; break;
//            case FPTOI: opStr = "fptoi"; break;
//        }
//        writer.printf("%s %s, %s\n", opStr, op.getDest(), op.getSrc());
//    }
//
//    private void printLiOp(MIRLiOp op) {
//        writer.printf("li %s, %s\n", op.getDest(), op.getImm());
//    }
//
//    private void printLuiOp(MIRLuiOp op) {
//        if (op.getGlobal() != null) {
//            writer.printf("lui %s, %%hi(%s)\n", op.getDest(), op.getGlobal());
//        } else {
//            writer.printf("lui %s, %s\n", op.getDest(), op.getImm());
//        }
//    }
//
//    private void printAllocOp(MIRAllocOp op) {
//        writer.printf("alloc %s, size=%d, align=%d\n",
//                op.getDest(), op.getSize(), op.getAlign());
//    }

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

    private String formatValue(Object value) {
        if (value instanceof Integer) {
            return String.valueOf(value);
        } else if (value instanceof Float) {
            return String.format("%ff", value);
        } else if (value instanceof Double) {
            return String.format("%fd", value);
        } else {
            return String.valueOf(value);
        }
    }
}