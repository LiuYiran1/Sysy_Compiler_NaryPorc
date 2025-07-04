package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

public abstract class GlobalValue extends Value {
    public GlobalValue(Type type, String name) {
        super(type, name);
    }
}
