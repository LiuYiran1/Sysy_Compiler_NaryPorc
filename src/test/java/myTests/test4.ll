; ModuleID = 'module'
source_filename = "module"

@a = global [2 x [3 x float]] [[3 x float] [float 1.000000e+00, float 2.000000e+00, float 3.000000e+00], [3 x float] [float 4.000000e+00, float 5.000000e+00, float 6.000000e+00]]

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

define i32 @main() {
mainEntry:
  %aArr = alloca [2 x [3 x float]], align 4
  %aFlatPtr = bitcast [2 x [3 x float]]* %aArr to float*
  %aElem0 = getelementptr float, float* %aFlatPtr, i64 0
  store float 1.000000e+00, float* %aElem0, align 4
  %aElem1 = getelementptr float, float* %aFlatPtr, i64 1
  store float 2.000000e+00, float* %aElem1, align 4
  %aElem2 = getelementptr float, float* %aFlatPtr, i64 2
  store float 3.000000e+00, float* %aElem2, align 4
  %aElem3 = getelementptr float, float* %aFlatPtr, i64 3
  store float 4.000000e+00, float* %aElem3, align 4
  %aElem4 = getelementptr float, float* %aFlatPtr, i64 4
  store float 5.000000e+00, float* %aElem4, align 4
  %aElem5 = getelementptr float, float* %aFlatPtr, i64 5
  store float 6.000000e+00, float* %aElem5, align 4
  %sum = alloca i32, align 4
  store i32 0, i32* %sum, align 4
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond

whileCond:                                        ; preds = %whileNext3, %mainEntry
  %val = load i32, i32* %i, align 4
  %lt = icmp slt i32 %val, 2
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %j = alloca i32, align 4
  store i32 0, i32* %j, align 4
  br label %whileCond1

whileNext:                                        ; preds = %whileCond
  %val15 = load i32, i32* %sum, align 4
  ret i32 %val15

whileCond1:                                       ; preds = %whileBody2, %whileBody
  %val4 = load i32, i32* %j, align 4
  %lt5 = icmp slt i32 %val4, 3
  %zextForLt6 = zext i1 %lt5 to i32
  %cond7 = icmp ne i32 %zextForLt6, 0
  br i1 %cond7, label %whileBody2, label %whileNext3

whileBody2:                                       ; preds = %whileCond1
  %val8 = load i32, i32* %sum, align 4
  %val9 = load [2 x [3 x float]], [2 x [3 x float]]* %aArr, align 4
  %val10 = load i32, i32* %i, align 4
  %val11 = load i32, i32* %j, align 4
  %array_element_ptr = getelementptr inbounds [2 x [3 x float]], [2 x [3 x float]]* %aArr, i32 0, i32 %val10, i32 %val11
  %array_element = load float, float* %array_element_ptr, align 4
  %lIToF = sitofp i32 %val8 to float
  %fAdd = fadd float %lIToF, %array_element
  %sumtem = load i32, i32* %sum, align 4
  %iLVar = fptosi float %fAdd to i32
  store i32 %iLVar, i32* %sum, align 4
  %val12 = load i32, i32* %j, align 4
  %iAdd = add i32 %val12, 1
  %jtem = load i32, i32* %j, align 4
  store i32 %iAdd, i32* %j, align 4
  br label %whileCond1

whileNext3:                                       ; preds = %whileCond1
  %val13 = load i32, i32* %i, align 4
  %iAdd14 = add i32 %val13, 1
  %item = load i32, i32* %i, align 4
  store i32 %iAdd14, i32* %i, align 4
  br label %whileCond
}
