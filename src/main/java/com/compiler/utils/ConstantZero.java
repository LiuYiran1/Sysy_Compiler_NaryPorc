package com.compiler.utils;

import com.compiler.ll.Types.FloatType;
import com.compiler.ll.Types.IntegerType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Types.TypeID;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;


public class ConstantZero {
    public static Constant get(Type type) {
        switch (type.getTypeID()) {
            case INTEGER:
                return new ConstantInt((IntegerType) type, 0);
            case FLOAT:
                return new ConstantFloat((FloatType) type, 0.0f);
            case ARRAY:
                return new ConstantAggregateZero(type); // e.g. [10 x i32] zeroinitializer
            default:
                throw new IllegalArgumentException("Unsupported type for default zero initializer: " + type);
        }
    }
}
