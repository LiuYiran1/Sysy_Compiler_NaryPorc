package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

public abstract class Constant extends User {
    public Constant(Type type) {
        super(type, null);  // 常量名字为其值
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
