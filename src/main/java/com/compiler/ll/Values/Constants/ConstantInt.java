package com.compiler.ll.Values.Constants;

import com.compiler.ll.Values.Constant;
import com.compiler.ll.Types.IntegerType;

public class ConstantInt extends Constant {
    private final int value;

    public ConstantInt(IntegerType type, int value) {
        super(type);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String getName() {
        return "" + value;
    }

    @Override
    public String toIR() {
        return Integer.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConstantInt)) return false;
        ConstantInt other = (ConstantInt) obj;
        return this.value == other.value && this.getType().equals(other.getType());
    }

    @Override
    public int hashCode() {
        return value + 31 * getType().hashCode();
    }
}
