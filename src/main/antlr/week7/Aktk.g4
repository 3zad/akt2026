grammar Aktk;

@header {
package week7;
}

program
    : statements EOF
    ;

statements
    : statement (';' statement)*
    ;

statement
    : varDecl
    | assignment
    | ifStmt
    | whileStmt
    | funDecl
    | returnStmt
    | block
    | expr
    ;

block
    : '{' statements '}'
    ;

varDecl
    : 'var' NAME (':' NAME)? ('=' expr)?
    ;

assignment
    : NAME '=' expr
    ;

ifStmt
    : 'if' expr 'then' thenStatement=statement 'else' elseStatement=statement
    ;

whileStmt
    : 'while' expr 'do' statement
    ;

funDecl
    : 'fun' NAME '(' paramList? ')' ('->' NAME)? block
    ;

paramList
    : param (',' param)*
    ;

param
    : NAME ':' NAME
    ;

returnStmt
    : 'return' expr
    ;

// ─── Expressions (precedence: low → high) ───────────────────────────────────

// Lowest: comparison (non-associative)
expr
    : addExpr (compOp addExpr)?
    ;

compOp
    : '==' | '!=' | '<=' | '>=' | '<' | '>'
    ;

// Addition / subtraction
addExpr
    : mulExpr (('+' | '-') mulExpr)*
    ;

// Multiplication / division / modulo
mulExpr
    : unaryExpr (('*' | '/' | '%') unaryExpr)*
    ;

// Unary minus (higher priority than binary arithmetic)
unaryExpr
    : '-' unaryExpr
    | atom
    ;

// Highest: literals, variables, parenthesised exprs, function calls
atom
    : INT_LIT
    | STR_LIT
    | NAME '(' argList? ')'   // function call
    | NAME
    | '(' expr ')'
    ;

argList
    : expr (',' expr)*
    ;

// ─── Lexer Rules ─────────────────────────────────────────────────────────────

PASS    : 'pass' ;

INT_LIT : '0' | [1-9][0-9]* ;

// String literals: anything except " and newline between quotes
STR_LIT : '"' ~["\r\n]* '"' ;

// Identifiers: start with a letter, then letters/digits/underscores
NAME    : [a-zA-Z][a-zA-Z0-9_]* ;

// Comments: non-greedy match so */ closes the first one found
COMMENT : '/*' .*? '*/' -> skip ;

// Whitespace
WS      : [ \t\r\n]+ -> skip ;
