package com.compiler.ll;

import com.compiler.ll.Types.FunctionType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.GlobalValues.GlobalVariable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Module {
    private Context context;
    private IRBuilder builder;
    private final String name;
    private final List<Function> functions = new ArrayList<>();
    private final List<GlobalVariable> globalVariables = new ArrayList<>();

    public Module(String name, Context context, IRBuilder irBuilder) {
        this.name = name;
        this.context = context;
        this.builder = irBuilder;
    }

    public String getName() {
        return name;
    }

    public Function addFunction(String name, FunctionType funcType) {
        Function function = new Function(funcType, name, this);
        functions.add(function);
        this.builder.setCurFunc(function);
        return function;
    }

    public Function getFirstFunction() {
        return functions.isEmpty() ? null : functions.get(0);
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public List<Function> getFunctionDefs(){
        List<Function> functionDefs = new LinkedList<>();
        for (Function function : functions) {
            if (function.isDef())
                functionDefs.add(function);
        }
        return functionDefs;
    }

    public GlobalVariable addGlobalVariable(String varName, Type type) {
        GlobalVariable gv = new GlobalVariable(context, type, varName, null);
        globalVariables.add(gv);
        return gv;
    }

    public String toIR() {
        StringBuilder sb = new StringBuilder();
        // 前缀
        sb.append("; ModuleID = '" + name + "'").append("\n").append("source_filename = \"" + name + "\"").append("\n\n");
        // 全局变量定义
        for (GlobalVariable gv : globalVariables) {
            sb.append(gv.toIR()).append("\n");
        }

        if (!globalVariables.isEmpty()) {
            sb.append("\n");
        }
        // 函数定义
        for (Function func : functions) {
            sb.append(func.toIR()).append("\n\n");
        }
        return sb.toString();
    }

    public IRBuilder getBuilder() {
        return builder;
    }

    public void dump(){
        System.out.println(toIR());
    }

    public void dump(File file){
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(toIR());
        } catch (IOException e) {
            System.err.println("写入文件失败: " + file.getAbsolutePath());
        }
    }
}