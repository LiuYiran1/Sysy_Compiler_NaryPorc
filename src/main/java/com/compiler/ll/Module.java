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

    public void addFunction(Function func) {
        functions.add(func);
        this.builder.setCurFunc(func);
    }

    public Function addFunction(String name, FunctionType funcType) {
        Function function = new Function(funcType, name);
        functions.add(function);
        this.builder.setCurFunc(function);
        return function;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public GlobalVariable addGlobalVariable(String varName, Type type) {
        GlobalVariable gv = new GlobalVariable(type, varName, null);
        globalVariables.add(gv);
        return gv;
    }

    public String toIR() {
        StringBuilder sb = new StringBuilder();
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