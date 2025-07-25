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
  %val = load i32, i32* %a, align 4
  %cond = icmp ne i32 %val, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %mainEntry
  store i32 2, i32* %a, align 4
  br label %ifNext

ifFalse:                                          ; preds = %mainEntry
  store i32 5, i32* %a, align 4
  store i32 10, i32* %b, align 4
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  %val1 = load i32, i32* %a, align 4
  store i32 %val1, i32* %b, align 4
  %val2 = load i32, i32* %b, align 4
  ret i32 %val2
}
