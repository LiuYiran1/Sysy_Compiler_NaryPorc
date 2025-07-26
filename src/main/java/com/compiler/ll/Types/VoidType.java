package com.compiler.ll.Types;

import com.compiler.ll.Context;

public class VoidType extends Type {
    public VoidType(Context context) {
        super(context, TypeID.VOID);
    }

    @Override
    public String toIR() {
        return "void";
    }
}
