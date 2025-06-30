; ModuleID = 'module'
source_filename = "module"

@mod = global i32 998244353
@d = global i32 0

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

define i32 @multiply(i32 %0, i32 %1) {
multiplyEntry:
  %a = alloca i32, align 4
  store i32 %0, i32* %a, align 4
  %b = alloca i32, align 4
  store i32 %1, i32* %b, align 4
  %val = load i32, i32* %b, align 4
  %eq = icmp eq i32 %val, 0
  %zextForEq = zext i1 %eq to i32
  %cond = icmp ne i32 %zextForEq, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %multiplyEntry
  ret i32 0

ifFalse:                                          ; preds = %multiplyEntry
  br label %ifNext

ifNext:                                           ; preds = %ifFalse
  %val4 = load i32, i32* %b, align 4
  %eq5 = icmp eq i32 %val4, 1
  %zextForEq6 = zext i1 %eq5 to i32
  %cond7 = icmp ne i32 %zextForEq6, 0
  br i1 %cond7, label %ifTrue1, label %ifFalse2

ifTrue1:                                          ; preds = %ifNext
  %val8 = load i32, i32* %a, align 4
  %iRem = srem i32 %val8, 998244353
  ret i32 %iRem

ifFalse2:                                         ; preds = %ifNext
  br label %ifNext3

ifNext3:                                          ; preds = %ifFalse2
  %cur = alloca i32, align 4
  %val9 = load i32, i32* %a, align 4
  %val10 = load i32, i32* %b, align 4
  %iDiv = sdiv i32 %val10, 2
  %2 = call i32 @multiply(i32 %val9, i32 %iDiv)
  store i32 %2, i32* %cur, align 4
  %val11 = load i32, i32* %cur, align 4
  %val12 = load i32, i32* %cur, align 4
  %iAdd = add i32 %val11, %val12
  %iRem13 = srem i32 %iAdd, 998244353
  store i32 %iRem13, i32* %cur, align 4
  %val17 = load i32, i32* %b, align 4
  %iRem18 = srem i32 %val17, 2
  %eq19 = icmp eq i32 %iRem18, 1
  %zextForEq20 = zext i1 %eq19 to i32
  %cond21 = icmp ne i32 %zextForEq20, 0
  br i1 %cond21, label %ifTrue14, label %ifFalse15

ifTrue14:                                         ; preds = %ifNext3
  %val22 = load i32, i32* %cur, align 4
  %val23 = load i32, i32* %a, align 4
  %iAdd24 = add i32 %val22, %val23
  %iRem25 = srem i32 %iAdd24, 998244353
  ret i32 %iRem25

ifFalse15:                                        ; preds = %ifNext3
  %val26 = load i32, i32* %cur, align 4
  ret i32 %val26

ifNext16:                                         ; No predecessors!
  ret i32 0
}
