; ModuleID = 'module'
source_filename = "module"

@a = global [2 x [3 x i32]] [[3 x i32] [i32 1, i32 2, i32 3], [3 x i32] [i32 4, i32 5, i32 6]]
@b = global i32 1

define i32 @x2() {
x2Entry:
  %val = load i32, i32* @b, align 4
  ret i32 %val
}
