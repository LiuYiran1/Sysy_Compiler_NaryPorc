; ModuleID = 'module'
source_filename = "module"
target datalayout = "e-m:e-p:64:64-i64:64-i128:128-n64-S128"
target triple = "riscv64-unknown-elf"

@c = global [3 x i32] [i32 2, i32 0, i32 0]
@d = global [4 x [5 x i32]] [[5 x i32] [i32 1, i32 2, i32 3, i32 0, i32 0], [5 x i32] zeroinitializer, [5 x i32] zeroinitializer, [5 x i32] zeroinitializer]

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
  %aArr = alloca [1 x i32], align 4
  %aFlatPtr = bitcast [1 x i32]* %aArr to i32*
  %aElem0 = getelementptr i32, i32* %aFlatPtr, i64 0
  store i32 2, i32* %aElem0, align 4
  %bArr = alloca [2 x [3 x [4 x i32]]], align 4
  %bFlatPtr = bitcast [2 x [3 x [4 x i32]]]* %bArr to i32*
  %bElem0 = getelementptr i32, i32* %bFlatPtr, i64 0
  store i32 5, i32* %bElem0, align 4
  %bElem1 = getelementptr i32, i32* %bFlatPtr, i64 1
  store i32 6, i32* %bElem1, align 4
  %bElem2 = getelementptr i32, i32* %bFlatPtr, i64 2
  store i32 7, i32* %bElem2, align 4
  %bElem3 = getelementptr i32, i32* %bFlatPtr, i64 3
  store i32 0, i32* %bElem3, align 4
  %bElem4 = getelementptr i32, i32* %bFlatPtr, i64 4
  store i32 0, i32* %bElem4, align 4
  %bElem5 = getelementptr i32, i32* %bFlatPtr, i64 5
  store i32 0, i32* %bElem5, align 4
  %bElem6 = getelementptr i32, i32* %bFlatPtr, i64 6
  store i32 0, i32* %bElem6, align 4
  %bElem7 = getelementptr i32, i32* %bFlatPtr, i64 7
  store i32 0, i32* %bElem7, align 4
  %bElem8 = getelementptr i32, i32* %bFlatPtr, i64 8
  store i32 0, i32* %bElem8, align 4
  %bElem9 = getelementptr i32, i32* %bFlatPtr, i64 9
  store i32 0, i32* %bElem9, align 4
  %bElem10 = getelementptr i32, i32* %bFlatPtr, i64 10
  store i32 0, i32* %bElem10, align 4
  %bElem11 = getelementptr i32, i32* %bFlatPtr, i64 11
  store i32 0, i32* %bElem11, align 4
  %bElem12 = getelementptr i32, i32* %bFlatPtr, i64 12
  store i32 0, i32* %bElem12, align 4
  %bElem13 = getelementptr i32, i32* %bFlatPtr, i64 13
  store i32 0, i32* %bElem13, align 4
  %bElem14 = getelementptr i32, i32* %bFlatPtr, i64 14
  store i32 0, i32* %bElem14, align 4
  %bElem15 = getelementptr i32, i32* %bFlatPtr, i64 15
  store i32 0, i32* %bElem15, align 4
  %bElem16 = getelementptr i32, i32* %bFlatPtr, i64 16
  store i32 0, i32* %bElem16, align 4
  %bElem17 = getelementptr i32, i32* %bFlatPtr, i64 17
  store i32 0, i32* %bElem17, align 4
  %bElem18 = getelementptr i32, i32* %bFlatPtr, i64 18
  store i32 0, i32* %bElem18, align 4
  %bElem19 = getelementptr i32, i32* %bFlatPtr, i64 19
  store i32 0, i32* %bElem19, align 4
  %bElem20 = getelementptr i32, i32* %bFlatPtr, i64 20
  store i32 0, i32* %bElem20, align 4
  %bElem21 = getelementptr i32, i32* %bFlatPtr, i64 21
  store i32 0, i32* %bElem21, align 4
  %bElem22 = getelementptr i32, i32* %bFlatPtr, i64 22
  store i32 0, i32* %bElem22, align 4
  %bElem23 = getelementptr i32, i32* %bFlatPtr, i64 23
  store i32 0, i32* %bElem23, align 4
  ret i32 0
}

define i32 @mm() {
mmEntry:
  %eArr = alloca [3 x [5 x i32]], align 4
  %eFlatPtr = bitcast [3 x [5 x i32]]* %eArr to i32*
  %eElem0 = getelementptr i32, i32* %eFlatPtr, i64 0
  store i32 0, i32* %eElem0, align 4
  %eElem1 = getelementptr i32, i32* %eFlatPtr, i64 1
  store i32 0, i32* %eElem1, align 4
  %eElem2 = getelementptr i32, i32* %eFlatPtr, i64 2
  store i32 0, i32* %eElem2, align 4
  %eElem3 = getelementptr i32, i32* %eFlatPtr, i64 3
  store i32 0, i32* %eElem3, align 4
  %eElem4 = getelementptr i32, i32* %eFlatPtr, i64 4
  store i32 0, i32* %eElem4, align 4
  %eElem5 = getelementptr i32, i32* %eFlatPtr, i64 5
  store i32 0, i32* %eElem5, align 4
  %eElem6 = getelementptr i32, i32* %eFlatPtr, i64 6
  store i32 0, i32* %eElem6, align 4
  %eElem7 = getelementptr i32, i32* %eFlatPtr, i64 7
  store i32 0, i32* %eElem7, align 4
  %eElem8 = getelementptr i32, i32* %eFlatPtr, i64 8
  store i32 0, i32* %eElem8, align 4
  %eElem9 = getelementptr i32, i32* %eFlatPtr, i64 9
  store i32 0, i32* %eElem9, align 4
  %eElem10 = getelementptr i32, i32* %eFlatPtr, i64 10
  store i32 0, i32* %eElem10, align 4
  %eElem11 = getelementptr i32, i32* %eFlatPtr, i64 11
  store i32 0, i32* %eElem11, align 4
  %eElem12 = getelementptr i32, i32* %eFlatPtr, i64 12
  store i32 0, i32* %eElem12, align 4
  %eElem13 = getelementptr i32, i32* %eFlatPtr, i64 13
  store i32 0, i32* %eElem13, align 4
  %eElem14 = getelementptr i32, i32* %eFlatPtr, i64 14
  store i32 0, i32* %eElem14, align 4
  %array_element = load i32, i32* getelementptr inbounds ([3 x i32], [3 x i32]* @c, i64 0, i64 1), align 4
  %arrayidx0 = getelementptr inbounds [3 x [5 x i32]], [3 x [5 x i32]]* %eArr, i64 0, i64 1
  %arrayidx1 = getelementptr inbounds [5 x i32], [5 x i32]* %arrayidx0, i64 0, i64 2
  store i32 %array_element, i32* %arrayidx1, align 4
  %array_element1 = load i32, i32* getelementptr inbounds ([4 x [5 x i32]], [4 x [5 x i32]]* @d, i64 0, i64 2, i64 3), align 4
  %arrayidx02 = getelementptr inbounds [3 x [5 x i32]], [3 x [5 x i32]]* %eArr, i64 0, i64 1
  %arrayidx13 = getelementptr inbounds [5 x i32], [5 x i32]* %arrayidx02, i64 0, i64 2
  store i32 %array_element1, i32* %arrayidx13, align 4
  ret i32 0
}

define i32 @mm1([2 x i32]* %0) {
mm1Entry:
  %array_element = load i32, i32* getelementptr inbounds ([3 x i32], [3 x i32]* @c, i64 0, i64 1), align 4
  %arrayidx0 = getelementptr inbounds [2 x i32], [2 x i32]* %0, i64 1
  %arrayidx1 = getelementptr inbounds [2 x i32], [2 x i32]* %arrayidx0, i64 0, i64 2
  store i32 %array_element, i32* %arrayidx1, align 4
  ret i32 0
}
