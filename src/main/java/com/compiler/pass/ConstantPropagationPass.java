package com.compiler.pass;

import com.compiler.ll.Context;
import com.compiler.ll.Module;
import com.compiler.ll.Types.*;
import com.compiler.ll.Values.*;
import com.compiler.ll.Values.Constants.ConstantFloat;
import com.compiler.ll.Values.Constants.ConstantInt;
import com.compiler.ll.Values.GlobalValues.Function;
import com.compiler.ll.Values.GlobalValues.GlobalVariable;
import com.compiler.ll.Values.Instructions.*;

import java.util.*;

public class ConstantPropagationPass implements Pass {

    Context context;

    boolean hasChanged = false;

    public ConstantPropagationPass(Context context) {
        this.context = context;
    }

    enum LatticeVal {
        UNDEF,      // 未知值
        CONST,      // 常量值
        OVERDEF     // 不确定值
    }

    static class ConstValue {
        LatticeVal state;
        Object value; // Integer or Float

        ConstValue() {
            this.state = LatticeVal.UNDEF;
        }

        ConstValue(Object value) {
            this.state = LatticeVal.CONST;
            this.value = value;
        }

        ConstValue(ConstValue other) {
            this.state = other.state;
            this.value = other.value;
        }

        void meet(ConstValue other) {
            if (other.state == LatticeVal.OVERDEF || this.state == LatticeVal.OVERDEF) {
                this.state = LatticeVal.OVERDEF;
                this.value = null;
            } else if (this.state == LatticeVal.UNDEF) {
                this.state = other.state;
                this.value = other.value;
            } else if (other.state == LatticeVal.UNDEF) {
                // do nothing
            } else if (!this.value.equals(other.value)) {
                this.state = LatticeVal.OVERDEF;
                this.value = null;
            }
        }
    }

    Map<Value, ConstValue> valueLattice = new HashMap<>();
    Set<BasicBlock> executableBlocks = new HashSet<>();

    Queue<Instruction> instWorkList = new LinkedList<>();
    Queue<BasicBlock> blockWorkList = new LinkedList<>();

    @Override
    public boolean run(Module module) {
        hasChanged = false;
        for (Function func : module.getFunctionDefs()) {
            runOnFunction(func);
        }
        return hasChanged;
    }

    private void runOnFunction(Function func) {
        valueLattice.clear();
        executableBlocks.clear();
        instWorkList.clear();
        blockWorkList.clear();

        BasicBlock entry = func.getEntryBlock();
        markExecutable(entry);

        while (!instWorkList.isEmpty() || !blockWorkList.isEmpty()) {
            while (!blockWorkList.isEmpty()) {
                BasicBlock bb = blockWorkList.poll();
                for (Instruction inst : bb.getInstructions()) {
                    visitInstruction(inst);
                }
            }

            while (!instWorkList.isEmpty()) {
                Instruction inst = instWorkList.poll();
                visitInstruction(inst);
            }
        }

        applyChanges(func);
    }

    private void markExecutable(BasicBlock bb) {
        if (executableBlocks.add(bb)) {
            blockWorkList.add(bb);
        }
    }

    private void visitInstruction(Instruction inst) {
        if (valueLattice.containsKey(inst) && valueLattice.get(inst).state == LatticeVal.OVERDEF)
            return;

        switch (inst.getOpcode()) {
            case ADD, SUB, MUL, SDIV, SREM, FADD, FSUB, FMUL, FDIV, FREM -> evalBinary(inst);
            case ICMP, FCMP -> evalCmp(inst);
            case ZEXT, FPTOSI, SITOFP -> evalCast(inst);
            case PHI -> evalPhi((PhiInst) inst);
            case CBR -> evalCondBr((CondBranchInst) inst);
            case BR -> {
                markExecutable(((BranchInst) inst).getTarget());
            }
            case LOAD -> evalLoad((LoadInst) inst);
            case RET, CALL -> valueLattice.put(inst, new ConstValue()); // 默认不处理 call/ret
        }
    }

    private void evalLoad(LoadInst inst) {
        System.out.println("BBBBB");
        Value ptr = inst.getOperand(0);

        ConstValue memVal = getConst(ptr);

        if (memVal != null && memVal.state == LatticeVal.CONST) {
            // 能确定的全局变量常量
            valueLattice.put(inst, new ConstValue(memVal));
        } else {
            // 没有写入记录，或多次写入不同值，设置为 OVERDEF
            valueLattice.put(inst, new ConstValue());
        }

        addUsersToWorklist(inst);
    }



    private void evalBinary(Instruction inst) {
        Value lhs = inst.getOperand(0), rhs = inst.getOperand(1);
        ConstValue l = getConst(lhs), r = getConst(rhs);

        ConstValue result = new ConstValue();

        if (l.state == LatticeVal.CONST && r.state == LatticeVal.CONST) {
            Object lv = l.value, rv = r.value;
            if (lv instanceof Integer && rv instanceof Integer) {
                int a = (int) lv, b = (int) rv;
                int res = switch (inst.getOpcode()) {
                    case ADD -> a + b;
                    case SUB -> a - b;
                    case MUL -> a * b;
                    case SDIV -> b == 0 ? 0 : a / b;
                    case SREM -> b == 0 ? 0 : a % b;
                    default -> 0;
                };
                result = new ConstValue(res);
            } else if (lv instanceof Float || rv instanceof Float) {
                float a = ((Number) lv).floatValue();
                float b = ((Number) rv).floatValue();
                float res = switch (inst.getOpcode()) {
                    case FADD -> a + b;
                    case FSUB -> a - b;
                    case FMUL -> a * b;
                    case FDIV -> b == 0 ? 0f : a / b;
                    case FREM -> b == 0 ? 0f : a % b;
                    default -> 0f;
                };
                result = new ConstValue(res);
            } else {
                result.state = LatticeVal.OVERDEF;
            }
        } else {
            result = meetState(l, r);
        }

        if (updateLattice(inst, result)) {
            addUsersToWorklist(inst);
        }
    }

    private void evalCmp(Instruction inst) {
        Value lhs = inst.getOperand(0), rhs = inst.getOperand(1);
        ConstValue l = getConst(lhs), r = getConst(rhs);

        ConstValue result = new ConstValue();
        if (l.state == LatticeVal.CONST && r.state == LatticeVal.CONST) {
            if (l.value instanceof Integer && r.value instanceof Integer) {
                int a = (int) l.value, b = (int) r.value;
                boolean cmp = switch (((ICmpInst) inst).getPredicate()) {
                    case EQ -> a == b;
                    case NE -> a != b;
                    case SLT -> a < b;
                    case SLE -> a <= b;
                    case SGT -> a > b;
                    case SGE -> a >= b;
                };
                result = new ConstValue(cmp ? 1 : 0);
            } else if (l.value instanceof Float || r.value instanceof Float) {
                float a = ((Number) l.value).floatValue();
                float b = ((Number) r.value).floatValue();
                boolean cmp = switch (((FCmpInst) inst).getPredicate()) {
                    case OEQ -> a == b;
                    case ONE -> a != b;
                    case OLT -> a < b;
                    case OLE -> a <= b;
                    case OGT -> a > b;
                    case OGE -> a >= b;
                };
                result = new ConstValue(cmp ? 1 : 0);
            } else {
                result.state = LatticeVal.OVERDEF;
            }
        } else {
            result = meetState(l, r);
        }

        if (updateLattice(inst, result)) {
            addUsersToWorklist(inst);
        }
    }

    private void evalCast(Instruction inst) {
        Value src = inst.getOperand(0);
        ConstValue val = getConst(src);
        ConstValue result = new ConstValue();

        if (val.state == LatticeVal.CONST) {
            switch (inst.getOpcode()) {
                case ZEXT -> result = new ConstValue(((int) val.value) & 0xFFFFFFFF);
                case SITOFP -> result = new ConstValue(((Number) val.value).floatValue());
                case FPTOSI -> result = new ConstValue(((Number) val.value).intValue());
            }
        } else {
            result.state = val.state;
        }

        if (updateLattice(inst, result)) {
            addUsersToWorklist(inst);
        }
    }

    private void evalPhi(PhiInst phi) {
        ConstValue result = new ConstValue();

        for (int i = 0; i < phi.getOperands().size(); i++) {
            BasicBlock from = phi.getIncomingBlocks().get(i);
            if (!executableBlocks.contains(from)) continue;

            Value incoming = phi.getOperands().get(i);
            result.meet(getConst(incoming));
        }

        if (updateLattice(phi, result)) {
            addUsersToWorklist(phi);
        }
    }

    private void evalCondBr(CondBranchInst cbr) {
        ConstValue cond = getConst(cbr.getOperand(0));
        if (cond.state == LatticeVal.CONST) {
            boolean takeTrue = ((int) cond.value) != 0;
            markExecutable(takeTrue ? cbr.getTrueBlock() : cbr.getFalseBlock());
        } else {
            markExecutable(cbr.getTrueBlock());
            markExecutable(cbr.getFalseBlock());
        }
    }

    private void applyChanges(Function func) {
        for (BasicBlock bb : func.getBasicBlocks()) {
            if (!executableBlocks.contains(bb)) continue;
            ListIterator<Instruction> iter = bb.getInstructions().listIterator();
            while (iter.hasNext()) {
                Instruction inst = iter.next();
                if (!valueLattice.containsKey(inst)) continue;
                ConstValue val = valueLattice.get(inst);
                if (val.state == LatticeVal.CONST) {
                    bb.replaceAllUse(inst, makeConst(val.value, inst.getType()));
                }
            }
        }
    }

    private Value makeConst(Object value, Type type) {
        if (value instanceof Integer) {
            return new ConstantInt(context.getInt32Type(), (int) value);
        } else {
            return new ConstantFloat(context.getFloatType(), (float) value);
        }
    }

    private ConstValue getConst(Value v) {
        if (v.isConstant()){
            Constant constValue = (Constant) v;
            Object value = null;
            if (constValue.getType().isIntegerType()) {
                value = (int)((ConstantInt) constValue).getValue();
            } else {
                value = ((ConstantFloat) constValue).getValue();
            }
            return valueLattice.getOrDefault(v, new ConstValue(value));
        } else if(v.isGlobalVariable()){
            System.out.println("AAAAA  " + v.getName() + "  " + v.getType().toIR() + " " + ((GlobalVariable) v).getInitializer().toIR() + " " + ((GlobalVariable) v).getInitializer().getType().toIR() + ((GlobalVariable) v).getInitializer().getType().isIntegerType());
            Constant constValue = ((GlobalVariable) v).getInitializer();
            Object value = null;
            if (constValue.getType().isIntegerType()) {
                value = (int)((ConstantInt) constValue).getValue();
            } else {
                value = ((ConstantFloat) constValue).getValue();
            }
            return valueLattice.getOrDefault(v, new ConstValue(value));
        } else {
            return valueLattice.getOrDefault(v, new ConstValue());
        }
    }

    private boolean updateLattice(Value v, ConstValue val) {
        ConstValue old = valueLattice.getOrDefault(v, new ConstValue());
        if (!Objects.equals(old.value, val.value) || old.state != val.state) {
            valueLattice.put(v, val);
            hasChanged = true;
            return true;
        }
        return false;
    }

    private void addUsersToWorklist(Instruction inst) {
        for (User user : inst.getUsers()) {
            instWorkList.add((Instruction) user);  // 强制转换是否正确？
        }
    }

    private ConstValue meetState(ConstValue a, ConstValue b) {
        ConstValue result = new ConstValue();
        result.meet(a);
        result.meet(b);
        return result;
    }

}

