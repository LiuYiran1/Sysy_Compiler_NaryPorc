package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.Instructions.Opcode;
import com.compiler.ll.exceptions.StoreException;

import java.util.ArrayList;
import java.util.List;

public abstract class Instruction extends User {
    protected BasicBlock block;
    protected final Opcode opcode;


    public Instruction(Type type, String name, Opcode opcode, BasicBlock block) {
        super(type, name);
        this.opcode = opcode;
        this.block = block;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public abstract String toIR();

    public boolean isTerminator(){
        return false;
    }

    public Instruction getNextInstruction() {
        BasicBlock parent = this.block;
        if (parent == null) return null;

        List<Instruction> list = parent.getInstructions();
        int index = list.indexOf(this);
        if (index == -1 || index + 1 >= list.size()) return null;
        return list.get(index + 1);
    }

    public void eraseFromParent() {
        if (block != null) {
            block.removeInst(this);
            this.block = null; // 解除引用
        }
    }


    protected String getOpStr(Value op) {
        if (op.isConstant()) {
            return op.toIR();
        } else if (op.isGlobalVariable()) {
            return "@" + op.getName();
        } else {
            if (op.getName() == null) {
                System.out.println("op class: " + op.getClass().getSimpleName());
            }
            // return op.getName() == null ? "undef" : "%" + op.getName();
            return op.getName() == null ? "0" : "%" + op.getName();
        }
    }
}
