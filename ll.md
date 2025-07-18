# ll 手册

1. 常数的name是null
2. 自动生成的 %1、%2等变量的名字就是1、2...
3. 注意区分pointerType、pointeeType
4. 接收函数调用返回值的变量由ll自动生成
5. IntegerType中的getConstantInt默认返回有符号数
6. constant中的array没有交给类型管理
7. 在处理arrayType时，ll中没有关注数组的维度的长度即[]中的数是多少 ~_~ ，默认是0
8. GlobalVariable的类型是指针类型，这是为了进行局部和全局的同一处理，也可以从其中得到其指向的类型
9. buildGetElementPtr 中 默认只处理 indices.size 为 1,2 两种情况


## 缺点