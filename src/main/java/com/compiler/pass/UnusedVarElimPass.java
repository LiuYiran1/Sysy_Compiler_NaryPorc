package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instruction;

import java.util.ArrayList;
import java.util.List;

public class UnusedVarElimPass implements Pass {

    boolean hasChanged = false;

    @Override
    public boolean run(Module module) {
        hasChanged = false;
        for (Function function : module.getFunctionDefs()) {
            for (BasicBlock bb : function.getBasicBlocks()) {
                List<Instruction> instructions = new ArrayList<>(bb.getInstructions());
                for (Instruction inst : instructions) {
                    if (inst.getUsers().isEmpty() && isPure(inst)) {
                        bb.removeInstruction(inst);
                        hasChanged = true;
                    }
                }
            }
        }
        return hasChanged;
    }

    private boolean isPure(Instruction inst) {
        // 只能删除没有 副作用 的指令
        return switch (inst.getOpcode()) {
            case ZEXT, FPTOSI, SITOFP, ICMP, FCMP,
                 MUL, SDIV, SREM, ADD, SUB,
                 FMUL, FDIV, FREM, FADD, FSUB,
                 BC, PHI, LOAD -> true;
            default -> false;
        };
    }
}

