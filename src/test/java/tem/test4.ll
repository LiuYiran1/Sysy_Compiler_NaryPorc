; ModuleID = 'module'
source_filename = "module"

@f = global [2 x [3 x float]] zeroinitializer

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 2, i32* %a, align 4
  %b = alloca float, align 4
  store float 1.500000e+00, float* %b, align 4
  %c = alloca float, align 4
  store float 2.500000e+00, float* %c, align 4
  %dArr = alloca [2 x [5 x i32]], align 4
  %dFlatPtr = bitcast [2 x [5 x i32]]* %dArr to i32*
  %dElem0 = getelementptr i32, i32* %dFlatPtr, i64 0
  store i32 1, i32* %dElem0, align 4
  %dElem1 = getelementptr i32, i32* %dFlatPtr, i64 1
  store i32 2, i32* %dElem1, align 4
  %dElem2 = getelementptr i32, i32* %dFlatPtr, i64 2
  store i32 0, i32* %dElem2, align 4
  %dElem3 = getelementptr i32, i32* %dFlatPtr, i64 3
  store i32 0, i32* %dElem3, align 4
  %dElem4 = getelementptr i32, i32* %dFlatPtr, i64 4
  store i32 0, i32* %dElem4, align 4
  %dElem5 = getelementptr i32, i32* %dFlatPtr, i64 5
  store i32 0, i32* %dElem5, align 4
  %dElem6 = getelementptr i32, i32* %dFlatPtr, i64 6
  store i32 0, i32* %dElem6, align 4
  %dElem7 = getelementptr i32, i32* %dFlatPtr, i64 7
  store i32 0, i32* %dElem7, align 4
  %dElem8 = getelementptr i32, i32* %dFlatPtr, i64 8
  store i32 0, i32* %dElem8, align 4
  %dElem9 = getelementptr i32, i32* %dFlatPtr, i64 9
  store i32 0, i32* %dElem9, align 4
  %eArr = alloca [3 x [3 x float]], align 4
  %eFlatPtr = bitcast [3 x [3 x float]]* %eArr to float*
  %eElem0 = getelementptr float, float* %eFlatPtr, i64 0
  store float 0.000000e+00, float* %eElem0, align 4
  %eElem1 = getelementptr float, float* %eFlatPtr, i64 1
  store float 0.000000e+00, float* %eElem1, align 4
  %eElem2 = getelementptr float, float* %eFlatPtr, i64 2
  store float 0.000000e+00, float* %eElem2, align 4
  %eElem3 = getelementptr float, float* %eFlatPtr, i64 3
  store float 0.000000e+00, float* %eElem3, align 4
  %eElem4 = getelementptr float, float* %eFlatPtr, i64 4
  store float 0.000000e+00, float* %eElem4, align 4
  %eElem5 = getelementptr float, float* %eFlatPtr, i64 5
  store float 0.000000e+00, float* %eElem5, align 4
  %eElem6 = getelementptr float, float* %eFlatPtr, i64 6
  store float 0.000000e+00, float* %eElem6, align 4
  %eElem7 = getelementptr float, float* %eFlatPtr, i64 7
  store float 0.000000e+00, float* %eElem7, align 4
  %eElem8 = getelementptr float, float* %eFlatPtr, i64 8
  store float 0.000000e+00, float* %eElem8, align 4
  %val = load float, float* %b, align 4
  %val1 = load float, float* %c, align 4
  %ogt = fcmp ogt float %val, %val1
  %zextForOGt = zext i1 %ogt to i32
  %cond = icmp ne i32 %zextForOGt, 0
  br i1 %cond, label %orTrue, label %orFalse

ifTrue:                                           ; preds = %orNext6
  ret i32 2
  br label %ifNext

ifFalse:                                          ; preds = %orNext6
  %val14 = load i32, i32* %a, align 4
  %val15 = load float, float* %b, align 4
  %lIToF = sitofp i32 %val14 to float
  %ole = fcmp ole float %lIToF, %val15
  %zextForOLe = zext i1 %ole to i32
  %cond19 = icmp ne i32 %zextForOLe, 0
  br i1 %cond19, label %orTrue16, label %orFalse17

ifNext:                                           ; preds = %ifNext13, %ifTrue
  ret i32 0

orTrue:                                           ; preds = %mainEntry
  br label %orNext

orFalse:                                          ; preds = %mainEntry
  %val2 = load float, float* %b, align 4
  %val3 = load float, float* %c, align 4
  %fSub = fsub float %val2, %val3
  br label %orNext

orNext:                                           ; preds = %orFalse, %orTrue
  %orPhi = phi float [ 1.000000e+00, %orTrue ], [ %fSub, %orFalse ]
  %cond7 = fcmp one float %orPhi, 0.000000e+00
  br i1 %cond7, label %orTrue4, label %orFalse5

orTrue4:                                          ; preds = %orNext
  br label %orNext6

orFalse5:                                         ; preds = %orNext
  %val8 = load [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, align 4
  %array_element_ptr = getelementptr inbounds [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, i32 0, i32 1, i32 2
  %array_element = load i32, i32* %array_element_ptr, align 4
  br label %orNext6

orNext6:                                          ; preds = %orFalse5, %orTrue4
  %orPhi9 = phi i32 [ 1, %orTrue4 ], [ %array_element, %orFalse5 ]
  %cond10 = icmp ne i32 %orPhi9, 0
  br i1 %cond10, label %ifTrue, label %ifFalse

ifTrue11:                                         ; preds = %orNext18
  %val30 = load float, float* %b, align 4
  %cond31 = fcmp one float %val30, 0.000000e+00
  br i1 %cond31, label %ifTrue27, label %ifFalse28

ifFalse12:                                        ; preds = %orNext18
  %val32 = load [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, align 4
  %array_element_ptr33 = getelementptr inbounds [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, i32 0, i32 0, i32 1
  %array_element34 = load i32, i32* %array_element_ptr33, align 4
  %val35 = load [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, align 4
  %array_element_ptr36 = getelementptr inbounds [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, i32 0, i32 0, i32 0
  %array_element37 = load i32, i32* %array_element_ptr36, align 4
  %iAdd = add i32 %array_element34, %array_element37
  %lIToF38 = sitofp i32 %iAdd to float
  %fAdd = fadd float %lIToF38, 3.500000e+00
  %etem = load [3 x [3 x float]], [3 x [3 x float]]* %eArr, align 4
  %val39 = load [3 x [3 x float]], [3 x [3 x float]]* %eArr, align 4
  %array_element_ptr40 = getelementptr inbounds [3 x [3 x float]], [3 x [3 x float]]* %eArr, i32 0, i32 2, i32 1
  store float %fAdd, float* %array_element_ptr40, align 4
  %val41 = load [3 x [3 x float]], [3 x [3 x float]]* %eArr, align 4
  %array_element_ptr42 = getelementptr inbounds [3 x [3 x float]], [3 x [3 x float]]* %eArr, i32 0, i32 1, i32 1
  %array_element43 = load float, float* %array_element_ptr42, align 4
  %val44 = load [3 x [3 x float]], [3 x [3 x float]]* %eArr, align 4
  %array_element_ptr45 = getelementptr inbounds [3 x [3 x float]], [3 x [3 x float]]* %eArr, i32 0, i32 2, i32 1
  %array_element46 = load float, float* %array_element_ptr45, align 4
  %val47 = load [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, align 4
  %array_element_ptr48 = getelementptr inbounds [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, i32 0, i32 1, i32 1
  %array_element49 = load i32, i32* %array_element_ptr48, align 4
  %rIToF50 = sitofp i32 %array_element49 to float
  %fMul = fmul float %array_element46, %rIToF50
  %fAdd51 = fadd float %array_element43, %fMul
  %ftem = load [2 x [3 x float]], [2 x [3 x float]]* @f, align 4
  %val52 = load [2 x [3 x float]], [2 x [3 x float]]* @f, align 4
  store float %fAdd51, float* getelementptr inbounds ([2 x [3 x float]], [2 x [3 x float]]* @f, i32 0, i32 1, i32 1), align 4
  %val53 = load [3 x [3 x float]], [3 x [3 x float]]* %eArr, align 4
  %array_element_ptr54 = getelementptr inbounds [3 x [3 x float]], [3 x [3 x float]]* %eArr, i32 0, i32 2, i32 1
  %array_element55 = load float, float* %array_element_ptr54, align 4
  %val56 = load [2 x [3 x float]], [2 x [3 x float]]* @f, align 4
  %array_element57 = load float, float* getelementptr inbounds ([2 x [3 x float]], [2 x [3 x float]]* @f, i32 0, i32 1, i32 1), align 4
  %fDiv = fdiv float %array_element55, %array_element57
  %dtem = load [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, align 4
  %val58 = load [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, align 4
  %array_element_ptr59 = getelementptr inbounds [2 x [5 x i32]], [2 x [5 x i32]]* %dArr, i32 0, i32 0, i32 4
  store float %fDiv, i32* %array_element_ptr59, align 4
  br label %ifNext13

ifNext13:                                         ; preds = %ifFalse12, %ifNext29
  br label %ifNext

orTrue16:                                         ; preds = %ifFalse
  br label %orNext18

orFalse17:                                        ; preds = %ifFalse
  %val20 = load float, float* %c, align 4
  %val21 = load i32, i32* %a, align 4
  %rIToF = sitofp i32 %val21 to float
  %fSub22 = fsub float %val20, %rIToF
  %cond23 = fcmp one float %fSub22, 0.000000e+00
  br i1 %cond23, label %andTrue, label %andFalse

orNext18:                                         ; preds = %andNext, %orTrue16
  %orPhi25 = phi i32 [ 1, %orTrue16 ], [ %andPhi, %andNext ]
  %cond26 = icmp ne i32 %orPhi25, 0
  br i1 %cond26, label %ifTrue11, label %ifFalse12

andTrue:                                          ; preds = %orFalse17
  %val24 = load i32, i32* %a, align 4
  %iMul = mul i32 %val24, 2
  br label %andNext

andFalse:                                         ; preds = %orFalse17
  br label %andNext

andNext:                                          ; preds = %andFalse, %andTrue
  %andPhi = phi i32 [ 0, %andFalse ], [ %iMul, %andTrue ]
  br label %orNext18

ifTrue27:                                         ; preds = %ifTrue11
  ret i32 3
  br label %ifNext29

ifFalse28:                                        ; preds = %ifTrue11
  ret i32 4
  br label %ifNext29

ifNext29:                                         ; preds = %ifFalse28, %ifTrue27
  ret i32 5
  br label %ifNext13
}
