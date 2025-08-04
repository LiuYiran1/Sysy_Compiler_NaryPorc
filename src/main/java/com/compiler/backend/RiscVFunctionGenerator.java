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
        asm.append("\n").append(function.getName()).append(":\n");

        // 函数序言
        generatePrologue();

        // 生成基本块代码
        for (MIRBasicBlock block : function.getBlocks()) {
            asm.append("\n").append(block.getLabel().toString()).append(":\n");
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
        asm.append("    addi sp, sp, -16").append("\n");
        asm.append("    sd ra, ").append("8").append("(sp)\n");
        asm.append("    sd s0, ").append("0").append("(sp)\n");
        //asm.append("    addi s0, sp, ").append(frameSize).append("\n");
        if(frameSize - 16 > 2048) {
            asm.append("    li t2, ").append(-(frameSize - 16)).append("\n");
            asm.append("    add sp, sp, t2").append("\n");
        } else {
            asm.append("    addi sp, sp, -").append(frameSize - 16).append("\n");
        }

        if(frameSize > 2048) {
            asm.append("    li t2, ").append(frameSize).append("\n");
            asm.append("    add s0, sp, t2").append("\n");
        } else {
            asm.append("    addi s0, sp, ").append(frameSize).append("\n");
        }


//        asm.append("    addi sp, sp, -").append(frameSize).append("\n");
//        asm.append("    sd ra, ").append(frameSize - 8).append("(sp)\n");
//        asm.append("    sd s0, ").append(frameSize - 16).append("(sp)\n");
//        asm.append("    addi s0, sp, ").append(frameSize).append("\n");

        if(!function.getName().equals("main")) {
            // main函数不需要保存
            // 保存被调用者保存寄存器
//            int offset = frameSize - 24;
            int offset = -24;
            for (PhysicalRegister reg : allocator.getUsedCalleeSaved()) {
                if(reg.name().startsWith("f")){
                    // 说明是float的
                    asm.append("    fsw ").append(reg).append(", ").append(offset).append("(s0)\n");
                } else {
                    asm.append("    sd ").append(reg).append(", ").append(offset).append("(s0)\n");
                }
                offset -= 8;
            }
        }

    }

    private void generateEpilogue() {
        int frameSize = stackManager.getFrameSize();

        asm.append("    # Function epilogue\n");

//        int offset = frameSize - 24;
//        for (PhysicalRegister reg : allocator.getUsedCalleeSaved()) {
//            asm.append("    ld ").append(reg).append(", ").append(offset).append("(sp)\n");
//            offset -= 8;
//        }
        if(!function.getName().equals("main")) {
            int offset = -24;
            for (PhysicalRegister reg : allocator.getUsedCalleeSaved()) {
                if(reg.name().startsWith("f")){
                    // 说明是float的
                    asm.append("    flw ").append(reg).append(", ").append(offset).append("(s0)\n");
                } else {
                    asm.append("    ld ").append(reg).append(", ").append(offset).append("(s0)\n");
                }
                offset -= 8;
            }
        }

        if(frameSize - 16 > 2048) {
            asm.append("    li t2, ").append(frameSize - 16).append("\n");
            asm.append("    add sp, sp, t2").append("\n");
        } else {
            asm.append("    addi sp, sp, ").append(frameSize - 16).append("\n");
        }

//        asm.append("    addi sp, sp, ").append(frameSize - 16).append("\n");
        asm.append("    ld ra, ").append("8").append("(sp)\n");
        asm.append("    ld s0, ").append("0").append("(sp)\n");
        asm.append("    addi sp, sp, ").append("16").append("\n");
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
        } else if (inst instanceof MIRShiftOp) {
            generateShiftOp((MIRShiftOp) inst);
        } else {
            System.err.println("Unknown instruction: " + inst);
            throw new IllegalArgumentException("Unsupported instruction type: " + inst.getClass().getSimpleName());
        }
    }

    private void generateShiftOp(MIRShiftOp inst) {
        MIRVirtualReg result = inst.getResult();
        String reg = getOperandAsm(inst.getSource(),false);

        asm.append("    slli ").append(getOperandAsm(result, true)).append(", ");
        if(currentDestTempReg != null){
            MIRVirtualReg vreg = currentDestOperand;
            PhysicalRegister tempReg = currentDestTempReg;
            asm.append(reg).append(", ").append(inst.getShiftAmount()).append("\n");
            storeSpilledDestOperand(vreg, tempReg);
        } else {
            asm.append(reg).append(", ").append(inst.getShiftAmount()).append("\n");
        }

    }

    private void generateLuiOp(MIRLuiOp inst) {
        MIRVirtualReg result = inst.getResult();
        asm.append("    lui ").append(getOperandAsm(result, true)).append(", ")
           .append(inst.getOperands().get(0).toString()).append("\n");

        if(currentDestTempReg != null) {
            storeSpilledDestOperand(currentDestOperand, currentDestTempReg);
        }
    }

    private void generateLiOp(MIRLiOp inst) {
        MIROperand result = inst.getResult() != null ? inst.getResult() : inst.getResultPhysicalReg();
        String value = inst.getOperands().get(0).toString();
        asm.append("    li ").append(getOperandAsm(result, true)).append(", ")
           .append(value).append("\n");
        if(currentDestTempReg != null) {
            storeSpilledDestOperand(currentDestOperand, currentDestTempReg);
        }
    }

    private void generateLaOp(MIRLaOp inst) {
        MIRVirtualReg result = inst.getResult();
        asm.append("    la ").append(getOperandAsm(result, true)).append(", ")
           .append(inst.getOperands().get(0).toString()).append("\n");
        if(currentDestTempReg != null) {
            storeSpilledDestOperand(currentDestOperand, currentDestTempReg);
        }
    }

    // TODO: 未完成
    private void generateAllocOp(MIRAllocOp inst) {

        MIRVirtualReg result = inst.getResult();
        int offset = stackManager.getArrayOffset(result);
        // 应该用个add
        if(offset > 2048) {
            asm.append("    li t2, ").append(-offset).append("\n");
            asm.append("    add ").append(getOperandAsm(result, true)).append(", ")
               .append("s0, ").append("t2").append("\n");
        } else {
            asm.append("    addi ").append(getOperandAsm(result, true)).append(", ")
               .append("s0, ").append("-").append(offset).append("\n");
        }

//        asm.append("    addi ").append(getOperandAsm(result, true)).append(", ")
//           .append("s0, ").append("-").append(offset).append("\n");
        if(currentDestTempReg != null) {
            storeSpilledDestOperand(currentDestOperand, currentDestTempReg);
        }
    }

    private void generateMoveOp(MIRMoveOp inst) {
        MIROperand src = inst.getOperands().get(0);
        MIROperand dest = inst.getResult() != null ? inst.getResult() : inst.getResultPhysicalReg();
        String op = null;
        if(inst.getMoveType() == MIRMoveOp.MoveType.INTEGER){
            op = "mv";
        } else if(inst.getMoveType() == MIRMoveOp.MoveType.FLOAT){
            op = "fmv.s";
        } else if (inst.getMoveType() == MIRMoveOp.MoveType.INT_TO_FLOAT) {
            op = "fmv.w.x";
        } else if(inst.getMoveType() == MIRMoveOp.MoveType.FLOAT_TO_INT){
            op = "fmv.x.w";
        }

        if (src instanceof MIRImmediate) {
            asm.append("    li ").append(getOperandAsm(dest, true)).append(", ")
               .append(src).append("\n");
            if(currentDestTempReg != null) {
                storeSpilledDestOperand(currentDestOperand, currentDestTempReg);
            }
        } else {
            String reg = getOperandAsm(src, false);
            asm.append("    ").append(op).append(" ")
               .append(getOperandAsm(dest, true)).append(", ");
            if(currentDestTempReg != null) {
                MIRVirtualReg vreg = currentDestOperand;
                PhysicalRegister tempReg = currentDestTempReg;
                asm.append(reg).append("\n");
                storeSpilledDestOperand(vreg, tempReg);
            } else {
                asm.append(reg).append("\n");
            }
        }
    }

    private void generateControlFlowOp(MIRControlFlowOp inst) {
        switch (inst.getType()) {
            case JMP:
                asm.append("    j ").append(inst.getTarget().toString()).append("\n");
                break;
            case COND_JMP:
                // 条件跳转已在CMP指令中设置条件，这里直接跳转
                String reg = getOperandAsm(inst.getCondition(), false);
                asm.append("    ").append("bnez").append(" ")
                   .append(reg).append(", ")
                   .append(inst.getTarget().toString()).append("\n");
                break;
            case RET:
                asm.append("    ").append("ret").append(" ");
                break;
            case CALL:
                asm.append("    call ").append(inst.getTarget().toString()).append("\n");
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

        String leftReg = getOperandAsm(left, false);
        String rightReg = getOperandAsm(right, false);
        if(!isFloat) {
            switch (inst.getOp()){
                case EQ:
                    // xor + seqz
                    asm.append("    xor").append(" ").append(getOperandAsm(result, true)).append(", ");

                    if(currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        asm.append("    seqz").append(" ").append(tempReg).append(", ").append(tempReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        String tempReg = getOperandAsm(result, false);
                        asm.append("    seqz").append(" ").append(getOperandAsm(result, true)).append(", ")
                           .append(tempReg).append("\n");
                    }

                    break;
                case NE:
                    // xor + snez
                    asm.append("    xor ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        asm.append("    snez ").append(tempReg).append(", ").append(tempReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        String tempReg = getOperandAsm(result, false);
                        asm.append("    snez ").append(getOperandAsm(result, true)).append(", ")
                           .append(tempReg).append("\n");
                    }

                    break;
                case LT:
                    // slt
                    asm.append("    slt ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                    }
                    break;
                case LE:
                    // slt(交换） + xori
                    asm.append("    slt ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                        asm.append("    xori ").append(tempReg).append(", ").append(tempReg).append(", 1\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                        String tempReg = getOperandAsm(result, false);
                        asm.append("    xori ").append(getOperandAsm(result, true)).append(", ")
                           .append(tempReg).append(", 1\n");
                    }
                    break;
                case GT:
                    // slt (交换src）
                    asm.append("    slt ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                    }
                    break;
                case GE:
                    // slt + xori
                    asm.append("    slt ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        asm.append("    xori ").append(tempReg).append(", ").append(tempReg).append(", 1\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        String tempReg = getOperandAsm(result, false);
                        asm.append("    xori ").append(getOperandAsm(result, true)).append(", ")
                           .append(tempReg).append(", 1\n");
                    }
                    break;
                default: throw new IllegalArgumentException("Unsupported float cmp op: " + inst.getOp());
            }
        } else {
            switch (inst.getOp()){
                case EQ:
                    // feq.s
                    asm.append("    feq.s ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                    }
                    break;
                case NE:
                    // feq.s + xori
                    asm.append("    feq.s ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        asm.append("    xori ").append(tempReg).append(", ").append(tempReg).append(", 1\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        String tempReg = getOperandAsm(result, false);
                        asm.append("    xori ").append(getOperandAsm(result, true)).append(", ")
                           .append(tempReg).append(", 1\n");
                    }
                    break;
                case LT:
                    // flt.s
                    asm.append("    flt.s ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                    }
                    break;
                case LE:
                    // fle.s
                    asm.append("    fle.s ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(leftReg).append(", ")
                           .append(rightReg).append("\n");
                    }
                    break;
                case GT:
                    // flt.s(交换src）
                    asm.append("    flt.s ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                    }
                    break;
                case GE:
                    // fle.s(交换src）
                    asm.append("    fle.s ").append(getOperandAsm(result, true)).append(", ");
                    if (currentDestTempReg != null) {
                        MIRVirtualReg vreg = currentDestOperand;
                        PhysicalRegister tempReg = currentDestTempReg;
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                        storeSpilledDestOperand(vreg, tempReg);
                    } else {
                        asm.append(rightReg).append(", ")
                           .append(leftReg).append("\n");
                    }

                    break;
                default: throw new IllegalArgumentException("Unsupported int cmp op: " + inst.getOp());
            }
        }
    }

    private void generateConvertOp(MIRConvertOp inst) {
        MIROperand src = inst.getOperands().get(0);
        MIRVirtualReg dest = inst.getResult();

        String reg = getOperandAsm(src, false);

        if(inst.getOp() == MIRConvertOp.Op.FPTOI){
            asm.append("    fcvt.w.s ").append(getOperandAsm(dest, true)).append(", ");

            if(currentDestTempReg != null) {
                MIRVirtualReg vreg = currentDestOperand;
                PhysicalRegister tempReg = currentDestTempReg;
                asm.append(reg).append(", rtz\n");
                storeSpilledDestOperand(vreg, tempReg);
            } else {
                asm.append(reg).append(", rtz\n");
            }

        } else if(inst.getOp() == MIRConvertOp.Op.ITOFP){
            asm.append("    fcvt.s.w ").append(getOperandAsm(dest, true)).append(", ");
            if(currentDestTempReg != null) {
                MIRVirtualReg vreg = currentDestOperand;
                PhysicalRegister tempReg = currentDestTempReg;
                asm.append(reg).append("\n");
                storeSpilledDestOperand(vreg, tempReg);
            } else {
                asm.append(reg).append("\n");
            }

        } else if (inst.getOp() == MIRConvertOp.Op.ZEXT) {
            asm.append("    mv ").append(getOperandAsm(dest, true)).append(", ");
            if(currentDestTempReg != null) {
                MIRVirtualReg vreg = currentDestOperand;
                PhysicalRegister tempReg = currentDestTempReg;
                asm.append(reg).append("\n");
                storeSpilledDestOperand(vreg, tempReg);
            } else {
                asm.append(reg).append("\n");
            }
        } else {
            throw new IllegalArgumentException("Unsupported convert op: " + inst.getOp());
        }
    }

    private void generateArithOp(MIRArithOp inst) {
        MIROperand left = inst.getOperands().get(0);
        MIROperand right = inst.getOperands().get(1);
        MIROperand result = inst.getResult() != null ? inst.getResult() : inst.getResultPhysicalReg();

        String leftReg = getOperandAsm(left, false);
        String rightReg = getOperandAsm(right, false);

        boolean isFloat = MIRType.isFloat(result.getType());
        String op = getArithOp(inst.getOp(), isFloat);

        if(op.equals("add") && right instanceof MIRImmediate){
            op = "addi";
        }

        asm.append("    ").append(op).append(" ")
           .append(getOperandAsm(result, true)).append(", ");
        if(currentDestTempReg != null) {
            MIRVirtualReg vreg = currentDestOperand;
            PhysicalRegister tempReg = currentDestTempReg;
            asm.append(leftReg).append(", ")
               .append(rightReg).append("\n");
            storeSpilledDestOperand(vreg, tempReg);
        } else {
            asm.append(leftReg).append(", ")
               .append(rightReg).append("\n");
        }

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

    // 针对数组的寻址存储
    private void generateMemoryOp(MIRMemoryOp op) {
        if (op.getOperands().size() == 1) { // LOAD
            MIRMemory mem = (MIRMemory) op.getOperands().get(0);
            MIRVirtualReg result = op.getResult();
            boolean isFloat = MIRType.isFloat(result.getType());
            String loadOp = isFloat ? "flw" : "lw";

            String base = getOperandAsm(mem.getBase(), false);

            asm.append("    ").append(loadOp).append(" ")
               .append(getOperandAsm(result, true)).append(", ");
            if(currentDestTempReg != null){
                MIRVirtualReg vreg = currentDestOperand;
                PhysicalRegister tempReg = currentDestTempReg;
                asm.append(mem.getOffset()).append("(")
                   .append(base).append(")\n");
                storeSpilledDestOperand(vreg, tempReg);
            } else {
                asm.append(mem.getOffset()).append("(")
                   .append(base).append(")\n");
            }

        } else { // STORE
            MIRMemory mem = (MIRMemory) op.getOperands().get(0);
            MIROperand value = op.getOperands().get(1);

            String val = getOperandAsm(value, false);
            String base = getOperandAsm(mem.getBase(), false);

            boolean isFloat = MIRType.isFloat(value.getType());
            String storeOp = isFloat ? "fsw" : "sw";

            asm.append("    ").append(storeOp).append(" ")
                    .append(val).append(", ")
                    .append(mem.getOffset()).append("(")
                    .append(base).append(")\n");
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


    // TODO: 下面这两个方法也有问题，没有区分浮点寄存器和整型寄存器。
    private void saveCallerSavedRegisters() {
        asm.append("    # Save caller-saved registers\n");
        int stackSize = allocator.getUsedCallerSaved().size() * 8;
        asm.append("    addi ").append("sp, sp, ").append(-stackSize).append("\n");
        int offset = stackSize - 8;
        for (PhysicalRegister reg : allocator.getUsedCallerSaved()) {
            if(reg.name().startsWith("f")){
                // 说明是float的
                asm.append("    fsw ").append(reg).append(", ").append(offset).append("(sp)\n");
            } else {
                asm.append("    sd ").append(reg).append(", ").append(offset).append("(sp)\n");
            }

//            asm.append("    sd ").append(reg).append(", ").append(offset).append("(s0)\n");
            offset -= 8;
        }

    }

    private void restoreCallerSavedRegisters() {
        asm.append("    # Restore caller-saved registers\n");
        int stackSize = allocator.getUsedCallerSaved().size() * 8;

        int offset = stackSize - 8;
        for (PhysicalRegister reg : allocator.getUsedCallerSaved()) {

            if(reg.name().startsWith("f")){
                // 说明是float的
                asm.append("    flw ").append(reg).append(", ").append(offset).append("(sp)\n");
            } else {
                asm.append("    ld ").append(reg).append(", ").append(offset).append("(sp)\n");
            }
//            asm.append("    ld ").append(reg).append(", ").append(offset).append("(s0)\n");
            offset -= 8;
        }

        asm.append("    addi ").append("sp, sp, ").append(stackSize).append("\n");
    }


    private String getOperandAsm(MIROperand operand, boolean isDest) {
        if (operand == null){
            throw new IllegalArgumentException("Operand cannot be null");
        }
        currentDestOperand = null; // 重置当前目标操作数
        currentDestTempReg = null; // 重置当前目标临时寄存器

        // 应该分为dest和非dest两种情况

        if (operand instanceof MIRVirtualReg) {
            MIRVirtualReg vreg = (MIRVirtualReg) operand;

            // 记录目标操作数（用于后续存储）
            if (isDest) {
                currentDestOperand = vreg;
            }

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
                String loadOp = MIRType.isFloat(vreg.getType()) ? "flw" : "ld";
                asm.append("    ").append(loadOp).append(" ")
                        .append(tempReg).append(", ")
                        .append(spillOffset).append("(s0)\n");
                return tempReg.toString();
            } else {
                throw new IllegalArgumentException("Spilled operand should be found in allocator: " + vreg);
            }
        } else if (operand instanceof MIRImmediate) {
            return operand.toString();
        } else if (operand instanceof MIRMemory) {
            // 这段应该专门为分配到栈上的函数调用参数准备

            MIRMemory mem = (MIRMemory) operand;
            MIRType type = mem.getType();
            PhysicalRegister tempReg = MIRType.isFloat(type) ?
                    allocator.getFloatTempReg() :
                    allocator.getIntTempReg();

            // 源操作数直接加载
            String loadOp = MIRType.isFloat(type) ? "flw" : "ld";
            asm.append("    ").append(loadOp).append(" ")
                    .append(tempReg).append(", ")
                    .append("-"+mem.getOffset().toString()).append("("+ mem.getBase().toString() + ")").append("\n");

            return tempReg.toString();
        } else if (operand instanceof MIRPhysicalReg) {
            return operand.toString();
        }

        return operand.toString();
    }

    private void storeSpilledDestOperand(MIRVirtualReg vreg, PhysicalRegister tempReg) {
        Integer spillOffset = allocator.getSpillLocation(vreg);
        if (spillOffset != null) {
            boolean isFloat = MIRType.isFloat(vreg.getType());
            String storeOp = isFloat ? "fsd" : "sd";
            asm.append("    ").append(storeOp).append(" ")
                    .append(tempReg).append(", ")
                    .append(spillOffset).append("(s0)\n");
        }
    }
}


