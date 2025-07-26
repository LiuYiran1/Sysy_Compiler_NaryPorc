package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

public abstract class User extends Value {
    public User(Type type, String name) {
        super(type, name);
    }
}
