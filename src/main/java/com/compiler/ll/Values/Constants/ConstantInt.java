package com.compiler.ll.Values.Constants;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Types.IntegerType;
import com.compiler.ll.Values.Value;
import com.compiler.ll.exceptions.ConstantException;

public class ConstantInt extends Constant {
    private final long value;

    public ConstantInt(IntegerType type, long value) {
        super(type);
        this.value = value;
    }

    public ConstantInt(Value val, Type targetType) {
        super(targetType);
        Type valType = val.getType();
        if (valType.isIntegerType()){
            this.value = ((ConstantInt)val).getValue();
        } else if (valType.isFloatType()){
            this.value = (long) ((ConstantFloat)val).getValue();
        } else {
            throw new ConstantException("ConstantInt type error");
        }
    }

    public boolean isZero(){
        return value == 0;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String getName() {
        return "" + value;
    }

    public long getSExtValue() {
        return value;
    }

    public long getZExtValue() {
        IntegerType intType = (IntegerType) getType();
        int bitWidth = intType.getBitWidth();
        return value & ((1L << bitWidth) - 1);
    }


    @Override
    public String toIR() {
        if(((IntegerType)this.type).getBitWidth() == 1){
            return value == 0 ? "false" : "true";
        }
        return Long.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConstantInt)) return false;
        ConstantInt other = (ConstantInt) obj;
        return this.value == other.value && this.getType().equals(other.getType());
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value) + 31 * getType().hashCode();
    }
}
