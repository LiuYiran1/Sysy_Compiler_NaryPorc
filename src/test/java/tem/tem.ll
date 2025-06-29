; ModuleID = 'module'
source_filename = "module"

@N = global i32 1024
@A = global [1024 x [1024 x i32]] zeroinitializer
@B = global [1024 x [1024 x i32]] zeroinitializer
@C = global [1024 x [1024 x i32]] zeroinitializer

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
  store i32 0, i32* %i, align 4
  store i32 0, i32* %j, align 4
  br label %whileCond

whileCond:                                        ; preds = %whileNext4, %mmEntry
  %val = load i32, i32* %i, align 4
  %val1 = load i32, i32* %n, align 4
  %lt = icmp slt i32 %val, %val1
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  store i32 0, i32* %j, align 4
  br label %whileCond2

whileNext:                                        ; preds = %whileCond
  store i32 0, i32* %i, align 4
  store i32 0, i32* %j, align 4
  store i32 0, i32* %k, align 4
  br label %whileCond16

whileCond2:                                       ; preds = %whileBody3, %whileBody
  %val5 = load i32, i32* %j, align 4
  %val6 = load i32, i32* %n, align 4
  %lt7 = icmp slt i32 %val5, %val6
  %zextForLt8 = zext i1 %lt7 to i32
  %cond9 = icmp ne i32 %zextForLt8, 0
  br i1 %cond9, label %whileBody3, label %whileNext4

whileBody3:                                       ; preds = %whileCond2
  %val10 = load i32, i32* %i, align 4
  %idx64 = sext i32 %val10 to i64
  %val11 = load i32, i32* %j, align 4
  %idx6412 = sext i32 %val11 to i64
  %load_array_param = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %arrayidx0 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param, i64 %idx64
  %arrayidx1 = getelementptr inbounds [1024 x i32], [1024 x i32]* %arrayidx0, i64 0, i64 %idx6412
  store i32 0, i32* %arrayidx1, align 4
  %val13 = load i32, i32* %j, align 4
  %iAdd = add i32 %val13, 1
  store i32 %iAdd, i32* %j, align 4
  br label %whileCond2

whileNext4:                                       ; preds = %whileCond2
  %val14 = load i32, i32* %i, align 4
  %iAdd15 = add i32 %val14, 1
  store i32 %iAdd15, i32* %i, align 4
  br label %whileCond

whileCond16:                                      ; preds = %whileNext26, %whileNext
  %val19 = load i32, i32* %k, align 4
  %val20 = load i32, i32* %n, align 4
  %lt21 = icmp slt i32 %val19, %val20
  %zextForLt22 = zext i1 %lt21 to i32
  %cond23 = icmp ne i32 %zextForLt22, 0
  br i1 %cond23, label %whileBody17, label %whileNext18

whileBody17:                                      ; preds = %whileCond16
  store i32 0, i32* %i, align 4
  br label %whileCond24

whileNext18:                                      ; preds = %whileCond16
  ret void

whileCond24:                                      ; preds = %whileNext44, %ifTrue, %whileBody17
  %val27 = load i32, i32* %i, align 4
  %val28 = load i32, i32* %n, align 4
  %lt29 = icmp slt i32 %val27, %val28
  %zextForLt30 = zext i1 %lt29 to i32
  %cond31 = icmp ne i32 %zextForLt30, 0
  br i1 %cond31, label %whileBody25, label %whileNext26

whileBody25:                                      ; preds = %whileCond24
  %val32 = load i32, i32* %i, align 4
  %idx6433 = sext i32 %val32 to i64
  %val34 = load i32, i32* %k, align 4
  %idx6435 = sext i32 %val34 to i64
  %load_array_param36 = load [1024 x i32]*, [1024 x i32]** %A, align 8
  %arrayidx037 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param36, i64 %idx6433
  %arrayidx138 = getelementptr inbounds [1024 x i32], [1024 x i32]* %arrayidx037, i64 0, i64 %idx6435
  %array_element = load i32, i32* %arrayidx138, align 4
  %eq = icmp eq i32 %array_element, 0
  %zextForEq = zext i1 %eq to i32
  %cond39 = icmp ne i32 %zextForEq, 0
  br i1 %cond39, label %ifTrue, label %ifFalse

whileNext26:                                      ; preds = %whileCond24
  %val86 = load i32, i32* %k, align 4
  %iAdd87 = add i32 %val86, 1
  store i32 %iAdd87, i32* %k, align 4
  br label %whileCond16

ifTrue:                                           ; preds = %whileBody25
  %val40 = load i32, i32* %i, align 4
  %iAdd41 = add i32 %val40, 1
  store i32 %iAdd41, i32* %i, align 4
  br label %whileCond24

ifFalse:                                          ; preds = %whileBody25
  br label %ifNext

ifNext:                                           ; preds = %ifFalse
  store i32 0, i32* %j, align 4
  br label %whileCond42

whileCond42:                                      ; preds = %whileBody43, %ifNext
  %val45 = load i32, i32* %j, align 4
  %val46 = load i32, i32* %n, align 4
  %lt47 = icmp slt i32 %val45, %val46
  %zextForLt48 = zext i1 %lt47 to i32
  %cond49 = icmp ne i32 %zextForLt48, 0
  br i1 %cond49, label %whileBody43, label %whileNext44

whileBody43:                                      ; preds = %whileCond42
  %val50 = load i32, i32* %i, align 4
  %idx6451 = sext i32 %val50 to i64
  %val52 = load i32, i32* %j, align 4
  %idx6453 = sext i32 %val52 to i64
  %load_array_param54 = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %arrayidx055 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param54, i64 %idx6451
  %arrayidx156 = getelementptr inbounds [1024 x i32], [1024 x i32]* %arrayidx055, i64 0, i64 %idx6453
  %array_element57 = load i32, i32* %arrayidx156, align 4
  %val58 = load i32, i32* %i, align 4
  %idx6459 = sext i32 %val58 to i64
  %val60 = load i32, i32* %k, align 4
  %idx6461 = sext i32 %val60 to i64
  %load_array_param62 = load [1024 x i32]*, [1024 x i32]** %A, align 8
  %arrayidx063 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param62, i64 %idx6459
  %arrayidx164 = getelementptr inbounds [1024 x i32], [1024 x i32]* %arrayidx063, i64 0, i64 %idx6461
  %array_element65 = load i32, i32* %arrayidx164, align 4
  %val66 = load i32, i32* %k, align 4
  %idx6467 = sext i32 %val66 to i64
  %val68 = load i32, i32* %j, align 4
  %idx6469 = sext i32 %val68 to i64
  %load_array_param70 = load [1024 x i32]*, [1024 x i32]** %B, align 8
  %arrayidx071 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param70, i64 %idx6467
  %arrayidx172 = getelementptr inbounds [1024 x i32], [1024 x i32]* %arrayidx071, i64 0, i64 %idx6469
  %array_element73 = load i32, i32* %arrayidx172, align 4
  %iMul = mul i32 %array_element65, %array_element73
  %iAdd74 = add i32 %array_element57, %iMul
  %val75 = load i32, i32* %i, align 4
  %idx6476 = sext i32 %val75 to i64
  %val77 = load i32, i32* %j, align 4
  %idx6478 = sext i32 %val77 to i64
  %load_array_param79 = load [1024 x i32]*, [1024 x i32]** %C, align 8
  %arrayidx080 = getelementptr inbounds [1024 x i32], [1024 x i32]* %load_array_param79, i64 %idx6476
  %arrayidx181 = getelementptr inbounds [1024 x i32], [1024 x i32]* %arrayidx080, i64 0, i64 %idx6478
  store i32 %iAdd74, i32* %arrayidx181, align 4
  %val82 = load i32, i32* %j, align 4
  %iAdd83 = add i32 %val82, 1
  store i32 %iAdd83, i32* %j, align 4
  br label %whileCond42

whileNext44:                                      ; preds = %whileCond42
  %val84 = load i32, i32* %i, align 4
  %iAdd85 = add i32 %val84, 1
  store i32 %iAdd85, i32* %i, align 4
  br label %whileCond24
}

define i32 @main() {
mainEntry:
  %n = alloca i32, align 4
  store i32 1, i32* %n, align 4
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  %j = alloca i32, align 4
  store i32 0, i32* %j, align 4
  store i32 0, i32* %i, align 4
  store i32 0, i32* %j, align 4
  br label %whileCond

whileCond:                                        ; preds = %whileNext4, %mainEntry
  %val = load i32, i32* %i, align 4
  %val1 = load i32, i32* %n, align 4
  %lt = icmp slt i32 %val, %val1
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  store i32 0, i32* %j, align 4
  br label %whileCond2

whileNext:                                        ; preds = %whileCond
  %ans = alloca i32, align 4
  store i32 0, i32* %ans, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond13

whileCond2:                                       ; preds = %whileBody3, %whileBody
  %val5 = load i32, i32* %j, align 4
  %val6 = load i32, i32* %n, align 4
  %lt7 = icmp slt i32 %val5, %val6
  %zextForLt8 = zext i1 %lt7 to i32
  %cond9 = icmp ne i32 %zextForLt8, 0
  br i1 %cond9, label %whileBody3, label %whileNext4

whileBody3:                                       ; preds = %whileCond2
  %val10 = load i32, i32* %j, align 4
  %iAdd = add i32 %val10, 1
  store i32 %iAdd, i32* %j, align 4
  br label %whileCond2

whileNext4:                                       ; preds = %whileCond2
  %val11 = load i32, i32* %i, align 4
  %iAdd12 = add i32 %val11, 1
  store i32 %iAdd12, i32* %i, align 4
  br label %whileCond

whileCond13:                                      ; preds = %whileNext23, %whileNext
  %val16 = load i32, i32* %i, align 4
  %val17 = load i32, i32* %n, align 4
  %lt18 = icmp slt i32 %val16, %val17
  %zextForLt19 = zext i1 %lt18 to i32
  %cond20 = icmp ne i32 %zextForLt19, 0
  br i1 %cond20, label %whileBody14, label %whileNext15

whileBody14:                                      ; preds = %whileCond13
  store i32 0, i32* %j, align 4
  br label %whileCond21

whileNext15:                                      ; preds = %whileCond13
  ret i32 0

whileCond21:                                      ; preds = %whileBody22, %whileBody14
  %val24 = load i32, i32* %j, align 4
  %val25 = load i32, i32* %n, align 4
  %lt26 = icmp slt i32 %val24, %val25
  %zextForLt27 = zext i1 %lt26 to i32
  %cond28 = icmp ne i32 %zextForLt27, 0
  br i1 %cond28, label %whileBody22, label %whileNext23

whileBody22:                                      ; preds = %whileCond21
  %val29 = load i32, i32* %ans, align 4
  %val30 = load i32, i32* %i, align 4
  %idx64 = sext i32 %val30 to i64
  %val31 = load i32, i32* %j, align 4
  %idx6432 = sext i32 %val31 to i64
  %arrayidx0 = getelementptr inbounds [1024 x [1024 x i32]], [1024 x [1024 x i32]]* @B, i64 0, i64 %idx64
  %arrayidx1 = getelementptr inbounds [1024 x i32], [1024 x i32]* %arrayidx0, i64 0, i64 %idx6432
  %array_element = load i32, i32* %arrayidx1, align 4
  %iAdd33 = add i32 %val29, %array_element
  store i32 %iAdd33, i32* %ans, align 4
  %val34 = load i32, i32* %j, align 4
  %iAdd35 = add i32 %val34, 1
  store i32 %iAdd35, i32* %j, align 4
  br label %whileCond21

whileNext23:                                      ; preds = %whileCond21
  %val36 = load i32, i32* %i, align 4
  %iAdd37 = add i32 %val36, 1
  store i32 %iAdd37, i32* %i, align 4
  br label %whileCond13
}
