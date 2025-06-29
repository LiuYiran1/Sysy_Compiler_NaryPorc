; ModuleID = 'module'
source_filename = "module"

@N = global i32 1024

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

define void @mm(i32 %0, [1024 x i32]* %1, [1024 x i32]* %2, [1024 x i32]* %3) {
mmEntry:
  %n = alloca i32, align 4
  store i32 %0, i32* %n, align 4
  %A = alloca [1024 x i32]*, align 8
  store [1024 x i32]* %1, [1024 x i32]** %A, align 8
  %B = alloca [1024 x i32]*, align 8
  store [1024 x i32]* %2, [1024 x i32]** %B, align 8
  %C = alloca [1024 x i32]*, align 8
  store [1024 x i32]* %3, [1024 x i32]** %C, align 8
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  %j = alloca i32, align 4
  store i32 0, i32* %j, align 4
  %k = alloca i32, align 4
  store i32 0, i32* %k, align 4
  %item = load i32, i32* %i, align 4
  store i32 0, i32* %i, align 4
  %jtem = load i32, i32* %j, align 4
  store i32 0, i32* %j, align 4
  %ktem = load i32, i32* %k, align 4
  store i32 0, i32* %k, align 4
  br label %whileCond

whileCond:                                        ; preds = %whileNext5, %mmEntry
  %val = load i32, i32* %k, align 4
  %val1 = load i32, i32* %n, align 4
  %lt = icmp slt i32 %val, %val1
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %item2 = load i32, i32* %i, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond3

whileNext:                                        ; preds = %whileCond
    ret;

whileCond3:                                       ; preds = %whileNext20, %ifTrue, %whileBody
  %val6 = load i32, i32* %i, align 4
  %val7 = load i32, i32* %n, align 4
  %lt8 = icmp slt i32 %val6, %val7
  %zextForLt9 = zext i1 %lt8 to i32
  %cond10 = icmp ne i32 %zextForLt9, 0
  br i1 %cond10, label %whileBody4, label %whileNext5

whileBody4:                                       ; preds = %whileCond3
  %val11 = load [1024 x i32]*, [1024 x i32]** %A, align 8
  %val12 = load i32, i32* %i, align 4
  %val13 = load i32, i32* %k, align 4
  %load_array_param = load [1024 x i32]*, [1024 x i32]** %A, align 8
  %array_element_ptr = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param, i32 %val12, i32 %val13
  %array_element = load i32, i32* %array_element_ptr, align 4
  %eq = icmp eq i32 %array_element, 0
  %zextForEq = zext i1 %eq to i32
  %cond14 = icmp ne i32 %zextForEq, 0
  br i1 %cond14, label %ifTrue, label %ifFalse

whileNext5:                                       ; preds = %whileCond3
  %val56 = load i32, i32* %k, align 4
  %iAdd57 = add i32 %val56, 1
  %ktem58 = load i32, i32* %k, align 4
  store i32 %iAdd57, i32* %k, align 4
  br label %whileCond

ifTrue:                                           ; preds = %whileBody4
  %val15 = load i32, i32* %i, align 4
  %iAdd = add i32 %val15, 1
  %item16 = load i32, i32* %i, align 4
  store i32 %iAdd, i32* %i, align 4
  br label %whileCond3
  br label %ifNext

ifFalse:                                          ; preds = %whileBody4
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  %jtem17 = load i32, i32* %j, align 4
  store i32 0, i32* %j, align 4
  br label %whileCond18

whileCond18:                                      ; preds = %whileBody19, %ifNext
  %val21 = load i32, i32* %j, align 4
  %val22 = load i32, i32* %n, align 4
  %lt23 = icmp slt i32 %val21, %val22
  %zextForLt24 = zext i1 %lt23 to i32
  %cond25 = icmp ne i32 %zextForLt24, 0
  br i1 %cond25, label %whileBody19, label %whileNext20

whileBody19:                                      ; preds = %whileCond18
  %val26 = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %val27 = load i32, i32* %i, align 4
  %val28 = load i32, i32* %j, align 4
  %load_array_param29 = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %array_element_ptr30 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param29, i32 %val27, i32 %val28
  %array_element31 = load i32, i32* %array_element_ptr30, align 4
  %val32 = load [1024 x i32]*, [1024 x i32]** %A, align 8
  %val33 = load i32, i32* %i, align 4
  %val34 = load i32, i32* %k, align 4
  %load_array_param35 = load [1024 x i32]*, [1024 x i32]** %A, align 8
  %array_element_ptr36 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param35, i32 %val33, i32 %val34
  %array_element37 = load i32, i32* %array_element_ptr36, align 4
  %val38 = load [1024 x i32]*, [1024 x i32]** %B, align 8
  %val39 = load i32, i32* %k, align 4
  %val40 = load i32, i32* %j, align 4
  %load_array_param41 = load [1024 x i32]*, [1024 x i32]** %B, align 8
  %array_element_ptr42 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param41, i32 %val39, i32 %val40
  %array_element43 = load i32, i32* %array_element_ptr42, align 4
  %iMul = mul i32 %array_element37, %array_element43
  %iAdd44 = add i32 %array_element31, %iMul
  %Ctem = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %val45 = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %val46 = load i32, i32* %i, align 4
  %val47 = load i32, i32* %j, align 4
  %load_array_param48 = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %array_element_ptr49 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param48, i32 %val46, i32 %val47
  store i32 %iAdd44, i32* %array_element_ptr49, align 4
  %val50 = load i32, i32* %j, align 4
  %iAdd51 = add i32 %val50, 1
  %jtem52 = load i32, i32* %j, align 4
  store i32 %iAdd51, i32* %j, align 4
  br label %whileCond18

whileNext20:                                      ; preds = %whileCond18
  %val53 = load i32, i32* %i, align 4
  %iAdd54 = add i32 %val53, 1
  %item55 = load i32, i32* %i, align 4
  store i32 %iAdd54, i32* %i, align 4
  br label %whileCond3
}
