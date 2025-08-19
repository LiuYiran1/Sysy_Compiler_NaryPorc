package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Instructions.Opcode;
import com.compiler.ll.Values.Value;

import java.util.ArrayList;
import java.util.List;

public class LoopInvariantPass implements Pass {
    private LoopAnalPass loopAnal;

    public LoopInvariantPass(LoopAnalPass loopAnal) {
        this.loopAnal = loopAnal;
    }

    @Override
    public boolean run(Module mod) {
        for (Function func : mod.getFunctionDefs()) {
            List<Loop> loops = loopAnal.findLoops(func);
            // 内层循环优先处理
            loops.sort((l1, l2) -> l2.bodyBlocks.size() - l1.bodyBlocks.size());

            for (Loop loop : loops) {
                System.out.println("Loop header: " + loop.header.getName());
                System.out.println("Loop body blocks: ");
                for (BasicBlock bb : loop.bodyBlocks) {
                    System.out.print(bb.getName() + " ");
                }
                System.out.println("\nLoop preHeader: " + loop.preHeader.getName());
                System.out.println("\n---");
            }

            for (Loop loop : loops) {
                hoistLoopInvariants(loop);
            }
        }
        return true;
    }

    private void hoistLoopInvariants(Loop loop) {
        boolean changed;
        do {
            changed = false;
            for (BasicBlock bb : loop.bodyBlocks) {
                // 跳过 header，header 上的 phi/branch 不处理
                if (bb == loop.header) continue;

                List<Instruction> insts = new ArrayList<>(bb.getInstructions());
                for (Instruction inst : insts) {
                    if (isLoopInvariant(inst, loop)) {
                        // 移动到 preheader
                        moveToPreheader(inst, loop.preHeader);
                        changed = true;
                    }
                }
            }
        } while (changed); // 迭代，直到没有新的指令可外提
    }

    private boolean isLoopInvariant(Instruction inst, Loop loop) {
        // 1. 有副作用的指令直接拒绝，终结也拒绝
        //if (inst.isTerminator()) return false;
        if (!isPure(inst)) return false;

        // 2. 所有操作数必须在循环外，或是不变量
        for (Value op : inst.getOperands()) {
            if (op instanceof Instruction) {
                Instruction def = (Instruction) op;
                if (loop.contains(def.getParent())) {
                    return false; // 定义在循环内部 -> 不是不变量
                }
            }
        }

        return true;
    }

    private void moveToPreheader(Instruction inst, BasicBlock preHeader) {
        if (inst.isTerminator()) {
            System.out.println(inst.toIR() + " " + inst.getOpcode().name());
            throw new RuntimeException("Cannot move terminator to preheader!");
        }
        System.err.println(inst.toIR() + " " + inst.getParent().getName() + " to " + preHeader.getName());
        inst.getParent().moveInstruction(inst, preHeader);
        //throw new RuntimeException("有用");
    }


    private boolean isPure(Instruction inst) {
        // 只能移动没有 副作用 的指令
        return switch (inst.getOpcode()) { // 这里不处理zext指令，这与后端特性适配
            case FPTOSI, SITOFP, ICMP, FCMP,
                 MUL, SDIV, SREM, ADD, SUB,
                 FMUL, FDIV, FREM, FADD, FSUB,
                 BC-> true;
            default -> false;
        };
    }


}

