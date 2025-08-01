package com.compiler.backend;

import com.compiler.backend.PhysicalRegister;
import com.compiler.backend.allocator.LinearScanRegisterAllocator;
import com.compiler.mir.*;
import com.compiler.mir.instruction.*;
import com.compiler.mir.operand.*;
import java.util.*;

public class RiscVFunctionGenerator {
    private final MIRFunction function;
    private final LinearScanRegisterAllocator allocator;
    private final StackManager stackManager;
    private final StringBuilder asm = new StringBuilder();
    private final Map<MIRVirtualReg, Integer> arrayOffsets = new LinkedHashMap<>();

    // 记录当前指令的目标操作数（用于处理spill变量）
    private MIRVirtualReg currentDestOperand;
    private PhysicalRegister currentDestTempReg;

    public RiscVFunctionGenerator(
            MIRFunction function,
            LinearScanRegisterAllocator allocator,
            StackManager stackManager
    ) {
        this.function = function;
        this.allocator = allocator;
        this.stackManager = stackManager;
    }

    public String generate() {
        // 函数头
//        asm.append("    .text\n");
//        asm.append("    .globl ").append(function.getName()).append("\n");
        asm.append(function.getName()).append(":\n");

        // 函数序言
        generatePrologue();

        // 生成基本块代码
        for (MIRBasicBlock block : function.getBlocks()) {
            asm.append(block.getLabel().toString()).append(":\n");
            for (MIRInstruction inst : block.getInstructions()) {
                generateInstruction(inst);
            }
        }

        // 函数收尾
//        asm.append("    .size ").append(function.getName()).append(", .-").append(function.getName()).append("\n");
        return asm.toString();
    }

    private void generatePrologue() {
        int frameSize = stackManager.getFrameSize();

        asm.append("    # Function prologue for ").append(function.getName()).append("\n");
        asm.append("    addi sp, sp, -").append(frameSize).append("\n");
        asm.append("    sd ra, ").append(frameSize - 8).append("(sp)\n");
        asm.append("    sd s0, ").append(frameSize - 16).append("(sp)\n");
        asm.append("    addi s0, sp, ").append(frameSize).append("\n");

        if(!function.getName().equals("main")) {
            // main函数不需要保存
            // 保存被调用者保存寄存器
            int offset = frameSize - 24;
            for (PhysicalRegister reg : allocator.getUsedCalleeSaved()) {
                asm.append("    sd ").append(reg).append(", ").append(offset).append("(sp)\n");
                offset -= 8;
            }
        }

    }

    private void generateEpilogue() {
        int frameSize = stackManager.getFrameSize();

        asm.append("    # Function epilogue\n");

        int offset = frameSize - 24;
        for (PhysicalRegister reg : allocator.getUsedCalleeSaved()) {
            asm.append("    ld ").append(reg).append(", ").append(offset).append("(sp)\n");
            offset -= 8;
        }

        asm.append("    ld ra, ").append(frameSize - 8).append("(sp)\n");
        asm.append("    ld s0, ").append(frameSize - 16).append("(sp)\n");
        asm.append("    addi sp, sp, ").append(frameSize).append("\n");
    }

    private void generateInstruction(MIRInstruction inst) {
        if (inst instanceof MIRPseudoOp) {
            generatePseudoOp((MIRPseudoOp) inst);
        } else if (inst instanceof MIRArithOp) {
            generateArithOp((MIRArithOp) inst);
        } else if (inst instanceof MIRMoveOp) {
            generateMoveOp((MIRMoveOp) inst);
        } else if (inst instanceof MIRMemoryOp) {
            generateMemoryOp((MIRMemoryOp) inst);
        } else if (inst instanceof MIRControlFlowOp) {
            generateControlFlowOp((MIRControlFlowOp) inst);
        } else if (inst instanceof MIRAllocOp) {
            generateAllocOp((MIRAllocOp) inst);
        } else if (inst instanceof MIRCmpOp) {
            generateCmpOp((MIRCmpOp) inst);
        } else if (inst instanceof MIRConvertOp) {
            generateConvertOp((MIRConvertOp) inst);
        } else if (inst instanceof MIRLaOp) {
            generateLaOp((MIRLaOp) inst);
        } else if (inst instanceof MIRLiOp) {
            generateLiOp((MIRLiOp) inst);
        } else if (inst instanceof MIRLuiOp) {
            generateLuiOp((MIRLuiOp) inst);
        } else {
            System.err.println("Unknown instruction: " + inst);
            throw new IllegalArgumentException("Unsupported instruction type: " + inst.getClass().getSimpleName());
        }
    }

    private void generateLuiOp(MIRLuiOp inst) {
        MIRVirtualReg result = inst.getResult();
        asm.append("    lui ").append(getOperandAsm(result, true)).append(", ")
           .append(inst.getOperands().get(0).toString()).append("\n");
    }

    private void generateLiOp(MIRLiOp inst) {
        MIRVirtualReg result = inst.getResult();
        String value = inst.getOperands().get(0).toString();
        asm.append("    li ").append(getOperandAsm(result, true)).append(", ")
           .append(value).append("\n");
    }

    private void generateLaOp(MIRLaOp inst) {
        MIRVirtualReg result = inst.getResult();
        asm.append("    la ").append(getOperandAsm(result, true)).append(", ")
           .append(inst.getOperands().get(0).toString()).append("\n");
    }

    private void generateAllocOp(MIRAllocOp inst) {
        // 数组分配已在栈管理器中处理，这里不需要生成代码
        MIRVirtualReg result = inst.getResult();
        int offset = stackManager.getArrayOffset(result);
        // 应该用个add

    }

    private void generateMoveOp(MIRMoveOp inst) {
        MIROperand src = inst.getOperands().get(0);
        MIRVirtualReg dest = inst.getResult();
        String op = null;
        if(inst.getMoveType() == MIRMoveOp.MoveType.INTEGER){
            op = "mv";
        } else if(inst.getMoveType() == MIRMoveOp.MoveType.FLOAT){
            op = "fmv.s";
        } else if (inst.getMoveType() == MIRMoveOp.MoveType.INT_TO_FLOAT) {
            op = "fmv.w.x";
        }

        if (src instanceof MIRImmediate) {
            asm.append("    li ").append(getOperandAsm(dest, true)).append(", ").append(src).append("\n");
        } else {
            asm.append("    ").append(op).append(" ")
                    .append(getOperandAsm(dest, true)).append(", ")
                    .append(getOperandAsm(src, false)).append("\n");
        }
    }

    private void generateControlFlowOp(MIRControlFlowOp inst) {
        switch (inst.getType()) {
            case JMP:
                asm.append("    j ").append(inst.getTarget()).append("\n");
                break;
            case COND_JMP:
                // 条件跳转已在CMP指令中设置条件，这里直接跳转
                asm.append("    ").append("bnez").append(" ")
                        .append(getOperandAsm(inst.getCondition(), false)).append(", ")
                        .append(inst.getTarget().toString()).append("\n");
                break;
            case RET:
                asm.append("    ").append("ret").append(" ");
                break;
            case CALL:
                generateFunctionCall(inst);
                break;
            default:
                throw new IllegalArgumentException("Unsupported control flow op: " + inst.getType());
        }
    }


    private void generateCmpOp(MIRCmpOp inst) {
        MIROperand left = inst.getOperands().get(0);
        MIROperand right = inst.getOperands().get(1);
        MIRVirtualReg result = inst.getResult();

        boolean isFloat = MIRType.isFloat(result.getType());
        String op = isFloat ? "feq.s" : "xor";

        asm.append("    ").append(getCmpOp(inst.getOp(), isFloat)).append(" ")
                .append(getOperandAsm(result, true)).append(", ")
                .append(getOperandAsm(left, false)).append(", ")
                .append(getOperandAsm(right, false)).append("\n");
    }

    private String getCmpOp(MIRCmpOp.Op op, boolean isFloat) {
        if (isFloat) {
            switch (op) {
                case EQ: return "feq.s";
                case NE: return "fne.s";
                case LT: return "flt.s";
                case LE: return "fle.s";
                case GT: return "fgt.s";
                case GE: return "fge.s";
                default: throw new IllegalArgumentException("Unsupported float cmp op: " + op);
            }
        } else {
            switch (op) {
                case EQ: return "seqz";
                case NE: return "snez";
                case LT: return "slt";
                case LE: return "sle";
                case GT: return "sgt";
                case GE: return "sge";
                default: throw new IllegalArgumentException("Unsupported int cmp op: " + op);
            }
        }
    }

    private void generateConvertOp(MIRConvertOp inst) {
        MIROperand src = inst.getOperands().get(0);
        MIRVirtualReg dest = inst.getResult();

        boolean srcFloat = MIRType.isFloat(src.getType());
        boolean destFloat = MIRType.isFloat(dest.getType());

        if (srcFloat && !destFloat) {
            asm.append("    fcvt.w.s ").append(getOperandAsm(dest, true)).append(", ").append(getOperandAsm(src, false)).append(", rtz\n");
        } else if (!srcFloat && destFloat) {
            asm.append("    fcvt.s.w ").append(getOperandAsm(dest, true)).append(", ").append(getOperandAsm(src, false)).append("\n");
        } else {
            asm.append("    mv ").append(getOperandAsm(dest, true)).append(", ").append(getOperandAsm(src, false)).append("\n");
        }
    }

    private void generateArithOp(MIRArithOp inst) {
        MIROperand left = inst.getOperands().get(0);
        MIROperand right = inst.getOperands().get(1);
        MIRVirtualReg result = inst.getResult();

        boolean isFloat = MIRType.isFloat(result.getType());
        String op = getArithOp(inst.getOp(), isFloat);

        asm.append("    ").append(op).append(" ")
                .append(getOperandAsm(result, true)).append(", ")
                .append(getOperandAsm(left, false)).append(", ")
                .append(getOperandAsm(right, false)).append("\n");
    }

    private String getArithOp(MIRArithOp.Op op, boolean isFloat) {
        if (isFloat) {
            switch (op) {
                case ADD: return "fadd.s";
                case SUB: return "fsub.s";
                case MUL: return "fmul.s";
                case DIV: return "fdiv.s";
                default: throw new IllegalArgumentException("Unsupported float op: " + op);
            }
        } else {
            switch (op) {
                case ADD: return "add";
                case SUB: return "sub";
                case MUL: return "mul";
                case DIV: return "div";
                case REM: return "rem";
                case XOR: return "xor";
                default: throw new IllegalArgumentException("Unsupported int op: " + op);
            }
        }
    }

    private void generateMemoryOp(MIRMemoryOp op) {
        if (op.getOperands().size() == 1) { // LOAD
            MIRMemory mem = (MIRMemory) op.getOperands().get(0);
            MIRVirtualReg result = op.getResult();
            boolean isFloat = MIRType.isFloat(result.getType());
            String loadOp = isFloat ? "flw" : "lw";

            asm.append("    ").append(loadOp).append(" ")
                    .append(getOperandAsm(result, true)).append(", ")
                    .append(mem.getOffset()).append("(")
                    .append(getOperandAsm(mem.getBase(), false)).append(")\n");
        } else { // STORE
            MIRMemory mem = (MIRMemory) op.getOperands().get(0);
            MIROperand value = op.getOperands().get(1);
            boolean isFloat = MIRType.isFloat(value.getType());
            String storeOp = isFloat ? "fsw" : "sw";

            asm.append("    ").append(storeOp).append(" ")
                    .append(getOperandAsm(value, false)).append(", ")
                    .append(mem.getOffset()).append("(")
                    .append(getOperandAsm(mem.getBase(), false)).append(")\n");
        }
    }

    private void generatePseudoOp(MIRPseudoOp op) {
        switch (op.getType()) {
            case PROLOGUE:
                // 已在函数序言处理
                break;
            case EPILOGUE:
                generateEpilogue();
                break;
            case CALLER_SAVE_REG:
                saveCallerSavedRegisters();
                break;
            case CALLER_RESTORE_REG:
                restoreCallerSavedRegisters();
                break;
            case CALLEE_SAVE_REG:
                saveCalleeSavedRegisters();
                break;
            case CALLEE_RESTORE_REG:
                restoreCalleeSavedRegisters();
                break;
            case SELECT:
                System.err.println("SELECT pseudo-op not implemented yet");
                throw new IllegalArgumentException("SELECT pseudo-op not implemented yet");
            default:
                break;
        }
    }

    private void saveCalleeSavedRegisters() {
        // 已在序言处理
    }

    private void restoreCalleeSavedRegisters() {
        // 已在收尾处理
    }

    private void saveCallerSavedRegisters() {
        asm.append("    # Save caller-saved registers\n");
        int offset = -8;
        for (PhysicalRegister reg : allocator.getUsedCallerSaved()) {
            asm.append("    sd ").append(reg).append(", ").append(offset).append("(s0)\n");
            offset -= 8;
        }
    }

    private void restoreCallerSavedRegisters() {
        asm.append("    # Restore caller-saved registers\n");
        int offset = -8;
        for (PhysicalRegister reg : allocator.getUsedCallerSaved()) {
            asm.append("    ld ").append(reg).append(", ").append(offset).append("(s0)\n");
            offset -= 8;
        }
    }

    private void generateFunctionCall(MIRControlFlowOp call) {
        // 保存调用者保存寄存器
        saveCallerSavedRegisters();

        // 设置参数 (最多8个寄存器参数)
//        List<MIROperand> args = call.getOperands();
//        int regArgs = Math.min(args.size(), 8);
//        for (int i = 0; i < regArgs; i++) {
//            MIROperand arg = args.get(i);
//            PhysicalRegister reg = MIRType.isFloat(arg.getType()) ?
//                    PhysicalRegister.getFloatArgReg(i) :
//                    PhysicalRegister.getIntArgReg(i);
//
//            asm.append("    mv ").append(reg).append(", ").append(getOperandAsm(arg)).append("\n");
//        }
//
//        // 处理栈上传参 (超出8个的参数)
//        if (args.size() > 8) {
//            int stackOffset = 0;
//            for (int i = 8; i < args.size(); i++) {
//                MIROperand arg = args.get(i);
//                asm.append("    li t0, ").append(arg).append("\n");
//                asm.append("    sd t0, ").append(stackOffset).append("(sp)\n");
//                stackOffset += 8;
//            }
//        }

        // 调用函数
        asm.append("    call ").append(call.getTarget().toString()).append("\n");

        // 恢复调用者保存寄存器
        restoreCallerSavedRegisters();

        // 处理返回值
        if (call.getResult() != null) {
            MIRVirtualReg resultReg = call.getResult();
            PhysicalRegister retReg = MIRType.isFloat(resultReg.getType()) ?
                    PhysicalRegister.FA0 : PhysicalRegister.A0;

            String moveOp = MIRType.isFloat(resultReg.getType()) ?
                    "fmv.s" : "mv";

            asm.append("    ").append(moveOp).append(" ")
                    .append(getOperandAsm(resultReg, true)).append(", ")
                    .append(retReg).append("\n");
        }
    }

    private String getOperandAsm(MIROperand operand, boolean isDest) {
        if (operand == null){
            throw new IllegalArgumentException("Operand cannot be null");
        }

        if (operand instanceof MIRVirtualReg) {
            MIRVirtualReg vreg = (MIRVirtualReg) operand;

            // 记录目标操作数（用于后续存储）
            if (isDest) {
                currentDestOperand = vreg;
            }

//            // 检查数组基址
//            if (arrayOffsets.containsKey(vreg)) {
//                return "t0"; // 已在generateMemoryOp中计算
//            }

            // 检查寄存器分配
            PhysicalRegister preg = allocator.getRegisterFor(vreg);
            if (preg != null) {
                return preg.toString();
            }

            // 处理spill情况
            int spillOffset = stackManager.getSpillOffset(vreg);
            if (spillOffset != 0) {
                // 加载到临时寄存器
                PhysicalRegister tempReg = MIRType.isFloat(vreg.getType()) ?
                        allocator.getFloatTempReg() :
                        allocator.getIntTempReg();
                // 如果是目标操作数，先记录临时寄存器（稍后存储）
                if (isDest) {
                    currentDestTempReg = tempReg;
                    return tempReg.toString();
                }
                // 源操作数直接加载
                String loadOp = MIRType.isFloat(vreg.getType()) ? "fld" : "ld";
                asm.append("    ").append(loadOp).append(" ")
                        .append(tempReg).append(", ")
                        .append(spillOffset).append("(s0)\n");
                return tempReg.toString();
            }
        } else if (operand instanceof MIRImmediate) {
            return operand.toString();
        } else if (operand instanceof MIRMemory) {
            MIRMemory mem = (MIRMemory) operand;
            return mem.getOffset() + "(" + getOperandAsm(mem.getBase(), false) + ")";
        } else if (operand instanceof MIRPhysicalReg) {
            return operand.toString();
        }

        return operand.toString();
    }

    private void storeSpilledDestOperand(MIRVirtualReg vreg, PhysicalRegister tempReg) {
        Integer spillOffset = allocator.getSpillLocation(vreg);
        if (spillOffset != null) {
            boolean isFloat = MIRType.isFloat(vreg.getType());
            String storeOp = isFloat ? "fsw" : "sw";
            asm.append("    ").append(storeOp).append(" ")
                    .append(tempReg).append(", ")
                    .append(spillOffset).append("(s0)\n");
        }
    }
}


