package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Instructions.BranchInst;
import com.compiler.ll.Values.Instructions.CondBranchInst;
import com.compiler.ll.Values.Instructions.Opcode;

import java.util.List;

public class DFGPass implements Pass {

    @Override
    public boolean run(Module module) {
        for (Function function : module.getFunctions()) {
            // 记录所有基本块
            List<BasicBlock> basicBlocks = function.getBasicBlocks();

            for (BasicBlock bb : basicBlocks) {
                Instruction terminator = bb.getTerminator();
                if (terminator.getOpcode() == Opcode.BR) {
                    BranchInst br = (BranchInst) terminator;
                    BasicBlock target = br.getTarget();
                    bb.addSuccessor(target);
                    target.addPredecessor(bb);
                } else if (terminator.getOpcode() == Opcode.CBR) {
                    CondBranchInst cbr = (CondBranchInst) terminator;
                    BasicBlock trueTarget = cbr.getTrueBlock();
                    BasicBlock falseTarget = cbr.getFalseBlock();

                    bb.addSuccessor(trueTarget);
                    bb.addSuccessor(falseTarget);

                    trueTarget.addPredecessor(bb);
                    falseTarget.addPredecessor(bb);
                }
            }
        }
        return true;
    }
}
