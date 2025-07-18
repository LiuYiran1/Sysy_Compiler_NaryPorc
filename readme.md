1. 对于运行时库该如何处理？ 应该就是直接call对应的运行时函数
2. 评测时如何编译java程序？ 至少目前可以直接使用javac编译Compiler.java
3. 在处理函数返回时，是需要使用ret还是系统调用结束？
4. 处理phi
5. 数组待优化部分: 1.对于myVisitInitVal的处理机制和官方不同  2. 对于局部数组要分成初始化是否包含变量两种
7. 03_sort  o4_spmv
8. 65 栈空间爆了
9. 对 79 ，要在符号表中处理变量名，切换变量名称

IR visit:
- 函数作用域：Stack<SymTable> 、curFunc 
- whileStack

- 数组
  - 全局 局部
  - 初始化中是否有变量

llvm api：
## LLVM C API（JavaCPP 绑定）调用：
```code
LLVMFunctionType
LLVMAddFunction
LLVMInt32TypeInContext
LLVMFloatTypeInContext
LLVMVoidTypeInContext
LLVMPointerType
LLVMConstIntGetSExtValue
LLVMGetFirstFunction
LLVMGetNextFunction
LLVMGetFirstBasicBlock
LLVMGetNextBasicBlock
LLVMGetBasicBlockTerminator
LLVMGetFirstInstruction
LLVMGetNextInstruction
LLVMGetInstructionOpcode
LLVMRet
LLVMBr
LLVMInstructionEraseFromParent
LLVMDeleteBasicBlock
LLVMGetValueName
LLVMInt64Type
LLVMConstInt
LLVMBuildGEP
LLVMBuildStore
LLVMBuildBitCast
LLVMSetInitializer
LLVMConstArray
LLVMArrayType
LLVMTypeOf
LLVMConstReal
LLVMFloatType
LLVMConstRealGetDouble
LLVMInt32Type
LLVMBuildCall
LLVMBuildLoad
LLVMVoidType
LLVMInt1Type
LLVMContextCreate
```
## 所有使用的 LLVM 类型

### a. JavaCPP 绑定的 LLVM 原始类型
```code
LLVMContextRef
LLVMTypeRef
LLVMValueRef
LLVMBasicBlockRef
PointerPointer
IntPointer
Pointer
```
### b. org.llvm4j.llvm4j.* 或封装类类型
```code
Context
IRBuilder
Module
IntegerType
FloatingPointType
VoidType
Type
PointerType
ArrayType
Function
BasicBlock
Value
Constant
ConstantInt
ConstantFP
WrapSemantics
IntPredicate
FloatPredicate
Option (org.llvm4j.optional.Option)
```

## 3. 封装类中的方法调用（与 LLVM 直接相关）

### a. IRBuilder 相关方法
```code
buildAlloca
buildStore
buildLoad
buildIntAdd
buildIntSub
buildIntMul
buildSignedDiv
buildSignedRem
buildFloatAdd
buildFloatSub
buildFloatMul
buildFloatDiv
buildFloatRem
buildReturn
buildCall
buildIntCompare
buildFloatCompare
buildConditionalBranch
buildBranch
buildPhi
buildGetElementPtr
buildFloatToSigned
buildSignedToFloat
buildZeroExt
positionAfter
getInsertionBlock
```
### b. Context 相关方法
```code
newIRBuilder
newModule
getInt1Type
getInt32Type
getInt64Type
getFloatType
getVoidType
newBasicBlock
getFunctionType
getArrayType
```
### c. Module 相关方法
```code
addGlobalVariable
addFunction
dump
getRef
```
### d. Type、IntegerType、FloatingPointType 相关方
```code
getConstant
getRef
isIntegerType
isFloatingPointType
isVoidType
isPointerType
isArrayType
getElementType (from PointerType or ArrayType)
getAsString
getConstantArray
```
### e. Function、BasicBlock 相关方法
```code
addBasicBlock
getParameters
getParameterCount
getName
```
### f. ConstantInt、ConstantFP 构造方法
```code
new ConstantInt(...)
new ConstantFP(...)
```


我要你实现指令系统并用一个枚举类标记指令的opcode
我要实现的指令有：
Return Alloca Store FloatToSigned SignedToFloat IntCompare FloatCompare
ConditionalBranch Branch Call ZeroExt IntMul SignedDiv SignedRem IntAdd
IntSub FloatMul FloatDiv FloatRem FloatAdd FloatSub Phi Load GetElementPtr