package com.compiler.ll.utils;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Values.Constants.ConstantArray;

public class ConstantAggregateZero extends ConstantArray {
    public ConstantAggregateZero(Type type) {
        super(type);
    }

    @Override
    public boolean isZero() {
        return true;
    }

    @Override
    public String toIR() {
        return "zeroinitializer";
    }
}
