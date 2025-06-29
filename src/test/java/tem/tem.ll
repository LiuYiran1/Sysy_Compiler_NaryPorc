; ModuleID = 'module'
source_filename = "module"

@m = global i32 998244353

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
  store i32 3, i32* %a, align 4
  %b = alloca i32, align 4
  store i32 9, i32* %b, align 4
  %c = alloca i32, align 4
  %val = load i32, i32* %a, align 4
  %iRem = srem i32 %val, 9
  store i32 %iRem, i32* %c, align 4
  %val1 = load i32, i32* %b, align 4
  %iRem2 = srem i32 %val1, 998244353
  ret i32 %iRem2
}
