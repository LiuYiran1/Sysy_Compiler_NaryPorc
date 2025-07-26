package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

public class Argument extends User {
    public Argument(Type type, String name) {
        super(type, name);
    }

    @Override
    public String toIR() {
        return type.toIR() + " %" + name;
    }
}
