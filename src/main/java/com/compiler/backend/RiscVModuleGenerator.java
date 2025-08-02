package com.compiler.backend;

import com.compiler.backend.allocator.LinearScanRegisterAllocator;
import com.compiler.backend.PhysicalRegister;
import com.compiler.mir.*;
import com.compiler.mir.instruction.*;
import com.compiler.mir.operand.*;
import java.util.*;

public class RiscVModuleGenerator {
    private final MIRModule mirModule;
    private final Map<MIRFunction, LinearScanRegisterAllocator> allocators = new LinkedHashMap<>();
    private final Map<MIRFunction, StackManager> stackManagers = new LinkedHashMap<>();
    private final StringBuilder asm = new StringBuilder();
    private final Set<MIRFunction> definedFunctions = new LinkedHashSet<>();
    private final Set<MIRFunction> declaredFunctions = new LinkedHashSet<>();

    public RiscVModuleGenerator(MIRModule mirModule) {
        this.mirModule = mirModule;
        initialize();
    }

    public LinearScanRegisterAllocator getLSRA(MIRFunction function) {
        return allocators.get(function);
    }

    public StackManager getStackManager(MIRFunction function) {
        return stackManagers.get(function);
    }

    private void initialize() {
        // 为每个函数创建寄存器分配器和栈管理器
        for (MIRFunction function : mirModule.getFunctions()) {
            if (function.getBlocks().isEmpty()) {
                declaredFunctions.add(function);
            } else {
                System.out.println("assigning function: " + function.getName());
                LinearScanRegisterAllocator allocator = new LinearScanRegisterAllocator(function);
                allocator.allocate();
                allocators.put(function, allocator);

                System.out.println("allocated function: " + function.getName());
                StackManager stackManager = new StackManager(function, allocator);
                stackManagers.put(function, stackManager);

                definedFunctions.add(function);

                System.out.println("defined function: " + function.getName());
            }
        }
    }

    public String generate() {
        // 生成数据段
        generateDataSection();

        // 生成文本段
        generateTextSection();

        // 生成函数代码
        generateFunctions();

        return asm.toString();
    }

    private void generateDataSection() {
        asm.append("    .data\n");

        // 生成全局变量
        for (MIRGlobalVariable global : mirModule.getGlobalVariables()) {
            asm.append(global.getName()).append(":\n");

            if (global.getInitializerType().equals(MIRGlobalVariable.InitializerType.ZERO_INITIALIZED)) {
                asm.append("    .zero ").append(global.getZeroSize() * 4).append("\n");  // int float 都是 4 字节
            } else if (global.getInitializerType().equals(MIRGlobalVariable.InitializerType.FLOAT)) {
                asm.append("    .float ").append(global.getFloatValue()).append("\n");
            } else if (global.getInitializerType().equals(MIRGlobalVariable.InitializerType.INT)) {
                asm.append("    .word ").append(global.getIntValue()).append("\n");
            } else if (global.getInitializerType().equals(MIRGlobalVariable.InitializerType.ARRAY)) {
                if (global.getType() == MIRType.I32) {
                    for (var value : global.getArrayValues()) {
                        asm.append("    .word ");
                        //System.err.println(value);
                        asm.append(value.toString()).append("\n");
                    }
                } else if (global.getType() == MIRType.F32) {
                    for (var value : global.getArrayValues()) {
                        asm.append("    .float ");
                        //System.err.println(value);
                        asm.append(value.toString()).append("\n");
                    }

                }


            }
            asm.append("\n");
        }

//        asm.append("\n");
    }

    private void generateTextSection() {
        asm.append("    .text\n");

        // 已声明函数(当成外部函数）
        for (MIRFunction function : declaredFunctions) {
            asm.append("    .extern ").append(function.getName()).append("\n");
        }

        // 声明已定义函数
        for (MIRFunction function : definedFunctions) {
            asm.append("    .globl ").append(function.getName()).append("\n");
        }

        asm.append("\n");
    }

    private void generateFunctions() {
        // 生成函数入口点（如果需要）
//        if (mirModule.getFunction("main") != null) {
//            asm.append("    .globl _start\n");
//            asm.append("_start:\n");
//            asm.append("    call main\n");
//            asm.append("    li a7, 93\n");  // exit syscall
//            asm.append("    ecall\n\n");
//        }

//         生成每个函数的具体实现
        for (MIRFunction function : definedFunctions) {
            RiscVFunctionGenerator funcGenerator = new RiscVFunctionGenerator(
                    function,
                    allocators.get(function),
                    stackManagers.get(function)
            );
            asm.append(funcGenerator.generate());
            asm.append("\n");
        }
    }
}