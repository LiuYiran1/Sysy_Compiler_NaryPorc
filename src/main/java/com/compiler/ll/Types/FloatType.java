package com.compiler.ll.Types;

import com.compiler.ll.Context;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;

public class FloatType extends Type {
    public FloatType(Context context) {
        super(context, TypeID.FLOAT);
    }

    public ConstantFloat getConstantFloat(float value) {
        return new ConstantFloat(this, value);
    }

    @Override
    public String toIR() {
        return "float";
    }
}
