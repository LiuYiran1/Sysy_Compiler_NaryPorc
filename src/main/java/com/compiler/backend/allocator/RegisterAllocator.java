package com.compiler.backend.allocator;

import com.compiler.backend.location.Location;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public interface RegisterAllocator {
    /**
     * 分配栈
     *
     * @param var
     */
    void allocate(LLVMValueRef var);

    /**
     *
     * @param var
     * @return
     */
    Location locate(LLVMValueRef var);
    /**
     * 获得函数所需栈空间
     * @return 栈空间大小，单位：？
     */
    int getStackSize();
    void setStackSize(int stackSize);
}
