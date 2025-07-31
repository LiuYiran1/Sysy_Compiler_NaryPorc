package com.compiler.ll.Values.GlobalValues;

import com.compiler.ll.Context;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.GlobalValue;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.utils.ConstantZero;

public class GlobalVariable extends GlobalValue {
    private Constant initializer;
    private Type valueType;

    public GlobalVariable(Context context, Type type, String name, Constant initializer) {
        super(context.getPointerType(type), name);
        this.initializer = initializer;
        this.valueType = type;
    }

    public boolean isGlobalVariable() {
        return true;
    }

    public Constant getInitializer() {
        return initializer;
    }

    public void setInitializer(Constant initializer) {
        this.initializer = initializer;
    }

    public Type getValueType() {
        return valueType;
    }

    @Override
    public String toIR() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(name).append(" = global ").append(valueType.toIR());

        // 自动默认初始化为 0
        if (initializer == null) {
            initializer = ConstantZero.get(valueType);
        }

        sb.append(" ").append(initializer.toIR());
        return sb.toString();
    }
}
