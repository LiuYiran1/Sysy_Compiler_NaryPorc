package com.compiler.ll.Values.Constants;

import com.compiler.ll.Types.FloatType;
import com.compiler.ll.Types.Type;
import com.compiler.ll.Values.Constant;
import com.compiler.ll.Values.Value;
import com.compiler.ll.exceptions.ConstantException;

public class ConstantFloat extends Constant {
    private final float value;

    public ConstantFloat(FloatType type, float value) {
        super(type);
        this.value = value;
    }

    public ConstantFloat(Value val, Type targetType) {
        super(targetType);
        Type valType = val.getType();
        if (valType.isIntegerType()){
            this.value = (float) ((ConstantInt)val).getValue();
        } else if (valType.isFloatType()){
            this.value = ((ConstantFloat)val).getValue();
        } else {
            throw new ConstantException("ConstantFloat type error");
        }
    }

    @Override
    public boolean isZero(){
        return value == 0;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String getName() {
        return "" + value;
    }

    @Override
    public String toIR() {
        // 将 float 转换为其 32-bit IEEE 754 编码
        int floatBits = Float.floatToRawIntBits(value);

        // 分解 float 位：sign(1), exponent(8), mantissa(23)
        long sign = (floatBits >>> 31) & 0x1L;
        long exponent = (floatBits >>> 23) & 0xFFL;
        long mantissa = floatBits & 0x7FFFFFL;

        // 映射到 double 的位宽（sign: 1bit, exp: 11bit, mantissa: 52bit）
        // 偏移指数从 float(127) -> double(1023)
        long doubleSign = sign << 63;
        long doubleExponent = (exponent == 0) ? 0 : ((exponent - 127 + 1023) & 0x7FFL) << 52;
        long doubleMantissa = mantissa << (52 - 23); // 高位对齐，低29位为0

        long doubleBits = doubleSign | doubleExponent | doubleMantissa;

        // 输出为 LLVM IR 所需格式：16位十六进制数（带前缀）
        return String.format("0x%016X", doubleBits);
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConstantFloat)) return false;
        ConstantFloat other = (ConstantFloat) obj;
        return this.value == other.value && this.getType().equals(other.getType());
    }

    @Override
    public int hashCode() {
        return Float.hashCode(value) + 31 * getType().hashCode();
    }
}
