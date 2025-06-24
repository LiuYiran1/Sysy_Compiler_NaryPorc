lexer grammar SysYLexer;

CONST     : 'const' ;
INT       : 'int' ;
FLOAT     : 'float' ;
VOID      : 'void' ;
IF        : 'if' ;
ELSE      : 'else' ;
WHILE     : 'while' ;
BREAK     : 'break' ;
CONTINUE  : 'continue' ;
RETURN    : 'return' ;

PLUS      : '+' ;
MINUS     : '-' ;
MUL       : '*' ;
DIV       : '/' ;
MOD       : '%' ;

ASSIGN    : '=' ;
EQ        : '==' ;
NEQ       : '!=' ;
LT        : '<' ;
GT        : '>' ;
LE        : '<=' ;
GE        : '>=' ;
NOT       : '!' ;
AND       : '&&' ;
OR        : '||' ;

L_PAREN   : '(' ;
R_PAREN   : ')' ;
L_BRACE   : '{' ;
R_BRACE   : '}' ;
L_BRACKT  : '[' ;
R_BRACKT  : ']' ;

COMMA     : ',' ;
SEMICOLON : ';' ;

IDENT     : [_a-zA-Z][_a-zA-Z0-9]* ;

INTEGER_CONST
    : [1-9][0-9]* | '0'[0-7]* | '0x'[0-9a-fA-F]+ | '0X'[0-9a-fA-F]+ ;

FLOAT_CONST
    : [0-9]+ '.' [0-9]* ([eE] [+-]? [0-9]+)?
    | '.' [0-9]+ ([eE] [+-]? [0-9]+)?
    | [0-9]+ [eE] [+-]? [0-9]+
    ;

WS                : [ \r\n\t]+ -> skip ;
LINE_COMMENT      : '//' ~[\r\n]* -> skip ;
MULTILINE_COMMENT : '/*' .*? '*/' -> skip ;
