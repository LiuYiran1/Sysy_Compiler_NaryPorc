; ModuleID = 'module'
source_filename = "module"

@RADIUS = global float 5.500000e+00
@PI = global float 0x400921FB60000000
@EPS = global float 0x3EB0C6F7A0000000
@PI_HEX = global float 0x400921FB60000000
@HEX2 = global float 7.812500e-02
@FACT = global float -3.300000e+04
@EVAL1 = global float 0x4057C21FC0000000
@EVAL2 = global float 0x4041475CE0000000
@EVAL3 = global float 0x4041475CE0000000
@CONV1 = global float 2.330000e+02
@CONV2 = global float 4.095000e+03
@MAX = global i32 1000000000
@TWO = global i32 2
@THREE = global i32 3
@FIVE = global i32 5

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

define float @float_abs(float %0) {
float_absEntry:
  %x = alloca float, align 4
  store float %0, float* %x, align 4
  %val = load float, float* %x, align 4
  %olt = fcmp olt float %val, 0.000000e+00
  %zextForOLt = zext i1 %olt to i32
  %cond = icmp ne i32 %zextForOLt, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %float_absEntry
  %val1 = load float, float* %x, align 4
  %neg_float = fsub float 0.000000e+00, %val1
  ret float %neg_float

ifFalse:                                          ; preds = %float_absEntry
  br label %ifNext

ifNext:                                           ; preds = %ifFalse
  %val2 = load float, float* %x, align 4
  ret float %val2
}

define float @circle_area(i32 %0) {
circle_areaEntry:
  %radius = alloca i32, align 4
  store i32 %0, i32* %radius, align 4
  %val = load i32, i32* %radius, align 4
  %rIToF = sitofp i32 %val to float
  %fMul = fmul float 0x400921FB60000000, %rIToF
  %val1 = load i32, i32* %radius, align 4
  %rIToF2 = sitofp i32 %val1 to float
  %fMul3 = fmul float %fMul, %rIToF2
  %val4 = load i32, i32* %radius, align 4
  %val5 = load i32, i32* %radius, align 4
  %iMul = mul i32 %val4, %val5
  %lIToF = sitofp i32 %iMul to float
  %fMul6 = fmul float %lIToF, 0x400921FB60000000
  %fAdd = fadd float %fMul3, %fMul6
  %fDiv = fdiv float %fAdd, 2.000000e+00
  ret float %fDiv
}

define i32 @float_eq(float %0, float %1) {
float_eqEntry:
  %a = alloca float, align 4
  store float %0, float* %a, align 4
  %b = alloca float, align 4
  store float %1, float* %b, align 4
  %val = load float, float* %a, align 4
  %val1 = load float, float* %b, align 4
  %fSub = fsub float %val, %val1
  %2 = call float @float_abs(float %fSub)
  %olt = fcmp olt float %2, 0x3EB0C6F7A0000000
  %zextForOLt = zext i1 %olt to i32
  %cond = icmp ne i32 %zextForOLt, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %float_eqEntry
  ret i32 1

ifFalse:                                          ; preds = %float_eqEntry
  ret i32 0

ifNext:                                           ; No predecessors!
  ret i32 0
}

define void @error() {
errorEntry:
  call void @putch(i32 101)
  call void @putch(i32 114)
  call void @putch(i32 114)
  call void @putch(i32 111)
  call void @putch(i32 114)
  call void @putch(i32 10)
  ret void
}

define void @ok() {
okEntry:
  call void @putch(i32 111)
  call void @putch(i32 107)
  call void @putch(i32 10)
  ret void
}

define void @assert(i32 %0) {
assertEntry:
  %cond = alloca i32, align 4
  store i32 %0, i32* %cond, align 4
  %val = load i32, i32* %cond, align 4
  %cmpI1 = icmp eq i32 %val, 0
  %cmpI32 = zext i1 %cmpI1 to i32
  %cond1 = icmp ne i32 %cmpI32, 0
  br i1 %cond1, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %assertEntry
  call void @error()
  br label %ifNext

ifFalse:                                          ; preds = %assertEntry
  call void @ok()
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  ret void
}

define void @assert_not(i32 %0) {
assert_notEntry:
  %cond = alloca i32, align 4
  store i32 %0, i32* %cond, align 4
  %val = load i32, i32* %cond, align 4
  %cond1 = icmp ne i32 %val, 0
  br i1 %cond1, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %assert_notEntry
  call void @error()
  br label %ifNext

ifFalse:                                          ; preds = %assert_notEntry
  call void @ok()
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  ret void
}

define i32 @main() {
mainEntry:
  %0 = call i32 @float_eq(float 7.812500e-02, float -3.300000e+04)
  call void @assert_not(i32 %0)
  %1 = call i32 @float_eq(float 0x4057C21FC0000000, float 0x4041475CE0000000)
  call void @assert_not(i32 %1)
  %2 = call i32 @float_eq(float 0x4041475CE0000000, float 0x4041475CE0000000)
  call void @assert(i32 %2)
  %3 = call float @circle_area(float 5.500000e+00)
  %4 = call float @circle_area(i32 5)
  %5 = call i32 @float_eq(float %3, float %4)
  call void @assert(i32 %5)
  %6 = call i32 @float_eq(float 2.330000e+02, float 4.095000e+03)
  call void @assert_not(i32 %6)
  call void @ok()
  br i1 true, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %mainEntry
  call void @ok()
  br label %ifNext

ifFalse:                                          ; preds = %mainEntry
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  br i1 true, label %ifTrue1, label %ifFalse2

ifTrue1:                                          ; preds = %ifNext
  call void @ok()
  br label %ifNext3

ifFalse2:                                         ; preds = %ifNext
  br label %ifNext3

ifNext3:                                          ; preds = %ifFalse2, %ifTrue1
  br i1 false, label %andTrue, label %andFalse

ifTrue4:                                          ; preds = %andNext
  call void @error()
  br label %ifNext6

ifFalse5:                                         ; preds = %andNext
  br label %ifNext6

ifNext6:                                          ; preds = %ifFalse5, %ifTrue4
  br i1 false, label %orTrue, label %orFalse

andTrue:                                          ; preds = %ifNext3
  br label %andNext

andFalse:                                         ; preds = %ifNext3
  br label %andNext

andNext:                                          ; preds = %andFalse, %andTrue
  %andPhi = phi i32 [ 0, %andFalse ], [ 3, %andTrue ]
  %cond = icmp ne i32 %andPhi, 0
  br i1 %cond, label %ifTrue4, label %ifFalse5

ifTrue7:                                          ; preds = %orNext
  call void @ok()
  br label %ifNext9

ifFalse8:                                         ; preds = %orNext
  br label %ifNext9

ifNext9:                                          ; preds = %ifFalse8, %ifTrue7
  %i = alloca i32, align 4
  store i32 1, i32* %i, align 4
  %p = alloca i32, align 4
  store i32 0, i32* %p, align 4
  %arrArr = alloca [10 x float], align 4
  %arrFlatPtr = bitcast [10 x float]* %arrArr to float*
  %arrElem0 = getelementptr float, float* %arrFlatPtr, i64 0
  store float 1.000000e+00, float* %arrElem0, align 4
  %arrElem1 = getelementptr float, float* %arrFlatPtr, i64 1
  store float 2.000000e+00, float* %arrElem1, align 4
  %arrElem2 = getelementptr float, float* %arrFlatPtr, i64 2
  store float 0.000000e+00, float* %arrElem2, align 4
  %arrElem3 = getelementptr float, float* %arrFlatPtr, i64 3
  store float 0.000000e+00, float* %arrElem3, align 4
  %arrElem4 = getelementptr float, float* %arrFlatPtr, i64 4
  store float 0.000000e+00, float* %arrElem4, align 4
  %arrElem5 = getelementptr float, float* %arrFlatPtr, i64 5
  store float 0.000000e+00, float* %arrElem5, align 4
  %arrElem6 = getelementptr float, float* %arrFlatPtr, i64 6
  store float 0.000000e+00, float* %arrElem6, align 4
  %arrElem7 = getelementptr float, float* %arrFlatPtr, i64 7
  store float 0.000000e+00, float* %arrElem7, align 4
  %arrElem8 = getelementptr float, float* %arrFlatPtr, i64 8
  store float 0.000000e+00, float* %arrElem8, align 4
  %arrElem9 = getelementptr float, float* %arrFlatPtr, i64 9
  store float 0.000000e+00, float* %arrElem9, align 4
  %len = alloca i32, align 4
  %7 = call i32 @getfarray([10 x float]* %arrArr)
  store i32 %7, i32* %len, align 4
  br label %whileCond

orTrue:                                           ; preds = %ifNext6
  br label %orNext

orFalse:                                          ; preds = %ifNext6
  br label %orNext

orNext:                                           ; preds = %orFalse, %orTrue
  %orPhi = phi float [ 1.000000e+00, %orTrue ], [ 0x3FD3333340000000, %orFalse ]
  %cond10 = fcmp one float %orPhi, 0.000000e+00
  br i1 %cond10, label %ifTrue7, label %ifFalse8

whileCond:                                        ; preds = %whileBody, %ifNext9
  %val = load i32, i32* %i, align 4
  %lt = icmp slt i32 %val, 1000000000
  %zextForLt = zext i1 %lt to i32
  %cond11 = icmp ne i32 %zextForLt, 0
  br i1 %cond11, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %input = alloca float, align 4
  %8 = call float @getfloat()
  store float %8, float* %input, align 4
  %area = alloca float, align 4
  %val12 = load float, float* %input, align 4
  %fMul = fmul float 0x400921FB60000000, %val12
  %val13 = load float, float* %input, align 4
  %fMul14 = fmul float %fMul, %val13
  store float %fMul14, float* %area, align 4
  %area_trunc = alloca float, align 4
  %val15 = load float, float* %input, align 4
  %9 = call float @circle_area(float %val15)
  store float %9, float* %area_trunc, align 4
  %val16 = load i32, i32* %p, align 4
  %idx64 = zext i32 %val16 to i64
  %arrayidx0 = getelementptr inbounds [10 x float], [10 x float]* %arrArr, i64 0, i64 %idx64
  %array_element = load float, float* %arrayidx0, align 4
  %val17 = load float, float* %input, align 4
  %fAdd = fadd float %array_element, %val17
  %val18 = load i32, i32* %p, align 4
  %idx6419 = zext i32 %val18 to i64
  %arrayidx020 = getelementptr inbounds [10 x float], [10 x float]* %arrArr, i64 0, i64 %idx6419
  store float %fAdd, float* %arrayidx020, align 4
  %val21 = load float, float* %area, align 4
  call void @putfloat(float %val21)
  call void @putch(i32 32)
  %val22 = load float, float* %area_trunc, align 4
  call void @putint(float %val22)
  call void @putch(i32 10)
  %val23 = load i32, i32* %i, align 4
  %lIToF = sitofp i32 %val23 to float
  %fMul24 = fmul float %lIToF, 1.000000e+01
  %iLVar = fptosi float %fMul24 to i32
  store i32 %iLVar, i32* %i, align 4
  %val25 = load i32, i32* %p, align 4
  %iAdd = add i32 %val25, 1
  store i32 %iAdd, i32* %p, align 4
  br label %whileCond

whileNext:                                        ; preds = %whileCond
  %val26 = load i32, i32* %len, align 4
  call void @putfarray(i32 %val26, [10 x float]* %arrArr)
  ret i32 0
}
