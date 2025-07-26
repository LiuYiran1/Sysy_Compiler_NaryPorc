package com.compiler.backend.location;

public enum LocationType {
    REGISTER,   // 寄存器
    STACK,      // 栈偏移
    GLOBAL,     // 全局变量
    IMMEDIATE   // 立即数（特殊类型）
}
