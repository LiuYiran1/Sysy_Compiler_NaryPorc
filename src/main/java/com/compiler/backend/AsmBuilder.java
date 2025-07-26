package com.compiler.backend;

import com.compiler.backend.location.Location;

public class AsmBuilder {
    private final StringBuffer buffer = new StringBuffer();

    public void text() {
        buffer.append("  .text").append("\n");
    }

    public void global() {
        buffer.append("  .globl ").append("main\n");
    }

    public void label(String label) {
        buffer.append(label).append(":\n");
    }

    public void data(String data) {
        buffer.append("  .data").append("\n").append(data).append(":\n");
    }

    public void word(int value) {
        buffer.append("  .word ").append(value).append("\n\n");
    }

    public void ecall() {
        buffer.append("  ecall").append("\n");
    }

    //两个操作数指令
    public void op2(String op, String dest, String lhs, String rhs) {
        buffer.append(String.format("  %s %s, %s, %s\n", op, dest, lhs, rhs));
    }

    // 一个操作数
    public void op(String op, String dest ,String hs) {
        buffer.append(String.format("  %s %s, %s\n", op, dest, hs));
    }

    // 立即数指令：addi dest, src, imm
    public void op2Imm(String op, String dest, String src, int imm) {
        buffer.append(String.format("  %s %s, %s, %d\n", op, dest, src, imm));
    }

    // 只包含立即数 li a0 1
    public void opImm(String op, String dest, int imm) {
        buffer.append(String.format("  %s %s, %d\n", op, dest, imm));
    }

    // 通用加载方法
    public void load(String destReg, Location src) {
        switch (src.getType()) {
            case STACK:
                buffer.append(String.format("  lw %s, %d(%s)\n", destReg, src.getOffset(), src.getIdentifier()));
                break;
            case GLOBAL:
                // 采用一个奇怪的处理 临时用a7（应该不影响）
                buffer.append(String.format("  la a7, %s\n", src.getIdentifier()));
                buffer.append(String.format("  lw %s, 0(a7)\n", destReg));
                break;
            case REGISTER:
                buffer.append(String.format("  mv %s, %s\n", destReg, src.getIdentifier()));
                break;
            case IMMEDIATE:
                buffer.append(String.format("  li %s, %d\n", destReg, src.getOffset()));
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    // 通用存储方法 这个方法感觉有些小问题 dest一定是由allocator提前分配好的吧？
    public void store(String srcReg, Location dest) {
        switch (dest.getType()) {
            case STACK:
                buffer.append(String.format("  sw %s, %d(%s)\n", srcReg, dest.getOffset(), dest.getIdentifier()));
                break;
            case REGISTER:
                buffer.append(String.format("  mv %s, %s\n", dest.getIdentifier(), srcReg));
                break;
            case GLOBAL:
                buffer.append(String.format("  la a7, %s\n", dest.getIdentifier()));
                buffer.append(String.format("  sw %s, 0(a7)\n", srcReg));
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    // 跳转指令： j label
    public void jump(String op, String label) {
        buffer.append(String.format("  %s %s\n",op, label));
    }

    // 条件跳转: bnez
    public void jump(String op, String reg, String label) {
        buffer.append(String.format("  %s %s, %s\n", op, reg, label));
    }

    // 获取生成的全部汇编代码
    public String getAsmCode() {
        return buffer.toString();
    }
}
