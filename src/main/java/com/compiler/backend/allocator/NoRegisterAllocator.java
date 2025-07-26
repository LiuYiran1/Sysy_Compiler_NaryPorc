package com.compiler.backend.allocator;
import com.compiler.backend.location.Location;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.*;

public class NoRegisterAllocator implements RegisterAllocator {
    private final Map<LLVMValueRef, Location> locationMap = new HashMap<>();
    private final Map<LLVMValueRef, Location> globalMap = new HashMap<>();
    private int reg = 0;
    private final LinkedList<String> availableRegisters = new LinkedList<>(Arrays.asList(
            "s5", "s6", "s7", "s8", "s9", "s10", "s11", "a1",
            "a2", "a3", "a4", "a5", "a6", "a0","ra","gp","tp"
    ));

    @Override
    public void allocate(LLVMValueRef value) {
        // 处理全局变量
        if (LLVMIsAGlobalVariable(value) != null) {
            globalMap.computeIfAbsent(value, v ->
                    Location.global(LLVMGetValueName(v).getString()));
            return;
        }

        // 先分配寄存器
        if(reg < availableRegisters.size()) {
            locationMap.computeIfAbsent(value, v -> {
                reg++;
                return Location.reg(availableRegisters.get(reg - 1));
            });
        }


    }

    @Override
    public Location locate(LLVMValueRef var) {
        // 找到var是global还是stack
        // 先检查全局变量
        if (globalMap.containsKey(var)) {
            return globalMap.get(var);
        }
        // 再检查栈分配
        if (locationMap.containsKey(var)) {
            return locationMap.get(var);
        }
        throw new RuntimeException("Undefined value: " + LLVMGetValueName(var));
    }

    @Override
    public int getStackSize() {
        // No stack allocation in this allocator
        return 0;
    }

    @Override
    public void setStackSize(int stackSize) {
        // No stack allocation in this allocator, so this method does nothing
    }
}
