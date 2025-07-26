package com.compiler.mir;

public enum MIRType {
    I1,I32,I64,
    F32,F64,
    ARRAY,
    VOID;
    /* ---------- 查询工具 ---------- */
    public static boolean isInt(MIRType t) {
        return t == I32;
    }
    public static boolean isFloat(MIRType t) {
        return t == F32;
    }
    public static boolean isScalar(MIRType t) {
        return isInt(t) || isFloat(t);
    }
}
