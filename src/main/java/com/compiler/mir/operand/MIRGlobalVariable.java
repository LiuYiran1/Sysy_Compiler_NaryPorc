package com.compiler.mir.operand;

import com.compiler.mir.MIRType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MIRGlobalVariable extends MIROperand {
    public enum InitializerType {
        INT,
        FLOAT,
        ARRAY,
        ZERO_INITIALIZED
    }

    private final String name;
    private final MIRType type;
    private final InitializerType initType;

    // 标量值
    private long intValue;
    private double floatValue;

    // 数组值
    private List<Object> arrayValues;
    private int arraySize;

    // 用于零初始化
    private int zeroSize;

    // 构造函数 - 整数标量
    public MIRGlobalVariable(String name, MIRType type, long value) {
        if (!MIRType.isInt(type)) {
            throw new IllegalArgumentException("Type must be integer for scalar int global");
        }
        this.name = name;
        this.type = type;
        this.intValue = value;
        this.initType = InitializerType.INT;
    }

    // 构造函数 - 浮点数标量
    public MIRGlobalVariable(String name, MIRType type, double value) {
        if (!MIRType.isFloat(type)) {
            throw new IllegalArgumentException("Type must be float for scalar float global");
        }
        this.name = name;
        this.type = type;
        this.floatValue = value;
        this.initType = InitializerType.FLOAT;
    }

    // 构造函数 - 数组
    public MIRGlobalVariable(String name, MIRType elementType, int size, List<Object> values) {
        this.name = name;
        this.type = elementType;
        this.arrayValues = new ArrayList<>(values);
        this.arraySize = size;
        this.initType = InitializerType.ARRAY;

        // 验证值类型
        for (Object value : arrayValues) {
            if (MIRType.isInt(elementType) && !(value instanceof Long || value instanceof Integer)) {
                throw new IllegalArgumentException("Array element type mismatch: expected integer");
            }
            if (MIRType.isFloat(elementType) && !(value instanceof Double || value instanceof Float)) {
                throw new IllegalArgumentException("Array element type mismatch: expected float");
            }
        }
    }

    // 构造函数 - 零初始化
    public MIRGlobalVariable(String name, MIRType type, int size) {
        this.name = name;
        this.type = type;
        this.zeroSize = size;
        this.initType = InitializerType.ZERO_INITIALIZED;
    }

    public String getName() {
        return name;
    }


    public InitializerType getInitializerType() {
        return initType;
    }

    // 获取标量整数值
    public long getIntValue() {
        if (initType != InitializerType.INT) {
            throw new IllegalStateException("Not an integer scalar global");
        }
        return intValue;
    }

    // 获取标量浮点数值
    public double getFloatValue() {
        if (initType != InitializerType.FLOAT) {
            throw new IllegalStateException("Not a float scalar global");
        }
        return floatValue;
    }

    // 获取数组值（不可修改）
    public List<Object> getArrayValues() {
        if (initType != InitializerType.ARRAY) {
            throw new IllegalStateException("Not an array global");
        }
        return Collections.unmodifiableList(arrayValues);
    }

    // 获取数组大小
    public int getArraySize() {
        if (initType != InitializerType.ARRAY) {
            throw new IllegalStateException("Not an array global");
        }
        return arraySize;
    }

    // 获取零初始化大小
    public int getZeroSize() {
        if (initType != InitializerType.ZERO_INITIALIZED) {
            throw new IllegalStateException("Not a zero-initialized global");
        }
        return zeroSize;
    }

    @Override
    public MIRType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "@" + name;
    }

}
