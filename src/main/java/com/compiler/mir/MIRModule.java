package com.compiler.mir;

import com.compiler.mir.operand.MIRGlobalVariable;

import java.util.*;

public class MIRModule {
    private final List<MIRFunction> functions = new ArrayList<>();
    private final List<MIRGlobalVariable> globalVariables = new ArrayList<>();
    private final Map<String, MIRFunction> functionMap = new LinkedHashMap<>();
    private final Map<String, MIRGlobalVariable> globalVariableMap = new LinkedHashMap<>();

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
}