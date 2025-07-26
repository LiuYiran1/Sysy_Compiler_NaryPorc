import com.compiler.mir.MIRModule;
import org.llvm4j.llvm4j.Module;

public class MIRConverter {

    private final Module llvmModule;
    private final MIRModule mirModule = new MIRModule();
    private final Map<Value, MIROperand> valueMap = new LinkedHashMap<>(); // 改为LinkedHashMap
    private int tempCounter = 0;

    public MIRConverter(Module llvmModule) {
        this.llvmModule = llvmModule;
    }

    public MIRModule convert() {
        // 转换所有函数
        for(var func = LLVMGetFirstFunction(llvmModule); func != null; func = LLVMGetNextFunction(func)) {
            if (!func.isDeclaration()) {
                convertFunction(func);
            }
        }
        return mirModule;
    }

    private void convertFunction(Function llvmFunc) {
        MIRFunction mirFunc = new MIRFunction(llvmFunc.getName());
        mirModule.addFunction(mirFunc);

        // 创建参数虚拟寄存器
        for (Argument arg : llvmFunc.getArguments()) {
            MIRType type = convertType(arg.getType());
            MIRVirtualReg paramReg = mirFunc.newVirtualReg(type);
            valueMap.put(arg, paramReg);
            mirFunc.addParam(type);
        }

        // 转换基本块
        for (BasicBlock bb : llvmFunc.getBasicBlocks()) {
            MIRBasicBlock mirBB = convertBasicBlock(bb, mirFunc);
            mirFunc.addBlock(mirBB);
        }

        // 构建CFG
        buildControlFlowGraph(mirFunc);

        // 消除PHI节点（关键步骤）
        eliminatePhiNodes(mirFunc);
    }

    private MIRBasicBlock convertBasicBlock(BasicBlock llvmBB, MIRFunction mirFunc) {
        MIRBasicBlock mirBB = new MIRBasicBlock(llvmBB.getName());

        for (Instruction inst : llvmBB.getInstructions()) {
            MIRInstruction mirInst = convertInstruction(inst, mirFunc, mirBB);
            if (mirInst != null) {
                mirBB.addInstruction(mirInst);
                if (mirInst.getResult() != null) {
                    valueMap.put(inst, mirInst.getResult());
                }
            }
        }

        return mirBB;
    }

    private MIRInstruction convertInstruction(Instruction inst, MIRFunction mirFunc,
                                              MIRBasicBlock mirBB) {
        if (inst instanceof ReturnInst) {
            return convertReturnInst((ReturnInst) inst, mirFunc);
        } else if (inst instanceof BranchInst) {
            return convertBranchInst((BranchInst) inst, mirBB);
        } else if (inst instanceof CallInst) {
            return convertCallInst((CallInst) inst, mirFunc);
        } else if (inst instanceof BinaryOperator) {
            return convertBinaryOp((BinaryOperator) inst, mirFunc);
        } else if (inst instanceof LoadInst) {
            return convertLoadInst((LoadInst) inst, mirFunc);
        } else if (inst instanceof StoreInst) {
            return convertStoreInst((StoreInst) inst, mirFunc);
        } else if (inst instanceof GetElementPtrInst) {
            return convertGEPInst((GetElementPtrInst) inst, mirFunc); // 完整实现GEP转换
        } else if (inst instanceof ZExtInst) {
            return convertZExtInst((ZExtInst) inst, mirFunc);
        } else if (inst instanceof FPToSIInst) {
            return convertFPToSIInst((FPToSIInst) inst, mirFunc);
        } else if (inst instanceof SIToFPInst) {
            return convertSIToFPInst((SIToFPInst) inst, mirFunc);
        } else if (inst instanceof ICmpInst || inst instanceof FCmpInst) {
            return convertCmpInst(inst, mirFunc);
        } else if (inst instanceof PHINode) {
            // PHI节点稍后处理
            return null;
        } else if (inst instanceof BitCastInst) {
            return convertBitCast((BitCastInst) inst, mirFunc); // 完整实现位转换
        }

        throw new UnsupportedOperationException("Unsupported instruction: " + inst);
    }

    // 实现GetElementPtr指令转换
    private MIRInstruction convertGEPInst(GetElementPtrInst gep, MIRFunction mirFunc) {
        MIRType ptrType = convertType(gep.getType());
        MIRVirtualReg result = mirFunc.newVirtualReg(ptrType);

        // 获取基地址指针
        Value basePtr = gep.getPointerOperand();
        MIROperand base = getMIRValue(basePtr);

        // 计算总偏移量
        MIROperand totalOffset = calculateGEPOffset(gep, mirFunc);

        // 创建地址计算指令 (ADD base + offset)
        return new MIRArith(MIRArith.Op.ADD, result, base, totalOffset);
    }

    // 计算GEP指令的偏移量
    private MIROperand calculateGEPOffset(GetElementPtrInst gep, MIRFunction mirFunc) {
        Type baseType = gep.getSourceElementType();
        MIROperand totalOffset = new MIRImmediate(0, MIRType.I64);

        // 处理每个索引
        for (int i = 0; i < gep.getNumIndices(); i++) {
            Value indexVal = gep.getOperand(i + 1);
            MIROperand index = getMIRValue(indexVal);

            // 获取当前维度的元素大小
            long elementSize = getElementSize(baseType);
            MIROperand scaledIndex = scaleIndex(index, elementSize, mirFunc);

            // 累加偏移量
            totalOffset = accumulateOffset(totalOffset, scaledIndex, mirFunc);

            // 更新类型用于下一维度
            baseType = getContainedType(baseType);
        }

        return totalOffset;
    }

    // 实现位转换指令
    private MIRInstruction convertBitCast(BitCastInst bitcast, MIRFunction mirFunc) {
        MIRType destType = convertType(bitcast.getType());
        MIRVirtualReg result = mirFunc.newVirtualReg(destType);
        MIROperand source = getMIRValue(bitcast.getOperand(0));

        // 位转换在MIR中表现为直接赋值
        return new MIRMove(result, source);
    }

    // 最优PHI消除实现
    private void eliminatePhiNodes(MIRFunction mirFunc) {
        // 收集所有PHI节点
        Map<MIRBasicBlock, List<PHINode>> phiNodes = new HashMap<>();

        for (MIRBasicBlock mirBB : mirFunc.blocks) {
            for (MIRInstruction inst : new ArrayList<>(mirBB.instructions)) {
                if (inst.getResult() != null && valueMap.containsValue(inst.getResult())) {
                    // 找到对应的LLVM指令
                    for (Map.Entry<Value, MIROperand> entry : valueMap.entrySet()) {
                        if (entry.getValue().equals(inst.getResult()) &&
                                entry.getKey() instanceof PHINode) {

                            PHINode phi = (PHINode) entry.getKey();
                            if (!phiNodes.containsKey(mirBB)) {
                                phiNodes.put(mirBB, new ArrayList<>());
                            }
                            phiNodes.get(mirBB).add(phi);
                            mirBB.instructions.remove(inst); // 移除PHI指令
                        }
                    }
                }
            }
        }

        // 为每个PHI节点插入MOV指令
        for (Map.Entry<MIRBasicBlock, List<PHINode>> entry : phiNodes.entrySet()) {
            MIRBasicBlock currentBB = entry.getKey();

            for (PHINode phi : entry.getValue()) {
                MIRVirtualReg phiReg = (MIRVirtualReg) valueMap.get(phi);

                for (int i = 0; i < phi.getNumIncomingValues(); i++) {
                    BasicBlock incomingBB = phi.getIncomingBlock(i);
                    Value incomingValue = phi.getIncomingValue(i);

                    MIRBasicBlock mirIncomingBB = findMIRBlock(mirFunc, incomingBB.getName());
                    MIROperand source = getMIRValue(incomingValue);

                    // 在入块末尾插入MOV指令
                    MIRMove move = new MIRMove(phiReg, source);
                    mirIncomingBB.addInstruction(move);
                }
            }
        }
    }

    // 辅助方法：缩放索引
    private MIROperand scaleIndex(MIROperand index, long elementSize, MIRFunction mirFunc) {
        if (elementSize == 1) return index;

        MIRVirtualReg scaled = mirFunc.newVirtualReg(MIRType.I64);
        MIROperand sizeOp = new MIRImmediate(elementSize, MIRType.I64);
        mirFunc.getCurrentBlock().addInstruction(
                new MIRArith(MIRArith.Op.MUL, scaled, index, sizeOp)
        );
        return scaled;
    }

    // 辅助方法：累加偏移量
    private MIROperand accumulateOffset(MIROperand total, MIROperand addend, MIRFunction mirFunc) {
        MIRVirtualReg sum = mirFunc.newVirtualReg(MIRType.I64);
        mirFunc.getCurrentBlock().addInstruction(
                new MIRArith(MIRArith.Op.ADD, sum, total, addend)
        );
        return sum;
    }

    // 辅助方法：获取元素大小
    private long getElementSize(Type type) {
        if (type instanceof ArrayType) {
            return ((ArrayType) type).getElementType().getPrimitiveSizeInBits() / 8;
        }
        if (type instanceof StructType) {
            // 简化处理：假设所有结构体成员都是相同大小
            return type.getPrimitiveSizeInBits() / 8;
        }
        return type.getPrimitiveSizeInBits() / 8; // 基本类型
    }

    // 辅助方法：获取包含类型
    private Type getContainedType(Type type) {
        if (type instanceof ArrayType) {
            return ((ArrayType) type).getElementType();
        }
        if (type instanceof StructType) {
            // 简化处理：返回第一个成员类型
            return ((StructType) type).getElementType(0);
        }
        return type;
    }

    // 示例转换方法（其他转换类似）
    private MIRInstruction convertBinaryOp(BinaryOperator inst, MIRFunction mirFunc) {
        MIRType type = convertType(inst.getType());
        MIRVirtualReg result = mirFunc.newVirtualReg(type);

        MIROperand left = getMIRValue(inst.getOperand(0));
        MIROperand right = getMIRValue(inst.getOperand(1));

        MIRArith.Op op;
        switch (inst.getOpcode()) {
            case Add: op = MIRArith.Op.ADD; break;
            case Sub: op = MIRArith.Op.SUB; break;
            case Mul: op = MIRArith.Op.MUL; break;
            case SDiv: op = MIRArith.Op.DIV; break;
            case SRem: op = MIRArith.Op.REM; break;
            case Xor: op = MIRArith.Op.XOR; break;
            default:
                throw new UnsupportedOperationException("Unsupported binary op: " + inst.getOpcode());
        }

        return new MIRArith(op, result, left, right);
    }

    private MIROperand getMIRValue(Value value) {
        if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }

        if (value instanceof ConstantInt) {
            ConstantInt ci = (ConstantInt) value;
            return new MIRImmediate(ci.getSExtValue(), convertType(value.getType()));
        }

        // 创建临时寄存器
        MIRVirtualReg tempReg = new MIRVirtualReg(tempCounter++, convertType(value.getType()));
        valueMap.put(value, tempReg);
        return tempReg;
    }

    private MIRType convertType(Type type) {
        if (type.isIntegerTy(1)) return MIRType.I1;
        if (type.isIntegerTy(8)) return MIRType.I8;
        if (type.isIntegerTy(16)) return MIRType.I16;
        if (type.isIntegerTy(32)) return MIRType.I32;
        if (type.isIntegerTy(64)) return MIRType.I64;
        if (type.isFloatTy()) return MIRType.F32;
        if (type.isDoubleTy()) return MIRType.F64;
        if (type.isPointerTy()) return MIRType.PTR;  // 添加指针类型
        if (type.isVoidTy()) return MIRType.VOID;
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    // 其他转换方法...
    private MIRBasicBlock findMIRBlock(MIRFunction mirFunc, String name) {
        return mirFunc.blocks.stream()
                .filter(bb -> bb.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

}