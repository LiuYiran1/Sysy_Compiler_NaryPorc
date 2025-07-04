package com.compiler.ll.Types;

import com.compiler.ll.Context;

public abstract class Type {
    protected final Context context;
    protected final TypeID typeID;

    public Type(Context context, TypeID typeID) {
        this.context = context;
        this.typeID = typeID;
    }

    public TypeID getTypeID() {
        return typeID;
    }

    public Context getContext() {
        return context;
    }

    public boolean isIntegerType() {
        return typeID == TypeID.INTEGER;
    }

    public boolean isPointerType() {
        return typeID == TypeID.POINTER;
    }

    public boolean isVoidType() {
        return typeID == TypeID.VOID;
    }

    public boolean isFloatType() {
        return typeID == TypeID.FLOAT;
    }

    public boolean isArrayType() {
        return typeID == TypeID.ARRAY;
    }

    public abstract String toIR(); // 输出如 i32、float、i32* 等
}
