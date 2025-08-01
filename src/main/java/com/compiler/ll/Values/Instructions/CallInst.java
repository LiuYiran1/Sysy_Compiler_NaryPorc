package com.compiler.ll.Values.Instructions;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CallInst extends Instruction {
    private final String calleeName;

    public CallInst(Type returnType, String name, String calleeName, BasicBlock block, Function function, Value... args) {
        super(returnType, name, Opcode.CALL, block);
        this.calleeName = calleeName;
        for (Value arg : args) {
            addOperand(arg);
        }
        addOperand(function);
    }

    @Override
    public String toIR() {
        List<Value> operandsTem = operands.subList(0, operands.size() - 1);
        String argsStr = operandsTem.stream()
                .map(op -> op.getType().toIR() + " " + getOpStr(op))
                .collect(Collectors.joining(", "));
        return name == null ? "call " + type.toIR() + " @" + calleeName + "(" + argsStr + ")"
                              : "%" + name + " = call " + type.toIR() + " @" + calleeName + "(" + argsStr + ")";
    }
}