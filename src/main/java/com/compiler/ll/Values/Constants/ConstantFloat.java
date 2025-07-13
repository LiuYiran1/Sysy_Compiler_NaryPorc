package com.compiler.ll.Values.Constants;

import com.compiler.ll.Types.FloatType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Values.Value;
import com.compiler.ll.exceptions.ConstantException;

public class ConstantFloat extends Constant {
    private final float value;

    public ConstantFloat(FloatType type, float value) {
        super(type);
        this.value = value;
    }

    public ConstantFloat(Value val, Type targetType) {
        super(targetType);
        Type valType = val.getType();
        if (valType.isIntegerType()){
            this.value = (float) ((ConstantInt)val).getValue();
        } else if (valType.isFloatType()){
            this.value = ((ConstantFloat)val).getValue();
        } else {
            throw new ConstantException("ConstantFloat type error");
        }
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
