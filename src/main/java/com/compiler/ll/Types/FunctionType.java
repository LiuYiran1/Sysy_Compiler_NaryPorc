package com.compiler.ll.Types;

import com.compiler.ll.Context;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FunctionType extends Type {
    private final Type returnType;
    private final List<Type> paramTypes;

    public FunctionType(Context context, Type returnType, List<Type> paramTypes) {
        super(context, TypeID.FUNCTION);
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    @Override
    public String toIR() { // 这里的构建不太对吧
        String paramIR = paramTypes.stream()
                .map(Type::toIR)
                .collect(Collectors.joining(", "));
        return returnType.toIR() + " (" + paramIR + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionType)) return false;
        FunctionType other = (FunctionType) o;
        return Objects.equals(this.returnType, other.returnType) &&
                Objects.equals(this.paramTypes, other.paramTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(TypeID.FUNCTION, returnType, paramTypes);
    }
}
