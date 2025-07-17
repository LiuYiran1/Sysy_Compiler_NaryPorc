package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

import java.util.stream.Collectors;

public class CallInst extends Instruction {
    private final String calleeName;

    public CallInst(Type returnType, String name, String calleeName, Value... args) {
        super(returnType, name, Opcode.CALL);
        this.calleeName = calleeName;
        for (Value arg : args) {
            addOperand(arg);
        }
    }

    @Override
    public String toIR() {
        String argsStr = operands.stream()
                .map(op -> op.getType().toIR() + " " + getOpStr(op))
                .collect(Collectors.joining(", "));
        return name == null ? "call " + type.toIR() + " @" + calleeName + "(" + argsStr + ")"
                              : "%" + name + " = call " + type.toIR() + " @" + calleeName + "(" + argsStr + ")";
    }
}