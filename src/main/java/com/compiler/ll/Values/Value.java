package com.compiler.ll.Values;

import com.compiler.ll.Types.Type;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Value {
    protected final Type type;
    protected String name;
    private final Set<User> users = new LinkedHashSet<>();

    public Value(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    public Set<User> getUsers() {
        return users;
    }


    public boolean isConstant() {
        return false;
    }

    public boolean isGlobalVariable() {
        return false;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String toIR();

    // 便于调试和符号表使用
//    @Override
//    public boolean equals(Object obj) {
//        if (!(obj instanceof Value)) return false;
//        Value other = (Value) obj;
//        return name != null && name.equals(other.name);
//    }
//
//    @Override
//    public int hashCode() {
//        return name == null ? 0 : name.hashCode();
//    }
}
