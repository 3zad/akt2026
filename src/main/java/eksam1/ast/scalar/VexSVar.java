package eksam1.ast.scalar;

public record VexSVar(String name) implements VexScalarNode {
    @Override
    public String toString() {
        return "svar(" +
                "\"" + name + "\"" +
                ")";
    }
}
