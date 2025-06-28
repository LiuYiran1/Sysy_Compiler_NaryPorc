; ModuleID = 'module'
source_filename = "module"

@a = global [2 x [3 x i32]] [[3 x i32] [i32 1, i32 2, i32 3], [3 x i32] [i32 4, i32 5, i32 6]]
@b = global [2 x [3 x i32]] [[3 x i32] [i32 1, i32 2, i32 0], [3 x i32] [i32 3, i32 0, i32 0]]
@c = global [4 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 4], [2 x i32] [i32 5, i32 6], [2 x i32] [i32 7, i32 8]]
@c1 = global [2 x [2 x [2 x i32]]] [[2 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 4]], [2 x [2 x i32]] [[2 x i32] [i32 5, i32 6], [2 x i32] [i32 7, i32 8]]]
@d = global [2 x [4 x i32]] zeroinitializer
@e = global [1 x [2 x [500 x i32]]] zeroinitializer
@f = global [4 x [4 x [4 x i32]]] [[4 x [4 x i32]] [[4 x i32] [i32 1, i32 2, i32 3, i32 0], [4 x i32] zeroinitializer, [4 x i32] zeroinitializer, [4 x i32] zeroinitializer], [4 x [4 x i32]] [[4 x i32] [i32 0, i32 4, i32 0, i32 0], [4 x i32] [i32 0, i32 5, i32 0, i32 0], [4 x i32] zeroinitializer, [4 x i32] zeroinitializer], [4 x [4 x i32]] [[4 x i32] [i32 0, i32 6, i32 0, i32 0], [4 x i32] zeroinitializer, [4 x i32] zeroinitializer, [4 x i32] zeroinitializer], [4 x [4 x i32]] zeroinitializer]
@f0 = global [4 x [4 x [4 x i32]]] [[4 x [4 x i32]] [[4 x i32] [i32 1, i32 2, i32 3, i32 4], [4 x i32] [i32 5, i32 6, i32 0, i32 0], [4 x i32] zeroinitializer, [4 x i32] zeroinitializer], [4 x [4 x i32]] zeroinitializer, [4 x [4 x i32]] zeroinitializer, [4 x [4 x i32]] zeroinitializer]
@f1 = global [4 x [4 x [4 x i32]]] [[4 x [4 x i32]] [[4 x i32] [i32 1, i32 0, i32 0, i32 0], [4 x i32] [i32 2, i32 0, i32 0, i32 0], [4 x i32] [i32 3, i32 0, i32 0, i32 0], [4 x i32] [i32 4, i32 0, i32 0, i32 0]], [4 x [4 x i32]] [[4 x i32] [i32 5, i32 6, i32 0, i32 0], [4 x i32] zeroinitializer, [4 x i32] zeroinitializer, [4 x i32] zeroinitializer], [4 x [4 x i32]] zeroinitializer, [4 x [4 x i32]] zeroinitializer]
@g = global [2 x [3 x i32]] [[3 x i32] [i32 1, i32 2, i32 3], [3 x i32] [i32 4, i32 5, i32 6]]
@h0 = global [3 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 4], [2 x i32] [i32 5, i32 6]]
@h1 = global [3 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 4], [2 x i32] [i32 5, i32 6]]
@h2 = global [3 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 4], [2 x i32] [i32 5, i32 6]]
@h3 = global [3 x [2 x i32]] [[2 x i32] zeroinitializer, [2 x i32] [i32 3, i32 4], [2 x i32] [i32 5, i32 6]]
@h4 = global [3 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 0], [2 x i32] [i32 5, i32 0]]
@h5 = global [3 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 0], [2 x i32] [i32 5, i32 0]]
@N = global i32 100
@H = global i32 100
@t = global [1 x i32] [i32 200]

define i32 @main() {
mainEntry:
  %m = alloca i32, align 4
  store i32 1, i32* %m, align 4
  %aArr = alloca [2 x [3 x i32]], align 4
  %lVar = load i32, i32* %m, align 4
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
  store i32 %lVar, i32* %aElem5, align 4
  %bArr = alloca [2 x [3 x float]], align 4
  %lVar1 = load i32, i32* %m, align 4
  %fArr = sitofp i32 %lVar1 to float
  %bFlatPtr = bitcast [2 x [3 x float]]* %bArr to float*
  %bElem0 = getelementptr float, float* %bFlatPtr, i64 0
  store float 1.000000e+00, float* %bElem0, align 4
  %bElem1 = getelementptr float, float* %bFlatPtr, i64 1
  store float 2.000000e+00, float* %bElem1, align 4
  %bElem2 = getelementptr float, float* %bFlatPtr, i64 2
  store float 3.000000e+00, float* %bElem2, align 4
  %bElem3 = getelementptr float, float* %bFlatPtr, i64 3
  store float 4.000000e+00, float* %bElem3, align 4
  %bElem4 = getelementptr float, float* %bFlatPtr, i64 4
  store float 5.000000e+00, float* %bElem4, align 4
  %bElem5 = getelementptr float, float* %bFlatPtr, i64 5
  store float %fArr, float* %bElem5, align 4
  ret i32 1
}
