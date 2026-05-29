package eksam2.ast;

public sealed interface JadaExpr extends JadaNode permits JadaAdd, JadaFrom, JadaNum, JadaVar {

}
