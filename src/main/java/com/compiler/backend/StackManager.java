package com.compiler.backend;

import com.compiler.backend.allocator.LinearScanRegisterAllocator;
import com.compiler.mir.*;
import com.compiler.mir.instruction.*;
import com.compiler.mir.operand.*;
import java.util.*;

public class StackManager {
    private final MIRFunction function;
    private final LinearScanRegisterAllocator allocator;
    private int registerSaveSize = 0;
    private int arraySectionOffset = 0;
    private int spillSectionOffset = 0;
    private final Map<MIRVirtualReg, Integer> arrayOffsets = new LinkedHashMap<>();
    private int frameSize;

    // 栈帧布局（从高地址到低地址）:
    // |-------------------|
    // | 寄存器(pro+callee)| <- s0
    // |-------------------|
    // | spill 变量        |
    // |-------------------|
    // | 数组              | <- sp (反过来存）
    // |-------------------|

    // 这两部分需要动态 分配 回收
    // |-------------------|
    // | caller-save       |
    // |-------------------|
    // | 额外参数空间       |
    // |-------------------|

    public StackManager(MIRFunction function, LinearScanRegisterAllocator allocator) {
        this.function = function;
        this.allocator = allocator;
        calculateFrameLayout();
    }

    private void calculateFrameLayout() {
        // 1. ra和s0的保存
        int entrySaveSize = 16; // ra和s0各8字节

        // 2. callee寄存器的save (main函数不需要保存)
        if(!function.getName().equals("main")) {
            registerSaveSize = allocator.getUsedCalleeSaved().size() * 8 + entrySaveSize; // 每个callee寄存器8字节 + ra和s0的保存
        } else {
            // main函数不需要保存寄存器
            registerSaveSize = entrySaveSize;
        }

        // 3. 计算spill区域大小
        int spillSize = allocator.getSpillMap().size() * 8; // 每个spill变量8字节（应该可以这么处理）
        spillSectionOffset = registerSaveSize + spillSize;

        System.err.println(function.getName() + "spill : " + spillSize / 8);

        // 4. 计算数组区域大小
        arraySectionOffset = spillSectionOffset;
        for (MIRBasicBlock block : function.getBlocks()) {
            for (MIRInstruction inst : block.getInstructions()) {
                if (inst instanceof MIRAllocOp alloc) {
                    arraySectionOffset += alloc.getSize();
                    arrayOffsets.put(alloc.getResult(), arraySectionOffset);
//                    arraySectionOffset += alloc.getSize();
                }
            }
        }

        // 5. 总帧大小
        frameSize = arraySectionOffset;

    }

    // s0 - offset
    public int getArrayOffset(MIRVirtualReg arrayReg) {
        return arrayOffsets.getOrDefault(arrayReg, 0);
    }

    // 这个方法理论上，能获得一个负数，与s0做运算，就能得到位置
    public int getSpillOffset(MIRVirtualReg vreg) {
        Integer spillLoc = allocator.getSpillLocation(vreg);
        return spillLoc != null ? -registerSaveSize + spillLoc : 0;
    }


    public int getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;
    }

    public int getRegisterSaveSize() {
        return allocator.getUsedCalleeSaved().size() * 8 + 16;
    }
}