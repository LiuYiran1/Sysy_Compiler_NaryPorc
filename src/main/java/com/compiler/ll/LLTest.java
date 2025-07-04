package com.compiler.ll;

public class LLTest {
    public static void main(String[] args) {
        Context context = new Context();
        System.out.println(context.getInt1Type().toIR());
    }
}
