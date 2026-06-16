package eksam2.ast;

public record JadaNum(int value) implements JadaExpr {
    @Override
    public String toString() {
        return "num(" +
                "" + value +
                ")";
    }
}
