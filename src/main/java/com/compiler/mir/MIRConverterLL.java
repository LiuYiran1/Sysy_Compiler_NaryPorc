package com.compiler.mir;

import com.compiler.ll.Module;
import com.compiler.ll.Types.*;
import com.compiler.ll.Values.*;
import com.compiler.ll.Values.Constants.ConstantArray;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.GlobalValues.GlobalVariable;
import com.compiler.ll.Values.Instructions.*;
import com.compiler.mir.instruction.*;
import com.compiler.mir.operand.*;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.compiler.mir.instruction.MIRControlFlowOp.Type.*;
import static org.bytedeco.llvm.global.LLVM.*;

public class MIRConverterLL {
    // 添加调试信息字段
    private String currentFunction = "";
    private String currentBasicBlock = "";
    private Instruction currentInstruction = null;
    private int instructionCount = 0;

    private final Module llModule;
    private final MIRModule mirModule = new MIRModule();
    private final Map<Value, MIROperand> valueMap = new LinkedHashMap<>();
    // 添加浮点常量池 优化时再用
//    private final Map<Double, MIRFloatConstant> floatConstantPool = new LinkedHashMap<>();
//    private final Map<MIRFunction, List<MIRFloatConstant>> functionFloatConstants = new LinkedHashMap<>();

    // 调试日志开关
    private static final boolean DEBUG = true;

    public MIRConverterLL(Module llModule) {
        this.llModule = llModule;
    }


    // 调试方法
    private void debugLog(String message) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + message);
        }
    }

    private void setCurrentContext(Function func, BasicBlock bb, Instruction inst) {
        if (func != null) {
            currentFunction = func.getName();
        }
        if (bb != null) {
            currentBasicBlock = bb.getName();
        }
        currentInstruction = inst;
    }

    private String getCurrentContext() {
        return String.format("Function: %s, Block: %s, Inst: %s",
                currentFunction, currentBasicBlock,
                currentInstruction != null ? currentInstruction.toIR() : "N/A");
    }

    public MIRModule convert() {
        // 转换全局变量
        List<GlobalVariable> globalVariables = this.llModule.getGlobalVariables();
        for (GlobalVariable global : globalVariables) {
            debugLog("Converting global: " + global.getName());
            convertGlobalVariable(global);
        }


        // 转换所有函数
        for(var func = llModule.getFirstFunction(); func != null; func = func.getNextFunction()) {
            debugLog("Converting function: " + func.getName());
            convertFunction(func);
        }
        return mirModule;
    }

    private void convertGlobalVariable(GlobalVariable global) {
        // 处理全局变量的初始值
        // TODO: 这里可以添加对全局变量初始值的处理逻辑
        String name = global.getName();
        debugLog("Processing global: " + name);

//        if (LLVMIsAGlobalVariable(global) == null) {
//            throw new IllegalArgumentException("Expected a global variable, but got: " + name);
//        }

        // 判断global指向的类型，看是不是数组
        Type pointedType = global.getValueType();
        MIRType mirType = convertType(pointedType);
        if(MIRType.isInt(mirType)) {
            // int型全局变量
            MIRGlobalVariable intGlobal = new MIRGlobalVariable(name, mirType, ((ConstantInt)global.getInitializer()).getSExtValue());
            System.out.println(intGlobal.getIntValue());
            mirModule.addGlobalVariable(intGlobal);

        } else if (MIRType.isFloat(mirType)) {
            // float型全局变量
            MIRGlobalVariable floatGlobal = new MIRGlobalVariable(name, mirType, ((ConstantFloat)global.getInitializer()).getValue());
            System.out.println(floatGlobal.getFloatValue());
            mirModule.addGlobalVariable(floatGlobal);

        } else {
            // 数组类型全局变量
            // 获取数组元素类型
            System.out.println("Unknown global type: " + global.getValueType().toIR());
            System.out.println("AAAAAA: " + global.getInitializer().toIR() + "   " + global.getInitializer().getClass().getName());
            ConstantArray element = (ConstantArray) global.getInitializer();
            MIRType elementType = convertType(getBaseElementType(pointedType));
            int arraySize = getArrayLength(pointedType);
            System.out.println(arraySize);
            if(element.isZero()) {
                // zero-initialized array
                MIRGlobalVariable mirGlobal = new MIRGlobalVariable(name, elementType,arraySize);
                System.out.println(name + " is zero-initialized array of type " + elementType);
                mirModule.addGlobalVariable(mirGlobal);
                return;
            }
            List<Object> values = new ArrayList<>();

            extractArrayValues(element, (ArrayType) pointedType, values);
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
    private void extractArrayValues(ConstantArray arrayValue, ArrayType arrayType, List<Object> values) {
        int arraySize = arrayType.getNumElements();
        // System.out.println(arraySize);
        Type elementType = arrayType.getElementType();
        TypeID elementKind = elementType.getTypeID();

        List<Constant> elements = arrayValue.getElements();

        for (int i = 0; i < arraySize; i++) {
            System.out.println("BBBBBBBBBBBB" + arraySize + " " + i);
            // System.out.println(LLVMIsAConstantArray(arrayValue) != null); // true

            Constant element = elements.get(i);
            if (elementKind == TypeID.ARRAY) {
                // 如果元素本身也是数组，递归处理
                extractArrayValues((ConstantArray) element, (ArrayType) elementType, values);
            } else {
                // 基本类型元素
                if (elementKind == TypeID.INTEGER) {
                    long intValue = ((ConstantInt) element).getSExtValue();
                    values.add(intValue);
                    debugLog("Array element[" + i + "] = " + intValue);
                }
                else if (elementKind == TypeID.FLOAT) {
                    double floatValue = ((ConstantFloat) element).getValue();
                    values.add(floatValue);
                    debugLog("Array element[" + i + "] = " + floatValue);
                }
                else if (elementKind == TypeID.POINTER) {
                    // 处理指针类型的零初始化
                    values.add(0);
                    debugLog("Array element[" + i + "] = null pointer");
                }
                else {
                    throw new IllegalArgumentException("Unsupported array element type: " +
                            elementType.toIR());
                }
            }
        }
    }

    private void convertFunction(Function llFunc) {
        String funcName = llFunc.getName();

        if(funcName.equals("starttime")){
            funcName = "_sysy_starttime";
        } else if (funcName.equals("stoptime")) {
            funcName = "_sysy_stoptime";
        }

        MIRFunction mirFunc = new MIRFunction(funcName);
        mirModule.addFunction(mirFunc);


        debugLog("Starting conversion for function: " + funcName);
        // 创建参数虚拟寄存器
        // TODO: 这里可以添加对函数参数的处理逻辑

        // 分别记录整数和浮点参数数量
        int intArgCount = 0;
        int floatArgCount = 0;

        int count = llFunc.getArgumentCount();
        for(int i = 0; i < count; i++) {
            Argument param = llFunc.getArgument(i);
           if (param == null) continue; // 忽略空参数
            MIRType mirType = convertType(param.getType());
            System.out.println("{{{{{{{{{{{{{{{{{{{: " + param.getType().toIR());
//            MIRVirtualReg paramReg = mirFunc.newVirtualReg(mirType);
//            valueMap.put(param, paramReg);
//            mirFunc.addParam(paramReg);
            boolean isFloat = MIRType.isFloat(mirType);
            if (isFloat) {
                // 浮点参数处理
                if (floatArgCount < 8) {
                    MIRPhysicalReg paramReg = new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.FA0.ordinal() + floatArgCount]);
                    valueMap.put(param, paramReg);
                    mirFunc.addParam(paramReg);
                    floatArgCount++;
                } else {
                    // 超过8个浮点参数，放在栈上
                    int floatOffset = (floatArgCount - 8 > 0) ? (floatArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int intOffset = (intArgCount - 8 > 0) ? (intArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int offset = floatOffset + intOffset;
//                    int offset = (floatArgCount + intArgCount - 8) * 8;
                    MIRMemory paramMem = new MIRMemory(
                            new MIRPhysicalReg(MIRPhysicalReg.PREGs.FP),
                            new MIRImmediate(offset, MIRType.I64),
                            mirType
                    );
                    valueMap.put(param, paramMem);
                    mirFunc.addParam(paramMem);
                    floatArgCount++;
                }
            } else {
                // 整数参数处理
                if (intArgCount < 8) {
                    MIRPhysicalReg paramReg = new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.A0.ordinal() + intArgCount]);
                    valueMap.put(param, paramReg);
                    mirFunc.addParam(paramReg);
                    intArgCount++;
                } else {
                    // 超过8个整数参数，放在栈上
//                    int offset = (floatArgCount + intArgCount - 8) * 8;
                    int floatOffset = (floatArgCount - 8 > 0) ? (floatArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int intOffset = (intArgCount - 8 > 0) ? (intArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int offset = floatOffset + intOffset;
                    MIRMemory paramMem = new MIRMemory(
                            new MIRPhysicalReg(MIRPhysicalReg.PREGs.FP),
                            new MIRImmediate(offset, MIRType.I64),
                            mirType
                    );
                    valueMap.put(param, paramMem);
                    mirFunc.addParam(paramMem);
                    intArgCount++;
                }
            }
        }

        if(llFunc.getFirstBasicBlock() == null) {
            System.out.println("procedure " + funcName + " has no basic blocks, skipping conversion.");
            return; // 如果函数没有基本块，直接跳过转换
        }
        // 转换基本块
        for(BasicBlock bb = llFunc.getFirstBasicBlock(); bb != null; bb = bb.getNextBasicBlock()) {

            setCurrentContext(llFunc, bb, null);
            System.out.println(getCurrentContext());
            convertBasicBlock(bb, mirFunc);
        }

        System.out.println("999999999999999999999999999999999999999999999999");

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

    private void convertBasicBlock(BasicBlock llBB, MIRFunction mirFunc) {
        String bbName = llBB.getName();
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

        for(Instruction inst = llBB.getFirstInstruction(); inst != null; inst = inst.getNextInstruction()) {
            setCurrentContext(null, llBB, inst);
            instructionCount++;
            debugLog("Converting instruction #" + instructionCount + ": " +
                    inst.toIR());
            System.out.println(getCurrentContext());
            convertInstruction(inst, mirFunc, mirBB);

        }

        mirFunc.addBlock(mirBB);
        System.out.println(mirBB.getLabel().toString());
    }

    private void convertInstruction(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        Opcode opcode = inst.getOpcode();

        if (currentBasicBlock.equals("ifNext"))
            System.out.println("VVVVVVV     " + currentBasicBlock + "   " + mirBB.getInstructions().size());

        switch (opcode) {
            case RET:
                convertReturnInst(inst, mirFunc, mirBB);
                break;
            case CALL:
                convertCallInst(inst, mirFunc, mirBB);
                break;
            case ADD:
            case FADD:
            case SUB:
            case FSUB:
            case MUL:
            case FMUL:
            case SDIV:
            case FDIV:
            case SREM:
            case FREM:
                convertBinaryOp(inst, mirFunc, mirBB, opcode);
                break;
            case ALLOCA:
                convertAlloca(inst, mirFunc, mirBB); ///
                break;
            case STORE:
                convertStoreInst(inst, mirFunc, mirBB); ///
                break;
            case LOAD:
                convertLoadInst(inst, mirFunc, mirBB); ///
                break;
            case BR:
            case CBR:
                convertBranchInst(inst, mirFunc, mirBB, opcode);
                break;
            case ICMP:
            case FCMP:
                convertCmpInst(inst, mirFunc, mirBB);
                break;
            case ZEXT:
                convertZExtInst(inst, mirFunc, mirBB);
                break;
            case FPTOSI:
                convertFPToSIInst(inst, mirFunc, mirBB);
                break;
            case SITOFP:
                convertSIToFPInst(inst, mirFunc, mirBB);
                break;
            case BC:
                convertBitCast(inst, mirFunc, mirBB);
                break;
            case GEP:
                convertGEPInst(inst, mirFunc, mirBB);
                break;
            case PHI:
                // PHI节点稍后处理
                convertPhiInst(inst, mirFunc, mirBB);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported instruction code: " + opcode);
        }
    }

    private void convertSelectInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.SELECT, 0)); // 添加选择指令的伪操作
    }

    private void convertSIToFPInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // fcvt.s.w
        Value srcVal = inst.getOperand(0);
        MIROperand src = getMIRValue(srcVal, mirFunc, mirBB);

        MIRVirtualReg dest = mirFunc.newVirtualReg(MIRType.F32);
        mirBB.getInstructions().add(new MIRConvertOp(MIRConvertOp.Op.ITOFP, dest, src));
        valueMap.put(inst, dest);
    }

    private void convertFPToSIInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // fcvt.w.s
        Value srcVal = inst.getOperand(0);
        MIROperand src = getMIRValue(srcVal, mirFunc, mirBB);

        MIRVirtualReg dest = mirFunc.newVirtualReg(MIRType.I32);
        mirBB.getInstructions().add(new MIRConvertOp(MIRConvertOp.Op.FPTOI, dest, src));
        valueMap.put(inst, dest);
    }


    // %idx646 = zext i32 %i.0 to i64
    //%zextForGt = zext i1 %gt to i32
    private void convertZExtInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        Value srcVal = inst.getOperand(0);
        MIROperand src = getMIRValue(srcVal, mirFunc, mirBB);
        MIRVirtualReg dest = null;
        if(MIRType.isInt(src.getType())) {
            dest = mirFunc.newVirtualReg(MIRType.I64);
            // TODO: 处理整数类型的零扩展
        } else {
            dest = mirFunc.newVirtualReg(MIRType.I32);
            // TODO: 处理其他类型的零扩展
        }
        if(src instanceof MIRImmediate){
            src = immToReg(mirFunc,mirBB,((MIRImmediate) src).getValue());
        }

        mirBB.getInstructions().add(new MIRConvertOp(MIRConvertOp.Op.ZEXT, dest, src));
        valueMap.put(inst, dest);
    }

    private void convertBranchInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB, Opcode opcode) {
        // TODO: 处理分支指令

        if (opcode == Opcode.BR) { // 无条件跳转
            BasicBlock target = ((BranchInst) inst).getTarget();
            String label = target.getName();

            MIRLabel mirLabel = new MIRLabel(label);
            System.out.println(mirLabel.toString());
            mirBB.getInstructions().add(new MIRControlFlowOp(mirLabel));
        } else if (opcode == Opcode.CBR) { // 条件跳转
            CondBranchInst condBranchInst = (CondBranchInst) inst;

            MIROperand cond = getMIRValue(inst.getOperand(0), mirFunc, mirBB);

            if(cond instanceof MIRImmediate){
                cond = immToReg(mirFunc,mirBB,((MIRImmediate) cond).getValue());
            }

            String trueLabel = condBranchInst.getTrueBlock().getName();
            String falseLabel = condBranchInst.getFalseBlock().getName();
            MIRLabel trueTarget = new MIRLabel(trueLabel);
            MIRLabel falseTarget = new MIRLabel(falseLabel);

            mirBB.getInstructions().add(new MIRControlFlowOp(cond, trueTarget)); // bnez
            mirBB.getInstructions().add(new MIRControlFlowOp(falseTarget));   // j
        }
        if (currentBasicBlock.equals("ifNext"))
            System.out.println("VVVVVVV     " + currentBasicBlock + "   " + mirBB.getInstructions().size());
    }

    private void convertCmpInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // 分配目标寄存器
        MIRVirtualReg result = mirFunc.newVirtualReg(MIRType.I1);
        valueMap.put(inst, result);

        // 处理操作数
        Value op1 = inst.getOperand(0);
        Value op2 = inst.getOperand(1);
        MIROperand src1 = getMIRValue(op1, mirFunc, mirBB);
        MIROperand src2 = getMIRValue(op2, mirFunc, mirBB);

        if(src1 instanceof MIRImmediate){
            src1 = immToReg(mirFunc,mirBB, ((MIRImmediate) src1).getValue());
        }
        if(src2 instanceof MIRImmediate){
            src2 = immToReg(mirFunc,mirBB, ((MIRImmediate) src2).getValue());
        }

        // 根据比较类型生成对应指令
        if (inst.isICmpInst()) {
            IntPredicate predicate = ((ICmpInst) inst).getPredicate();
            switch (predicate) {
                case EQ:  // ==
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.EQ, result, src1, src2));
                    break;
                case NE:  // !=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.NE, result, src1, src2));
                    break;
                case SGT: // > (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.GT, result, src1, src2));
                    break;
                case SLT: // < (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.LT, result, src1, src2));
                    break;
                case SGE: // >= (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.GE, result, src1, src2));
                    break;
                case SLE: // <= (有符号)
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.INT, MIRCmpOp.Op.LE, result, src1, src2));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported comparison predicate");
            }
        } else {
            FloatPredicate predicate = ((FCmpInst) inst).getPredicate();
            switch (predicate) {
                case OEQ: // ==
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.EQ, result, src1, src2));
                    break;
                case ONE: // !=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.NE, result, src1, src2));
                    break;
                case OGT: // >
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.GT, result, src1, src2));
                    break;
                case OLT: // <
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.LT, result, src1, src2));
                    break;
                case OGE: // >=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.GE, result, src1, src2));
                    break;
                case OLE: // <=
                    mirBB.getInstructions().add(new MIRCmpOp(MIRCmpOp.Type.FLOAT, MIRCmpOp.Op.LE, result, src1, src2));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported floating-point comparison predicate");
            }
        }
    }

    // 注意load和store的出现场景
    // offset其实都是0
    private void convertLoadInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIROperand addr = getMIRValue(inst.getOperand(0), mirFunc, mirBB);
        MIRType type = convertType(((PointerType)inst.getOperand(0).getType()).getPointeeType());
        MIRMemory memory = new MIRMemory(addr, new MIRImmediate(0, MIRType.I64), type);
        MIRVirtualReg result = mirFunc.newVirtualReg(type);
        valueMap.put(inst, result);

        if (MIRType.isFloat(type)) {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.LOAD, MIRMemoryOp.Type.FLOAT, memory, result));
        } else {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.LOAD, MIRMemoryOp.Type.INTEGER, memory, result));
        }
    }

    private void convertStoreInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIROperand value = getMIRValue(inst.getOperand(0), mirFunc, mirBB);
        MIROperand addr = getMIRValue(inst.getOperand(1), mirFunc, mirBB);
        MIRMemory memory = new MIRMemory(addr, new MIRImmediate(0, MIRType.I64), value.getType());

        if(value instanceof MIRImmediate){
            value = immToReg(mirFunc,mirBB,((MIRImmediate) value).getValue());
        }

        if (MIRType.isFloat(value.getType())) {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.FLOAT, memory,value));
        } else {
            mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.INTEGER, memory, value));
        }
    }

    private void convertAlloca(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) { // 这里是不是只能处理一维数组
        // TODO: 实现内存分配指令转换
        // 两处： 函数返回值和数组
        // 首先分配栈空间
        int offset = 0;
        Type type = ((PointerType) inst.getType()).getPointeeType();
        TypeID kind = type.getTypeID();
        if(kind == TypeID.ARRAY) {
            ArrayType arrayType = (ArrayType) type;
            // 数组类型
//            int arraySize = arrayType.getNumElements();
            int arraySize = getArrayLength(arrayType);
            Type elementType = arrayType.getElementType();
            MIRType mirType = convertType(elementType);
            offset = arraySize * 4; // 每个元素占8个字节（riscV64）,不对，应该还是4
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

    private void convertCallInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // TODO: 实现函数调用指令转换
        // 保存相关寄存器
        // 怎么能在这里做个标记在后端处理时能完成这个操作
        mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLER_SAVE_REG,0));

        // 处理参数 + 传参
        List<MIROperand> args = new ArrayList<>();
        int numOperands = inst.getNumOperands();

        int intArgCount = 0; // 整数参数计数
        int floatArgCount = 0; // 浮点参数计数

        for (int i = 0; i < numOperands - 1; i++) {
            Value arg = inst.getOperand(i);
            MIRType argType = convertType(arg.getType());
            boolean isFloat = MIRType.isFloat(argType);

            if(isFloat) {
                floatArgCount++;
            } else {
                intArgCount++;
            }
        }

        int stackSize = ((floatArgCount - 8 > 0) ? (floatArgCount - 8) * 8 : 0 ) + ((intArgCount - 8 > 0) ? (intArgCount - 8) * 8 : 0);

        mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD, new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), MIRArithOp.Type.INT, new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP),new MIRImmediate(-stackSize,MIRType.I64)));

        intArgCount = 0;
        floatArgCount = 0;

        for (int i = 0; i < numOperands - 1; i++) { // 从0开始，最后一个是callee
            Value arg = inst.getOperand(i);
            MIRType argType = convertType(arg.getType());
            MIROperand mirArg = getMIRValue(arg, mirFunc, mirBB);
            args.add(mirArg);

            boolean isFloat = MIRType.isFloat(argType);
            if (isFloat) {
                // 浮点参数处理
                if (floatArgCount < 8) {
                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.FA0.ordinal() + floatArgCount]),mirArg, MIRMoveOp.MoveType.FLOAT));
                    floatArgCount++;
                } else {
                    // 超过8个浮点参数，放在栈上
//                    int offset = (floatArgCount + intArgCount - 8) * 8;
                    int floatOffset = (floatArgCount - 8 > 0) ? (floatArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int intOffset = (intArgCount - 8 > 0) ? (intArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int offset = floatOffset + intOffset;
                    MIRMemory argMem = new MIRMemory(new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), new MIRImmediate(offset, MIRType.I64), mirArg.getType());
                    mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.FLOAT, argMem, mirArg));
                    floatArgCount++;
                }
            } else {
                // 整数参数处理
                if (intArgCount < 8) {
                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.A0.ordinal() + intArgCount]),mirArg, MIRMoveOp.MoveType.INTEGER));
                    intArgCount++;
                } else {
                    // 超过8个整数参数，放在栈上
//                    int offset = (floatArgCount + intArgCount - 8) * 8;
                    int floatOffset = (floatArgCount - 8 > 0) ? (floatArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int intOffset = (intArgCount - 8 > 0) ? (intArgCount - 8) * 8 : 0; // 确保偏移量正确
                    int offset = floatOffset + intOffset;
                    MIRMemory argMem = new MIRMemory(new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), new MIRImmediate(offset, MIRType.I64), mirArg.getType());
                    if(MIRType.isInt(argType)) {
                        // 整数类型
                        mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.INTEGER, argMem, mirArg));
                    } else {
                        // 指针类型
                        mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.POINTER, argMem, mirArg));
                    }
                    intArgCount++;
                }
            }
//            if(i < 8){
//                if(MIRType.isFloat(argType)) {
//                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.FA0.ordinal() + i]),mirArg, MIRMoveOp.MoveType.FLOAT));
//                } else {
//                    // 整数类型或指针类型
//                    mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.values()[MIRPhysicalReg.PREGs.A0.ordinal() + i]),mirArg, MIRMoveOp.MoveType.INTEGER));
//                }
//            } else {
//                // 如果参数超过8个，使用栈传递
//                // 这里的偏移量需要计算
//                int offset = (i - 8) * 8; // 每个参数占8个字节
//                MIRMemory argMem = new MIRMemory(new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), new MIRImmediate(offset, MIRType.I64), mirArg.getType());
//                if(MIRType.isFloat(argType)) {
//                    mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.FLOAT, argMem, mirArg));
//                } else if(MIRType.isInt(argType)) {
//                    // 整数类型
//                    mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.INTEGER, argMem, mirArg));
//                } else {
//                    // 指针类型
//                    mirBB.getInstructions().add(new MIRMemoryOp(MIRMemoryOp.Op.STORE, MIRMemoryOp.Type.POINTER, argMem, mirArg));
//                }
//
//            }
        }

        // 为创建调用指令做准备
        Value callee = inst.getOperand(numOperands - 1);

        // 默认就是函数
//        if (LLVMIsAFunction(callee) == null) {
//            throw new IllegalArgumentException("Expected a function call, but got: " + LLVMGetValueName(callee).getString());
//        }
        String calleeName = callee.getName();
        MIRLabel funcLabel = new MIRLabel(calleeName);

        // 接受返回值，但void呢？
        MIRType mirType = convertType(inst.getType());
        if(MIRType.isVoid(mirType)) {
            // 如果是void类型,就不用接受返回值了
            mirBB.getInstructions().add(new MIRControlFlowOp(funcLabel, args));

//            mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), MIRArithOp.Type.INT,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP),new MIRImmediate(stackSize,MIRType.I64)));

            // 恢复相关寄存器
            // 怎么能在这里做个标记在后端处理时能完成这个操作
//            mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLER_RESTORE_REG,0));
        } else {
            MIRVirtualReg result = mirFunc.newVirtualReg(mirType);
            valueMap.put(inst, result);
            mirBB.getInstructions().add(new MIRControlFlowOp(funcLabel, result, args));

//            mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), MIRArithOp.Type.INT,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP),new MIRImmediate(stackSize,MIRType.I64)));

            // 恢复相关寄存器
            // 怎么能在这里做个标记在后端处理时能完成这个操作
//            mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLER_RESTORE_REG,0));

            //下面是LLVM处理
//        %avg = alloca float, align 4
//        %0 = call float @calculate_average([5 x i32]* %int_arrayArr, i32 5)
//        store float %0, float* %avg, align 4
            // 下面是LL处理
//        %call.2 = call float @calculate_average([5 x i32]* %int_arrayArr, i32 5)

            // 存储返回值 以LL为准处理
            if (MIRType.isFloat(result.getType())) {
                // 如果返回值是浮点数
//                mirBB.getInstructions().add(new MIRMoveOp(result, new MIRPhysicalReg(MIRPhysicalReg.PREGs.FA0), MIRMoveOp.MoveType.FLOAT));
                mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.TP),new MIRPhysicalReg(MIRPhysicalReg.PREGs.FA0), MIRMoveOp.MoveType.FLOAT_TO_INT));
            } else {
                // 如果返回值是整数
//                mirBB.getInstructions().add(new MIRMoveOp(result, new MIRPhysicalReg(MIRPhysicalReg.PREGs.A0), MIRMoveOp.MoveType.INTEGER));
                mirBB.getInstructions().add(new MIRMoveOp(new MIRPhysicalReg(MIRPhysicalReg.PREGs.TP),new MIRPhysicalReg(MIRPhysicalReg.PREGs.A0), MIRMoveOp.MoveType.INTEGER));
            }


            mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), MIRArithOp.Type.INT,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP),new MIRImmediate(stackSize,MIRType.I64)));

            // 恢复相关寄存器
            // 怎么能在这里做个标记在后端处理时能完成这个操作
            mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLER_RESTORE_REG,0));

            // 把tp里的返回值存回去
            if (MIRType.isFloat(result.getType())) {
                mirBB.getInstructions().add(new MIRMoveOp(result,new MIRPhysicalReg(MIRPhysicalReg.PREGs.TP),MIRMoveOp.MoveType.INT_TO_FLOAT));
            } else {
                mirBB.getInstructions().add(new MIRMoveOp(result,new MIRPhysicalReg(MIRPhysicalReg.PREGs.TP),MIRMoveOp.MoveType.INTEGER));
            }

        }

//        mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP), MIRArithOp.Type.INT,new MIRPhysicalReg(MIRPhysicalReg.PREGs.SP),new MIRImmediate(stackSize,MIRType.I64)));
//
//        // 恢复相关寄存器
//        // 怎么能在这里做个标记在后端处理时能完成这个操作
//        mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.CALLER_RESTORE_REG,0));



    }

    // bug4,这个方法写的真是错漏百出
    private void convertReturnInst(Instruction inst, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // TODO: 实现返回指令转换
        // 恢复栈空间，恢复某些寄存器的值，这些步骤都交给exitbb处理

        // 把返回值存到a0然后跳转到func.name + "Return"
//        LLVMTypeRef typeRef = LLVMTypeOf(inst);
//        int kind = LLVMGetTypeKind(typeRef);// bug3,比较大的一个问题，这里永远都是void，
        int kind = inst.getNumOperands();
        System.out.println(kind);

        if (kind == 0) {
            // 无返回值，直接返回
            mirBB.getInstructions().add(new MIRControlFlowOp(new MIRLabel(mirFunc.getName() + "Return")));
            return;
        } else {
            // 有返回值，处理返回值
            Value retVal = inst.getOperand(0);
            MIRType mirType = convertType(retVal.getType());

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
    private void convertGEPInst(Instruction gep, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIRType ptrType = convertType(gep.getType()); // 获取指针类型
        MIRVirtualReg result = mirFunc.newVirtualReg(ptrType);
        valueMap.put(gep, result);
        // TODO: 处理GEP的操作数

        if (gep.getNumOperands() == 2) {
            // TODO: 两个操作数的

            Value basePtr = gep.getOperand(0);
            MIROperand base = getMIRValue(basePtr, mirFunc, mirBB);

            Value index = gep.getOperand(1);
            MIROperand indexOp = getMIRValue(index, mirFunc, mirBB);

            if(indexOp instanceof MIRImmediate){
                // 如果是立即数，转换为寄存器
                indexOp = immToReg(mirFunc, mirBB, ((MIRImmediate) indexOp).getValue() * 4); // 这里应该*4吧？
            } else if (indexOp instanceof  MIRVirtualReg){
                // 如果是虚拟寄存器，得先左移2位
                mirBB.getInstructions().add(new MIRShiftOp(MIRShiftOp.Op.SLL, (MIRVirtualReg) indexOp, (MIRVirtualReg) indexOp,new MIRImmediate(2,MIRType.I32)));
            } else {
                throw new IllegalArgumentException("the type of index is wrong");
            }

            if(basePtr.isGlobalVariable()){
                mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,result,MIRArithOp.Type.INT,base, indexOp));
            } else {
                mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.SUB,result,MIRArithOp.Type.INT,base, indexOp));
            }

        } else {
            // TODO: 三个操作数的
            Value basePtr = gep.getOperand(0);
            MIROperand base = getMIRValue(basePtr, mirFunc, mirBB);

            Type elementType = ((PointerType) basePtr.getType()).getPointeeType();
            TypeID kind = elementType.getTypeID();
            if(kind == TypeID.ARRAY){
                // 数组类型

                int ptrSize = getArrayLength(((ArrayType) elementType).getElementType()); // 获取指针指向的大小

                if(ptrSize <= 0) {
                    System.err.println("arrayLen = " + ptrSize);
                    throw new IllegalArgumentException("Array size must be greater than 0");
                }
                Value index = gep.getOperand(2);
                MIROperand indexOp = getMIRValue(index, mirFunc, mirBB);

                if(indexOp instanceof MIRImmediate){
                    // 如果是立即数，转换为寄存器
                    indexOp = immToReg(mirFunc, mirBB, ((MIRImmediate) indexOp).getValue() * ptrSize * 4); // 这里应该*4吧？
                } else if (indexOp instanceof  MIRVirtualReg) {
                    // 如果是寄存器，那就给寄存器的值*4
//                    mirBB.getInstructions().add(new MIRShiftOp(MIRShiftOp.Op.SLL, (MIRVirtualReg) indexOp, (MIRVirtualReg) indexOp,new MIRImmediate(2,MIRType.I32)));
                    MIRVirtualReg tempReg = (MIRVirtualReg) immToReg(mirFunc,mirBB,ptrSize * 4);
                    mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.MUL, (MIRVirtualReg) indexOp,MIRArithOp.Type.INT,indexOp,tempReg));
                } else {
                    throw new IllegalArgumentException("the type of index is wrong");
                }


                if(isOriginalBaseGlobal(basePtr)){
                    mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,result,MIRArithOp.Type.INT,base, indexOp));
                } else {
                    mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.SUB,result,MIRArithOp.Type.INT,base, indexOp));
                }
//                mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.SUB,result,MIRArithOp.Type.INT,base, indexOp));
            } else {
                // 说明是一个 i32* 或 f32*
                // 有这种情况吗？ 没有
                throw new IllegalArgumentException("Unsupported base pointer type: " + basePtr.getType().toIR());
            }


        }

    }

    // 实现位转换指令
    private void convertBitCast(Instruction bitCast, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        MIRType destType = convertType(bitCast.getType());
        MIRVirtualReg result = mirFunc.newVirtualReg(destType);
        valueMap.put(bitCast, result);
        MIROperand source = getMIRValue(bitCast.getOperand(0),mirFunc,mirBB);
        // TODO : 处理位转换逻辑
        // 位转换在MIR中表现为直接赋值
        // MOVE不太合适 ,或者说没必要？
        mirBB.getInstructions().add(new MIRMoveOp(result, source, MIRMoveOp.MoveType.INTEGER));
    }

    private void convertBinaryOp(Instruction inst, MIRFunction mirFunc,MIRBasicBlock mirBB, Opcode opcode) {

        MIRType type = convertType(inst.getType());
        System.out.println("Binary Op Type: " + type);
        MIRVirtualReg result = mirFunc.newVirtualReg(type);
        valueMap.put(inst, result);
        MIROperand left = getMIRValue(inst.getOperand(0),mirFunc,mirBB);
        MIROperand right = getMIRValue(inst.getOperand(1),mirFunc,mirBB);

        if(left instanceof MIRImmediate){
            left = immToReg(mirFunc,mirBB, ((MIRImmediate) left).getValue());
        }
        if(right instanceof MIRImmediate){
            right = immToReg(mirFunc,mirBB, ((MIRImmediate) right).getValue());
        }

        // TODO: 处理二元运算逻辑
        MIRArithOp.Op op = null;
        switch (opcode) {
            case ADD:
            case FADD:
                op = MIRArithOp.Op.ADD;
                break;
            case SUB:
            case FSUB:
                op = MIRArithOp.Op.SUB;
                break;
            case MUL:
            case FMUL:
                op = MIRArithOp.Op.MUL;
                break;
            case SDIV:
            case FDIV:
                op = MIRArithOp.Op.DIV;
                break;
            case SREM:
            case FREM:
                op = MIRArithOp.Op.REM;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported binary op: " + opcode);
        }
        mirBB.getInstructions().add(new MIRArithOp(op, result,MIRType.isFloat(type) ? MIRArithOp.Type.FLOAT : MIRArithOp.Type.INT, left, right));
    }

    private void convertPhiInst(Instruction phi,MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // 收集所有PHI节点
        MIRVirtualReg phiReg = mirFunc.newVirtualReg(convertType(phi.getType()));
        valueMap.put(phi, phiReg);

        mirFunc.addPhiNode(mirBB,phi);
        //mirBB.getInstructions().add(new MIRPseudoOp(MIRPseudoOp.Type.SELECT,0));

    }

    // 最优PHI消除实现
    private void eliminatePhiNodes(MIRFunction mirFunc) {

        // 为每个PHI节点插入MOV指令
        for (Map.Entry<MIRBasicBlock, List<Instruction>> entry : mirFunc.getPhiNodes().entrySet()) {
            MIRBasicBlock currentBB = entry.getKey();

            if (currentBB.getLabel().toString().equals("ifNext"))
                System.out.println("777777777777777777777777777777777777   " + currentBB.getInstructions().size());

            for (Instruction inst : entry.getValue()) {
                PhiInst phi = (PhiInst) inst;
                MIRVirtualReg phiReg = (MIRVirtualReg) valueMap.get(phi);

                for (int i = 0; i < phi.getNumOperands(); i++) {
                    BasicBlock incomingBB = phi.getIncomingBlock(i);
                    Value incomingValue = phi.getOperand(i);

                    System.out.println(incomingBB.getName());

                    MIRBasicBlock mirIncomingBB = findMIRBlock(mirFunc, incomingBB.getName());


                    MIRBasicBlock tempBB = new MIRBasicBlock("TEMP");
//                    MIROperand source = getMIRValue(incomingValue, mirFunc, currentBB);
                    MIROperand source = getMIRValue(incomingValue, mirFunc, tempBB);

                    if(source instanceof MIRImmediate){
                        source = immToReg(mirFunc,tempBB,((MIRImmediate) source).getValue());
                    }

                    if (mirIncomingBB.getLabel().toString().equals("ifNext"))
                        System.out.println("666666666666666666666666666666   " + mirIncomingBB.getInstructions().size());


                    //在入块末尾插入MOV指令
                    int size = mirIncomingBB.getInstructions().size();
                    System.out.println("55555555555555555555555555555555    " + size);
                    if (size > 1 && mirIncomingBB.getInstructions().get(size - 2) instanceof MIRControlFlowOp && ((MIRControlFlowOp) mirIncomingBB.getInstructions().get(size - 2)).getType() ==  COND_JMP) {
                        if (currentBasicBlock.equals("ifNext"))
                            System.out.println("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD   " + size);
                        // 如果倒数第二条指令是控制流指令，插入到前面
                        if(!tempBB.getInstructions().isEmpty()){
                            int sizeOfTemp  = tempBB.getInstructions().size();
                            for(int j =0 ;j < sizeOfTemp;j++){
                                System.out.println(tempBB.getInstructions().get(j).toString());
                                mirIncomingBB.getInstructions().add(size + j - 2, tempBB.getInstructions().get(j));
                            }
                        }
                        size = mirIncomingBB.getInstructions().size();
                        if (MIRType.isFloat(phiReg.getType())) {

                            mirIncomingBB.getInstructions().add(size - 2, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.FLOAT));

                        } else {
                            mirIncomingBB.getInstructions().add(size - 2, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.INTEGER));
                        }

                    } else if (size > 0 && mirIncomingBB.getInstructions().get(size - 1) instanceof MIRControlFlowOp && ((MIRControlFlowOp) mirIncomingBB.getInstructions().get(size - 1)).getType() == JMP) {
                        if (currentBasicBlock.equals("ifNext"))
                            System.out.println("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC   " + size);
                        // 如果倒数第一条指令是控制流指令，插入到前面
                        if(!tempBB.getInstructions().isEmpty()){
                            int sizeOfTemp  = tempBB.getInstructions().size();
                            for(int j =0 ;j < sizeOfTemp;j++){
                                System.out.println(tempBB.getInstructions().get(j).toString());
                                mirIncomingBB.getInstructions().add(size + j - 1, tempBB.getInstructions().get(j));
                            }
                        }
                        size = mirIncomingBB.getInstructions().size();
                        if (MIRType.isFloat(phiReg.getType())) {
                            mirIncomingBB.getInstructions().add(size - 1, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.FLOAT));
                        } else {
                            mirIncomingBB.getInstructions().add(size - 1, new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.INTEGER));
                        }
                    } else {
                        // 否则直接添加到末尾 这种情况不应该出现
                        mirIncomingBB.getInstructions().add(new MIRMoveOp(phiReg, source, MIRMoveOp.MoveType.INTEGER));
                        System.out.println("AAAAA  " + size);
                        System.out.println("BBBBB  " + mirIncomingBB.getInstructions().get(size - 3));
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
    private MIROperand getMIRValue(Value value, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // 如果已经处理过，直接返回
        if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }

        // 处理常量值 比如 add %1, 2
        if (value.isConstant() && value.getType().isIntegerType()) {
            return handleConstantInt((ConstantInt) value, mirFunc,mirBB);
        } else if (value.isConstant() && value.getType().isFloatType()) {
            return handleFloatConstant((ConstantFloat) value, mirFunc, mirBB);
        } else if (value.isGlobalVariable()) {
            // load store getelementptr等指令涉及全局变量
            return handleGlobalVariable((GlobalVariable) value, mirFunc, mirBB);
        }

        // 未知值创建临时寄存器
        MIRVirtualReg tempReg = mirFunc.newVirtualReg(convertType(value.getType()));
        valueMap.put(value, tempReg);
        return tempReg;
    }

    private MIROperand handleGlobalVariable(GlobalVariable value, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // %hi 和 %lo
        // 相当于去处理一个I64地址 lui + add 是静态链接吗？
        MIRVirtualReg reg = mirFunc.newVirtualReg(MIRType.I64);
        System.out.println(value.getName());
        MIRGlobalVariable globalVar = mirModule.getGlobalVariableMap().get(value.getName());
//        mirBB.getInstructions().add(new MIRLuiOp(reg,globalVar));
//        mirBB.getInstructions().add(new MIRArithOp(MIRArithOp.Op.ADD,reg, MIRArithOp.Type.INT,reg,globalVar));
        mirBB.getInstructions().add(new MIRLaOp(MIRLaOp.Op.La, reg, globalVar));
        return reg;

    }

    private MIROperand handleFloatConstant(ConstantFloat constant, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        // 浮点数也弄成lui + add + fmv.w.x的形式
        float floatValue = constant.getValue(); // 转换为单精度

        // 获取浮点数的位表示
        int bits = Float.floatToRawIntBits(floatValue);

        // 创建目标浮点寄存器
        MIRVirtualReg floatReg = mirFunc.newVirtualReg(MIRType.F32);

        // 步骤1: 加载位模式到整数寄存器
        MIRVirtualReg intReg = mirFunc.newVirtualReg(MIRType.I32);
//        List<MIRInstruction> loadInsts = loadLargeImmediate(intReg, bits, mirFunc);
//        for (MIRInstruction inst : loadInsts) {
//            mirBB.getInstructions().add(inst);
//        }
        mirBB.getInstructions().add(new MIRLiOp(MIRLiOp.Op.LI,intReg,new MIRImmediate(bits,MIRType.I32)));

        // 步骤2: 将位模式移动到浮点寄存器 (fmv.w.x)
        mirBB.getInstructions().add(new MIRMoveOp(
                floatReg,
                intReg,
                MIRMoveOp.MoveType.INT_TO_FLOAT
        ));

        return floatReg;
    }

    private MIROperand handleConstantInt(ConstantInt constant, MIRFunction mirFunc, MIRBasicBlock mirBB) {
        long value = constant.getSExtValue();
        // 小立即数直接使用，大立即数特殊处理
        if (value >= -2048 && value < 2048) {
            return new MIRImmediate((int)value, convertType(constant.getType()));
        } else {
            // 大立即数生成加载指令
            MIRVirtualReg reg = mirFunc.newVirtualReg(MIRType.I32);
//            List<MIRInstruction> insts = loadLargeImmediate(reg, value, mirFunc);
//            for(MIRInstruction inst : insts) {
//                mirBB.getInstructions().add(inst);
//            }
            mirBB.getInstructions().add(new MIRLiOp(MIRLiOp.Op.LI,reg,new MIRImmediate((int)value, convertType(constant.getType()))));
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


    private MIRType convertType(Type type) {
        TypeID typeKind = type.getTypeID();
        switch (typeKind) {
            // 整数类型
            case INTEGER:
                int bitWidth = ((IntegerType)type).getBitWidth();
                return convertIntegerType(bitWidth);

            // 浮点类型
            case FLOAT:
                return MIRType.F32;

            // 空类型
            case VOID:
                return MIRType.VOID;

            //  数组类型
            case ARRAY:
                return  MIRType.ARRAY;

            // IR中的ptr类型均视为64位整数
            case POINTER:
                return MIRType.I64;

            // 不支持的类型
            default:
                // 获取类型字符串用于错误信息
                String typeName = type.toIR();;
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

    private Type getBaseElementType(Type type) {
        TypeID kind = type.getTypeID();

        if (kind == TypeID.POINTER) {
            // 如果是指针，获取指向的类型
            return getBaseElementType(((PointerType) type).getPointeeType());
        } else if (kind == TypeID.ARRAY) {
            // 如果是数组，获取元素类型
            return getBaseElementType(((ArrayType) type).getElementType());
        } else {
            // 基本类型（整数、浮点等）
            return type;
        }
    }

    private int getArrayLength(Type type) {
        if (type.getTypeID() == TypeID.ARRAY) {
            return ((ArrayType) type).getNumElements() * getArrayLength(((ArrayType) type).getElementType());
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

    private boolean isOriginalBaseGlobal(Value base) {
        if (base.isGlobalVariable()) {
            return true;
        }
        if (base instanceof Instruction) {
            Instruction inst = (Instruction) base;
            if (inst instanceof GetElementPtrInst) {
                return isOriginalBaseGlobal(inst.getOperand(0));
            }
        }
        return false; // 默认为局部变量
    }
}