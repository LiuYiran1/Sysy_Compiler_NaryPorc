package com.compiler.ll.Values.GlobalValues;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.GlobalValue;
import com.compiler.ll.Values.Constant;

public class GlobalVariable extends GlobalValue {
    private Constant initializer;

    public GlobalVariable(Type type, String name, Constant initializer) {
        super(type, name);
        this.initializer = initializer;
    }

    public Constant getInitializer() {
        return initializer;
    }

    @Override
    public String toIR() {
        return "@" + name + " = global " + type.toIR() + " " + initializer.toIR();
    }
}
