grammar Estolog;
@header { package proovieksam; }

init : prog EOF;

prog
    : (def ';')* expr
    ;

def
    : ID ':=' expr
    ;

expr
    : 'KUI' expr 'SIIS' expr ('MUIDU' expr)?   #ifExpr
    | ningExpr                                 #toNing
    ;

ningExpr
    : voiExpr ('NING' voiExpr)*                #ningOp
    ;

voiExpr
    : jaExpr ('VOI' jaExpr)*                   #voiOp
    ;

jaExpr
    : eqExpr ('JA' eqExpr)*                    #jaOp
    ;

eqExpr
    : atom ('=' atom)?                         #eqOp
    ;

atom
    : '(' expr ')'                             #paren
    | ID                                       #var
    | '1'                                      #trueLit
    | '0'                                      #falseLit
    ;

KUI : 'KUI';
SIIS : 'SIIS';
MUIDU : 'MUIDU';
JA : 'JA';
VOI : 'VOI';
NING : 'NING';

ID : [a-zA-Z]+;

WS : [ \t\r\n]+ -> skip;