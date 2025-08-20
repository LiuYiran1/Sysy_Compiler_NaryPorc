package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.FloatType;
import com.compiler.ll.Types.IntegerType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;
import com.compiler.ll.utils.NameManager;

import java.util.ArrayList;
import java.util.List;

public class PhiInst extends Instruction {
    private final List<BasicBlock> incomingBlocks = new ArrayList<>();

    private AllocaInst variable; // 用来记录在memToReg中这个phi是对应哪个变量的，为了优化方便

    public PhiInst(Type type, String name, BasicBlock block) {
        super(type, name, Opcode.PHI, block);
    }

    public void addIncoming(BasicBlock block, Value value) {
        addOperand(value);
        incomingBlocks.add(block);
    }

    public void replaceIncomingBlock(BasicBlock oldPred, BasicBlock newPred) {
        for (int i = 0; i < incomingBlocks.size(); i++) {
            if (incomingBlocks.get(i) == oldPred) {
                incomingBlocks.set(i, newPred);
                break; // 假设每个前驱只会出现一次
            }
        }
    }


    public List<BasicBlock> getIncomingBlocks() {
        return incomingBlocks;
    }

    public BasicBlock getIncomingBlock(int index) {
        return incomingBlocks.get(index);
    }

    public Value getIncomingValue(BasicBlock incomingBlock) {
        int index = incomingBlocks.indexOf(incomingBlock);
        return operands.get(index);
    }

    public int getIncomingBlocksSize() {
        return incomingBlocks.size();
    }

    public void setVariable(AllocaInst variable) {
        this.variable = variable;
    }

    public AllocaInst getVariable() {
        return variable;
    }

    public void removeIncomingFrom(BasicBlock block) {
        for (int i = 0; i < incomingBlocks.size(); ) {
            if (incomingBlocks.get(i) == block) {
                incomingBlocks.remove(i);
                removeOperand(i);  // 同时移除对应的 value operand
            } else {
                i++;
            }
        }
        // 如果只有一个incoming，直接降级为add
        if (incomingBlocks.size() == 1){
            Value op = operands.get(0);
            this.block.replaceAllUse(this, op);
        }
    }


    @Override
    public String toIR() {
        StringBuilder sb = new StringBuilder("%" + name + " = phi " + type.toIR() + " ");
        for (int i = 0; i < operands.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("[ " + getOpStr(operands.get(i)) + ", %" + incomingBlocks.get(i).getName() + " ]");
        }
        return sb.toString();
    }

    @Override
    public Instruction clone() {
        throw new RuntimeException("Not implemented");
    }

}