package eksam2.ast;

public sealed interface JadaStmt extends JadaNode permits JadaAssign, JadaBlock, JadaForeach {

}
