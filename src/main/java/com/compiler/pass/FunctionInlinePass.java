package com.compiler.pass;

import com.compiler.ll.Values.BasicBlock;
import com.compiler.ll.Module;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.Instruction;
import com.compiler.ll.Values.Instructions.CallInst;
import com.compiler.ll.Values.Instructions.Opcode;
import com.compiler.ll.Values.Instructions.ReturnInst;
import com.compiler.ll.Values.Value;

import java.util.HashMap;
import java.util.Map;

public class FunctionInlinePass implements Pass {
    @Override
    public boolean run(Module module) {
        boolean changed = false;

        // 遍历所有函数
        for (Function caller : module.getFunctionDefs()) {
            // 遍历所有 call 指令
            for (CallInst call : caller.getCallInstructions()) {
                Function callee = call.getFunction();
                if (callee == null) continue;

                if (shouldInline(caller, callee)) {
                    System.err.println("PPPPPPPP Inline candidate: " + callee.getName()
                            + " (called in " + caller.getName() + ")");
                    inlineFunction(caller, call, callee);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private void inlineFunction(Function caller, CallInst call, Function callee) {
        if (callee.getBasicBlocks().size() != 1) return;
        // 拿到调用指令所在块
        BasicBlock callBlock = call.getParent();

        // 1. 建立参数映射：callee 参数 -> call 实参
        Map<Value, Value> valueMap = new HashMap<>();
        int idx = 0;
        for (Value param : callee.getArguments()) {
            Value actual = call.getOperand(idx++);
            valueMap.put(param, actual);
        }

// 遍历 valueMap 并打印
        for (Map.Entry<Value, Value> entry : valueMap.entrySet()) {
            System.err.println("param: " + entry.getKey().toIR() + " -> actual: " + entry.getValue().toIR());
        }

        // 2. 遍历 callee 的基本块指令并拷贝到 caller
        // 这里假设 callee 只有一个基本块
        BasicBlock calleeBB = callee.getBasicBlocks().get(0);
        for (Instruction inst : calleeBB.getInstructions()) {
            if (inst.getOpcode() == Opcode.RET) {
                ReturnInst ret = (ReturnInst) inst;
                // 3. 处理返回值
                if (!ret.getOperands().isEmpty()) {
                    Value retVal = valueMap.getOrDefault(ret.getOperand(0), ret.getOperand(0));
                    // 将 call 的所有使用替换为 retVal
                    calleeBB.replaceAllUse(call, retVal);
                }
                // return 本身不插入
            } else {
                // 拷贝指令并替换操作数
                Instruction cloned = inst.clone();
                System.err.println("33333333" + cloned.toIR());
                System.err.println("44444444" + inst.toIR());
                // 替换操作数
                for (int i = 0; i < cloned.getOperands().size(); i++) {
                    Value op = cloned.getOperand(i);

                    if (valueMap.containsKey(op)) {
                        cloned.setOperand(i, valueMap.get(op));
                    }
                }
                // 添加到 caller 的 callBlock
                callBlock.insertBefore(cloned, call);
                // 把 cloned 的结果加入 valueMap
                valueMap.put(inst, cloned);
            }
        }

        // 4. 删除原始 call 指令
        call.eraseFromParent();
    }



    /**
     * 判断 callee 是否符合内联条件
     */
    private boolean shouldInline(Function caller, Function callee) {


        // 1. 只有一个基本块
        if (callee.getBasicBlocks().size() != 1) return false;

        // 2. 没有全局变量使用
        for (BasicBlock bb : callee.getBasicBlocks()) {
            for (Instruction inst : bb.getInstructions()) {
                for (Value op : inst.getOperands()) {
                    if (op.isGlobalVariable()) {
                        return false;
                    }
                }
            }
        }

        // 3. 函数内部不能再调用其他函数
        for (BasicBlock bb : callee.getBasicBlocks()) {
            for (Instruction inst : bb.getInstructions()) {
                if (inst.getOpcode() == Opcode.CALL || inst.getOpcode() == Opcode.GEP) {
                    return false; // 叶子函数约束
                }
            }
        }

        // 4. 缺点：
        // 2. 函数参数不能是数组或指针
        for (Value param : callee.getArguments()) {
            if (param.getType().isPointerType() || param.getType().isArrayType()) {
                return false;
            }
        }

        return true;
    }
}
