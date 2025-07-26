package com.compiler.backend;

import com.compiler.backend.allocator.NoRegisterAllocator;
import com.compiler.backend.allocator.RegisterAllocator;
import com.compiler.backend.location.Location;
import com.compiler.ir.LLVisitor;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.*;

public class RiscVGen {

    LLVisitor visitor;
    AsmBuilder asmBuilder;
    RegisterAllocator allocator;
    int varCount;
    int stackSize;

    // 怎样让String 能够只从t0 t1 t2中选取，并有一个动态分配的策略，每经过两条IR指令就必定释放，还有些特殊情况处理
//    Deque<String> availableRegs = new ArrayDeque<>(Arrays.asList("t2", "t1", "t0")); // 寄存器分配栈
    String[] availableRegs = {"t3", "t2", "t1", "t0", "t4", "t5", "t6", "s0", "s1","s2","s3","s4"}; // 寄存器数组
    int nextRegIndex = 0; // 下一个要分配的寄存器索引
    //    LinkedHashMap<LLVMValueRef, String> activeRegisters = new LinkedHashMap<>(50, 0.75f, true);
    Map<LLVMValueRef, String> activeRegisters = new HashMap<>();


    public RiscVGen(LLVisitor visitor) {
        this.visitor = visitor;
        asmBuilder = new AsmBuilder();
        allocator = new NoRegisterAllocator();
//        allocator = new LinearScanRegisterAllocator();
    }

    public void generate(File outputFile) {
        var moduleRef = visitor.getMod().getRef();
        // 先行遍历一次
        for (LLVMValueRef func = LLVMGetFirstFunction(moduleRef); func != null; func = LLVMGetNextFunction(func)) {
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                    int opcode = LLVMGetInstructionOpcode(inst);
                    switch (opcode) {
                        case LLVMAlloca:
                            varCount++;
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        varCount *= 4;
        if(varCount % 16 == 0){
            stackSize = varCount;
        } else {
            stackSize = varCount + 16 - (varCount % 16);
        }
        allocator.setStackSize(stackSize);

        int pos = 0;
        for (LLVMValueRef func = LLVMGetFirstFunction(moduleRef); func != null; func = LLVMGetNextFunction(func)) {
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {

                }
            }
        }

        traverseModule(moduleRef);
        String asmCode = asmBuilder.getAsmCode();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(asmCode);
            System.out.println("ASM code has been written to the file successfully.");
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    private void traverseModule(LLVMModuleRef module) {
        // 遍历全局变量
        for (LLVMValueRef value = LLVMGetFirstGlobal(module); value != null; value = LLVMGetNextGlobal(value)) {
            if (LLVMIsAGlobalVariable(value) != null) {
//                System.out.println("Found a global variable: " + LLVMGetValueName(value).getString());
                asmBuilder.data(LLVMGetValueName(value).getString());
                LLVMValueRef initializer = LLVMGetInitializer(value);
                if (LLVMIsAConstantInt(initializer) != null) {
                    int intValue = (int) LLVMConstIntGetSExtValue(initializer);
//                    System.out.println("Initializer Value: " + intValue);
                    asmBuilder.word(intValue);
                    allocator.allocate(value);
                } else {
//                    System.out.println("Uninitializer Value");
                    asmBuilder.word(0);
                    allocator.allocate(value);
                }
            }
        }

        // 遍历函数
        for (LLVMValueRef func = LLVMGetFirstFunction(module); func != null; func = LLVMGetNextFunction(func)) {
            asmBuilder.text();
            asmBuilder.global();
            asmBuilder.label("main");
            asmBuilder.op2Imm("addi","sp","sp",-stackSize);
            traverseFunction(func);
//            asmBuilder.afterFun(allocator.getStackSize(),"main");
        }
    }

    // 遍历函数中的所有基本块
    private void traverseFunction(LLVMValueRef func) {
        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
//            System.out.println(LLVMGetBasicBlockName(bb).getString());
            asmBuilder.label(LLVMGetBasicBlockName(bb).getString());
            traverseBasicBlock(bb);
        }
    }

    // 遍历基本块中的所有指令
    private void traverseBasicBlock(LLVMBasicBlockRef bb) {
        for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
            int opcode = LLVMGetInstructionOpcode(inst);
            switch (opcode) {
                case LLVMRet:
                    handleRet(inst);
                    break;
                case LLVMAdd:
                    handleAdd(inst);
                    break;
                case LLVMSub:
                    handleSub(inst);
                    break;
                case LLVMMul:
                    handleMul(inst);
                    break;
                case LLVMSDiv:
                    handleSDiv(inst);
                    break;
                case LLVMSRem:
                    handleSRem(inst);
                    break;
                case LLVMAlloca:
                    handleAlloc(inst);
                    break;
                case LLVMStore:
                    handleStore(inst);
                    break;
                case LLVMLoad:
                    handleLoad(inst);
                    break;
                case LLVMXor:
                    handleXor(inst);
                    break;
                case LLVMBr:
                    handleBr(inst);
                    break;
                case LLVMICmp:
                    handleICmp(inst);
                    break;
                case LLVMZExt:
                    handleZExt(inst);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleRet(LLVMValueRef ret) {
        if (LLVMGetNumOperands(ret) > 0) {
            LLVMValueRef retVal = LLVMGetOperand(ret, 0);
            // 先检查它在不在tmpReg中，如果不在，调用locate
            String retReg = handleOperand(retVal);
            // 加载返回值到a0
            asmBuilder.op("mv", "a0", retReg);
        } else {
            System.out.println("retError");
            throw new UnsupportedOperationException();
        }
//         恢复栈指针
        asmBuilder.op2Imm("addi", "sp", "sp", stackSize);

        asmBuilder.opImm("li", "a7", 93);
        asmBuilder.ecall();
    }

    private void handleAdd(LLVMValueRef add) {
        processBinaryOp(add, "add", "addi");
    }

    private void handleSub(LLVMValueRef sub) {
        processBinaryOp(sub, "sub", null);
    }

    private void handleMul(LLVMValueRef mul) {
        processBinaryOp(mul, "mul", null);
    }

    private void handleSDiv(LLVMValueRef div) {
        processBinaryOp(div, "div", null);
    }

    private void handleSRem(LLVMValueRef rem) {
        processBinaryOp(rem, "rem", null);
    }

    private void handleXor(LLVMValueRef xor) {
        processBinaryOp(xor, "xor", "xori");
    }


    private void processBinaryOp(LLVMValueRef op, String inst, String immInst) {
        LLVMValueRef op1 = LLVMGetOperand(op, 0);
        LLVMValueRef op2 = LLVMGetOperand(op, 1);
//        System.out.println("op1: " + LLVMGetValueName(op1).getString());
//        System.out.println("op2: " + LLVMGetValueName(op2).getString());

        // 分配目标寄存器
        String destReg = allocRegister(op);

        if (immInst != null && LLVMIsAConstantInt(op1) != null) {
            // op1 是立即数
            int imm = (int) LLVMConstIntGetSExtValue(op1);
            String src2Reg = handleOperand(op2);
            asmBuilder.op2Imm(immInst, destReg, src2Reg, imm);

        } else if (immInst != null && LLVMIsAConstantInt(op2) != null) {
            // op2 是立即数
            int imm = (int) LLVMConstIntGetSExtValue(op2);
            String src1Reg = handleOperand(op1);
            asmBuilder.op2Imm(immInst, destReg, src1Reg, imm);
        } else {
            String src1Reg = handleOperand(op1);
            String src2Reg = handleOperand(op2);
            asmBuilder.op2(inst, destReg, src1Reg, src2Reg);
        }

    }


    private void handleAlloc(LLVMValueRef alloc) {
        allocator.allocate(alloc);
    }

    private void handleLoad(LLVMValueRef load) {
        // load  全局变量、栈上局部变量、寄存器中局部变量 将他们加载到tmpRegs
        // 确保调用 load()传入的dest 都是临时寄存器
        LLVMValueRef ptr = LLVMGetOperand(load, 0);
        String destReg = allocRegister(load);
        Location src = allocator.locate(ptr);
        asmBuilder.load(destReg, src);
    }

    private void handleStore(LLVMValueRef store) {
        // store 将立即数和tmpRegs 加载到 全局变量、栈上局部变量、寄存器中局部变量
        // 得先区分 value是Imm还是Reg 确保调用store()传入的都是临时寄存器。
        LLVMValueRef value = LLVMGetOperand(store, 0);
        LLVMValueRef ptr = LLVMGetOperand(store, 1);
        String valueReg = handleOperand(value);
        Location dest = allocator.locate(ptr);
        asmBuilder.store(valueReg, dest);
    }

    private void handleBr(LLVMValueRef br) {
        int numOperands = LLVMGetNumOperands(br);
        LLVMValueRef target = LLVMGetOperand(br, 0);

        if (numOperands == 1) { // 无条件跳转
            String label = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(target)).getString();
            asmBuilder.jump("j", label);
        } else if (numOperands == 3) { // 条件跳转
            String condReg = handleOperand(target);
            String trueLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(LLVMGetOperand(br, 2))).getString();
            String falseLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(LLVMGetOperand(br, 1))).getString();

            asmBuilder.jump("bnez", condReg, trueLabel);
            asmBuilder.jump("j", falseLabel);
        }
    }

    private void handleICmp(LLVMValueRef icmp) {
        int predicate = LLVMGetICmpPredicate(icmp);
        LLVMValueRef op1 = LLVMGetOperand(icmp, 0);
        LLVMValueRef op2 = LLVMGetOperand(icmp, 1);

        // 分配目标寄存器
        String destReg = allocRegister(icmp);

        // 处理操作数
        String op1Reg = handleOperand(op1);
        String op2Reg = handleOperand(op2);

        // 根据比较类型生成对应指令
        switch (predicate) {
            case LLVMIntEQ:  // ==
                asmBuilder.op2("xor", destReg, op1Reg, op2Reg);
                asmBuilder.op("seqz", destReg, destReg);
//                asmBuilder.op2Imm("sltiu", destReg, destReg, 1);  // 相等时为1
                break;
            case LLVMIntNE:  // !=
                asmBuilder.op2("xor", destReg, op1Reg, op2Reg);
                asmBuilder.op("snez", destReg, destReg);
//                asmBuilder.op2("sltu", destReg, "zero", destReg); // 不等时为1
                break;
            case LLVMIntSGT: // > (有符号)
                asmBuilder.op2("slt", destReg, op2Reg, op1Reg);  // a > b => b < a
                break;
            case LLVMIntSLT: // < (有符号)
                asmBuilder.op2("slt", destReg, op1Reg, op2Reg);
                break;
            case LLVMIntSGE: // >= (有符号)
                asmBuilder.op2("slt", destReg, op1Reg, op2Reg);
                asmBuilder.op2Imm("xori", destReg, destReg, 1);  // 取反
                break;
            case LLVMIntSLE: // <= (有符号)
                asmBuilder.op2("slt", destReg, op2Reg, op1Reg);
                asmBuilder.op2Imm("xori", destReg, destReg, 1);  // 取反
                break;
            default:
                throw new UnsupportedOperationException("Unsupported comparison predicate");
        }
    }

    private void handleZExt(LLVMValueRef zExt) {
        LLVMValueRef srcVal = LLVMGetOperand(zExt, 0);
        String srcReg = handleOperand(srcVal);
        String destReg = allocRegister(zExt);
//        asmBuilder.op2Imm("andi", destReg, srcReg, 1);
        asmBuilder.op2Imm("slli", destReg, srcReg, 31);
        asmBuilder.op2Imm("srli", destReg, destReg, 31);
    }


    // 返回目的寄存器
    private String handleOperand(LLVMValueRef value) {

        // 如果是常量立即数
        if (LLVMIsAConstantInt(value) != null) {
            int imm = (int) LLVMConstIntGetSExtValue(value);
            String tmpReg = allocRegister(value);
            asmBuilder.opImm("li", tmpReg, imm);
            return tmpReg;
        }
        // 如果已经是寄存器中的值
        if (activeRegisters.containsKey(value)) {
            return activeRegisters.get(value);
        }
        // 应该报错
        System.out.println("!!!");
        throw new UnsupportedOperationException();
    }

    private String allocRegister(LLVMValueRef value) {
        // 分配新寄存器

        String reg = availableRegs[nextRegIndex];
        nextRegIndex = (nextRegIndex + 1) % availableRegs.length;
//        System.out.println(reg);
        activeRegisters.put(value, reg);
        return reg;
    }

}
