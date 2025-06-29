; ModuleID = 'module'
source_filename = "module"

@c = global [3 x i32] [i32 1, i32 2, i32 3]

define i32 @main(i32* %0) {
mainEntry:
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
  %b1 = alloca i32, align 4
  %val = load [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, align 4
  %array_element_ptr = getelementptr inbounds [2 x [3 x i32]], [2 x [3 x i32]]* %aArr, i32 0, i32 1, i32 2
  %array_element = load i32, i32* %array_element_ptr, align 4
  store i32 %array_element, i32* %b1, align 4
  %val2 = load i32, i32* %b1, align 4
  ret i32 %val2
}
