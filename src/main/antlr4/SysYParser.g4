parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;
}

// 编译单元
program : compUnit EOF ;

compUnit : (decl | funcDef)+ ;

// 声明
decl : constDecl | varDecl ;

// 常量声明
constDecl : CONST bType constDef (COMMA constDef)* SEMICOLON ;

// 类型
bType : INT | FLOAT ;

// 常量定义
constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal ;

constInitVal
    : constExp
    | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE
    ;

// 变量声明
varDecl : bType varDef (COMMA varDef)* SEMICOLON ;

varDef
    : IDENT (L_BRACKT constExp R_BRACKT)*
    | IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal
    ;

initVal
    : exp
    | L_BRACE (initVal (COMMA initVal)*)? R_BRACE
    ;

// 函数定义
funcDef : funcType IDENT L_PAREN funcFParams? R_PAREN block ;

// 函数返回类型
funcType : VOID | INT | FLOAT ; // ★ 加入 float 返回值支持

// 形参表
funcFParams : funcFParam (COMMA funcFParam)* ;

funcFParam : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)? ;

// 语句块
block : L_BRACE blockItem* R_BRACE ;

// 语句块项
blockItem : decl | stmt ;

// 语句
stmt
    : lVal ASSIGN exp SEMICOLON
    | exp? SEMICOLON
    | block
    | IF L_PAREN cond R_PAREN stmt (ELSE stmt)?
    | WHILE L_PAREN cond R_PAREN stmt
    | BREAK SEMICOLON
    | CONTINUE SEMICOLON
    | RETURN exp? SEMICOLON
    ;

exp
   : L_PAREN exp R_PAREN
   | lVal
   | number
   | IDENT L_PAREN funcRParams? R_PAREN
   | unaryOp exp
   | exp (MUL | DIV | MOD) exp
   | exp (PLUS | MINUS) exp
   ;

cond
   : exp
   | cond (LT | GT | LE | GE) cond
   | cond (EQ | NEQ) cond
   | cond AND cond
   | cond OR cond
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

number
   : INTEGER_CONST | FLOAT_CONST
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
   ;

funcRParams
   : param (COMMA param)*
   ;

param
   : exp
   ;

constExp
   : exp
   ;