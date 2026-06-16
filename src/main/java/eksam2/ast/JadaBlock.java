package eksam2.ast;

import java.util.List;

public record JadaBlock(List<JadaStmt> stmts) implements JadaStmt {
    @Override
    public String toString() {
        return "block(" +
                "" + stmts.toString().replaceAll("[\\[\\]{}]", "") +
                ")";
    }
}
