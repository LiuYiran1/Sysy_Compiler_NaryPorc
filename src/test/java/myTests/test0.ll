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
  ret i32 2
}

define float @test0() {
test0Entry:
  ret float 0x40617BBBA0000000
}

define i32 @test1() {
test1Entry:
  ret i32 1
}

define float @test2() {
test2Entry:
  ret float 0x4022555540000000
}

define float @test3() {
test3Entry:
  ret float 0xC024333340000000
}

define float @test4() {
test4Entry:
  ret float 1.000000e+00
}
