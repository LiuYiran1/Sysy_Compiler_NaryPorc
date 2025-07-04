package com.compiler.ll.Types;

import com.compiler.ll.Context;

public class FloatType extends Type {
    public FloatType(Context context) {
        super(context, TypeID.FLOAT);
    }

    @Override
    public String toIR() {
        return "float";
    }
}
