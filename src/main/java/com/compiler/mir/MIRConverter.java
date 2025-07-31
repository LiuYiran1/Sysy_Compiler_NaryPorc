package com.compiler.mir;

import com.compiler.mir.instruction.*;
import com.compiler.mir.operand.*;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import org.llvm4j.llvm4j.Module;

import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class MIRConverter {

    // 添加调试信息字段
    private String currentFunction = "";
    private String currentBasicBlock = "";
    private LLVMValueRef currentInstruction = null;
    private int instructionCount = 0;

    private final Module llvmModule;
    private final MIRModule mirModule = new MIRModule();
    private final Map<LLVMValueRef, MIROperand> valueMap = new LinkedHashMap<>();
    // 添加浮点常量池 优化时再用
//    private final Map<Double, MIRFloatConstant> floatConstantPool = new LinkedHashMap<>();
//    private final Map<MIRFunction, List<MIRFloatConstant>> functionFloatConstants = new LinkedHashMap<>();

    // 调试日志开关
    private static final boolean DEBUG = true;

    public MIRConverter(Module llvmModule) {
        this.llvmModule = llvmModule;
    }


    // 调试方法
    private void debugLog(String message) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + message);
        }
    }

    private void setCurrentContext(LLVMValueRef func, LLVMBasicBlockRef bb, LLVMValueRef inst) {
        if (func != null) {
            currentFunction = LLVMGetValueName(func).getString();
        }
        if (bb != null) {
            currentBasicBlock = LLVMGetBasicBlockName(bb).getString();
        }
        currentInstruction = inst;
    }

    private String getCurrentContext() {
        return String.format("Function: %s, Block: %s, Inst: %s",
                currentFunction, currentBasicBlock,
                currentInstruction != null ? LLVMPrintValueToString(currentInstruction).getString() : "N/A");
    }

    public MIRModule convert() {
        // 转换全局变量
        for(var global = LLVMGetFirstGlobal(llvmModule.getRef()); global != null; global = LLVMGetNextGlobal(global)) {
            debugLog("Converting global: " + LLVMGetValueName(global).getString());
            convertGlobalVariable(global);
        }

        // 转换所有函数
        for(var func = LLVMGetFirstFunction(llvmModule.getRef()); func != null; func = LLVMGetNextFunction(func)) {
            debugLog("Converting function: " + LLVMGetValueName(func).getString());
            convertFunction(func);
        }
        return mirModule;
    }

    private void convertGlobalVariable(LLVMValueRef global) {
        // 处理全局变量的初始值
        // TODO: 这里可以添加对全局变量初始值的处理逻辑
        String name = LLVMGetValueName(global).getString();
        debugLog("Processing global: " + name);

        if (LLVMIsAGlobalVariable(global) == null) {
            throw new IllegalArgumentException("Expected a global variable, but got: " + name);
        }
        // 判断global指向的类型，看是不是数组
        LLVMTypeRef pointedType = LLVMGetElementType(LLVMTypeOf(global));
        MIRType mirType = convertType(pointedType);
        if(MIRType.isInt(mirType)) {
            // int型全局变量

            MIRGlobalVariable intGlobal = new MIRGlobalVariable(name, mirType, LLVMConstIntGetSExtValue(LLVMGetInitializer(global)));
            System.out.println(intGlobal.getIntValue());
            mirModule.addGlobalVariable(intGlobal);

        } else if (MIRType.isFloat(mirType)) {
            // float型全局变量

            MIRGlobalVariable floatGlobal = new MIRGlobalVariable(name, mirType, LLVMConstRealGetDouble(LLVMGetInitializer(global), new IntPointer(1)));
            System.out.println(floatGlobal.getFloatValue());
            mirModule.addGlobalVariable(floatGlobal);

        } else {
            // 数组类型全局变量
            // 获取数组元素类型
            LLVMValueRef element = LLVMGetInitializer(global);
            MIRType elementType = convertType(getBaseElementType(pointedType));
            int arraySize = getArrayLength(pointedType);
            System.out.println(arraySize);
            if(LLVMIsAConstantAggregateZero(element) != null) {
                // zero-initialized array
                MIRGlobalVariable mirGlobal = new MIRGlobalVariable(name, elementType,arraySize);
                System.out.println(name + " is zero-initialized array of type " + elementType);
                mirModule.addGlobalVariable(mirGlobal);
                return;
            }
            List<Object> values = new ArrayList<>();

            extractArrayValues(element, pointedType, values);
            MIRGlobalVariable mirGlobal = new MIRGlobalVariable(name, elementType, arraySize, values);
            mirModule.addGlobalVariable(mirGlobal);
//            if(MIRType.isInt(elementType)) {
//                // int[][] ……
//                for (int i = 0; i < arraySize; i++) {
//                    int value = (int)LLVMConstIntGetSExtValue(LLVMGetElementAsConstant(element,i));
//                    System.out.println(name + i + " : " + value);
//                    values.add(value);
//                }
//                MIRGlobalVariable mirGlobal = new MIRGlobalVariable(name, elementType, arraySize, values);
//                mirModule.addGlobalVariable(mirGlobal);
//            } else if (MIRType.isFloat(elementType)) {
//                // float[][] ……
//                for (int i = 0; i < arraySize; i++) {
//
//                    double value = LLVMConstRealGetDouble(LLVMGetElementAsConstant(element,i), new IntPointer(1));
//                    System.out.println(name + i + " : " + value);
//                    values.add(value);
//                }
//                MIRGlobalVariable mirGlobal = new MIRGlobalVariable(name, elementType, arraySize, values);
//                mirModule.addGlobalVariable(mirGlobal);
//            } else {
//                throw new IllegalArgumentException("Unsupported global variable type: " + elementType);
//            }

        }
    }

    // 递归提取多维数组的值
    // 哎，javaAPI少了个LLVMGetAggregateElement,只能处理一位数组
    private void extractArrayValues(LLVMValueRef arrayValue, LLVMTypeRef arrayType, List<Object> values) {
        int arraySize = LLVMGetArrayLength(arrayType);
        System.out.println(arraySize);
        LLVMTypeRef elementType = LLVMGetElementType(arrayType);
        int elementKind = LLVMGetTypeKind(elementType);


        for (int i = 0; i < arraySize; i++) {
            System.out.println(LLVMIsAConstantArray(arrayValue) != null); // true

            LLVMValueRef element = LLVMGetElementAsConstant(arrayValue, i);
            if (elementKind == LLVMArrayTypeKind) {
                // 如果元素本身也是数组，递归处理
                extractArrayValues(element, elementType, values);
            } else {
                // 基本类型元素
                if (LLVMIsAConstantInt(element) != null) {
                    long intValue = LLVMConstIntGetSExtValue(element);
                    values.add(intValue);
                    debugLog("Array element[" + i + "] = " + intValue);
                }
                else if (LLVMIsAConstantFP(element) != null) {
                    double floatValue = LLVMConstRealGetDouble(element, new IntPointer(1));
                    values.add(floatValue);
                    debugLog("Array element[" + i + "] = " + floatValue);
                }
                else if (LLVMIsAConstantPointerNull(element) != null) {
                    // 处理指针类型的零初始化
                    values.add(0);
                    debugLog("Array element[" + i + "] = null pointer");
                }
                else {
                    throw new IllegalArgumentException("Unsupported array element type: " +
                            LLVMPrintTypeToString(elementType).getString());
                }
            }
        }
    }

    private void convertFunction(LLVMValueRef llvmFunc) {
        String funcName = LLVMGetValueName(llvmFunc).getString();

        MIRFunction mirFunc = new MIRFunction(funcName);
        mirModule.addFunction(mirFunc);

        debugLog("Starting conversion for function: " + funcName);
        // 创建参数虚拟寄存器
        // TODO: 这里可以添加对函数参数的处理逻辑
        int count = LLVMCountParams(llvmFunc);
        for(int i = 0; i < count; i++) {
            LLVMValueRef param = LLVMGetParam(llvmFunc, i);
            if (param == null) continue; // 忽略空参数
            MIRType mirType = convertType(LLVMTypeOf(param));
//            MIRVirtualReg paramReg = mirFunc.newVirtualReg(mirType);
//            valueMap.put(param, paramReg);
//            mirFunc.addParam(paramReg);
            if (i < 8) {
                MIRPhysicalReg paramReg;
                if (MIRType.isFloat(mirType)) {
                    // 浮点类型参数
                    paramReg = new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.FA0.ordinal() + i]);
                } else {
                    // ptr + 整数类型
                    paramReg = new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.A0.ordinal() + i]);
                }
                valueMap.put(param, paramReg);
                mirFunc.addParam(paramReg);
            } else {
                int offset = (i - 8) * 8; // 从第9个参数开始，每个参数占8个字节
                MIRMemory paramMem = new MIRMemory(
                        new MIRPhysicalReg(MIRPhysicalReg.PREGs.FP), // 使用FP寄存器作为基址
                        new MIRImmediate(offset, MIRType.I64), // 偏移量为0
                        mirType
                );
                valueMap.put(param, paramMem);
                mirFunc.addParam(paramMem);
            }

        }

        if(LLVMGetFirstBasicBlock(llvmFunc) == null) {
            System.out.println("procedure " + funcName + " has no basic blocks, skipping conversion.");
            return; // 如果函数没有基本块，直接跳过转换
        }
        // 转换基本块
        for(var bb = LLVMGetFirstBasicBlock(llvmFunc); bb != null; bb = LLVMGetNextBasicBlock(bb)) {

            setCurrentContext(llvmFunc, bb, null);
            System.out.println(getCurrentContext());
            convertBasicBlock(bb, mirFunc);
        }

        MIRBasicBlock exitBlock = new MIRBasicBlock(mirFunc.getName() + "Return");
        // 添加函数返回块
        if(!mirFunc.getName().equals("main")){
            exitBlock.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLEE_RESTORE_REG,0));
        }
        exitBlock.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.EPILOGUE, 0)); // 函数出口
        exitBlock.getInstructions().add(new MIRControlFlowOp()); // ret

        mirFunc.addBlock(exitBlock);
        // 构建CFG
        //buildControlFlowGraph(mirFunc);

        // 消除PHI节点（关键步骤）
        eliminatePhiNodes(mirFunc);

        debugLog("Finished conversion for function: " + funcName);
    }

    private void convertBasicBlock(LLVMBasicBlockRef llvmBB, MIRFunction mirFunc) {
        String bbName = LLVMGetBasicBlockName(llvmBB).getString();
        MIRBasicBlock mirBB = new MIRBasicBlock(bbName);

        debugLog("Converting basic block: " + bbName);
        if(mirBB.getLabel().toString().contains("Entry")) {
            // 如果是入口块，添加函数序言
            mirBB.getInstructions().add(new MIRPseudoOp(
                        MIRPseudoOp.Type.PROLOGUE,
                        0 // 初始帧大小为0
            ));
            if(!mirFunc.getName().equals("main")){
                // 如果不是main函数，添加保存寄存器的指令
                mirBB.getInstructions().add(new MIRPseudoOp(
                        MIRPseudoOp.Type.CALLEE_SAVE_REG,
                        0 // 这个0无意义
                ));
            }
        }

        for(var inst = LLVMGetFirstInstruction(llvmBB); inst != null; inst = LLVMGetNextInstruction(inst)) {
            setCurrentContext(null, llvmBB, inst);
            instructionCount++;
            debugLog("Converting instruction #" + instructionCount + ": " +
                    LLVMPrintValueToString(inst).getString().trim());
            System.out.println(getCurrentContext());
            convertInstruction(inst, mirFunc, mirBB);

        }

        mirFunc.addBlock(mirBB);
        System.out.println(mirBB.getLabel().toString());
    }

    private void convertInstruction(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        int opcode = LLVMGetInstructionOpcode(inst);
        switch (opcode) {
            case LLVMRet:
                convertReturnInst(inst, mirFunc, mirBB);
                break;
            case LLVMCall:
                convertCallInst(inst, mirFunc, mirBB);
                break;
            case LLVMAdd:
            case LLVMFAdd:
            case LLVMSub:
            case LLVMFSub:
            case LLVMMul:
            case LLVMFMul:
            case LLVMSDiv:
            case LLVMFDiv:
            case LLVMSRem:
            case LLVMFRem:
            case LLVMXor:
                convertBinaryOp(inst, mirFunc, mirBB);
                break;
            case LLVMAlloca:
                convertAlloca(inst, mirFunc, mirBB);
                break;
            case LLVMStore:
                convertStoreInst(inst, mirFunc, mirBB);
                break;
            case LLVMLoad:
                convertLoadInst(inst, mirFunc, mirBB);
                break;
            case LLVMBr:
                convertBranchInst(inst, mirFunc, mirBB);
                break;
            case LLVMICmp:
            case LLVMFCmp:
                convertCmpInst(inst, mirFunc, mirBB);
                break;
            case LLVMZExt:
                convertZExtInst(inst, mirFunc, mirBB);
                break;
            case LLVMFPToSI:
                convertFPToSIInst(inst, mirFunc, mirBB);
                break;
            case LLVMSIToFP:
                convertSIToFPInst(inst, mirFunc, mirBB);
                break;
            case LLVMBitCast:
                convertBitCast(inst, mirFunc, mirBB);
                break;
            case LLVMGetElementPtr:
                convertGEPInst(inst, mirFunc, mirBB);
                break;
            case LLVMPHI:
                // PHI节点稍后处理
                convertPhiInst(inst, mirFunc, mirBB);
                break;
            case LLVMSelect:
                // 处理选择指令
                convertSelectInst(inst, mirFunc, mirBB);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported instruction code: " + opcode);
        }
    }

    private void convertSelectInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.SELECT, 0)); // 添加选择指令的伪操作
    }

    private void convertSIToFPInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // fcvt.s.w
        LLVMValueRef srcVal = LLVMGetOperand(inst, 0);
        MIROperand src = getMIRValue(srcVal, mirFunc, mirBB);

        MIRVirtualReg dest = mirFunc.newVirtualReg(MIRType.F32);
        mirBB.getInstructions().add(new MIRConvertOp(MIRConvertOp.Op.ITOFP, dest, src));
        valueMap.put(inst, dest);
    }

    private void convertFPToSIInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // fcvt.w.s
        LLVMValueRef srcVal = LLVMGetOperand(inst, 0);
        MIROperand src = getMIRValue(srcVal, mirFunc, mirBB);

        MIRVirtualReg dest = mirFunc.newVirtualReg(MIRType.I32);
        mirBB.getInstructions().add(new MIRConvertOp(MIRConvertOp.Op.FPTOI, dest, src));
        valueMap.put(inst, dest);
    }


    // %idx646 = zext i32 %i.0 to i64
    //%zextForGt = zext i1 %gt to i32
    private void convertZExtInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        LLVMValueRef srcVal = LLVMGetOperand(inst, 0);
        MIROperand src = getMIRValue(srcVal, mirFunc, mirBB);
        MIRVirtualReg dest = null;
        if(MIRType.isInt(src.getType())) {
            dest = mirFunc.newVirtualReg(MIRType.I64);
            // TODO: 处理整数类型的零扩展
        } else {
            dest = mirFunc.newVirtualReg(MIRType.I32);
            // TODO: 处理其他类型的零扩展
        }
        mirBB.getInstructions().add(new MIRConvertOp(MIRConvertOp.Op.ZEXT, dest, src));
        valueMap.put(inst, dest);
    }

    private void convertBranchInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        int numOperands = LLVMGetNumOperands(inst);
        LLVMValueRef target = LLVMGetOperand(inst, 0);
        // TODO: 处理分支指令

        if (numOperands == 1) { // 无条件跳转
            String label = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(target)).getString();

            MIRLabel mirLabel = new MIRLabel(label);
            System.out.println(mirLabel.toString());
            mirBB.getInstructions().add(new MIRControlFlowOp(mirLabel));
        } else if (numOperands == 3) { // 条件跳转
            MIROperand cond = getMIRValue(LLVMGetOperand(inst, 0), mirFunc, mirBB);
            String trueLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(LLVMGetOperand(inst, 2))).getString();
            String falseLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(LLVMGetOperand(inst, 1))).getString();
            MIRLabel trueTarget = new MIRLabel(trueLabel);
            MIRLabel falseTarget = new MIRLabel(falseLabel);

            mirBB.getInstructions().add(new MIRControlFlowOp(cond, trueTarget)); // bnez
            mirBB.getInstructions().add(new MIRControlFlowOp(falseTarget));   // j
        }
    }

    private void convertCmpInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        int predicate = LLVMIsAICmpInst(inst) != null ?
                LLVMGetICmpPredicate(inst) :
                LLVMGetFCmpPredicate(inst);

        // 分配目标寄存器
        MIRVirtualReg result = mirFunc.newVirtualReg(MIRType.I1);
        valueMap.put(inst, result);

        // 处理操作数
        LLVMValueRef op1 = LLVMGetOperand(inst, 0);
        LLVMValueRef op2 = LLVMGetOperand(inst, 1);
        MIROperand src1 = getMIRValue(op1, mirFunc, mirBB);
        MIROperand src2 = getMIRValue(op2, mirFunc, mirBB);

        if(src1 instanceof MIRImmediate){
            src1 = immToReg(mirFunc,mirBB, ((MIRImmediate) src1).getValue());
        } else if(src2 instanceof MIRImmediate){
            src2 = immToReg(mirFunc,mirBB, ((MIRImmediate) src2).getValue());
        }

        // 根据比较类型生成对应指令
        if (LLVMIsAICmpInst(inst) != null) {
            switch (predicate) {
                case LLVMIntEQ:  // ==
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.EQ, result, src1, src2));
                    break;
                case LLVMIntNE:  // !=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.NE, result, src1, src2));
                    break;
                case LLVMIntSGT: // > (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.GT, result, src1, src2));
                    break;
                case LLVMIntSLT: // < (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.LT, result, src1, src2));
                    break;
                case LLVMIntSGE: // >= (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.GE, result, src1, src2));
                    break;
                case LLVMIntSLE: // <= (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.LE, result, src1, src2));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported comparison predicate");
            }
        } else {
            switch (predicate) {
                case LLVMRealOEQ: // ==
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.EQ, result, src1, src2));
                    break;
                case LLVMRealONE: // !=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.NE, result, src1, src2));
                    break;
                case LLVMRealOGT: // >
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.GT, result, src1, src2));
                    break;
                case LLVMRealOLT: // <
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.LT, result, src1, src2));
                    break;
                case LLVMRealOGE: // >=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.GE, result, src1, src2));
                    break;
                case LLVMRealOLE: // <=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.LE, result, src1, src2));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported floating-point comparison predicate");
            }
        }
    }

    // 注意load和store的出现场景
    // offset其实都是0
    private void convertLoadInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIROperand addr = getMIRValue(LLVMGetOperand(inst, 0), mirFunc, mirBB);
        MIRType type = convertType(LLVMGetElementType(LLVMTypeOf(LLVMGetOperand(inst, 0)))); // bug1 已经修正
        MIRMemory memory = new MIRMemory(addr, new MIRImmediate(0, MIRType.I64), type);
        MIRVirtualReg result = mirFunc.newVirtualReg(type);
        valueMap.put(inst, result);

        if (MIRType.isFloat(type)) {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.LOAD, MIRMemoryOp.Type.FLOAT, memory, result));
        } else {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.LOAD, MIRMemoryOp.Type.INTEGER, memory, result));
        }
    }

    private void convertStoreInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIROperand value = getMIRValue(LLVMGetOperand(inst, 0), mirFunc, mirBB);
        MIROperand addr = getMIRValue(LLVMGetOperand(inst, 1), mirFunc, mirBB);
        MIRMemory memory = new MIRMemory(addr, new MIRImmediate(0, MIRType.I64), value.getType());

        if (MIRType.isFloat(value.getType())) {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.FLOAT, memory,value));
        } else {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.INTEGER, memory, value));
        }
    }

    private void convertAlloca(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
         // TODO: 实现内存分配指令转换
        // 两处： 函数返回值和数组
        // 首先分配栈空间
        int offset = 0;
        LLVMTypeRef typeRef = LLVMGetElementType(LLVMTypeOf(inst));
        int kind = LLVMGetTypeKind(typeRef);
        if(kind == LLVMArrayTypeKind) {
            // 数组类型
            int arraySize = getArrayLength(typeRef);
            LLVMTypeRef elementType = LLVMGetElementType(typeRef);
            MIRType mirType = convertType(elementType);
            offset = arraySize * 4; // 每个元素占8个字节（riscV64） 不对，应该还是4
        } else {
            // 单个变量
            offset = 4;
        }
        // 关于sp的offset只需统计所有的alloc指令的size即可
        MIRVirtualReg result = mirFunc.newVirtualReg(MIRType.I64);
        valueMap.put(inst, result); // 又找出一个bug
        mirBB.getInstructions().add(new MIRAllocOp(result,offset,0));
        // 后期得替换成%0 = sp - offset 类似形式
    }

    private void convertCallInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
         // TODO: 实现函数调用指令转换
        // 保存相关寄存器
        // 怎么能在这里做个标记在后端处理时能完成这个操作
        mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLER_SAVE_REG,0));

        // 处理参数 + 传参
        List<MIROperand> args = new ArrayList<>();
        int numOperands = LLVMGetNumOperands(inst);
        for (int i = 0; i < numOperands - 1; i++) { // 从0开始，最后一个是callee
            LLVMValueRef arg = LLVMGetOperand(inst, i);
            MIRType argType = convertType(LLVMTypeOf(arg));
            MIROperand mirArg = getMIRValue(arg, mirFunc, mirBB);
            args.add(mirArg);
            if(i < 8){
                if(MIRType.isFloat(argType)) {
                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.FA0.ordinal() + i]),mirArg, MIRMoveOp.MoveType.FLOAT));
                } else {
                    // 整数类型或指针类型
                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.A0.ordinal() + i]),mirArg, MIRMoveOp.MoveType.INTEGER));
                }
            } else {
                // 如果参数超过8个，使用栈传递
                // 这里的偏移量需要计算
                int offset = (i - 8) * 8; // 每个参数占8个字节

                MIRMemory argMem = new MIRMemory(new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), new MIRImmediate(offset, MIRType.I64), mirArg.getType());
                if(MIRType.isFloat(argType)) {
                    mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.FLOAT, argMem, mirArg));
                } else if(MIRType.isInt(argType)) {
                    // 整数类型
                    mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.INTEGER, argMem, mirArg));
                } else {
                    // 指针类型
                    mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.POINTER, argMem, mirArg));
                }

            }
        }

        // 为创建调用指令做准备
        LLVMValueRef callee = LLVMGetOperand(inst, numOperands - 1);
        if (LLVMIsAFunction(callee) == null) {
            throw new IllegalArgumentException("Expected a function call, but got: " + LLVMGetValueName(callee).getString());
        }
        String calleeName = LLVMGetValueName(callee).getString();
        MIRLabel funcLabel = new MIRLabel(calleeName);

        // 接受返回值，但void呢？
        MIRType mirType = convertType(LLVMTypeOf(inst));
        if(MIRType.isVoid(mirType)) {
            // 如果是void类型,就不用接受返回值了
            mirBB.getInstructions().add(new MIRControlFlowOp(funcLabel, args));
        } else {
            MIRVirtualReg result = mirFunc.newVirtualReg(mirType);
            valueMap.put(inst, result);
            mirBB.getInstructions().add(new MIRControlFlowOp(funcLabel, result, args));

            //下面是LLVM处理
//        %avg = alloca float, align 4
//        %0 = call float @calculate_average([5 x i32]* %int_arrayArr, i32 5)
//        store float %0, float* %avg, align 4
            // 下面是LL处理
//        %call.2 = call float @calculate_average([5 x i32]* %int_arrayArr, i32 5)

            // 存储返回值 以LL为准处理
            if (MIRType.isFloat(result.getType())) {
                // 如果返回值是浮点数
                mirBB.getInstructions().add(new MIRMoveOp(result, new MIRPhysicalReg(MIRPhysicalReg.PREGs.FA0), MIRMoveOp.MoveType.FLOAT));
            } else {
                // 如果返回值是整数
                mirBB.getInstructions().add(new MIRMoveOp(result, new MIRPhysicalReg(MIRPhysicalReg.PREGs.A0), MIRMoveOp.MoveType.INTEGER));
            }

        }

        // 恢复相关寄存器
        // 怎么能在这里做个标记在后端处理时能完成这个操作
        mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLER_RESTORE_REG,0));
    }

    // bug4,这个方法写的真是错漏百出
    private void convertReturnInst(LLVMValueRef inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
         // TODO: 实现返回指令转换
        // 恢复栈空间，恢复某些寄存器的值，这些步骤都交给exitbb处理

        // 把返回值存到a0然后跳转到func.name + "Return"
//        LLVMTypeRef typeRef = LLVMTypeOf(inst);
//        int kind = LLVMGetTypeKind(typeRef);// bug3,比较大的一个问题，这里永远都是void，
        int kind = LLVMGetNumOperands(inst);
        System.out.println(kind);

        if (kind == 0) {
            // 无返回值，直接返回
            mirBB.getInstructions().add(new MIRControlFlowOp(new MIRLabel(mirFunc.getName() + "Return")));
            return;
        } else {
            // 有返回值，处理返回值
            LLVMValueRef retVal = LLVMGetOperand(inst, 0);
            MIRType mirType = convertType(LLVMTypeOf(retVal));

            MIROperand retOperand = getMIRValue(retVal, mirFunc, mirBB);

            if(retOperand instanceof MIRImmediate) {
                // 如果是立即数，转换为寄存器
                mirBB.getInstructions().add(new MIRLiOp(MIRLiOp.Op.LI, new MIRPhysicalReg(MIRPhysicalReg.PREGs.A0), (MIRImmediate) retOperand));
                //retOperand = immToReg(mirFunc, mirBB, ((MIRImmediate) retOperand).getValue());
            } else {
                // 将返回值存储到寄存器
                if(MIRType.isFloat(mirType)){
                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.FA0), retOperand, MIRMoveOp.MoveType.FLOAT));
                } else {
                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.A0), retOperand, MIRMoveOp.MoveType.INTEGER));
                }

            }

            // 跳转到函数返回标签
            mirBB.getInstructions().add(new MIRControlFlowOp(new MIRLabel(mirFunc.getName() + "Return")));
        }

    }

    // 实现GetElementPtr指令转换
    private void convertGEPInst(LLVMValueRef gep, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIRType ptrType = convertType(LLVMTypeOf(gep)); // 获取指针类型
        MIRVirtualReg result = mirFunc.newVirtualReg(ptrType);
        valueMap.put(gep, result);
        // TODO: 处理GEP的操作数

        if (LLVMGetNumOperands(gep) == 2) {
            // TODO: 两个操作数的

            LLVMValueRef basePtr = LLVMGetOperand(gep, 0);
            MIROperand base = getMIRValue(basePtr, mirFunc, mirBB);

            LLVMValueRef index = LLVMGetOperand(gep, 1);
            MIROperand indexOp = getMIRValue(index, mirFunc, mirBB);

            if(indexOp instanceof MIRImmediate){
                // 如果是立即数，转换为寄存器
                indexOp = immToReg(mirFunc, mirBB, ((MIRImmediate) indexOp).getValue() * 4); // 这里应该*4吧？
            }

            mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.SUB,result,MIRArithOp.Type.INT,base, indexOp));

        } else {
            // TODO: 三个操作数的
            LLVMValueRef basePtr = LLVMGetOperand(gep, 0);
            MIROperand base = getMIRValue(basePtr, mirFunc, mirBB);

            LLVMTypeRef elementType = LLVMGetElementType(LLVMTypeOf(basePtr));
            int kind = LLVMGetTypeKind(elementType);
            if(kind == LLVMArrayTypeKind){
                // 数组类型

                int ptrSize = getArrayLength(LLVMGetElementType(elementType)); // 获取指针指向的大小

                if(ptrSize <= 0) {
                    System.err.println("arrayLen = " + ptrSize);
                    throw new IllegalArgumentException("Array size must be greater than 0");
                }
                LLVMValueRef index = LLVMGetOperand(gep, 2);
                MIROperand indexOp = getMIRValue(index, mirFunc, mirBB);

                if(indexOp instanceof MIRImmediate){
                    // 如果是立即数，转换为寄存器
                    indexOp = immToReg(mirFunc, mirBB, ((MIRImmediate) indexOp).getValue() * ptrSize * 4); // 这里应该*4吧？

                }
                mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.SUB,result,MIRArithOp.Type.INT,base, indexOp));
            } else {
                // 说明是一个 i32* 或 f32*
                // 有这种情况吗？ 没有
                throw new IllegalArgumentException("Unsupported base pointer type: " + LLVMPrintTypeToString(LLVMTypeOf(basePtr)).getString());
            }


        }

    }

    // 实现位转换指令
    private void convertBitCast(LLVMValueRef bitCast, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIRType destType = convertType(LLVMTypeOf(bitCast));
        MIRVirtualReg result = mirFunc.newVirtualReg(destType);
        valueMap.put(bitCast, result);
        MIROperand source = getMIRValue(LLVMGetOperand(bitCast,0),mirFunc,mirBB);
         // TODO : 处理位转换逻辑
        // 位转换在MIR中表现为直接赋值
        // MOVE不太合适 ,或者说没必要？
        mirBB.getInstructions().add(new MIRMoveOp(result, source, MIRMoveOp.MoveType.INTEGER));
    }

    private void convertBinaryOp(LLVMValueRef inst, MIRFunction mirFunc,MIRBasicBlock mirBB) {

        MIRType type = convertType(LLVMTypeOf(inst));
        System.out.println("Binary Op Type: " + type);
        MIRVirtualReg result = mirFunc.newVirtualReg(type);
        valueMap.put(inst, result);
        MIROperand left = getMIRValue(LLVMGetOperand(inst,0),mirFunc,mirBB);
        MIROperand right = getMIRValue(LLVMGetOperand(inst,1),mirFunc,mirBB);

        int opcode = LLVMGetInstructionOpcode(inst);

        // TODO: 处理二元运算逻辑
        MIRArithOp.Op op = null;
        switch (opcode) {
            case LLVMAdd:
            case LLVMFAdd:
                op = MIRArithOp.Op.ADD;
                break;
            case LLVMSub:
            case LLVMFSub:
                op = MIRArithOp.Op.SUB;
                break;
            case LLVMMul:
            case LLVMFMul:
                op = MIRArithOp.Op.MUL;
                break;
            case LLVMSDiv:
            case LLVMFDiv:
                op = MIRArithOp.Op.DIV;
                break;
            case LLVMSRem: op = MIRArithOp.Op.REM; break;
            case LLVMXor: op = MIRArithOp.Op.XOR; break;
            default:
                throw new UnsupportedOperationException("Unsupported binary op: " + opcode);
        }
        mirBB.getInstructions().add(new MIRArithOp(op, result,MIRType.isFloat(type) ? MIRArithOp.Type.FLOAT : MIRArithOp.Type.INT, left, right));
    }

    private void convertPhiInst(LLVMValueRef phi,MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // 收集所有PHI节点
        MIRVirtualReg phiReg = mirFunc.newVirtualReg(convertType(LLVMTypeOf(phi)));
        valueMap.put(phi, phiReg);

        mirFunc.addLLVMPhiNode(mirBB,phi);

    }

    // 最优PHI消除实现
    private void eliminatePhiNodes(MIRFunction mirFunc) {

        // 为每个PHI节点插入MOV指令
        for (Map.Entry<MIRBasicBlock, List<LLVMValueRef>> entry : mirFunc.getLLVMPhiNodes().entrySet()) {
            MIRBasicBlock currentBB = entry.getKey();

            for (LLVMValueRef phi : entry.getValue()) {
                MIRVirtualReg phiReg = (MIRVirtualReg) valueMap.get(phi);

                for (int i = 0; i < LLVMGetNumOperands(phi); i++) {
                    LLVMBasicBlockRef incomingBB = LLVMGetIncomingBlock(phi,i);
                    LLVMValueRef incomingValue = LLVMGetIncomingValue(phi,i);

                    System.out.println(LLVMGetBasicBlockName(incomingBB).getString());

                    MIRBasicBlock mirIncomingBB = findMIRBlock(mirFunc, LLVMGetBasicBlockName(incomingBB).getString());

                    MIROperand source = getMIRValue(incomingValue, mirFunc, currentBB);

                    //在入块末尾插入MOV指令
                    int size = mirIncomingBB.getInstructions().size();
                    System.out.println(size);
                    if (size > 0 && mirIncomingBB.getInstructions().get(size - 2) instanceof MIRControlFlowOp) {
                        // 如果倒数第二条指令是控制流指令，插入到前面
                        if(MIRType.isFloat(phiReg.getType())){
                            mirIncomingBB.getInstructions().add(size - 2, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.FLOAT));
                        } else {
                            mirIncomingBB.getInstructions().add(size - 2, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.INTEGER));
                        }

                    } else if(size > 0 && mirIncomingBB.getInstructions().get(size - 1) instanceof MIRControlFlowOp){
                        // 如果倒数第一条指令是控制流指令，插入到前面
                        if(MIRType.isFloat(phiReg.getType())){
                            mirIncomingBB.getInstructions().add(size - 1, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.FLOAT));
                        } else {
                            mirIncomingBB.getInstructions().add(size - 1, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.INTEGER));
                        }
                    } else {
                        // 否则直接添加到末尾 这种情况不应该出现
                        mirIncomingBB.getInstructions().add(new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.INTEGER));
                        throw new RuntimeException("Unexpected instruction at the end of basic block: " + mirIncomingBB.getLabel());
                    }
//                    MIRMoveOp move = new MIRMoveOp(phiReg, source, MIRType.isFloat());
//                    mirIncomingBB.addInstruction(move);
                }
            }
        }
    }


    // 就是在分析一条指令时，获得对应的operand
    /*
        * 找到LLVMValueRef对应的MIR操作数
        * 找到整数立即数对应的MIRImmediate  %iAdd28 = add i32 %i.0, 1
        * 找到浮点数立即数对应的MIRFloatConstant  store float 0x3FF19999A0000000, float* %float_arrayElem0, align 4   %fAdd38 = fadd float %val36, 2.500000e+00
        * 找到全局变量对应的MIRGlobalVariable  %val39 = load i32, i32* @global_counter, align 4
     */
    private MIROperand getMIRValue(LLVMValueRef valueRef, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // 如果已经处理过，直接返回
        if (valueMap.containsKey(valueRef)) {
            return valueMap.get(valueRef);
        }

        // 处理常量值 比如 add %1, 2
        if (LLVMIsAConstantInt(valueRef) != null) {
            return handleConstantInt(valueRef, mirFunc,mirBB);
        } else if (LLVMIsAConstantFP(valueRef) != null) {
            return handleFloatConstant(valueRef, mirFunc, mirBB);
        } else if (LLVMIsAGlobalVariable(valueRef) != null) {
            // load store getelementptr等指令涉及全局变量
            return handleGlobalVariable(valueRef, mirFunc, mirBB);
        }

        // 未知值创建临时寄存器
        MIRVirtualReg tempReg = mirFunc.newVirtualReg(convertType(LLVMTypeOf(valueRef)));
        valueMap.put(valueRef, tempReg);
        return tempReg;
    }

    private MIROperand handleGlobalVariable(LLVMValueRef valueRef, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // %hi 和 %lo
        // 相当于去处理一个I64地址 lui + add 是静态链接吗？
        MIRVirtualReg reg = mirFunc.newVirtualReg(MIRType.I64);
        System.out.println(LLVMGetValueName(valueRef).getString());
        MIRGlobalVariable globalVar = mirModule.getGlobalVariableMap().get(LLVMGetValueName(valueRef).getString());
//        mirBB.getInstructions().add(new MIRLuiOp(reg,globalVar));
//        mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,reg, MIRArithOp.Type.INT,reg,globalVar));
        mirBB.getInstructions().add(new MIRLaOp(MIRLaOp.Op.La, reg, globalVar));
        return reg;

    }

    private MIROperand handleFloatConstant(LLVMValueRef valueRef, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        double value = LLVMConstRealGetDouble(valueRef, new IntPointer(1));
        // 浮点数也弄成lui + add + fmv.w.x的形式
        float floatValue = (float) value; // 转换为单精度

        // 获取浮点数的位表示
        int bits = Float.floatToRawIntBits(floatValue);

        // 创建目标浮点寄存器
        MIRVirtualReg floatReg = mirFunc.newVirtualReg(MIRType.F32);

        // 步骤1: 加载位模式到整数寄存器
        MIRVirtualReg intReg = mirFunc.newVirtualReg(MIRType.I32);
        List<MIRInstruction> loadInsts = loadLargeImmediate(intReg, bits, mirFunc);
        for (MIRInstruction inst : loadInsts) {
            mirBB.getInstructions().add(inst);
        }

        // 步骤2: 将位模式移动到浮点寄存器 (fmv.w.x)
        mirBB.getInstructions().add(new MIRMoveOp(
                floatReg,
                intReg,
                MIRMoveOp.MoveType.INT_TO_FLOAT
        ));

        return floatReg;
    }

    private MIROperand handleConstantInt(LLVMValueRef valueRef, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        long value = LLVMConstIntGetSExtValue(valueRef);
        // 小立即数直接使用，大立即数特殊处理
        if (value >= -2048 && value < 2048) {
            return new MIRImmediate((int)value, convertType(LLVMTypeOf(valueRef)));
        } else {
            // 大立即数生成加载指令
            MIRVirtualReg reg = mirFunc.newVirtualReg(MIRType.I32);
            List<MIRInstruction> insts = loadLargeImmediate(reg, value, mirFunc);
            for(MIRInstruction inst : insts) {
                mirBB.getInstructions().add(inst);
            }
            return reg;
        }
    }
    // 加载大立即数（为RISC-V优化）
    private List<MIRInstruction> loadLargeImmediate(MIRVirtualReg dest, long value, MIRFunction mirFunc) {
        List<MIRInstruction> instructions = new ArrayList<>();

        // 分解立即数（RISC-V风格）
        int low = (int)(value & 0xFFF);
        int high = (int)((value >> 12) & 0xFFFFF);

        // LUI 加载高20位
        instructions.add(new MIRLuiOp(dest, new MIRImmediate(high, MIRType.I32)));

        // 如果有低12位，使用ADDI
        if (low != 0) {
            instructions.add(new MIRArithOp(
                    MIRArithOp.Op.ADD,
                    dest,
                    MIRArithOp.Type.INT,
                    dest,
                    new MIRImmediate(low, MIRType.I32)
            ));
        }
        return instructions;
    }


    private MIRType convertType(LLVMTypeRef typeRef) {
        int typeKind = LLVMGetTypeKind(typeRef);
        switch (typeKind) {
            // 整数类型
            case LLVMIntegerTypeKind:
                int bitWidth = LLVMGetIntTypeWidth(typeRef);
                return convertIntegerType(bitWidth);

            // 浮点类型
            case LLVMFloatTypeKind:
                return MIRType.F32;

            case LLVMDoubleTypeKind:
                return MIRType.F64;

            // 空类型
            case LLVMVoidTypeKind:
                return MIRType.VOID;

            //  数组类型
            case LLVMArrayTypeKind:
                return  MIRType.ARRAY;

            // IR中的ptr类型均视为64位整数
            case LLVMPointerTypeKind:
                return MIRType.I64;

            // 不支持的类型
            default:
                // 获取类型字符串用于错误信息
                String typeName = LLVMPrintTypeToString(typeRef).getString();;
                throw new IllegalArgumentException("Unsupported LLVM type: " + typeName + " (kind=" + typeKind + ")");
        }
    }

    // 处理整数类型
    private MIRType convertIntegerType(int bitWidth) {
        switch (bitWidth) {
            case 1:  return MIRType.I1;
            case 32: return MIRType.I32;
            case 64: return MIRType.I64;
            default:
                throw new IllegalArgumentException("Unsupported LLVM bitWidth: " + bitWidth);
        }
    }

    private LLVMTypeRef getBaseElementType(LLVMTypeRef typeRef) {
        int kind = LLVMGetTypeKind(typeRef);

        if (kind == LLVMPointerTypeKind) {
            // 如果是指针，获取指向的类型
            return getBaseElementType(LLVMGetElementType(typeRef));
        } else if (kind == LLVMArrayTypeKind) {
            // 如果是数组，获取元素类型
            return getBaseElementType(LLVMGetElementType(typeRef));
        } else if (kind == LLVMVectorTypeKind) {
            // 如果是向量，获取元素类型
            return getBaseElementType(LLVMGetElementType(typeRef));
        } else {
            // 基本类型（整数、浮点等）
            return typeRef;
        }
    }

    private int getArrayLength(LLVMTypeRef typeRef) {
        if (LLVMGetTypeKind(typeRef) == LLVMArrayTypeKind) {
            return LLVMGetArrayLength(typeRef) * getArrayLength(LLVMGetElementType(typeRef));
        } else {
            return 1; // 基本类型返回1
        }
    }

    private MIROperand immToReg(MIRFunction mirFunc, MIRBasicBlock mirBB, long value) {
        // 将整数立即数转换为寄存器
        MIRVirtualReg reg = mirFunc.newVirtualReg(MIRType.I32);
        // 直接创建一个立即数操作数
        MIRImmediate imm = new MIRImmediate(value, MIRType.I32);
        mirBB.getInstructions().add(new MIRLiOp(MIRLiOp.Op.LI, reg, imm));
        return reg;
    }

    private MIRBasicBlock findMIRBlock(MIRFunction mirFunc, String name) {
        System.out.println(name);
        return mirFunc.getBlocks().stream()
                .filter(bb -> bb.getLabel().toString().equals(name))
                .findFirst()
                .orElseThrow();
    }
}