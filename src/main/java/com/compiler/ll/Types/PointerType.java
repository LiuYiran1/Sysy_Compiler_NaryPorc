package com.compiler.ll.Types;

import com.compiler.ll.Context;

import java.util.Objects;

public class PointerType extends Type {
    private final Type pointeeType;

    public PointerType(Context context, Type pointeeType) {
        super(context, TypeID.POINTER);
        this.pointeeType = pointeeType;
    }

    public Type getPointeeType() {
        return pointeeType;
    }

    @Override
    public String toIR() {
        return pointeeType.toIR() + "*";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PointerType)) return false;
        PointerType other = (PointerType) o;
        return Objects.equals(this.pointeeType, other.pointeeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(TypeID.POINTER, pointeeType);
    }
}
