package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

public abstract class GlobalValue extends User {
    public GlobalValue(Type type, String name) {
        super(type, name);
    }
}
