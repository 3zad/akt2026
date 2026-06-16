package eksam2.ast;

public record JadaFrom(int value) implements JadaExpr {
    @Override
    public String toString() {
        return "from(" +
                "" + value +
                ")";
    }
}
