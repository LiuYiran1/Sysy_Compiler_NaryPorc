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
  %val = load i32, i32* %a, align 4
  %cond = icmp ne i32 %val, 0
  br i1 %cond, label %orTrue, label %orFalse

ifTrue:                                           ; preds = %orNext
  store i32 2, i32* %a, align 4
  br label %ifNext

ifFalse:                                          ; preds = %orNext
  store i32 3, i32* %a, align 4
  br label %ifNext

ifNext:                                           ; preds = %ifFalse, %ifTrue
  ret i32 5

orTrue:                                           ; preds = %mainEntry
  br label %orNext

orFalse:                                          ; preds = %mainEntry
  br label %orNext

orNext:                                           ; preds = %orFalse, %orTrue
  %orPhi = phi i32 [ 1, %orTrue ], [ 1, %orFalse ]
  %cond1 = icmp ne i32 %orPhi, 0
  br i1 %cond1, label %ifTrue, label %ifFalse
}
