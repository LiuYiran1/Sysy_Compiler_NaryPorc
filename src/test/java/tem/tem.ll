; ModuleID = 'module'
source_filename = "module"
target datalayout = "e-m:e-p:64:64-i64:64-i128:128-n64-S128"
target triple = "riscv64-unknown-elf"

@global_counter = global i32 0
@global_float = global float 0x40091EB860000000
@global_array = global [5 x i32] [i32 1, i32 2, i32 3, i32 4, i32 5]
@global_float_array = global [3 x float] [float 0x3FB99999A0000000, float 0x3FC99999A0000000, float 0x3FD3333340000000]
@zero_array = global [5 x i32] zeroinitializer
@zero_float_array = global [3 x float] zeroinitializer

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

define void @test_function() {
test_functionEntry:
  %gt = icmp sgt i32 0, 10
  %zextForGt = zext i1 %gt to i32
  %cond = icmp ne i32 %zextForGt, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %test_functionEntry
  %val1 = load i32, i32* @global_counter, align 4
  %iAdd = add i32 %val1, 0
  store i32 %iAdd, i32* @global_counter, align 4
  br label %ifNext

ifFalse:                                          ; preds = %test_functionEntry
  %val3 = load float, float* @global_float, align 4
  %fAdd = fadd float %val3, 1.000000e+00
  store float %fAdd, float* @global_float, align 4
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  ret void
}

define i32 @process_data(i32 %0) {
process_dataEntry:
  %iMul = mul i32 %0, 2
  %gt = icmp sgt i32 %0, 10
  %zextForGt = zext i1 %gt to i32
  %cond = icmp ne i32 %zextForGt, 0
  %iAdd = add i32 %iMul, 5
  %iSub = sub i32 %iMul, 3
  %common.ret.op = select i1 %cond, i32 %iAdd, i32 %iSub
  ret i32 %common.ret.op
}

define float @calculate_average(i32* %0, i32 %1) {
calculate_averageEntry:
  br label %whileCond

whileCond:                                        ; preds = %whileBody, %calculate_averageEntry
  %sum.0 = phi i32 [ 0, %calculate_averageEntry ], [ %iAdd, %whileBody ]
  %j.0 = phi i32 [ 0, %calculate_averageEntry ], [ %iAdd5, %whileBody ]
  %lt = icmp slt i32 %j.0, %1
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %idx64 = zext i32 %j.0 to i64
  %arrayidx0 = getelementptr inbounds i32, i32* %0, i64 %idx64
  %array_element = load i32, i32* %arrayidx0, align 4
  %iAdd = add i32 %array_element, %sum.0
  %iAdd5 = add i32 %j.0, 1
  br label %whileCond

whileNext:                                        ; preds = %whileCond
  %iDiv = sdiv i32 %sum.0, %1
  %fRet = sitofp i32 %iDiv to float
  ret float %fRet
}

define i32 @main() {
mainEntry:
  %int_arrayArr = alloca [5 x i32], align 4
  %array_element = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @global_array, i64 0, i64 0), align 4
  %int_arrayFlatPtr = bitcast [5 x i32]* %int_arrayArr to i32*
  %int_arrayElem0 = getelementptr i32, i32* %int_arrayFlatPtr, i64 0
  store i32 %array_element, i32* %int_arrayElem0, align 4
  %int_arrayElem1 = getelementptr i32, i32* %int_arrayFlatPtr, i64 1
  store i32 10, i32* %int_arrayElem1, align 4
  %int_arrayElem2 = getelementptr i32, i32* %int_arrayFlatPtr, i64 2
  store i32 15, i32* %int_arrayElem2, align 4
  %int_arrayElem3 = getelementptr i32, i32* %int_arrayFlatPtr, i64 3
  store i32 20, i32* %int_arrayElem3, align 4
  %int_arrayElem4 = getelementptr i32, i32* %int_arrayFlatPtr, i64 4
  store i32 25, i32* %int_arrayElem4, align 4
  %float_arrayArr = alloca [3 x float], align 4
  %float_arrayFlatPtr = bitcast [3 x float]* %float_arrayArr to float*
  %float_arrayElem0 = getelementptr float, float* %float_arrayFlatPtr, i64 0
  store float 0x3FF19999A0000000, float* %float_arrayElem0, align 4
  %float_arrayElem1 = getelementptr float, float* %float_arrayFlatPtr, i64 1
  store float 0x40019999A0000000, float* %float_arrayElem1, align 4
  %float_arrayElem2 = getelementptr float, float* %float_arrayFlatPtr, i64 2
  store float 0x400A666660000000, float* %float_arrayElem2, align 4
  %int_array_2dArr = alloca [2 x [3 x i32]], align 4
  %int_array_2dFlatPtr = bitcast [2 x [3 x i32]]* %int_array_2dArr to i32*
  %int_array_2dElem0 = getelementptr i32, i32* %int_array_2dFlatPtr, i64 0
  store i32 1, i32* %int_array_2dElem0, align 4
  %int_array_2dElem1 = getelementptr i32, i32* %int_array_2dFlatPtr, i64 1
  store i32 2, i32* %int_array_2dElem1, align 4
  %int_array_2dElem2 = getelementptr i32, i32* %int_array_2dFlatPtr, i64 2
  store i32 3, i32* %int_array_2dElem2, align 4
  %int_array_2dElem3 = getelementptr i32, i32* %int_array_2dFlatPtr, i64 3
  store i32 4, i32* %int_array_2dElem3, align 4
  %int_array_2dElem4 = getelementptr i32, i32* %int_array_2dFlatPtr, i64 4
  store i32 5, i32* %int_array_2dElem4, align 4
  %int_array_2dElem5 = getelementptr i32, i32* %int_array_2dFlatPtr, i64 5
  store i32 6, i32* %int_array_2dElem5, align 4
  %int_array_3dArr = alloca [2 x [2 x [3 x i32]]], align 4
  %int_array_3dFlatPtr = bitcast [2 x [2 x [3 x i32]]]* %int_array_3dArr to i32*
  %int_array_3dElem0 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 0
  store i32 1, i32* %int_array_3dElem0, align 4
  %int_array_3dElem1 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 1
  store i32 2, i32* %int_array_3dElem1, align 4
  %int_array_3dElem2 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 2
  store i32 3, i32* %int_array_3dElem2, align 4
  %int_array_3dElem3 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 3
  store i32 4, i32* %int_array_3dElem3, align 4
  %int_array_3dElem4 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 4
  store i32 5, i32* %int_array_3dElem4, align 4
  %int_array_3dElem5 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 5
  store i32 6, i32* %int_array_3dElem5, align 4
  %int_array_3dElem6 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 6
  store i32 7, i32* %int_array_3dElem6, align 4
  %int_array_3dElem7 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 7
  store i32 8, i32* %int_array_3dElem7, align 4
  %int_array_3dElem8 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 8
  store i32 9, i32* %int_array_3dElem8, align 4
  %int_array_3dElem9 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 9
  store i32 10, i32* %int_array_3dElem9, align 4
  %int_array_3dElem10 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 10
  store i32 11, i32* %int_array_3dElem10, align 4
  %int_array_3dElem11 = getelementptr i32, i32* %int_array_3dFlatPtr, i64 11
  store i32 12, i32* %int_array_3dElem11, align 4
  %float_array_2dArr = alloca [2 x [3 x float]], align 4
  %float_array_2dFlatPtr = bitcast [2 x [3 x float]]* %float_array_2dArr to float*
  %float_array_2dElem0 = getelementptr float, float* %float_array_2dFlatPtr, i64 0
  store float 0x3FF19999A0000000, float* %float_array_2dElem0, align 4
  %float_array_2dElem1 = getelementptr float, float* %float_array_2dFlatPtr, i64 1
  store float 0x40019999A0000000, float* %float_array_2dElem1, align 4
  %float_array_2dElem2 = getelementptr float, float* %float_array_2dFlatPtr, i64 2
  store float 0x400A666660000000, float* %float_array_2dElem2, align 4
  %float_array_2dElem3 = getelementptr float, float* %float_array_2dFlatPtr, i64 3
  store float 0x40119999A0000000, float* %float_array_2dElem3, align 4
  %float_array_2dElem4 = getelementptr float, float* %float_array_2dFlatPtr, i64 4
  store float 5.500000e+00, float* %float_array_2dElem4, align 4
  %float_array_2dElem5 = getelementptr float, float* %float_array_2dFlatPtr, i64 5
  store float 0x401A666660000000, float* %float_array_2dElem5, align 4
  %val = load i32, i32* @global_counter, align 4
  %iAdd = add i32 %val, 10
  store i32 %iAdd, i32* @global_counter, align 4
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond

whileCond:                                        ; preds = %ifNext34, %ifTrue32, %mainEntry
  %local_int.1 = phi i32 [ 10, %mainEntry ], [ %local_int.2, %ifTrue32 ], [ %local_int.2, %ifNext34 ]
  %val4 = load i32, i32* %i, align 4
  %lt = icmp slt i32 %val4, 5
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %val8 = load i32, i32* %i, align 4
  %idx64 = zext i32 %val8 to i64
  %arrayidx0 = getelementptr inbounds [5 x i32], [5 x i32]* %int_arrayArr, i64 0, i64 %idx64
  %array_element9 = load i32, i32* %arrayidx0, align 4
  %gt = icmp sgt i32 %array_element9, 15
  %zextForGt = zext i1 %gt to i32
  %cond10 = icmp ne i32 %zextForGt, 0
  br i1 %cond10, label %ifTrue5, label %ifFalse6

whileNext:                                        ; preds = %ifNext7, %whileCond
  %avg = alloca float, align 4
  %0 = call float @calculate_average([5 x i32]* %int_arrayArr, i32 5)
  store float %0, float* %avg, align 4
  %val46 = load float, float* %avg, align 4
  %fAdd48 = fadd float %val46, 2.500000e+00
  %val49 = load i32, i32* @global_counter, align 4
  %lIToF = sitofp i32 %val49 to float
  %fAdd51 = fadd float %lIToF, %fAdd48
  %iRet = fptosi float %fAdd51 to i32
  ret i32 %iRet

ifTrue5:                                          ; preds = %whileBody
  %val11 = load i32, i32* @global_counter, align 4
  %val12 = load i32, i32* %i, align 4
  %idx6413 = zext i32 %val12 to i64
  %arrayidx014 = getelementptr inbounds [2 x [3 x i32]], [2 x [3 x i32]]* %int_array_2dArr, i64 0, i64 1
  %arrayidx1 = getelementptr inbounds [3 x i32], [3 x i32]* %arrayidx014, i64 0, i64 %idx6413
  %array_element15 = load i32, i32* %arrayidx1, align 4
  %iAdd16 = add i32 %val11, %array_element15
  store i32 %iAdd16, i32* @global_counter, align 4
  br label %ifNext7

ifFalse6:                                         ; preds = %whileBody
  %result17 = alloca i32, align 4
  %val18 = load i32, i32* %i, align 4
  %idx6419 = zext i32 %val18 to i64
  %arrayidx020 = getelementptr inbounds [5 x i32], [5 x i32]* %int_arrayArr, i64 0, i64 %idx6419
  %array_element21 = load i32, i32* %arrayidx020, align 4
  %1 = call i32 @process_data(i32 %array_element21)
  store i32 %1, i32* %result17, align 4
  %val22 = load i32, i32* %result17, align 4
  %iAdd24 = add i32 %val22, %local_int.1
  br label %ifNext7

ifNext7:                                          ; preds = %ifFalse6, %ifTrue5
  %local_int.2 = phi i32 [ %local_int.1, %ifTrue5 ], [ %iAdd24, %ifFalse6 ]
  %val28 = load i32, i32* @global_counter, align 4
  %gt29 = icmp sgt i32 %val28, 40
  %zextForGt30 = zext i1 %gt29 to i32
  %cond31 = icmp ne i32 %zextForGt30, 0
  br i1 %cond31, label %whileNext, label %ifNext27

ifNext27:                                         ; preds = %ifNext7
  %val35 = load i32, i32* %i, align 4
  %eq = icmp eq i32 %val35, 2
  %zextForEq = zext i1 %eq to i32
  %cond36 = icmp ne i32 %zextForEq, 0
  br i1 %cond36, label %ifTrue32, label %ifNext34

ifTrue32:                                         ; preds = %ifNext27
  %val37 = load i32, i32* %i, align 4
  %iAdd38 = add i32 %val37, 1
  store i32 %iAdd38, i32* %i, align 4
  br label %whileCond

ifNext34:                                         ; preds = %ifNext27
  %val39 = load i32, i32* %i, align 4
  %iRem = srem i32 %val39, 3
  %idx6440 = zext i32 %iRem to i64
  %arrayidx041 = getelementptr inbounds [3 x float], [3 x float]* %float_arrayArr, i64 0, i64 %idx6440
  %array_element42 = load float, float* %arrayidx041, align 4
  %val43 = load float, float* @global_float, align 4
  %fAdd = fadd float %array_element42, %val43
  store float %fAdd, float* @global_float, align 4
  %val44 = load i32, i32* %i, align 4
  %iAdd45 = add i32 %val44, 1
  store i32 %iAdd45, i32* %i, align 4
  br label %whileCond
}
