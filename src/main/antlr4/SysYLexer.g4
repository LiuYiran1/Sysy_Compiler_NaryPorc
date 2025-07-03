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

INTEGER_CONST: DEC
             | OCT
             | HEX
             ;
fragment DEC: [1-9][0-9]* ;
fragment OCT: '0'[0-7]* ;
fragment HEX: ('0x' | '0X')[0-9a-fA-F]+ ;

FLOAT_CONST: DEC_FLOAT
           | HEX_FLOAT
           ;

fragment DEC_FLOAT
    : FRACTIONAL_CONSTANT EXPONENT_PART?
    | DIGIT_SEQUENCE EXPONENT_PART
    ;

fragment HEX_FLOAT
    : ('0x' | '0X') HEXADECIMAL_FRACTIONAL_CONSTANT BINARY_EXPONENT_PART
    | ('0x' | '0X') HEXADECIMAL_DIGIT_SEQUENCE BINARY_EXPONENT_PART
    ;

fragment FRACTIONAL_CONSTANT
    : DIGIT_SEQUENCE? '.' DIGIT_SEQUENCE
    | DIGIT_SEQUENCE '.'
    ;

fragment HEXADECIMAL_FRACTIONAL_CONSTANT
    : HEXADECIMAL_DIGIT_SEQUENCE? '.' HEXADECIMAL_DIGIT_SEQUENCE
    | HEXADECIMAL_DIGIT_SEQUENCE '.'
    ;

fragment EXPONENT_PART              : [eE] [+-]? [0-9]+ ;
fragment BINARY_EXPONENT_PART       : [pP] [+-]? [0-9]+ ;
fragment HEXADECIMAL_DIGIT_SEQUENCE : [0-9a-fA-F]+ ;
fragment DIGIT_SEQUENCE             : [0-9]+ ;


WS                : [ \r\n\t]+ -> skip ;
LINE_COMMENT      : '//' ~[\r\n]* -> skip ;
MULTILINE_COMMENT : '/*' .*? '*/' -> skip ;
