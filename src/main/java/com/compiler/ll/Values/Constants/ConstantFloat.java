package com.compiler.ll.Values.Constants;

import com.compiler.ll.Types.FloatType;
import com.compiler.ll.Values.Constant;

public class ConstantFloat extends Constant {
    private final float value;

    public ConstantFloat(FloatType type, float value) {
        super(type);
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String getName() {
        return "" + value;
    }

    @Override
    public String toIR() {
        return Float.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConstantFloat)) return false;
        ConstantFloat other = (ConstantFloat) obj;
        return this.value == other.value && this.getType().equals(other.getType());
    }

    @Override
    public int hashCode() {
        return Float.hashCode(value) + 31 * getType().hashCode();
    }
}
