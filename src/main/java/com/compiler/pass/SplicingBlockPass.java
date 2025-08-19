package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Instructions.BranchInst;
import com.compiler.ll.Values.Instructions.CondBranchInst;
import com.compiler.ll.Values.Instructions.Opcode;
import com.compiler.ll.Values.Instructions.PhiInst;
import com.compiler.ll.Values.Value;

import java.util.ArrayList;
import java.util.List;

public class SplicingBlockPass implements Pass {

    boolean hasChanged = false;

    @Override
    public boolean run(Module module) {
        hasChanged = false;
        for(Function function : module.getFunctionDefs()) {
            rewriteTerminator(function);
            splicingBlocks(function);
        }
        return hasChanged;
    }

    private void splicingBlocks(Function function) {
        List<BasicBlock> blocks = new ArrayList<>(function.getBasicBlocks());
        List<BasicBlock> deletedBlocks = new ArrayList<>();

        for (BasicBlock bb : blocks) {

            if (deletedBlocks.contains(bb)) continue;

            Instruction term = bb.getTerminator();

            if (!(term.getOpcode() == Opcode.BR)) continue;

            BranchInst br = (BranchInst) term;
            BasicBlock target = br.getTarget();

            // 必须保证 target 只有一个前驱（就是 bb）
            if (target.getPredecessors().size() != 1) continue;

            // 1. 删除 bb 的 terminator
            bb.removeInstruction(br);

            // 2. 处理 target 的 PHI
            for (PhiInst phi : new ArrayList<>(target.getAllPhiInsts())) {
                if(!target.getSuccessors().isEmpty()){
                    phi.removeIncomingFrom(target.getSuccessors().get(0));
                }
            }

            // 3. 把 target 的指令（除了 phi）移动到 bb
            for (Instruction inst : new ArrayList<>(target.getInstructions())) {
                // if (inst.getOpcode() == Opcode.PHI) continue;
                inst.setBlock(bb);
                bb.addInstruction(inst);
            }

            target.getInstructions().clear();

            // 4. 更新 CFG：bb 接管 target 的 successors
            for (BasicBlock succ : new ArrayList<>(target.getSuccessors())) {
                bb.addSuccessor(succ);
                succ.replacePredecessor(target, bb);
            }
            bb.removeSuccessor(target);

            // 5. 删除 target 块
            function.removeBasicBlock(target);
            deletedBlocks.add(target);
            hasChanged = true;
        }
    }



    private void rewriteTerminator(Function function) {
        for (BasicBlock bb : function.getBasicBlocks()) {
            Instruction terminator = bb.getTerminator();
            if (!(terminator.getOpcode() == Opcode.CBR)) continue;

            CondBranchInst br = (CondBranchInst) terminator;
            Value cond = br.getOperand(0);

            if (cond.isConstant() && cond.getType().isIntegerType()) {
                BasicBlock target;
                if (((Constant)cond).isZero()) {
                    target = br.getFalseBlock();
                    // 移除 trueTarget 前驱关系
                    br.getTrueBlock().removePredecessor(bb);
                    bb.removeSuccessor(br.getTrueBlock());
                } else {
                    target = br.getTrueBlock();
                    // 移除 falseTarget 前驱关系
                    br.getFalseBlock().removePredecessor(bb);
                    bb.removeSuccessor(br.getFalseBlock());

                }

                // 用新的无条件跳转替换
                BranchInst newBr = new BranchInst(target, bb);
                bb.setTerminator(newBr);
                hasChanged = true;
            }
        }
    }
}
