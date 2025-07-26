package com.compiler.ll.Values.Instructions;

public enum IntPredicate {
    EQ("eq"),
    NE("ne"),
    SLT("slt"),
    SGT("sgt"),
    SLE("sle"),
    SGE("sge");

    private final String irName;

    IntPredicate(String irName) {
        this.irName = irName;
    }

    public String getIRName() {
        return irName;
    }
}
