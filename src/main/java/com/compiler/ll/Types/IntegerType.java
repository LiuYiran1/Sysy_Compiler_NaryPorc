package com.compiler.ll.Types;

import com.compiler.ll.Context;

import java.util.Objects;

public class IntegerType extends Type {
    private final int bitWidth; // i1 or i32 or i64

    public IntegerType(Context context, int bitWidth) {
        super(context, TypeID.INTEGER);
        this.bitWidth = bitWidth;
    }

    public int getBitWidth() {
        return bitWidth;
    }

    @Override
    public String toIR() {
        return "i" + bitWidth;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntegerType)) return false;
        IntegerType other = (IntegerType) o;
        return this.bitWidth == other.bitWidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(TypeID.INTEGER, bitWidth);
    }
}
