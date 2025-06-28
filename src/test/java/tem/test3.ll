; ModuleID = 'module'
source_filename = "module"

@a = global [2 x [3 x i32]] [[3 x i32] [i32 1, i32 2, i32 3], [3 x i32] [i32 4, i32 5, i32 6]]
@b = global [1 x i32] zeroinitializer

define i32 @m(i32 %0, float %1) {
mEntry:
  %a = alloca i32, align 4
  store i32 %0, i32* %a, align 4
  %b = alloca float, align 4
  store float %1, float* %b, align 4
  ret i32 1
}

define i32 @x(i32* %0, float* %1) {
xEntry:
  %b = alloca i32*, align 8
  store i32* %0, i32** %b, align 8
  %a = alloca float*, align 8
  store float* %1, float** %a, align 8
  ret i32 1
}
