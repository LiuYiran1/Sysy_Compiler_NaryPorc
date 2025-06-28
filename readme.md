1. 对于运行时库该如何处理？ 应该就是直接call对应的运行时函数
2. 评测时如何编译java程序？ 至少目前可以直接使用javac编译Compiler.java
3. 在处理函数返回时，是需要使用ret还是系统调用结束？
4. 处理phi
5. 数组待优化部分: 1.对于myVisitInitVal的处理机制和官方不同  2. 对于局部数组要分成初始化是否包含变量两种

IR visit:
- 函数作用域：Stack<SymTable> 、curFunc 
- whileStack

- 数组
  - 全局 局部
  - 初始化中是否有变量

