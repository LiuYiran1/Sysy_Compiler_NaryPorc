; ModuleID = 'module'
source_filename = "module"

@a = global i32 10
@b = global i32 2
@c = global i32 2
@d = global float 4.000000e+00
@e = global float 9.000000e+00
@f = global float 1.900000e+01
@g = global float 0x40617BBBA0000000

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
  store i32 139, i32* %a, align 4
  %b = alloca float, align 4
  store float 0x40617BBBA0000000, float* %b, align 4
  ret i32 0
}
