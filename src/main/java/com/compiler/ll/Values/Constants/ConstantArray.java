package com.compiler.ll.Values.Constants;

import com.compiler.ll.Context;
import com.compiler.ll.Types.ArrayType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConstantArray extends Constant {
    private final List<Constant> elements;

    // 改为允许传入任意 Constant[]（用于高维构造）
    public ConstantArray(Context context, Type elementType, List<Constant> elements) {
        super(new ArrayType(context, elementType, elements.size()));
        this.elements = elements;
    }

    public ConstantArray(Type type) { // 为了全零数组做的
        super(type);
        this.elements = new ArrayList<>();
    }

    public List<Constant> getElements() {
        return elements;
    }

    @Override
    public String toIR() {
        if (isZero()) return "zeroinitializer";
        return "[" + elements.stream()
                .map(e -> e.getType().toIR() + " " + e.toIR())
                .collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public boolean isZero(){
        boolean isZero = true;
        for (Constant element : elements) {
            if (!element.isZero()) isZero = false;
        }
        return isZero;
    }
}
