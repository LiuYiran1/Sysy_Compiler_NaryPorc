package com.compiler.ir;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.bytedeco.llvm.global.LLVM.*;

public class IRTypeAnalyzer {

    private final LLVisitor llVisitor;
    private final String outputFilePath;

    public IRTypeAnalyzer(LLVisitor llVisitor, String outputFilePath) {
        this.llVisitor = llVisitor;
        this.outputFilePath = outputFilePath;
    }

    public void analyze() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            var mod = llVisitor.getMod();

            // 新增全局变量分析
            writer.println("\nGlobal Variables:");
            for (var global = LLVMGetFirstGlobal(mod.getRef()); global != null; global = LLVMGetNextGlobal(global)) {
                String globalName = LLVMGetValueName(global).getString();
                writer.println("  Global: " + globalName);

                // 获取全局变量类型
                LLVMTypeRef globalType = LLVMTypeOf(global);
                String typeStr = getTypeKindName(globalType);
                writer.println("    Type: " + typeStr);

                // 获取并打印初始值
                LLVMValueRef initValue = LLVMGetInitializer(global);
                if (initValue != null) {
                    String initStr = LLVMPrintValueToString(initValue).getString();
                    writer.println("    Initial Value: " + initStr);
                } else {
                    writer.println("    Initial Value: <none>");
                }

                // 打印对齐信息
                long align = LLVMGetAlignment(global);
                if (align > 0) {
                    writer.println("    Alignment: " + align + " bytes");
                }
                writer.println("    --------------------");
            }


            for (var func = LLVMGetFirstFunction(mod.getRef()); func != null; func = LLVMGetNextFunction(func)) {
                String funcName = LLVMGetValueName(func).getString();
                writer.println("\nFunction: " + funcName);

                for (var block = LLVMGetFirstBasicBlock(func); block != null; block = LLVMGetNextBasicBlock(block)) {
                    String blockName = LLVMGetBasicBlockName(block).getString();
                    writer.println("  Basic Block: " + (blockName.isEmpty() ? "<unnamed>" : blockName));

                    for (var inst = LLVMGetFirstInstruction(block); inst != null; inst = LLVMGetNextInstruction(inst)) {
                        // 获取指令操作码和名称
                        int opcode = LLVMGetInstructionOpcode(inst);
                        String opcodeName = getOpcodeName(opcode);
//                        if(!opcodeName.equals("zext") && !opcodeName.equals("bitcast") && !opcodeName.equals("sitofp") && !opcodeName.equals("fptosi")) {
//                            continue;
//                        }
//                        if(!opcodeName.equals("store") && !opcodeName.equals("load") && !opcodeName.equals("alloc")) {
//                            continue;
//                        }
//                        if(!opcodeName.equals("icmp") && !opcodeName.equals("fcmp")) {
//                            continue;
//                        }
                        if(!opcodeName.equals("getelementptr")){
                            continue;
                        }

                        // 打印指令本身
                        writer.println("    Instruction: " + LLVMPrintValueToString(inst).getString());


                        // 打印结果类型
                        LLVMTypeRef resultType = LLVMTypeOf(inst);
                        String typeStr = getTypeKindName(resultType);
                        writer.println("      Result Type: " + typeStr);

                        // 打印操作数类型和值
                        int numOperands = LLVMGetNumOperands(inst);
                        if (numOperands > 0) {
                            writer.println("      Operand Types and Values:");
                            for (int i = 0; i < numOperands; i++) {
                                LLVMValueRef operand = LLVMGetOperand(inst, i);
                                LLVMTypeRef operandType = LLVMTypeOf(operand);
                                String operandTypeStr = getTypeKindName(operandType);

                                // 获取操作数值的字符串表示
                                writer.println("        Operand " + i + ": [" + operandTypeStr + "] " + LLVMPrintValueToString(operand).getString());

                            }
                        } else {
                            writer.println("      No operands");
                        }
                        // 打印对齐信息（如果有）
                        long align = LLVMGetAlignment(inst);
                        if (align > 0) {
                            writer.println("      Alignment: " + align + " bytes");
                        }
                        writer.println("    --------------------");
                    }
                }
            }
            System.out.println("IR analysis saved to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + outputFilePath);
            e.printStackTrace();
        }
    }


    // 获取操作码名称
    private String getOpcodeName(int opcode) {
        return switch (opcode) {
            case LLVMRet -> "ret";
            case LLVMBr -> "br";
            case LLVMCall -> "call";
            case LLVMAlloca -> "alloc";
            case LLVMStore -> "store";
            case LLVMLoad -> "load";
            case LLVMGetElementPtr -> "getelementptr";
            case LLVMAdd -> "add";
            case LLVMSub -> "sub";
            case LLVMMul -> "mul";
            case LLVMSDiv -> "div";
            case LLVMSRem -> "rem";
            case LLVMBitCast -> "bitcast";
            case LLVMICmp -> "icmp";
            case LLVMFCmp -> "fcmp";
            case LLVMZExt -> "zext";
            case LLVMSIToFP -> "sitofp";
            case LLVMFPToSI -> "fptosi";
            case LLVMFAdd -> "fadd";
            case LLVMFSub -> "fsub";
            case LLVMFMul -> "fmul";
            case LLVMFDiv -> "fdiv";
            default -> "UnknownOpcode(" + opcode + ")";
        };
    }

    // 将LLVMTypeKind转换为可读字符串
    private String getTypeKindName(LLVMTypeRef typeRef) {
        int kind = LLVMGetTypeKind(typeRef);
        return switch (kind) {
            case LLVMVoidTypeKind -> "void";
            case LLVMHalfTypeKind -> "half";
            case LLVMFloatTypeKind -> "float";
            case LLVMDoubleTypeKind -> "double";
            case LLVMX86_FP80TypeKind -> "x86_fp80";
            case LLVMFP128TypeKind -> "fp128";
            case LLVMPPC_FP128TypeKind -> "ppc_fp128";
            case LLVMLabelTypeKind -> "label";
            case LLVMIntegerTypeKind ->
                    "i" + LLVMGetIntTypeWidth(typeRef); // 包含整数位宽
            case LLVMFunctionTypeKind -> "function";
            case LLVMStructTypeKind -> "struct";
            case LLVMArrayTypeKind -> {
                long length = LLVMGetArrayLength(typeRef);
                LLVMTypeRef elementType = LLVMGetElementType(typeRef);
                String elementTypeName = getTypeKindName(elementType); // 指的是数组中每个index存的类型 i32/float
                yield "array[" + length + "] of " + elementTypeName;
            }
            case LLVMPointerTypeKind -> {
                LLVMTypeRef pointedType = LLVMGetElementType(typeRef);
                String pointedTypeName = getTypeKindName(pointedType);
                // 特殊处理指针指向数组的情况
                if (LLVMGetTypeKind(pointedType) == LLVMArrayTypeKind) {
                    yield "pointer to " + pointedTypeName;
                }
                yield "pointer to " + pointedTypeName;
            } // 显示指针指向类型
            case LLVMVectorTypeKind ->
                    "vector[" + LLVMGetVectorSize(typeRef) + "] of " + getTypeKindName(LLVMGetElementType(typeRef));
            case LLVMMetadataTypeKind -> "metadata";
            case LLVMTokenTypeKind -> "token";
            default -> "UnknownTypeKind(" + kind + ")";
        };
    }

}