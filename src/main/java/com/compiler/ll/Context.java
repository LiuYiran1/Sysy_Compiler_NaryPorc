package com.compiler.ll;

import com.compiler.ll.Types.*;
import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.utils.NameManager;

import java.util.*;

public class Context {
    private final Map<Integer, IntegerType> intTypes = new HashMap<>();
    private final Map<Type, PointerType> pointerTypes = new HashMap<>();
    private final Map<ListKey, FunctionType> functionTypes = new HashMap<>();
    private final Map<ListKey, ArrayType> arrayTypes = new HashMap<>();
    private final VoidType voidType = new VoidType(this);
    private final FloatType floatType = new FloatType(this);

    private Module module;
    private IRBuilder builder;

    public VoidType getVoidType() {
        return voidType;
    }

    public FloatType getFloatType() {
        return floatType;
    }

    public IntegerType getInt1Type() {
        return getIntType(1);
    }

    public IntegerType getInt32Type() {
        return getIntType(32);
    }

    public IntegerType getInt64Type() {
        return getIntType(64);
    }

    public IntegerType getIntType(int bitWidth) {
        return intTypes.computeIfAbsent(bitWidth, bw -> new IntegerType(this, bw));
    }

    public PointerType getPointerType(Type pointee) {
        return pointerTypes.computeIfAbsent(pointee, pt -> new PointerType(this, pt));
    }

    public ArrayType getArrayType(Type elem, int num) {
        return arrayTypes.computeIfAbsent(new ListKey(elem, num), k -> new ArrayType(this, elem, num));
    }

    public FunctionType getFunctionType(Type ret, List<Type> params) {
        return functionTypes.computeIfAbsent(new ListKey(ret, params), k -> new FunctionType(this, ret, params));
    }

    // 内部类用于作为 Map key
    private static class ListKey {
        final Object a;
        final Object b;

        ListKey(Object a, Object b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ListKey)) return false;
            ListKey k = (ListKey) o;
            return Objects.equals(a, k.a) && Objects.equals(b, k.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }

    // 是否要加一个module的成员变量
    public Module getModule(String name) {
        if(module == null) {
            this.module = new Module(name, this, getIRBuilder());
        }
        return module;
    }

    public IRBuilder getIRBuilder() {
        if(builder == null) {
            this.builder = new IRBuilder(this, new NameManager());
        }
        return builder;
    }

    public BasicBlock newBasicBlock(String name) {
        BasicBlock nb = new BasicBlock(name);
        this.builder.addBlock(nb);
        return nb;
    }
}
