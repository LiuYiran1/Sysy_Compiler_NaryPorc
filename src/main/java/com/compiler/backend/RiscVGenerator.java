//package com.compiler.backend;
//
//
//import com.compiler.backend.allocator.LinearScanRegisterAllocator;
//import com.compiler.mir.MIRBasicBlock;
//import com.compiler.mir.MIRFunction;
//import com.compiler.mir.MIRModule;
//import com.compiler.mir.instruction.*;
//import com.compiler.mir.operand.MIRGlobalVariable;
//import com.compiler.mir.operand.MIRImmediate;
//import com.compiler.mir.operand.MIROperand;
//import com.compiler.mir.operand.MIRVirtualReg;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//public class RiscVGenerator {
//
//    private final MIRModule mirModule;
//    private final StringBuilder asm = new StringBuilder();
//    private final Map<MIRFunction, LinearScanRegisterAllocator> allocators = new LinkedHashMap<>();
//
//    public RiscVGenerator(MIRModule mirModule) {
//        this.mirModule = mirModule;
//    }
//
//    public String generate() {
//        generateDataSection();
//        generateTextSection();
//        return asm.toString();
//    }
//
//    private void generateDataSection() {
//        asm.append(".data\n");
//        for (MIRGlobalVariable global : mirModule.getGlobalVariables()) {
//            asm.append(global.getName()).append(": ");
//            if (global.getInitializerType().equals(MIRGlobalVariable.InitializerType.ZERO_INITIALIZED)) {
//                asm.append(".zero ").append(global.getSize() * 4);
//            } else if (global.getType() == MIRType.F32) {
//                asm.append(".float ").append(global.getFloatValue());
//            } else if (global.getType() == MIRType.I32) {
//                asm.append(".word ").append(global.getIntValue());
//            } else if (global.getType() == MIRType.ARRAY) {
//                asm.append(".word ");
//                for (Object value : global.getArrayValues()) {
//                    if (value instanceof Integer) {
//                        asm.append(value).append(", ");
//                    } else if (value instanceof Float) {
//                        asm.append(Float.floatToRawIntBits((Float) value)).append(", ");
//                    }
//                }
//                asm.setLength(asm.length() - 2); // 移除最后的逗号
//            }
//            asm.append("\n");
//        }
//    }
//
//    private void generateTextSection() {
//        asm.append("\n.text\n");
//        for (MIRFunction function : mirModule.getFunctions()) {
//            generateFunction(function);
//        }
//    }
//
//    private void generateFunction(MIRFunction function) {
//        LinearScanRegisterAllocator allocator = new LinearScanRegisterAllocator(function);
//        allocator.allocate();
//        allocators.put(function, allocator);
//
//        asm.append(function.getName()).append(":\n");
//        generatePrologue(function, allocator);
//
//        for (MIRBasicBlock block : function.getBlocks()) {
//            asm.append(block.getLabel()).append(":\n");
//            for (MIRInstruction inst : block.getInstructions()) {
//                asm.append(generateInstruction(inst, function, allocator));
//            }
//        }
//
//        asm.append("\n");
//    }
//
//    private void generatePrologue(MIRFunction function, LinearScanRegisterAllocator allocator) {
//        int frameSize = allocator.getFrameSize();
//
//        // 函数序言
//        asm.append("  addi sp, sp, -").append(frameSize).append("\n");
//        asm.append("  sw ra, ").append(frameSize - 4).append("(sp)\n");
//        asm.append("  sw s0, ").append(frameSize - 8).append("(sp)\n");
//        asm.append("  addi s0, sp, ").append(frameSize).append("\n");
//
//        // 被调用者保存寄存器
//        for (PhysicalRegister reg : PhysicalRegister.CALLEE_SAVED_INT) {
//            asm.append("  sw ").append(reg).append(", ")
//                    .append(allocator.getSpillOffset(null)).append("(s0)\n");
//        }
//    }
//
//    private String generateInstruction(MIRInstruction inst, MIRFunction function,
//                                       LinearScanRegisterAllocator allocator) {
//        if (inst instanceof MIRPseudoOp) {
//            return generatePseudoOp((MIRPseudoOp) inst, function, allocator);
//        } else if (inst instanceof MIRArithOp) {
//            return generateArithOp((MIRArithOp) inst, allocator);
//        } else if (inst instanceof MIRControlFlowOp) {
//            return generateControlFlowOp((MIRControlFlowOp) inst);
//        } else if (inst instanceof MIRMemoryOp) {
//            return generateMemoryOp((MIRMemoryOp) inst, allocator);
//        }
//        return "";
//    }
//
//    private String generatePseudoOp(MIRPseudoOp op, MIRFunction function,
//                                    LinearScanRegisterAllocator allocator) {
//        switch (op.getType()) {
//            case PROLOGUE:
//                return "";
//            case EPILOGUE:
//                return generateEpilogue(function, allocator);
//            case CALLEE_SAVE_REG:
//                return generateCalleeSave(allocator);
//            case CALLEE_RESTORE_REG:
//                return generateCalleeRestore(allocator);
//            default:
//                return "";
//        }
//    }
//
//    private String generateEpilogue(MIRFunction function, LinearScanRegisterAllocator allocator) {
//        StringBuilder sb = new StringBuilder();
//        int frameSize = allocator.getFrameSize();
//
//        // 恢复被调用者保存寄存器
//        for (PhysicalRegister reg : PhysicalRegister.CALLEE_SAVED_INT) {
//            sb.append("  lw ").append(reg).append(", ")
//                    .append(allocator.getSpillOffset(null)).append("(s0)\n");
//        }
//
//        // 函数收尾
//        sb.append("  lw ra, ").append(frameSize - 4).append("(sp)\n");
//        sb.append("  lw s0, ").append(frameSize - 8).append("(sp)\n");
//        sb.append("  addi sp, sp, ").append(frameSize).append("\n");
//        sb.append("  ret\n");
//
//        return sb.toString();
//    }
//
//    private String generateArithOp(MIRArithOp op, LinearScanRegisterAllocator allocator) {
//        PhysicalRegister dest = allocator.getPhysicalReg(op.getDest());
//        MIROperand src1 = op.getSrc1();
//        MIROperand src2 = op.getSrc2();
//
//        String opcode = "";
//        switch (op.getOp()) {
//            case ADD: opcode = "add"; break;
//            case SUB: opcode = "sub"; break;
//            case MUL: opcode = "mul"; break;
//            case DIV: opcode = "div"; break;
//        }
//
//        if (src2 instanceof MIRImmediate) {
//            return String.format("  %si %s, %s, %d\n",
//                    opcode, dest, getOperandAsm(src1, allocator),
//                    ((MIRImmediate) src2).getValue());
//        } else {
//            return String.format("  %s %s, %s, %s\n",
//                    opcode, dest, getOperandAsm(src1, allocator),
//                    getOperandAsm(src2, allocator));
//        }
//    }
//
//    private String getOperandAsm(MIROperand operand, LinearScanRegisterAllocator allocator) {
//        if (operand instanceof MIRVirtualReg) {
//            return allocator.getPhysicalReg((MIRVirtualReg) operand).toString();
//        } else if (operand instanceof MIRImmediate) {
//            return "#" + ((MIRImmediate) operand).getValue();
//        }
//        return "";
//    }
//}