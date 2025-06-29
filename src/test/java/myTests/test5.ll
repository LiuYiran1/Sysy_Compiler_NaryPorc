; ModuleID = 'module'
source_filename = "module"

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

define i32 @m([3 x i32]* %0, i32 %1, i32* %2) {
mEntry:
  %a = alloca [3 x i32]*, align 8
  store [3 x i32]* %0, [3 x i32]** %a, align 8
  %c = alloca i32, align 4
  store i32 %1, i32* %c, align 4
  %d = alloca i32*, align 8
  store i32* %2, i32** %d, align 8
  %sum = alloca i32, align 4
  store i32 0, i32* %sum, align 4
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond

whileCond:                                        ; preds = %whileNext3, %mEntry
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
  %val16 = load i32, i32* %sum, align 4
  %val17 = load i32, i32* %c, align 4
  %iAdd18 = add i32 %val16, %val17
  %val19 = load i32*, i32** %d, align 8
  %load_array_param20 = load i32*, i32** %d, align 8
  %array_element_ptr21 = getelementptr inbounds i32, i32* %load_array_param20, i32 1
  %array_element22 = load i32, i32* %array_element_ptr21, align 4
  %iAdd23 = add i32 %iAdd18, %array_element22
  ret i32 %iAdd23

whileCond1:                                       ; preds = %whileBody2, %whileBody
  %val4 = load i32, i32* %j, align 4
  %lt5 = icmp slt i32 %val4, 3
  %zextForLt6 = zext i1 %lt5 to i32
  %cond7 = icmp ne i32 %zextForLt6, 0
  br i1 %cond7, label %whileBody2, label %whileNext3

whileBody2:                                       ; preds = %whileCond1
  %val8 = load i32, i32* %sum, align 4
  %val9 = load [3 x i32]*, [3 x i32]** %a, align 8
  %val10 = load i32, i32* %i, align 4
  %val11 = load i32, i32* %j, align 4
  %load_array_param = load [3 x i32]*, [3 x i32]** %a, align 8
  %array_element_ptr = getelementptr inbounds [3 x i32], [3 x i32]* %load_array_param, i32 %val10, i32 %val11
  %array_element = load i32, i32* %array_element_ptr, align 4
  %iAdd = add i32 %val8, %array_element
  %sumtem = load i32, i32* %sum, align 4
  store i32 %iAdd, i32* %sum, align 4
  %val12 = load i32, i32* %j, align 4
  %iAdd13 = add i32 %val12, 1
  %jtem = load i32, i32* %j, align 4
  store i32 %iAdd13, i32* %j, align 4
  br label %whileCond1

whileNext3:                                       ; preds = %whileCond1
  %val14 = load i32, i32* %i, align 4
  %iAdd15 = add i32 %val14, 1
  %item = load i32, i32* %i, align 4
  store i32 %iAdd15, i32* %i, align 4
  br label %whileCond
}

define i32 @main() {
mainEntry:
  %aArr = alloca [2 x [3 x i32]], align 4
  %aFlatPtr = bitcast [2 x [3 x i32]]* %aArr to i32*
  %aElem0 = getelementptr i32, i32* %aFlatPtr, i64 0
  store i32 1, i32* %aElem0, align 4
  %aElem1 = getelementptr i32, i32* %aFlatPtr, i64 1
  store i32 2, i32* %aElem1, align 4
  %aElem2 = getelementptr i32, i32* %aFlatPtr, i64 2
  store i32 3, i32* %aElem2, align 4
  %aElem3 = getelementptr i32, i32* %aFlatPtr, i64 3
  store i32 4, i32* %aElem3, align 4
  %aElem4 = getelementptr i32, i32* %aFlatPtr, i64 4
  store i32 5, i32* %aElem4, align 4
  %aElem5 = getelementptr i32, i32* %aFlatPtr, i64 5
  store i32 6, i32* %aElem5, align 4
  %val = load [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, align 4
  %array_element_ptr = getelementptr inbounds [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, i32 0
  %array_element = load [2 x [3 x i32]], [2 x [3 x i32]]* %array_element_ptr, align 4
  %val1 = load [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, align 4
  %array_element_ptr2 = getelementptr inbounds [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, i32 0, i32 1, i32 2
  %array_element3 = load i32, i32* %array_element_ptr2, align 4
  %val4 = load [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, align 4
  %array_element_ptr5 = getelementptr inbounds [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, i32 0, i32 1
  %array_element6 = load [3 x i32], [3 x i32]* %array_element_ptr5, align 4
  %0 = call i32 @m([2 x [3 x i32]]* %array_element_ptr, i32 %array_element3, [3 x i32]* %array_element_ptr5)
  ret i32 %0
}
