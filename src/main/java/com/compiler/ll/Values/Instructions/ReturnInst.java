// ReturnInst.java
package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

public class ReturnInst extends TerminatorInst {
    public ReturnInst(Value retVal, BasicBlock block) {
        super(retVal.getType(), "", Opcode.RET, block);
        addOperand(retVal);
    }

    @Override
    public String toIR() {
        Value op = operands.get(0);
        String opStr = getOpStr(op);
        return "ret " + operands.get(0).getType().toIR() + " " + opStr;
    }

    @Override
    public Instruction clone() {
        throw new RuntimeException("Not implemented");
    }
}