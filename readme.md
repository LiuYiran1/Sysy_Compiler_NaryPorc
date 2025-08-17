# 设计文档

#### 编译器前端-- 词法分析、语法分析

我组使用 **antlr** 实现编译器前端

- 通过 `RenamingSysYLexer` 类继承 `SysYLexer` 类，重写 `Token emit()` 方法，对长度超出限制（长度大于32）的变量名进行重命名操作
- 通过 `LexerListener` 类继承 `BaseErrorListener` 类，重写 `syntaxError` 方法，打印不合法的词法单元及其相关信息
- 通过 `ParserListener` 类继承 `BaseErrorListener` 类， 重写 `syntaxError` 方法，打印不合法的语法的行数及其相关信息

#### 编译器中端-- LLVM IR生成、LL实现（模拟LLVM）、优化

1. **LLVM IR生成**
    - 使用 ANTLR 的 **Visitor 模式** 来构建 IR

2. **LL实现**
    - LL的实现参考了LLVM IR的设计
    - 基本组件
        - `Context`：管理类型对象的创建，类型包括 void、int、float、array、pointer，管理 Module 和 IRbuilder
        - `Module`：包含一个程序的函数，全局变量信息。
        - `IRBulider`：用来实现构建 IR 的 api
        - `Value`： Value是 IR 中的数据表示，是一切数据的父类
        - `User`：继承自 Value ，用于维护变量之间的调用链，方便实现优化
        - `Constanst`：继承自User，是 IR 中的常量
        - `GlobalValues`：继承自User，是 IR 中全局变量和函数的父类
        - `BasicBlock`：继承自User，是 IR 中的基本块
        - `Instructions`：继承自User，是 IR 中指令的父类

3. **优化**：

   中端实现了各种优化，提升了代码的性能，具体实现的优化如下：

    - **MemToReg**：实现寄存器提升，消除了 IR 中除数组外的 alloc、store、load 指令
    - **DeadCodeElim**：实现了死代码消除，对不可达块进行了消除
    - **UnusedVarElim**：实现了未使用变量消除

111