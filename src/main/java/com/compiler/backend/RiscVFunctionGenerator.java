package com.compiler.backend;

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

        return asm.toString();
    }

    private void generatePrologue() {
        // 这一部分将ra和s0单独操作，之后再移动sp去保存寄存器
        int frameSize = stackManager.getFrameSize();

        asm.append("    # Function prologue for ").append(function.getName()).append("\n");
        asm.append("    addi sp, sp, -16").append("\n");
        asm.append("    sd ra, ").append("8").append("(sp)\n");
        asm.append("    sd s0, ").append("0").append("(sp)\n");

        if(frameSize - 16 >= 2048) {
            asm.append("    li t2, ").append(-(frameSize - 16)).append("\n");
            asm.append("    add sp, sp, t2").append("\n");
        } else {
            asm.append("    addi sp, sp, -").append(frameSize - 16).append("\n");
        }

        if(frameSize >= 2048) {
            asm.append("    li t2, ").append(frameSize).append("\n");
            asm.append("    add s0, sp, t2").append("\n");
        } else {
            asm.append("    addi s0, sp, ").append(frameSize).append("\n");
        }

        if(!function.getName().equals("main")) {
            // main函数不需要保存
            // 保存被调用者保存寄存器
            int offset = -24;
            for (PhysicalRegister reg : allocator.getUsedCalleeSaved()) {
                if(reg.name().startsWith("F")){
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

        if(!function.getName().equals("main")) {
            int offset = -24;
            for (PhysicalRegister reg : allocator.getUsedCalleeSaved()) {
                if(reg.name().startsWith("F")){
                    // 说明是float的
                    asm.append("    flw ").append(reg).append(", ").append(offset).append("(s0)\n");
                } else {
                    asm.append("    ld ").append(reg).append(", ").append(offset).append("(s0)\n");
                }
                offset -= 8;
            }
        }

        if(frameSize - 16 >= 2048) {
            asm.append("    li t2, ").append(frameSize - 16).append("\n");
            asm.append("    add sp, sp, t2").append("\n");
        } else {
            asm.append("    addi sp, sp, ").append(frameSize - 16).append("\n");
        }

        asm.append("    ld ra, ").append("8").append("(sp)\n");
        asm.append("    ld s0, ").append("0").append("(sp)\n");
        asm.append("    addi sp, sp, ").append("16").append("\n");
    }

    private void generateInstruction(MIRInstruction inst) {
        if (inst instanceof MIRPseudoOp) {
            generatePseudoOp((MIRPseudoOp) inst);
        } else if (inst instanceof MIRArithOp) {
//            generateArithOp((MIRArithOp) inst);
            generateArithOpOptimized((MIRArithOp) inst);
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
        } else if (inst instanceof MIRShiftOp) {
            generateShiftOp((MIRShiftOp) inst);
        } else {
            System.err.println("Unknown instruction: " + inst);
            throw new IllegalArgumentException("Unsupported instruction type: " + inst.getClass().getSimpleName());
        }
    }

    private void generateShiftOp(MIRShiftOp inst) {
        // 目前只涉及到左移

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

    private void generateAllocOp(MIRAllocOp inst) {

        MIRVirtualReg result = inst.getResult();
        int offset = stackManager.getArrayOffset(result);
        // 应该用个add
        if(offset >= 2048) {
            asm.append("    li t2, ").append(-offset).append("\n");
            asm.append("    add ").append(getOperandAsm(result, true)).append(", ")
               .append("s0, ").append("t2").append("\n");
        } else {
            asm.append("    addi ").append(getOperandAsm(result, true)).append(", ")
               .append("s0, ").append("-").append(offset).append("\n");
        }

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

        String leftReg = getOperandAsm(left, false);
        String rightReg = getOperandAsm(right, false);
        if(inst.getCmpType() == MIRCmpOp.Type.INT) {
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
        String resultReg = getOperandAsm(result, true);

        String op = getArithOp(inst.getOp(), inst.getType());

        if(op.equals("add") && right instanceof MIRImmediate){
            op = "addi";
        } else if(op.equals("addw") && right instanceof MIRImmediate){
            op = "addiw";
        } else if(right instanceof MIRImmediate){
            System.err.println(rightReg);
            asm.append("    li t2,").append(rightReg).append("\n");
            rightReg = "t2";
        }

        asm.append("    ").append(op).append(" ")
           .append(resultReg).append(", ");
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

    private void generateArithOpOptimized(MIRArithOp inst) {
        MIROperand left = inst.getOperands().get(0);
        MIROperand right = inst.getOperands().get(1);
        MIROperand result = inst.getResult() != null ? inst.getResult() : inst.getResultPhysicalReg();

        String leftReg = getOperandAsm(left, false);
        String rightReg = getOperandAsm(right, false);
        String resultReg = getOperandAsm(result, true);
        //System.err.println(resultReg);

        // 尝试优化整数运算（特别是乘除）
        if (inst.getType() == MIRArithOp.Type.INT && tryOptimizeArithOp(inst, leftReg, right, resultReg)) {
            return; // 优化成功，直接返回
        }

        // 未优化或无法优化的情况
        String op = getArithOp(inst.getOp(), inst.getType());

        // 处理加减法的立即数优化
        if (right instanceof MIRImmediate) {
            long imm = ((MIRImmediate) right).getValue();
            switch (op) {
                case "add":
                case "addw":
                    op = op.equals("add") ? "addi" : "addiw";
                    break;
                case "sub":
                case "subw":
                    // 减法转换为加负数
                    op = op.equals("sub") ? "addi" : "addiw";
                    imm = -imm;
                    rightReg = String.valueOf(imm);
                    System.err.println("subw: " + rightReg);
                    break;
                case "xor":
                    op = "xori";
                    break;
                default:
                    System.err.println("default: " + rightReg);
                    asm.append("    li t2,").append(rightReg).append("\n");
                    rightReg = "t2";
                    break;

            }
        }

        asm.append("    ").append(op).append(" ")
           .append(resultReg).append(", ");

        if (currentDestTempReg != null) {
            MIRVirtualReg vreg = currentDestOperand;
            PhysicalRegister tempReg = currentDestTempReg;
            asm.append(leftReg).append(", ").append(rightReg).append("\n");
            storeSpilledDestOperand(vreg, tempReg);
        } else {
            asm.append(leftReg).append(", ").append(rightReg).append("\n");
        }
    }

    // 尝试优化算术操作（乘除和求余）
    private boolean tryOptimizeArithOp(MIRArithOp inst, String leftReg, MIROperand right, String resultReg) {
        // 只处理整数运算
        if (inst.getType() != MIRArithOp.Type.INT) {
            return false;
        }

        // 1. 乘法优化
        if (inst.getOp() == MIRArithOp.Op.MUL && right instanceof MIRImmediate) {
            long imm = ((MIRImmediate) right).getValue();
            boolean isNegative = imm < 0;
            long absImm = Math.abs(imm);
            System.err.println(absImm);

            // a. 乘以0 - 直接清零
            if (absImm == 0) {
                if (currentDestTempReg != null) {
                    MIRVirtualReg vreg = currentDestOperand;
                    PhysicalRegister tempReg = currentDestTempReg;
                    asm.append("    li ").append(resultReg).append(", 0\n");
                    storeSpilledDestOperand(vreg, tempReg);
                } else {
                    asm.append("    li ").append(resultReg).append(", 0\n");
                }
                return true;
            }

            // b. 乘以1 - 直接移动
            if (absImm == 1) {
                if (currentDestTempReg != null) {
                    MIRVirtualReg vreg = currentDestOperand;
                    PhysicalRegister tempReg = currentDestTempReg;
                    if (isNegative) {
                        asm.append("    negw ").append(resultReg).append(", ").append(leftReg).append("\n");
                    } else {
                        asm.append("    mv ").append(resultReg).append(", ").append(leftReg).append("\n");
                    }
                    storeSpilledDestOperand(vreg, tempReg);
                } else {
                    if (isNegative) {
                        asm.append("    negw ").append(resultReg).append(", ").append(leftReg).append("\n");
                    } else {
                        asm.append("    mv ").append(resultReg).append(", ").append(leftReg).append("\n");
                    }
                }
                return true;
            }

            // c. 乘以2的幂 - 移位
            if (isPowerOfTwo(absImm)) {
                int shift = Long.numberOfTrailingZeros(absImm);
                //System.err.println("imm: " + absImm + " shift: " + shift);
                if (currentDestTempReg != null) {
                    MIRVirtualReg vreg = currentDestOperand;
                    PhysicalRegister tempReg = currentDestTempReg;
                    asm.append("    slliw ").append(resultReg).append(", ")
                       .append(leftReg).append(", ").append(shift).append("\n");
                    if (isNegative) {
                        asm.append("    negw ").append(resultReg).append(", ").append(resultReg).append("\n");
                    }
                    storeSpilledDestOperand(vreg, tempReg);
                } else {
                    asm.append("    slliw ").append(resultReg).append(", ")
                       .append(leftReg).append(", ").append(shift).append("\n");
                    if (isNegative) {
                        asm.append("    negw ").append(resultReg).append(", ").append(resultReg).append("\n");
                    }
                }
                return true;
            }

            // d. 特殊分解优化
            if (decomposeMultiplication(leftReg, resultReg, absImm, isNegative)) {
                return true;
            }

        }

        // 2. 除法优化
        if (inst.getOp() == MIRArithOp.Op.DIV && right instanceof MIRImmediate) {
            long divisor = ((MIRImmediate) right).getValue();
            boolean isNegative = divisor < 0;
            long absDivisor = Math.abs(divisor);

            // a. 除以1 - 直接移动
            if (absDivisor == 1) {
                if (isNegative) {
                    asm.append("    negw ").append(resultReg).append(", ").append(leftReg).append("\n");
                } else {
                    asm.append("    mv ").append(resultReg).append(", ").append(leftReg).append("\n");
                }
                if (currentDestTempReg != null) {
                    MIRVirtualReg vreg = currentDestOperand;
                    PhysicalRegister tempReg = currentDestTempReg;
                    storeSpilledDestOperand(vreg, tempReg);
                }
                return true;
            }

            // b. 除以2的幂 - 需要更复杂的处理
            if (isPowerOfTwo(absDivisor)) {
                int shift = Long.numberOfTrailingZeros(absDivisor);

                // 计算被除数加上(除数-1)如果被除数是负数
                asm.append("    srai ").append("t2").append(", ").append(leftReg).append(", 31\n");
                asm.append("    andi ").append("t2").append(", ").append("t2").append(", ").append(absDivisor - 1).append("\n");
                asm.append("    add ").append(resultReg).append(", ").append(leftReg).append(", ").append("t2").append("\n");
                asm.append("    srai ").append(resultReg).append(", ").append(resultReg).append(", ").append(shift).append("\n");

                // 如果除数是负数，需要取负
                if (isNegative) {
                    asm.append("    negw ").append(resultReg).append(", ").append(resultReg).append("\n");
                }

                if (currentDestTempReg != null) {
                    MIRVirtualReg vreg = currentDestOperand;
                    PhysicalRegister tempReg = currentDestTempReg;
                    storeSpilledDestOperand(vreg, tempReg);
                }
                return true;
            }
        }


        // 3. 取模优化
        if (inst.getOp() == MIRArithOp.Op.REM && right instanceof MIRImmediate) {
            long divisor = ((MIRImmediate) right).getValue();
            long absDivisor = Math.abs(divisor);
            boolean isDivisorNegative = divisor < 0;

            // a. 模1或-1 - 直接清零 (结果符号与被除数相同，但0的符号无所谓)
            if (absDivisor == 1) {
                asm.append("    li ").append(resultReg).append(", 0\n");
                if (currentDestTempReg != null) {
                    MIRVirtualReg vreg = currentDestOperand;
                    PhysicalRegister tempReg = currentDestTempReg;
                    storeSpilledDestOperand(vreg, tempReg);
                }
                return true;
            }

            // b. 模2的幂 (处理所有符号情况)
            if (isPowerOfTwo(absDivisor)) {
                long mask = absDivisor - 1;

                // 1. 计算 raw modulus: tempReg2 = leftReg & mask
                asm.append("    andi ").append(resultReg).append(", ").append(leftReg).append(", ").append(mask).append("\n");

                // 2. 生成符号掩码: tempReg1 = (leftReg < 0) ? -1 : 0
                asm.append("    srai ").append("t2").append(", ").append(leftReg).append(", 31\n");

                // 3. 将符号掩码与 |divisor| 进行与操作，得到调整值: tempReg1 = (leftReg < 0) ? absDivisor : 0
                //    注意：如果除数是负数，我们需要用 |divisor| 而不是 divisor。
                asm.append("    andi ").append("t2").append(", ").append("t2").append(", ").append(absDivisor).append("\n");

                // 4. 关键调整:
                //    if (leftReg < 0 && tempReg2 != 0) {
                //        result = tempReg2 - absDivisor;
                //    } else {
                //        result = tempReg2;
                //    }
                // 等价于: result = tempReg2 - tempReg1
                // 但如果tempReg2==0，减去absDivisor会得到错误结果。所以需要判断。

                // 更安全的方法：先判断是否需要调整
                // 如果 tempReg1 != 0 (即被除数为负) 且 tempReg2 != 0，则 resultReg = tempReg2 - absDivisor
                // 否则 resultReg = tempReg2

                String labelAdjust = ".L_adjust_" + System.identityHashCode(inst); // 生成唯一标签
                String labelDone = ".L_done_" + System.identityHashCode(inst);

                // 如果被除数是正数 (tempReg1 == 0)，直接跳转到最后，结果就是 tempReg2
                asm.append("    beqz ").append("t2").append(", ").append(labelDone).append("\n");
                // 如果余数已经是0 (tempReg2 == 0)，也跳转到最后，结果是0
                asm.append("    beqz ").append(resultReg).append(", ").append(labelDone).append("\n");

                // 如果需要调整（被除数为负且余数非零）：执行减法
                asm.append("    sub ").append(resultReg).append(", ").append(resultReg).append(", ").append("t2").append("\n");
                asm.append("    j ").append(labelAdjust).append("\n");

                // 如果不需要调整：将 tempReg2 移动到结果寄存器
                asm.append(labelDone).append(":\n");
                asm.append("    mv ").append(resultReg).append(", ").append(resultReg).append("\n");

                asm.append(labelAdjust).append(":\n");

                if (currentDestTempReg != null) {
                    MIRVirtualReg vreg = currentDestOperand;
                    PhysicalRegister tempReg = currentDestTempReg;
                    storeSpilledDestOperand(vreg, tempReg);
                }
                return true;
            }
        }
        return false; // 未优化
    }


    // 通用乘法分解算法
    private boolean decomposeMultiplication(String operand, String result, long constant, boolean isNegative) {
        // 找到最接近的2的幂次
        int closestShift = 63 - Long.numberOfLeadingZeros(constant);
        System.err.println(closestShift);
        long closestPower = 1L << closestShift;
        long diff = constant - closestPower;

        // 生成最接近的2的幂次乘法
        asm.append("    slliw ").append("t2").append(", ")
           .append(operand).append(", ").append(closestShift).append("\n");

        // 处理差值
        if (diff == 0) {
            // 正好是2的幂次（理论上不会发生，因为前面已处理）
            throw new IllegalArgumentException("diff is power of 2");
        } else if (isPowerOfTwo(Math.abs(diff))) {
            // 差值是2的幂次
            int diffShift = Long.numberOfTrailingZeros(Math.abs(diff));
            System.err.println(diffShift);
            if (diff > 0) {
                asm.append("    slliw ").append(result).append(", ")
                   .append(operand).append(", ").append(diffShift).append("\n");
                asm.append("    addw ").append(result).append(", ").append(result)
                   .append(", ").append("t2").append("\n");
            } else {
                asm.append("    slliw ").append(result).append(", ")
                   .append(operand).append(", ").append(diffShift).append("\n");
                asm.append("    subw ").append(result).append(", ").append("t2")
                   .append(", ").append(result).append("\n");
            }

            if (currentDestTempReg != null) {
                MIRVirtualReg vreg = currentDestOperand;
                PhysicalRegister tempReg = currentDestTempReg;
                storeSpilledDestOperand(vreg, tempReg);
            }
        } else if (diff == 1 || diff == -1) {
            // 差值为±1
            if (diff > 0) {
                asm.append("    addiw ").append(result).append(", ").append("t2")
                   .append(", 1").append("\n");
            } else {
                asm.append("    addiw ").append(result).append(", ").append("t2")
                   .append(", -1").append("\n");
            }
            if (currentDestTempReg != null) {
                MIRVirtualReg vreg = currentDestOperand;
                PhysicalRegister tempReg = currentDestTempReg;
                storeSpilledDestOperand(vreg, tempReg);
            }
        } else {
            return false;
        }

        // 处理符号
        if (isNegative) {
            asm.append("    negw ").append(result).append(", ").append(result).append("\n");
        }
        return true;
    }

    // 2的幂次检查
    private boolean isPowerOfTwo(long n) {
        if (n <= 0) return false;
        // 处理32位整数范围
        return (n & (n - 1)) == 0;
    }

    private String getArithOp(MIRArithOp.Op op, MIRArithOp.Type type) {
        if (type == MIRArithOp.Type.FLOAT) {
            switch (op) {
                case ADD: return "fadd.s";
                case SUB: return "fsub.s";
                case MUL: return "fmul.s";
                case DIV: return "fdiv.s";
                default: throw new IllegalArgumentException("Unsupported float op: " + op);
            }
        } else if (type == MIRArithOp.Type.INT) {
            switch (op) {
                case ADD: return "addw";
                case SUB: return "subw";
                case MUL: return "mulw";
                case DIV: return "divw";
                case REM: return "remw";
                case XOR: return "xor";
                default: throw new IllegalArgumentException("Unsupported int op: " + op);
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
            MIROperand result = op.getResult() != null ? op.getResult() : op.getResultPhysicalReg();
            MIRMemoryOp.Type type = op.getType();

            String loadOp = null;
            if(type == MIRMemoryOp.Type.FLOAT){
                loadOp = "flw";
            } else if(type == MIRMemoryOp.Type.INTEGER){
                loadOp = "lw";
            } else {
                loadOp = "ld";
            }


            String base = getOperandAsm(mem.getBase(), false);

            long offset = ((MIRImmediate)(mem.getOffset())).getValue();

            if( offset >= 2048){
                asm.append("    li t2, ").append(offset).append("\n");
                asm.append("    add ").append("t2").append(", sp, t2").append("\n");
                asm.append("    ").append(loadOp).append(" ")
                        .append(getOperandAsm(result, true)).append(", 0(t2)\n");
            } else {
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
            }

        } else { // STORE
            MIRMemory mem = (MIRMemory) op.getOperands().get(0);
            MIROperand value = op.getOperands().get(1);

            String val = getOperandAsm(value, false);
            String base = getOperandAsm(mem.getBase(), false);

            MIRMemoryOp.Type type = op.getType();

            String storeOp = null;
            if(type == MIRMemoryOp.Type.FLOAT){
                storeOp = "fsw";
            } else if(type == MIRMemoryOp.Type.INTEGER){
                storeOp = "sw";
            } else {
                storeOp = "sd";
            }

            long offset = ((MIRImmediate)(mem.getOffset())).getValue();

            if( offset >= 2048){
                asm.append("    li t2, ").append(offset).append("\n");
                asm.append("    add ").append("t2").append(", sp, t2").append("\n");
                asm.append("    ").append(storeOp).append(" ")
                        .append(val).append(", 0(t2)\n");
            } else {
                asm.append("    ").append(storeOp).append(" ")
                        .append(val).append(", ")
                        .append(mem.getOffset()).append("(")
                        .append(base).append(")\n");
            }
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
                saveCallerSavedRegisters(op.getInstName());
                break;
            case CALLER_RESTORE_REG:
                restoreCallerSavedRegisters(op.getInstName());
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

    private void saveCallerSavedRegisters(MIRLabel name) {
        asm.append("    # Save caller-saved registers\n");
        // 8个字节对齐
        if(stackManager.getFrameSize() % 8 != 0){
            int size = (stackManager.getFrameSize() | 7) + 1;
            int sub = size - stackManager.getFrameSize();
            stackManager.setFrameSize(size);
            asm.append("    addi sp, sp, -").append(sub).append("\n");
        }
        int stackSize = allocator.getUsedCallerSaved().size() * 8;
//        int stackSize = allocator.getCallLiveRegisters(name).size() * 8;
        asm.append("    addi ").append("sp, sp, ").append(-stackSize).append("\n");
        int offset = stackSize - 8;
//        for (PhysicalRegister reg : allocator.getUsedCallerSaved()) {
//            if(reg.name().startsWith("F")){
//                // 说明是float的
//                asm.append("    fsw ").append(reg).append(", ").append(offset).append("(sp)\n");
//            } else {
//                asm.append("    sd ").append(reg).append(", ").append(offset).append("(sp)\n");
//            }
//            offset -= 8;
//        }
        for (PhysicalRegister reg : allocator.getCallLiveRegisters(name)) {
            if(reg.name().startsWith("F")){
                // 说明是float的
                asm.append("    fsw ").append(reg).append(", ").append(offset).append("(sp)\n");
            } else {
                asm.append("    sd ").append(reg).append(", ").append(offset).append("(sp)\n");
            }
            offset -= 8;
        }

    }

    private void restoreCallerSavedRegisters(MIRLabel name) {
        asm.append("    # Restore caller-saved registers\n");
        int stackSize = allocator.getUsedCallerSaved().size() * 8;
//        int stackSize = allocator.getCallLiveRegisters(name).size() * 8;
        int offset = stackSize - 8;
//        for (PhysicalRegister reg : allocator.getUsedCallerSaved()) {
//
//            if(reg.name().startsWith("F")){
//                // 说明是float的
//                asm.append("    flw ").append(reg).append(", ").append(offset).append("(sp)\n");
//            } else {
//                asm.append("    ld ").append(reg).append(", ").append(offset).append("(sp)\n");
//            }
//            offset -= 8;
//        }
        for (PhysicalRegister reg : allocator.getCallLiveRegisters(name)) {
            if(reg.name().startsWith("F")){
                // 说明是float的
                asm.append("    flw ").append(reg).append(", ").append(offset).append("(sp)\n");
            } else {
                asm.append("    ld ").append(reg).append(", ").append(offset).append("(sp)\n");
            }
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
//            System.err.println(spillOffset);
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
                if(spillOffset >= 2048 || spillOffset < -2048){
                    asm.append("    li t2, ").append(spillOffset).append("\n");
                    asm.append("    add ").append("t2").append(", s0, t2").append("\n");
                    asm.append("    ").append(loadOp).append(" ")
                            .append(tempReg).append(", 0(t2)\n");
                } else {

                    asm.append("    ").append(loadOp).append(" ")
                            .append(tempReg).append(", ")
                            .append(spillOffset).append("(s0)\n");
                }

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
            String loadOp = null;
            if(MIRType.isFloat(type)){
                loadOp = "flw";
            } else if (MIRType.isInt(type)) {
                loadOp = "lw";
            } else {
                loadOp = "ld";
            }

            long offset = ((MIRImmediate)(mem.getOffset())).getValue();
            if(offset >= 2048 || offset < -2048){
                asm.append("    li t2, ").append(offset).append("\n");
                asm.append("    add ").append("t2").append(", s0, t2").append("\n");
                asm.append("    ").append(loadOp).append(" ")
                        .append(tempReg).append(", 0(t2)\n");
            } else {

                asm.append("    ").append(loadOp).append(" ")
                        .append(tempReg).append(", ")
                        .append(offset).append("(s0)\n");
            }

            return tempReg.toString();
        } else if (operand instanceof MIRPhysicalReg) {
            return operand.toString();
        }

        return operand.toString();
    }

    private void storeSpilledDestOperand(MIRVirtualReg vreg, PhysicalRegister tempReg) {
        Integer spillOffset = stackManager.getSpillOffset(vreg);
        boolean isFloat = MIRType.isFloat(vreg.getType());
        String storeOp = isFloat ? "fsd" : "sd";

        if(spillOffset >= 2048 || spillOffset < -2048){
            asm.append("    li t2, ").append(spillOffset).append("\n");
            asm.append("    add ").append("t2").append(", s0, t2").append("\n");
            asm.append("    ").append(storeOp).append(" ")
                    .append(tempReg).append(", 0(t2)\n");
        } else {
            asm.append("    ").append(storeOp).append(" ")
                    .append(tempReg).append(", ")
                    .append(spillOffset).append("(s0)\n");
        }
    }
}


