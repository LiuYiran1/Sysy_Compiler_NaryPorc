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

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 1, i32* %a, align 4
  %b = alloca i32, align 4
  store i32 9, i32* %b, align 4
  br label %whileCond

whileCond:                                        ; preds = %ifNext, %mainEntry
  %val = load i32, i32* %a, align 4
  %lt = icmp slt i32 %val, 100
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %val1 = load i32, i32* %a, align 4
  %iRem = srem i32 %val1, 2
  %eq = icmp eq i32 %iRem, 1
  %zextForEq = zext i1 %eq to i32
  %cond2 = icmp ne i32 %zextForEq, 0
  br i1 %cond2, label %ifTrue, label %ifFalse

whileNext:                                        ; preds = %whileCond
  %val6 = load i32, i32* %a, align 4
  store i32 %val6, i32* %b, align 4
  %val7 = load i32, i32* %b, align 4
  ret i32 %val7

ifTrue:                                           ; preds = %whileBody
  %val3 = load i32, i32* %a, align 4
  %iAdd = add i32 %val3, 1
  store i32 %iAdd, i32* %a, align 4
  br label %ifNext

ifFalse:                                          ; preds = %whileBody
  %val4 = load i32, i32* %a, align 4
  %iAdd5 = add i32 %val4, 2
  store i32 %iAdd5, i32* %a, align 4
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  br label %whileCond
}
