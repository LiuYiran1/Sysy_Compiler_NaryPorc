package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class User extends Value {
    public User(Type type, String name) {
        super(type, name);
    }

    protected final List<Value> operands = new ArrayList<>();

    public void addOperand(Value operand) {
        operands.add(operand);
        if (operand != null) {
            operand.addUser(this); // 注册使用关系
        }
    }

    public void setOperand(int index, Value newOperand) {
        Value old = operands.get(index);
        old.removeUser(this); // 解除旧的使用
        operands.set(index, newOperand);
        newOperand.addUser(this); // 添加新的使用
    }

    public List<Value> getOperands() {
        return operands;
    }

    public int getNumOperands() {
        return operands.size();
    }

    public Value getOperand(int index) {
        return operands.get(index);
    }

    public void replaceOperand(Value oldValue, Value newValue) {
        for (int i = 0; i < operands.size(); i++) {
            if (operands.get(i) == oldValue) {
                setOperand(i, newValue);
            }
        }
    }
}
