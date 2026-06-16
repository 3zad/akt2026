package eksam2.ast;

public record JadaAdd(JadaExpr left, JadaExpr right) implements JadaExpr {
    @Override
    public String toString() {
        return "add(" +
                "" + left +
                ", " + right +
                ")";
    }
}
