package com.compiler.ll.Types;

import com.compiler.ll.Context;

import java.util.Objects;

public class ArrayType extends Type {
    private final Type elementType;
    private final int numElements;

    public ArrayType(Context context, Type elementType, int numElements) {
        super(context, TypeID.ARRAY);
        this.elementType = elementType;
        this.numElements = numElements;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getNumElements() {
        return numElements;
    }

    @Override
    public String toIR() {
        return "[" + numElements + " x " + elementType.toIR() + "]";
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArrayType)) return false;
        ArrayType other = (ArrayType) o;
        return this.numElements == other.numElements &&
                Objects.equals(this.elementType, other.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(TypeID.ARRAY, elementType, numElements);
    }
}
