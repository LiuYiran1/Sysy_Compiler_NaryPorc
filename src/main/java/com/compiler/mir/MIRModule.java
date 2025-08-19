package com.compiler.mir;

import com.compiler.ll.Values.Value;
import com.compiler.mir.operand.MIRGlobalVariable;
import com.compiler.mir.operand.MIROperand;

import java.util.*;

public class MIRModule {
    private final List<MIRFunction> functions = new ArrayList<>();
    private final List<MIRGlobalVariable> globalVariables = new ArrayList<>();
    private final Map<String, MIRFunction> functionMap = new LinkedHashMap<>();
    private final Map<String, MIRGlobalVariable> globalVariableMap = new LinkedHashMap<>();
    private final Map<Value, MIROperand> valueMap = new LinkedHashMap<>();

    public void addFunction(MIRFunction function) {
        functions.add(function);
        functionMap.put(function.getName(), function);
    }

    public void addGlobalVariable(MIRGlobalVariable globalVariable) {
        globalVariables.add(globalVariable);
        globalVariableMap.put(globalVariable.getName(), globalVariable);
    }

    public List<MIRFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    public List<MIRGlobalVariable> getGlobalVariables() {
        return Collections.unmodifiableList(globalVariables);
    }

    public Map<String, MIRFunction> getFunctionMap() {
        return Collections.unmodifiableMap(functionMap);
    }

    public Map<String, MIRGlobalVariable> getGlobalVariableMap() {
        return Collections.unmodifiableMap(globalVariableMap);
    }

    public Map<Value,MIROperand> getValueMap(){
        return valueMap;
    }

    public Value findKeyByOperand(MIROperand operand) {
        return valueMap.entrySet()
                .stream()
                .filter(entry -> operand.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // 未找到时返回 null
    }
}