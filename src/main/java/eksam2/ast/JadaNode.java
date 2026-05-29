package eksam2.ast;

import toylangs.AbstractNode;

import java.util.List;

public sealed interface JadaNode extends AbstractNode permits JadaExpr, JadaStmt {

    static JadaNum num(int value) {
        return new JadaNum(value);
    }

    static JadaFrom from(int value) {
        return new JadaFrom(value);
    }

    static JadaVar var(String name) {
        return new JadaVar(name);
    }

    static JadaVar var(String name, JadaExpr index) {
        return new JadaVar(name, index);
    }

    static JadaAdd add(JadaExpr left, JadaExpr right) {
        return new JadaAdd(left, right);
    }


    static JadaAssign assign(String name, JadaExpr expr) {
        return new JadaAssign(name, expr);
    }

    static JadaAssign assign(String name, JadaExpr index, JadaExpr expr) {
        return new JadaAssign(name, index, expr);
    }

    static JadaBlock block(List<JadaStmt> stmts) {
        return new JadaBlock(stmts);
    }

    static JadaBlock block(JadaStmt... stmts) {
        return block(List.of(stmts));
    }

    static JadaForeach foreach(String element, String array, JadaStmt body) {
        return new JadaForeach(element, array, body);
    }
}
