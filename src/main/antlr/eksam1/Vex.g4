grammar Vex;
@header { package eksam1; }

SCALAR
    : ('-'?)[1-9]([0-9])*
    | '0'
    ;

ROTATION : 'L' | 'R';

AXIS : 'x' | 'y';

SCALAR_ID
    : [a-z]+ ;

VECTOR_ID
    : [A-Z][a-z]* ;

WS : [ \t\r\n]+ -> skip;

// Seda reeglit pole vaja muuta
init : vector EOF;

expr : expr op=('*' | '/') expr
     | expr op=('+' | '-') expr
     | SCALAR
     | SCALAR_ID
     | AXIS
     | '(' expr ')'
     | proj
     | dot
     ;

simpleVector
    : '<' expr ',' expr '>'
    | VECTOR_ID
    | ROTATION
    | simpleVector ROTATION
    | '(' vector ')'
    ;

dot : simpleVector '.' simpleVector;

proj : simpleVector '|' AXIS;

simpleScalar : SCALAR | SCALAR_ID | '(' expr ')';

// Seda reeglit tuleb muuta / täiendada
// (Ilmselt soovid ka defineerida uusi abireegleid)

vector
    : simpleVector
    | simpleScalar '*' simpleVector
    | vector '+' vector
    ;



