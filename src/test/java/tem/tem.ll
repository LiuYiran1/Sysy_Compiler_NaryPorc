; ModuleID = 'module'
source_filename = "module"
target datalayout = "e-m:e-p:64:64-i64:64-i128:128-n64-S128"
target triple = "riscv64-unknown-elf"

@COUNT = global i32 5000

declare i32 @getint()

declare i32 @getch()

declare float @getfloat()

declare i32 @getarray(i32*)

declare i32 @getfarray(float*)

declare void @putint(i32)

declare void @putch(i32)

declare void @putfloat(float)

declare void @putarray(i32, i32*)

declare void @putfarray(i32, float*)

declare void @starttime()

declare void @stoptime()

define float @loop(float* %0, float* %1, i32 %2) {
loopEntry:
  br label %whileCond

whileCond:                                        ; preds = %whileBody, %loopEntry
  %i.0 = phi i32 [ 0, %loopEntry ], [ %iAdd, %whileBody ]
  %accum.0 = phi float [ 0.000000e+00, %loopEntry ], [ %fAdd, %whileBody ]
  %lt = icmp slt i32 %i.0, %2
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %idx64 = zext i32 %i.0 to i64
  %arrayidx0 = getelementptr inbounds float, float* %0, i64 %idx64
  %array_element = load float, float* %arrayidx0, align 4
  %idx645 = zext i32 %i.0 to i64
  %arrayidx07 = getelementptr inbounds float, float* %1, i64 %idx645
  %array_element8 = load float, float* %arrayidx07, align 4
  %fMul = fmul float %array_element, %array_element8
  %fAdd = fadd float %accum.0, %fMul
  %iAdd = add i32 %i.0, 1
  br label %whileCond

whileNext:                                        ; preds = %whileCond
  ret float %accum.0
}

define i32 @main() {
mainEntry:
  %xArr = alloca [4900 x float], align 4
  %yArr = alloca [4900 x float], align 4
  call void @starttime()
  br label %whileCond

whileCond:                                        ; preds = %whileNext9, %mainEntry
  %a.0 = phi float [ 0.000000e+00, %mainEntry ], [ %a.1, %whileNext9 ]
  %total.0 = phi float [ 0.000000e+00, %mainEntry ], [ %fAdd29, %whileNext9 ]
  %b.0 = phi float [ 1.000000e+00, %mainEntry ], [ %b.1, %whileNext9 ]
  %i.0 = phi i32 [ 0, %mainEntry ], [ %iAdd31, %whileNext9 ]
  %val1 = load i32, i32* @COUNT, align 4
  %lt = icmp slt i32 %i.0, %val1
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %iRem = srem i32 %i.0, 10
  %cond3 = icmp ne i32 %iRem, 0
  %fAdd = fadd float %a.0, 0x3FB99999A0000000
  %fAdd6 = fadd float %b.0, 0x3FC99999A0000000
  %a.1 = select i1 %cond3, float 0.000000e+00, float %fAdd
  %b.1 = select i1 %cond3, float 1.000000e+00, float %fAdd6
  br label %whileCond7

whileNext:                                        ; preds = %whileCond
  call void @stoptime()
  %final = alloca float, align 4
  %fSub = fsub float %total.0, 0x42E64A8760000000
  store float %fSub, float* %final, align 4
  %val36 = load float, float* %final, align 4
  %ole = fcmp ole float %val36, 0x3EB0C6F7A0000000
  %zextForOLe = zext i1 %ole to i32
  %cond37 = icmp ne i32 %zextForOLe, 0
  %val38 = load float, float* %final, align 4
  %oge = fcmp oge float %val38, 0xBEB0C6F7A0000000
  %zextForOGe = zext i1 %oge to i32
  %andPhi = select i1 %cond37, i32 %zextForOGe, i32 0
  %cond39 = icmp ne i32 %andPhi, 0
  br i1 %cond39, label %ifTrue33, label %ifFalse34

whileCond7:                                       ; preds = %whileBody8, %whileBody
  %j.0 = phi i32 [ 0, %whileBody ], [ %iAdd, %whileBody8 ]
  %lt12 = icmp slt i32 %j.0, 4900
  %zextForLt13 = zext i1 %lt12 to i32
  %cond14 = icmp ne i32 %zextForLt13, 0
  br i1 %cond14, label %whileBody8, label %whileNext9

whileBody8:                                       ; preds = %whileCond7
  %rIToF = sitofp i32 %j.0 to float
  %fAdd17 = fadd float %a.1, %rIToF
  %idx64 = zext i32 %j.0 to i64
  %arrayidx0 = getelementptr inbounds [4900 x float], [4900 x float]* %xArr, i64 0, i64 %idx64
  store float %fAdd17, float* %arrayidx0, align 4
  %rIToF21 = sitofp i32 %j.0 to float
  %fAdd22 = fadd float %b.1, %rIToF21
  %idx6424 = zext i32 %j.0 to i64
  %arrayidx025 = getelementptr inbounds [4900 x float], [4900 x float]* %yArr, i64 0, i64 %idx6424
  store float %fAdd22, float* %arrayidx025, align 4
  %iAdd = add i32 %j.0, 1
  br label %whileCond7

whileNext9:                                       ; preds = %whileCond7
  %0 = call float @loop([4900 x float]* %xArr, [4900 x float]* %yArr, i32 4900)
  %fAdd29 = fadd float %total.0, %0
  %iAdd31 = add i32 %i.0, 1
  br label %whileCond

common.ret:                                       ; preds = %ifFalse34, %ifTrue33
  %common.ret.op = phi i32 [ 0, %ifTrue33 ], [ 1, %ifFalse34 ]
  ret i32 %common.ret.op

ifTrue33:                                         ; preds = %whileNext
  call void @putint(i32 10)
  br label %common.ret

ifFalse34:                                        ; preds = %whileNext
  call void @putint(i32 1)
  br label %common.ret
}
