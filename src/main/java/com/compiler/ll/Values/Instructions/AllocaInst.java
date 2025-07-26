package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.PointerType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.exceptions.AllocException;

public class AllocaInst extends Instruction {
    public AllocaInst(Type pointerType, String name, BasicBlock block) {
        super(pointerType, name, Opcode.ALLOCA, block);
    }

    public static AllocaInst getUndef(){
        AllocaInst tem =  new AllocaInst(null,null,null);
        return tem;
    }

    @Override
    public String toIR() {
        if (type.isPointerType()){
            return "%" + name + " = alloca " + ((PointerType)type).getPointeeType().toIR();
        } else {
            throw new AllocException("Cannot allocate " + type + " into " + name);
        }

    }
}