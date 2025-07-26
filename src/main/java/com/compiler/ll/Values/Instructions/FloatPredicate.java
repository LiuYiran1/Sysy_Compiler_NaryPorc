package com.compiler.ll.Values.Instructions;

public enum FloatPredicate {
    OEQ("oeq"),
    ONE("one"),
    OLT("olt"),
    OGT("ogt"),
    OLE("ole"),
    OGE("oge");

    private final String irName;

    FloatPredicate(String irName) {
        this.irName = irName;
    }

    public String getIRName() {
        return irName;
    }
}
