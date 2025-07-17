package com.compiler.ll.Values.Instructions;

public enum Opcode {
    RET,
    ALLOCA,
    STORE,
    FPTOSI,
    SITOFP,
    ICMP,
    FCMP,
    BR,
    CBR,  // conditional branch
    CALL,
    ZEXT,
    MUL,
    SDIV,
    SREM,
    ADD,
    SUB,
    FMUL,
    FDIV,
    FREM,
    FADD,
    FSUB,
    PHI,
    LOAD,
    GEP,
    BC // bitCast
}
