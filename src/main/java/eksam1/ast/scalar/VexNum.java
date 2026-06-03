package eksam1.ast.scalar;

public record VexNum(int value) implements VexScalarNode {
    @Override
    public String toString() {
        return "num(" +
                "" + value +
                ")";
    }
}
