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

    public void removeOperand(Value operand) {
        if (operand == null) return;

        // 遍历 operands 列表，找到所有匹配的并删除
        for (int i = operands.size() - 1; i >= 0; i--) {
            if (operands.get(i) == operand) {
                operands.remove(i);
                operand.removeUser(this); // 解除使用关系
            }
        }
    }

    public void removeOperand(int index) {
        if (index < 0 || index >= operands.size()) return;

        Value operand = operands.remove(index); // 从列表中移除
        if (operand != null) {
            operand.removeUser(this); // 同步解除使用关系
        }
    }


}
