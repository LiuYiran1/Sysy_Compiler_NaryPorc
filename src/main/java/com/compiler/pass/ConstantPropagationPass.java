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

        static ConstValue makeOverdef() {
            ConstValue v = new ConstValue();
            v.state = LatticeVal.OVERDEF;
            return v;
        }

        void meet(ConstValue other) {
            if (other.state == LatticeVal.OVERDEF || this.state == LatticeVal.OVERDEF || true) {
                this.state = LatticeVal.OVERDEF;
                this.value = null;
            } else if (this.state == LatticeVal.UNDEF || other.state == LatticeVal.UNDEF) {
                this.state = LatticeVal.UNDEF;
            } else if (other.state == LatticeVal.UNDEF) {
                // do nothing
            } else if (!this.value.equals(other.value)) {
                this.state = LatticeVal.OVERDEF;
                this.value = null;
            }
        }

        boolean isSmallerThan(ConstValue other) {
            if (this.state == LatticeVal.OVERDEF) {
                return false;
            } else if (other.state == LatticeVal.UNDEF) {
                return false;
            }
            return true;
        }
    }

    Map<Value, ConstValue> valueLattice = new LinkedHashMap<>();
    Set<BasicBlock> executableBlocks = new LinkedHashSet<>();

    Queue<Instruction> instWorkList = new LinkedList<>();
    Queue<BasicBlock> blockWorkList = new LinkedList<>();

    Function function;
    BasicBlock block;

    @Override
    public boolean run(Module module) {
        hasChanged = false;
        for (Function func : module.getFunctionDefs()) {
            this.function = func;
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
                this.block = bb;
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
        boolean tem = executableBlocks.add(bb);
        if (tem) {
            //System.out.println(function.getName() + "  " + bb.getName() + "   AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA   " + tem);
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
            case BR -> evalBr((BranchInst) inst);
            // 数组不管，全局变量现在跨函数了
//            case LOAD -> evalLoad((LoadInst) inst);
//            case STORE -> evalStore((StoreInst) inst);
            case RET, CALL, LOAD, STORE -> valueLattice.put(inst, new ConstValue()); // 默认不处理 call/ret
        }
    }

    private void evalStore(StoreInst inst) {
    }


    private void evalLoad(LoadInst inst) {
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
            deleteBlockFromMark(this.block);
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
            deleteBlockFromMark(this.block);
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
            deleteBlockFromMark(this.block);
        }
    }

    private void evalPhi(PhiInst phi) {
        ConstValue result = new ConstValue();
        //System.out.println(phi.toIR());
        for (int i = 0; i < phi.getOperands().size(); i++) {
            BasicBlock from = phi.getIncomingBlocks().get(i);
            //if (!executableBlocks.contains(from)) continue;

            Value incoming = phi.getOperands().get(i);

            ConstValue tem = getConst(incoming);
            result.meet(tem);
        }

        if (updateLattice(phi, result)) {
            addUsersToWorklist(phi);
            deleteBlockFromMark(this.block);
        }
    }

    private void evalBr(BranchInst br){
        if (!executableBlocks.contains(this.block)){
            executableBlocks.add(br.getTarget());
            blockWorkList.add(br.getTarget());
            return;
        }
        markExecutable(br.getTarget());
    }

    private void evalCondBr(CondBranchInst cbr) {
        ConstValue cond = getConst(cbr.getOperand(0));
        if (cond.state == LatticeVal.CONST) {
            boolean takeTrue = ((int) cond.value) != 0;
            if (!executableBlocks.contains(this.block)){
                executableBlocks.add(takeTrue ? cbr.getTrueBlock() : cbr.getFalseBlock());
                blockWorkList.add(takeTrue ? cbr.getTrueBlock() : cbr.getFalseBlock());
                return;
            }
            markExecutable(takeTrue ? cbr.getTrueBlock() : cbr.getFalseBlock());
        } else {
            if (!executableBlocks.contains(this.block)){
                executableBlocks.add(cbr.getTrueBlock());
                executableBlocks.add(cbr.getFalseBlock());
                blockWorkList.add(cbr.getTrueBlock());
                blockWorkList.add(cbr.getFalseBlock());
                return;
            }
            markExecutable(cbr.getTrueBlock());
            markExecutable(cbr.getFalseBlock());
        }
    }

    private void applyChanges(Function func) {
        for (BasicBlock bb : func.getBasicBlocks()) {
            // if (!executableBlocks.contains(bb)) continue;
            ListIterator<Instruction> iter = bb.getInstructions().listIterator();
            while (iter.hasNext()) {
                Instruction inst = iter.next();
                if (!valueLattice.containsKey(inst)) continue;
                ConstValue val = valueLattice.get(inst);
                if (val.state == LatticeVal.CONST && !inst.getUsers().isEmpty()) {
                    bb.replaceAllUse(inst, makeConst(val.value, inst.getType()));
                    hasChanged = true;
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
        if (valueLattice.containsKey(v)) {
            return valueLattice.get(v);
        }

        if (v.isConstant()) {
            Constant constValue = (Constant) v;
            Object value = null;
            if (constValue.getType().isIntegerType()) {
                value = (int) ((ConstantInt) constValue).getValue();
            } else if (constValue.getType().isFloatType()) {
                value = ((ConstantFloat) constValue).getValue();
            } else {
                throw new RuntimeException("ARRAY!!!!!");
            }
            ConstValue cv = new ConstValue(value);
            valueLattice.put(v, cv);
            return cv;

        } else if (v.isGlobalVariable()) { // 这里不对

            ConstValue cv = new ConstValue();
            cv.state = LatticeVal.OVERDEF;
            valueLattice.put(v, cv);
            return cv;
        }

        ConstValue undef = new ConstValue();
        valueLattice.put(v, undef);
        return undef;
    }


    private boolean updateLattice(Value v, ConstValue val) {
        ConstValue old = valueLattice.get(v);
        if (old == null || old.state != val.state || !Objects.equals(old.value, val.value)) {

            if (old != null && !old.isSmallerThan(val)) {
                return false;
            }

            valueLattice.put(v, new ConstValue(val)); // 深拷贝
            return true;
        }
        return false;
    }


    private void addUsersToWorklist(Instruction inst) {
//        for (User user : inst.getUsers()) {
//            instWorkList.add((Instruction) user);
//        }
    }

    private void deleteBlockFromMark(BasicBlock bb) {
        executableBlocks.remove(bb);
    }

    private ConstValue meetState(ConstValue a, ConstValue b) {
        if (a.state == LatticeVal.OVERDEF || b.state == LatticeVal.OVERDEF) {
            return ConstValue.makeOverdef(); // OVERDEF by default
        }else {
            return new ConstValue();
        }
    }


}

