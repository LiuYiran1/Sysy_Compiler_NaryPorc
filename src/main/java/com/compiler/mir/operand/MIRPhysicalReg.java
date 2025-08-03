package com.compiler.mir.operand;

import com.compiler.mir.MIRType;

public class MIRPhysicalReg extends MIROperand {
    // 主要用于处理特定寄存器，比如说传参、函数返回值(a0,a1)
    // 物理寄存器没有类型，直接使用字符串表示

    public enum PREGs {
        A0("a0"), A1("a1"), A2("a2"), A3("a3"), A4("a4"), A5("a5"), A6("a6"), A7("a7"),
        FA0("fa0"), FA1("fa1"), FA2("fa2"), FA3("fa3"), FA4("fa4"), FA5("fa5"), FA6("fa6"), FA7("fa7"),
        FP("fp"), SP("sp"), RA("ra"), ZERO("zero"),TP("tp");

        private final String name;

        PREGs(String name) {
            this.name = name;
        }

    }
    
    private final PREGs pReg;

    public MIRPhysicalReg(PREGs pReg) {
        this.pReg = pReg;
    }

    @Override
    public String toString() {
        return pReg.name;
    }

    @Override
    public MIRType getType() {
        return null; // 物理寄存器没有类型
    }

}
