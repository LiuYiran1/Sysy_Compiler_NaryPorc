package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

public abstract class Constant extends User {
    public Constant(Type type) {
        super(type, null);
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
