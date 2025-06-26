package com.compiler.ir;

import org.llvm4j.llvm4j.Value;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    private final Stack<Map<String, Value>> symbolStack;

    public SymbolTable() {
        symbolStack = new Stack<>();
        symbolStack.push(new LinkedHashMap<>()); // 初始化全局作用域
    }

    public void enterScope() {
        symbolStack.push(new LinkedHashMap<>()); // 进入新的作用域
    }

    public void exitScope() {
        symbolStack.pop(); // 离开当前作用域
    }

    public void addSymbol(String name, Value value) {
        symbolStack.peek().put(name, value);
    }

    public Value getSymbol(String name) {
        for (int i = symbolStack.size() - 1; i >= 0; i--) {
            if (symbolStack.get(i).containsKey(name)) {
                return symbolStack.get(i).get(name);
            }
        }
        return null; // 未找到符号
    }

    public boolean containsSymbol(String name) {
        for (int i = symbolStack.size() - 1; i >= 0; i--) {
            if (symbolStack.get(i).containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBottom(){
        return symbolStack.size() == 1;
    }
}
