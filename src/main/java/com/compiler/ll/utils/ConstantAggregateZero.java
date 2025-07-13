package com.compiler.ll.utils;

import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Constant;

public class ConstantAggregateZero extends Constant {
    public ConstantAggregateZero(Type type) {
        super(type);
    }

    @Override
    public String toIR() {
        return "zeroinitializer";
    }
}
