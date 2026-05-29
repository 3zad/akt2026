package eksam2.ast;

public record JadaAssign(String name, JadaExpr index, JadaExpr expr) implements JadaStmt {
    public JadaAssign(String name, JadaExpr expr) {
        this(name, null, expr);
    }

    @Override
    public String toString() {
        return "assign(" +
                "\"" + name + "\"" +
                // kui index == null, siis jätame selle printimata
                (index == null
                        ? ""
                        : ", " + index
                ) +
                ", " + expr +
                ")";
    }
}
