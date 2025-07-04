package com.compiler.ll;

import com.compiler.ll.Values.GlobalValues.Function;
import java.util.*;

public class Module {
    private Context context;
    private final String name;
    private final List<Function> functions = new ArrayList<>();

    public Module(String name, Context context) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addFunction(Function func) {
        functions.add(func);
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public String toIR() {
        StringBuilder sb = new StringBuilder();
        for (Function func : functions) {
            sb.append(func.toIR()).append("\n\n");
        }
        return sb.toString();
    }

    public void dump(){
        System.out.println(toIR());
    }
}