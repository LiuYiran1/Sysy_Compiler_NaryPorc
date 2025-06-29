; ModuleID = 'module'
source_filename = "module"

@mod = global i32 998244353
@d = global i32 0
@maxlen = global i32 2097152
@temp = global [2097152 x i32] zeroinitializer
@a = global [2097152 x i32] zeroinitializer
@b = global [2097152 x i32] zeroinitializer
@c = global [2097152 x i32] zeroinitializer

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

define i32 @multiply(i32 %0, i32 %1) {
multiplyEntry:
  %a = alloca i32, align 4
  store i32 %0, i32* %a, align 4
  %b = alloca i32, align 4
  store i32 %1, i32* %b, align 4
  %val = load i32, i32* %b, align 4
  %eq = icmp eq i32 %val, 0
  %zextForEq = zext i1 %eq to i32
  %cond = icmp ne i32 %zextForEq, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %multiplyEntry
  ret i32 0

ifFalse:                                          ; preds = %multiplyEntry
  br label %ifNext

ifNext:                                           ; preds = %ifFalse
  %val4 = load i32, i32* %b, align 4
  %eq5 = icmp eq i32 %val4, 1
  %zextForEq6 = zext i1 %eq5 to i32
  %cond7 = icmp ne i32 %zextForEq6, 0
  br i1 %cond7, label %ifTrue1, label %ifFalse2

ifTrue1:                                          ; preds = %ifNext
  %val8 = load i32, i32* %a, align 4
  %iRem = srem i32 %val8, 998244353
  ret i32 %iRem

ifFalse2:                                         ; preds = %ifNext
  br label %ifNext3

ifNext3:                                          ; preds = %ifFalse2
  %cur = alloca i32, align 4
  %val9 = load i32, i32* %a, align 4
  %val10 = load i32, i32* %b, align 4
  %iDiv = sdiv i32 %val10, 2
  %2 = call i32 @multiply(i32 %val9, i32 %iDiv)
  store i32 %2, i32* %cur, align 4
  %val11 = load i32, i32* %cur, align 4
  %val12 = load i32, i32* %cur, align 4
  %iAdd = add i32 %val11, %val12
  %iRem13 = srem i32 %iAdd, 998244353
  %curtem = load i32, i32* %cur, align 4
  store i32 %iRem13, i32* %cur, align 4
  %val17 = load i32, i32* %b, align 4
  %iRem18 = srem i32 %val17, 2
  %eq19 = icmp eq i32 %iRem18, 1
  %zextForEq20 = zext i1 %eq19 to i32
  %cond21 = icmp ne i32 %zextForEq20, 0
  br i1 %cond21, label %ifTrue14, label %ifFalse15

ifTrue14:                                         ; preds = %ifNext3
  %val22 = load i32, i32* %cur, align 4
  %val23 = load i32, i32* %a, align 4
  %iAdd24 = add i32 %val22, %val23
  %iRem25 = srem i32 %iAdd24, 998244353
  ret i32 %iRem25

ifFalse15:                                        ; preds = %ifNext3
  %val26 = load i32, i32* %cur, align 4
  ret i32 %val26
}

define i32 @power(i32 %0, i32 %1) {
powerEntry:
  %a = alloca i32, align 4
  store i32 %0, i32* %a, align 4
  %b = alloca i32, align 4
  store i32 %1, i32* %b, align 4
  %val = load i32, i32* %b, align 4
  %eq = icmp eq i32 %val, 0
  %zextForEq = zext i1 %eq to i32
  %cond = icmp ne i32 %zextForEq, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %powerEntry
  ret i32 1

ifFalse:                                          ; preds = %powerEntry
  br label %ifNext

ifNext:                                           ; preds = %ifFalse
  %cur = alloca i32, align 4
  %val1 = load i32, i32* %a, align 4
  %val2 = load i32, i32* %b, align 4
  %iDiv = sdiv i32 %val2, 2
  %2 = call i32 @power(i32 %val1, i32 %iDiv)
  store i32 %2, i32* %cur, align 4
  %val3 = load i32, i32* %cur, align 4
  %val4 = load i32, i32* %cur, align 4
  %3 = call i32 @multiply(i32 %val3, i32 %val4)
  %curtem = load i32, i32* %cur, align 4
  store i32 %3, i32* %cur, align 4
  %val8 = load i32, i32* %b, align 4
  %iRem = srem i32 %val8, 2
  %eq9 = icmp eq i32 %iRem, 1
  %zextForEq10 = zext i1 %eq9 to i32
  %cond11 = icmp ne i32 %zextForEq10, 0
  br i1 %cond11, label %ifTrue5, label %ifFalse6

ifTrue5:                                          ; preds = %ifNext
  %val12 = load i32, i32* %cur, align 4
  %val13 = load i32, i32* %a, align 4
  %4 = call i32 @multiply(i32 %val12, i32 %val13)
  ret i32 %4

ifFalse6:                                         ; preds = %ifNext
  %val14 = load i32, i32* %cur, align 4
  ret i32 %val14
}

define i32 @memmove(i32* %0, i32 %1, i32* %2, i32 %3) {
memmoveEntry:
  %dst = alloca i32*, align 8
  store i32* %0, i32** %dst, align 8
  %dst_pos = alloca i32, align 4
  store i32 %1, i32* %dst_pos, align 4
  %src = alloca i32*, align 8
  store i32* %2, i32** %src, align 8
  %len = alloca i32, align 4
  store i32 %3, i32* %len, align 4
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond

whileCond:                                        ; preds = %whileBody, %memmoveEntry
  %val = load i32, i32* %i, align 4
  %val1 = load i32, i32* %len, align 4
  %lt = icmp slt i32 %val, %val1
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %val2 = load i32*, i32** %src, align 8
  %val3 = load i32, i32* %i, align 4
  %load_array_param = load i32*, i32** %src, align 8
  %array_element_ptr = getelementptr inbounds i32, i32* %load_array_param, i32 %val3
  %array_element = load i32, i32* %array_element_ptr, align 4
  %dsttem = load i32*, i32** %dst, align 8
  %val4 = load i32*, i32** %dst, align 8
  %val5 = load i32, i32* %dst_pos, align 4
  %val6 = load i32, i32* %i, align 4
  %iAdd = add i32 %val5, %val6
  %load_array_param7 = load i32*, i32** %dst, align 8
  %array_element_ptr8 = getelementptr inbounds i32, i32* %load_array_param7, i32 %iAdd
  store i32 %array_element, i32* %array_element_ptr8, align 4
  %val9 = load i32, i32* %i, align 4
  %iAdd10 = add i32 %val9, 1
  %item = load i32, i32* %i, align 4
  store i32 %iAdd10, i32* %i, align 4
  br label %whileCond

whileNext:                                        ; preds = %whileCond
  %val11 = load i32, i32* %i, align 4
  ret i32 %val11
}

define i32 @fft(i32* %0, i32 %1, i32 %2, i32 %3) {
fftEntry:
  %arr = alloca i32*, align 8
  store i32* %0, i32** %arr, align 8
  %begin_pos = alloca i32, align 4
  store i32 %1, i32* %begin_pos, align 4
  %n = alloca i32, align 4
  store i32 %2, i32* %n, align 4
  %w = alloca i32, align 4
  store i32 %3, i32* %w, align 4
  %val = load i32, i32* %n, align 4
  %eq = icmp eq i32 %val, 1
  %zextForEq = zext i1 %eq to i32
  %cond = icmp ne i32 %zextForEq, 0
  br i1 %cond, label %ifTrue, label %ifFalse

ifTrue:                                           ; preds = %fftEntry
  ret i32 1

ifFalse:                                          ; preds = %fftEntry
  br label %ifNext

ifNext:                                           ; preds = %ifFalse
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond

whileCond:                                        ; preds = %ifNext6, %ifNext
  %val1 = load i32, i32* %i, align 4
  %val2 = load i32, i32* %n, align 4
  %lt = icmp slt i32 %val1, %val2
  %zextForLt = zext i1 %lt to i32
  %cond3 = icmp ne i32 %zextForLt, 0
  br i1 %cond3, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %val7 = load i32, i32* %i, align 4
  %iRem = srem i32 %val7, 2
  %eq8 = icmp eq i32 %iRem, 0
  %zextForEq9 = zext i1 %eq8 to i32
  %cond10 = icmp ne i32 %zextForEq9, 0
  br i1 %cond10, label %ifTrue4, label %ifFalse5

whileNext:                                        ; preds = %whileCond
  %val34 = load i32*, i32** %arr, align 8
  %load_array_param35 = load i32*, i32** %arr, align 8
  %array_element_ptr36 = getelementptr inbounds i32, i32* %load_array_param35
  %array_element37 = load i32, i32* %array_element_ptr36, align 4
  %val38 = load i32, i32* %begin_pos, align 4
  %val39 = load [2097152 x i32], [2097152 x i32]* @temp, align 4
  %array_element40 = load [2097152 x i32], [2097152 x i32]* @temp, align 4
  %val41 = load i32, i32* %n, align 4
  %4 = call i32 @memmove(i32 %array_element37, i32 %val38, [2097152 x i32]* @temp, i32 %val41)
  %val42 = load i32*, i32** %arr, align 8
  %load_array_param43 = load i32*, i32** %arr, align 8
  %array_element_ptr44 = getelementptr inbounds i32, i32* %load_array_param43
  %array_element45 = load i32, i32* %array_element_ptr44, align 4
  %val46 = load i32, i32* %begin_pos, align 4
  %val47 = load i32, i32* %n, align 4
  %iDiv48 = sdiv i32 %val47, 2
  %val49 = load i32, i32* %w, align 4
  %val50 = load i32, i32* %w, align 4
  %5 = call i32 @multiply(i32 %val49, i32 %val50)
  %6 = call i32 @fft(i32 %array_element45, i32 %val46, i32 %iDiv48, i32 %5)
  %val51 = load i32*, i32** %arr, align 8
  %load_array_param52 = load i32*, i32** %arr, align 8
  %array_element_ptr53 = getelementptr inbounds i32, i32* %load_array_param52
  %array_element54 = load i32, i32* %array_element_ptr53, align 4
  %val55 = load i32, i32* %begin_pos, align 4
  %val56 = load i32, i32* %n, align 4
  %iDiv57 = sdiv i32 %val56, 2
  %iAdd58 = add i32 %val55, %iDiv57
  %val59 = load i32, i32* %n, align 4
  %iDiv60 = sdiv i32 %val59, 2
  %val61 = load i32, i32* %w, align 4
  %val62 = load i32, i32* %w, align 4
  %7 = call i32 @multiply(i32 %val61, i32 %val62)
  %8 = call i32 @fft(i32 %array_element54, i32 %iAdd58, i32 %iDiv60, i32 %7)
  %item63 = load i32, i32* %i, align 4
  store i32 0, i32* %i, align 4
  %wn = alloca i32, align 4
  store i32 1, i32* %wn, align 4
  br label %whileCond64

ifTrue4:                                          ; preds = %whileBody
  %val11 = load i32*, i32** %arr, align 8
  %val12 = load i32, i32* %i, align 4
  %val13 = load i32, i32* %begin_pos, align 4
  %iAdd = add i32 %val12, %val13
  %load_array_param = load i32*, i32** %arr, align 8
  %array_element_ptr = getelementptr inbounds i32, i32* %load_array_param, i32 %iAdd
  %array_element = load i32, i32* %array_element_ptr, align 4
  %temptem = load [2097152 x i32], [2097152 x i32]* @temp, align 4
  %val14 = load [2097152 x i32], [2097152 x i32]* @temp, align 4
  %val15 = load i32, i32* %i, align 4
  %iDiv = sdiv i32 %val15, 2
  %array_element_ptr16 = getelementptr inbounds [2097152 x i32], [2097152 x i32]* @temp, i32 0, i32 %iDiv
  store i32 %array_element, i32* %array_element_ptr16, align 4
  br label %ifNext6

ifFalse5:                                         ; preds = %whileBody
  %val17 = load i32*, i32** %arr, align 8
  %val18 = load i32, i32* %i, align 4
  %val19 = load i32, i32* %begin_pos, align 4
  %iAdd20 = add i32 %val18, %val19
  %load_array_param21 = load i32*, i32** %arr, align 8
  %array_element_ptr22 = getelementptr inbounds i32, i32* %load_array_param21, i32 %iAdd20
  %array_element23 = load i32, i32* %array_element_ptr22, align 4
  %temptem24 = load [2097152 x i32], [2097152 x i32]* @temp, align 4
  %val25 = load [2097152 x i32], [2097152 x i32]* @temp, align 4
  %val26 = load i32, i32* %n, align 4
  %iDiv27 = sdiv i32 %val26, 2
  %val28 = load i32, i32* %i, align 4
  %iDiv29 = sdiv i32 %val28, 2
  %iAdd30 = add i32 %iDiv27, %iDiv29
  %array_element_ptr31 = getelementptr inbounds [2097152 x i32], [2097152 x i32]* @temp, i32 0, i32 %iAdd30
  store i32 %array_element23, i32* %array_element_ptr31, align 4
  br label %ifNext6

ifNext6:                                          ; preds = %ifFalse5, %ifTrue4
  %val32 = load i32, i32* %i, align 4
  %iAdd33 = add i32 %val32, 1
  %item = load i32, i32* %i, align 4
  store i32 %iAdd33, i32* %i, align 4
  br label %whileCond

whileCond64:                                      ; preds = %whileBody65, %whileNext
  %val67 = load i32, i32* %i, align 4
  %val68 = load i32, i32* %n, align 4
  %iDiv69 = sdiv i32 %val68, 2
  %lt70 = icmp slt i32 %val67, %iDiv69
  %zextForLt71 = zext i1 %lt70 to i32
  %cond72 = icmp ne i32 %zextForLt71, 0
  br i1 %cond72, label %whileBody65, label %whileNext66

whileBody65:                                      ; preds = %whileCond64
  %x = alloca i32, align 4
  %val73 = load i32*, i32** %arr, align 8
  %val74 = load i32, i32* %begin_pos, align 4
  %val75 = load i32, i32* %i, align 4
  %iAdd76 = add i32 %val74, %val75
  %load_array_param77 = load i32*, i32** %arr, align 8
  %array_element_ptr78 = getelementptr inbounds i32, i32* %load_array_param77, i32 %iAdd76
  %array_element79 = load i32, i32* %array_element_ptr78, align 4
  store i32 %array_element79, i32* %x, align 4
  %y = alloca i32, align 4
  %val80 = load i32*, i32** %arr, align 8
  %val81 = load i32, i32* %begin_pos, align 4
  %val82 = load i32, i32* %i, align 4
  %iAdd83 = add i32 %val81, %val82
  %val84 = load i32, i32* %n, align 4
  %iDiv85 = sdiv i32 %val84, 2
  %iAdd86 = add i32 %iAdd83, %iDiv85
  %load_array_param87 = load i32*, i32** %arr, align 8
  %array_element_ptr88 = getelementptr inbounds i32, i32* %load_array_param87, i32 %iAdd86
  %array_element89 = load i32, i32* %array_element_ptr88, align 4
  store i32 %array_element89, i32* %y, align 4
  %val90 = load i32, i32* %x, align 4
  %val91 = load i32, i32* %wn, align 4
  %val92 = load i32, i32* %y, align 4
  %9 = call i32 @multiply(i32 %val91, i32 %val92)
  %iAdd93 = add i32 %val90, %9
  %iRem94 = srem i32 %iAdd93, 998244353
  %arrtem = load i32*, i32** %arr, align 8
  %val95 = load i32*, i32** %arr, align 8
  %val96 = load i32, i32* %begin_pos, align 4
  %val97 = load i32, i32* %i, align 4
  %iAdd98 = add i32 %val96, %val97
  %load_array_param99 = load i32*, i32** %arr, align 8
  %array_element_ptr100 = getelementptr inbounds i32, i32* %load_array_param99, i32 %iAdd98
  store i32 %iRem94, i32* %array_element_ptr100, align 4
  %val101 = load i32, i32* %x, align 4
  %val102 = load i32, i32* %wn, align 4
  %val103 = load i32, i32* %y, align 4
  %10 = call i32 @multiply(i32 %val102, i32 %val103)
  %iSub = sub i32 %val101, %10
  %iAdd104 = add i32 %iSub, 998244353
  %iRem105 = srem i32 %iAdd104, 998244353
  %arrtem106 = load i32*, i32** %arr, align 8
  %val107 = load i32*, i32** %arr, align 8
  %val108 = load i32, i32* %begin_pos, align 4
  %val109 = load i32, i32* %i, align 4
  %iAdd110 = add i32 %val108, %val109
  %val111 = load i32, i32* %n, align 4
  %iDiv112 = sdiv i32 %val111, 2
  %iAdd113 = add i32 %iAdd110, %iDiv112
  %load_array_param114 = load i32*, i32** %arr, align 8
  %array_element_ptr115 = getelementptr inbounds i32, i32* %load_array_param114, i32 %iAdd113
  store i32 %iRem105, i32* %array_element_ptr115, align 4
  %val116 = load i32, i32* %wn, align 4
  %val117 = load i32, i32* %w, align 4
  %11 = call i32 @multiply(i32 %val116, i32 %val117)
  %wntem = load i32, i32* %wn, align 4
  store i32 %11, i32* %wn, align 4
  %val118 = load i32, i32* %i, align 4
  %iAdd119 = add i32 %val118, 1
  %item120 = load i32, i32* %i, align 4
  store i32 %iAdd119, i32* %i, align 4
  br label %whileCond64

whileNext66:                                      ; preds = %whileCond64
  ret i32 0
}

define i32 @main() {
mainEntry:
  %n = alloca i32, align 4
  %val = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %array_element = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %0 = call i32 @getarray([2097152 x i32]* @a)
  store i32 %0, i32* %n, align 4
  %m = alloca i32, align 4
  %val1 = load [2097152 x i32], [2097152 x i32]* @b, align 4
  %array_element2 = load [2097152 x i32], [2097152 x i32]* @b, align 4
  %1 = call i32 @getarray([2097152 x i32]* @b)
  store i32 %1, i32* %m, align 4
  call void @starttime()
  %dtem = load i32, i32* @d, align 4
  store i32 1, i32* @d, align 4
  br label %whileCond

whileCond:                                        ; preds = %whileBody, %mainEntry
  %val3 = load i32, i32* @d, align 4
  %val4 = load i32, i32* %n, align 4
  %val5 = load i32, i32* %m, align 4
  %iAdd = add i32 %val4, %val5
  %iSub = sub i32 %iAdd, 1
  %lt = icmp slt i32 %val3, %iSub
  %zextForLt = zext i1 %lt to i32
  %cond = icmp ne i32 %zextForLt, 0
  br i1 %cond, label %whileBody, label %whileNext

whileBody:                                        ; preds = %whileCond
  %val6 = load i32, i32* @d, align 4
  %iMul = mul i32 %val6, 2
  %dtem7 = load i32, i32* @d, align 4
  store i32 %iMul, i32* @d, align 4
  br label %whileCond

whileNext:                                        ; preds = %whileCond
  %val8 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %array_element9 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val10 = load i32, i32* @d, align 4
  %val11 = load i32, i32* @d, align 4
  %iDiv = sdiv i32 998244352, %val11
  %2 = call i32 @power(i32 3, i32 %iDiv)
  %3 = call i32 @fft([2097152 x i32]* @a, i32 0, i32 %val10, i32 %2)
  %val12 = load [2097152 x i32], [2097152 x i32]* @b, align 4
  %array_element13 = load [2097152 x i32], [2097152 x i32]* @b, align 4
  %val14 = load i32, i32* @d, align 4
  %val15 = load i32, i32* @d, align 4
  %iDiv16 = sdiv i32 998244352, %val15
  %4 = call i32 @power(i32 3, i32 %iDiv16)
  %5 = call i32 @fft([2097152 x i32]* @b, i32 0, i32 %val14, i32 %4)
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond17

whileCond17:                                      ; preds = %whileBody18, %whileNext
  %val20 = load i32, i32* %i, align 4
  %val21 = load i32, i32* @d, align 4
  %lt22 = icmp slt i32 %val20, %val21
  %zextForLt23 = zext i1 %lt22 to i32
  %cond24 = icmp ne i32 %zextForLt23, 0
  br i1 %cond24, label %whileBody18, label %whileNext19

whileBody18:                                      ; preds = %whileCond17
  %val25 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val26 = load i32, i32* %i, align 4
  %array_element_ptr = getelementptr inbounds [2097152 x i32], [2097152 x i32]* @a, i32 0, i32 %val26
  %array_element27 = load i32, i32* %array_element_ptr, align 4
  %val28 = load [2097152 x i32], [2097152 x i32]* @b, align 4
  %val29 = load i32, i32* %i, align 4
  %array_element_ptr30 = getelementptr inbounds [2097152 x i32], [2097152 x i32]* @b, i32 0, i32 %val29
  %array_element31 = load i32, i32* %array_element_ptr30, align 4
  %6 = call i32 @multiply(i32 %array_element27, i32 %array_element31)
  %atem = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val32 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val33 = load i32, i32* %i, align 4
  %array_element_ptr34 = getelementptr inbounds [2097152 x i32], [2097152 x i32]* @a, i32 0, i32 %val33
  store i32 %6, i32* %array_element_ptr34, align 4
  %val35 = load i32, i32* %i, align 4
  %iAdd36 = add i32 %val35, 1
  %item = load i32, i32* %i, align 4
  store i32 %iAdd36, i32* %i, align 4
  br label %whileCond17

whileNext19:                                      ; preds = %whileCond17
  %val37 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %array_element38 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val39 = load i32, i32* @d, align 4
  %val40 = load i32, i32* @d, align 4
  %iDiv41 = sdiv i32 998244352, %val40
  %iSub42 = sub i32 998244352, %iDiv41
  %7 = call i32 @power(i32 3, i32 %iSub42)
  %8 = call i32 @fft([2097152 x i32]* @a, i32 0, i32 %val39, i32 %7)
  %item43 = load i32, i32* %i, align 4
  store i32 0, i32* %i, align 4
  br label %whileCond44

whileCond44:                                      ; preds = %whileBody45, %whileNext19
  %val47 = load i32, i32* %i, align 4
  %val48 = load i32, i32* @d, align 4
  %lt49 = icmp slt i32 %val47, %val48
  %zextForLt50 = zext i1 %lt49 to i32
  %cond51 = icmp ne i32 %zextForLt50, 0
  br i1 %cond51, label %whileBody45, label %whileNext46

whileBody45:                                      ; preds = %whileCond44
  %val52 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val53 = load i32, i32* %i, align 4
  %array_element_ptr54 = getelementptr inbounds [2097152 x i32], [2097152 x i32]* @a, i32 0, i32 %val53
  %array_element55 = load i32, i32* %array_element_ptr54, align 4
  %val56 = load i32, i32* @d, align 4
  %9 = call i32 @power(i32 %val56, i32 998244351)
  %10 = call i32 @multiply(i32 %array_element55, i32 %9)
  %atem57 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val58 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %val59 = load i32, i32* %i, align 4
  %array_element_ptr60 = getelementptr inbounds [2097152 x i32], [2097152 x i32]* @a, i32 0, i32 %val59
  store i32 %10, i32* %array_element_ptr60, align 4
  %val61 = load i32, i32* %i, align 4
  %iAdd62 = add i32 %val61, 1
  %item63 = load i32, i32* %i, align 4
  store i32 %iAdd62, i32* %i, align 4
  br label %whileCond44

whileNext46:                                      ; preds = %whileCond44
  call void @stoptime()
  %val64 = load i32, i32* %n, align 4
  %val65 = load i32, i32* %m, align 4
  %iAdd66 = add i32 %val64, %val65
  %iSub67 = sub i32 %iAdd66, 1
  %val68 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  %array_element69 = load [2097152 x i32], [2097152 x i32]* @a, align 4
  call void @putarray(i32 %iSub67, [2097152 x i32]* @a)
  ret i32 0
}
