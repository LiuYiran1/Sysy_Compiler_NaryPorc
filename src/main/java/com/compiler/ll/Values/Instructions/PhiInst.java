package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

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

    public List<BasicBlock> getIncomingBlocks() {
        return incomingBlocks;
    }

    public BasicBlock getIncomingBlock(int index) {
        return incomingBlocks.get(index);
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
                operands.remove(i);  // 同时移除对应的 value operand
            } else {
                i++;
            }
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
}