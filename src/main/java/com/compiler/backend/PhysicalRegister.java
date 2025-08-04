package com.compiler.backend;

import java.util.Arrays;
import java.util.List;

public enum PhysicalRegister {
    // 整数寄存器
    ZERO("x0"), RA("x1"), SP("x2"), GP("x3"), TP("x4"),
    T0("x5"), T1("x6"), T2("x7"), S0("x8"), S1("x9"),
    A0("x10"), A1("x11"), A2("x12"), A3("x13"), A4("x14"),
    A5("x15"), A6("x16"), A7("x17"), S2("x18"), S3("x19"),
    S4("x20"), S5("x21"), S6("x22"), S7("x23"), S8("x24"),
    S9("x25"), S10("x26"), S11("x27"), T3("x28"), T4("x29"),
    T5("x30"), T6("x31"),

    // 浮点寄存器
    FT0("f0"), FT1("f1"), FT2("f2"), FT3("f3"), FT4("f4"),
    FT5("f5"), FT6("f6"), FT7("f7"), FS0("f8"), FS1("f9"),
    FA0("f10"), FA1("f11"), FA2("f12"), FA3("f13"), FA4("f14"),
    FA5("f15"), FA6("f16"), FA7("f17"), FS2("f18"), FS3("f19"),
    FS4("f20"), FS5("f21"), FS6("f22"), FS7("f23"), FS8("f24"),
    FS9("f25"), FS10("f26"), FS11("f27"), FT8("f28"), FT9("f29"),
    FT10("f30"), FT11("f31");

    private final String name;

    PhysicalRegister(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final List<PhysicalRegister> PROLOGUE = Arrays.asList(
            RA, S0, SP
    );

    public static final List<PhysicalRegister> EPILOGUE = Arrays.asList(
            S0, RA, SP
    );

    public static final List<PhysicalRegister> INT_ARG_REGS = Arrays.asList(
            A0, A1, A2, A3, A4, A5, A6, A7
    );

    public static final List<PhysicalRegister> RETURN_INT_REGS = Arrays.asList(
            A0, A1
    );

    public static final List<PhysicalRegister> FLOAT_ARG_REGS = Arrays.asList(
            FA0, FA1, FA2, FA3, FA4, FA5, FA6, FA7
    );

    public static final List<PhysicalRegister> RETURN_FLOAT_REGS = Arrays.asList(
            FA0, FA1
    );

    public static final List<PhysicalRegister> CALLER_SAVED_INT = Arrays.asList(
            T0, T1, T3, T4, T5, T6, A0, A1, A2, A3, A4, A5, A6, A7
    );

    public static final List<PhysicalRegister> CALLER_SAVED_FLOAT = Arrays.asList(
            FT0, FT1, FT2, FT3, FT4, FT5, FT6, FT7, FT8, FT9, FT10, FT11, FA0, FA1, FA2, FA3, FA4, FA5, FA6, FA7
    );

    // s0 也是fp,按理说在PROLOGUE时候就应该处理
    public static final List<PhysicalRegister> CALLEE_SAVED_INT = Arrays.asList(
            S1, S2, S3, S4, S5, S6, S7, S8, S9, S10, S11
    );

    public static final List<PhysicalRegister> CALLEE_SAVED_FLOAT = Arrays.asList(
            FS0, FS1, FS2, FS3, FS4, FS5, FS6, FS7, FS8, FS9, FS10, FS11
    );
}