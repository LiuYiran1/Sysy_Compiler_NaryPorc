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

define i32 @x(i32* %0, [3 x float]* %1) {
xEntry:
  %b = alloca i32*, align 8
  store i32* %0, i32** %b, align 8
  %a = alloca [3 x float]*, align 8
  store [3 x float]* %1, [3 x float]** %a, align 8
  ret i32 1
}

define i32 @x1(i32* %0, [3 x i32]* %1) {
x1Entry:
  %b = alloca i32*, align 8
  store i32* %0, i32** %b, align 8
  %a = alloca [3 x i32]*, align 8
  store [3 x i32]* %1, [3 x i32]** %a, align 8
  %loadVar = load [3 x i32]*, [3 x i32]** %a, align 8
  ret [3 x i32]* %loadVar
}

define i32 @x2(i32* %0) {
x2Entry:
  %b = alloca i32*, align 8
  store i32* %0, i32** %b, align 8
  %loadVar = load [2 x [3 x i32]], [2 x [3 x i32]]* @a, align 4
  ret [2 x [3 x i32]] %loadVar
}

define i32 @x3(i32* %0) {
x3Entry:
  %b = alloca i32*, align 8
  store i32* %0, i32** %b, align 8
  %aArr = alloca [2 x [3 x i32]], align 4
  %aFlatPtr = bitcast [2 x [3 x i32]]* %aArr to i32*
  %aElem0 = getelementptr i32, i32* %aFlatPtr, i64 0
  store i32 1, i32* %aElem0, align 4
  %aElem1 = getelementptr i32, i32* %aFlatPtr, i64 1
  store i32 2, i32* %aElem1, align 4
  %aElem2 = getelementptr i32, i32* %aFlatPtr, i64 2
  store i32 3, i32* %aElem2, align 4
  %aElem3 = getelementptr i32, i32* %aFlatPtr, i64 3
  store i32 4, i32* %aElem3, align 4
  %aElem4 = getelementptr i32, i32* %aFlatPtr, i64 4
  store i32 5, i32* %aElem4, align 4
  %aElem5 = getelementptr i32, i32* %aFlatPtr, i64 5
  store i32 6, i32* %aElem5, align 4
  %loadVar = load [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, align 4
  ret [2 x [3 x i32]] %loadVar
}
