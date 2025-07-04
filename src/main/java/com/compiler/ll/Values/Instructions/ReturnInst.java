// ReturnInst.java
package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class ReturnInst extends Instruction {
    public ReturnInst(Value retVal) {
        super(retVal.getType(), "", Opcode.RET);
        addOperand(retVal);
    }

    @Override
    public String toIR() {
        return "ret " + operands.get(0).getType().toIR() + " " + operands.get(0).getName();
    }
}