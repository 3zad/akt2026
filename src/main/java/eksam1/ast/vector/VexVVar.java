package eksam1.ast.vector;

public record VexVVar(String name) implements VexVectorNode {
    @Override
    public String toString() {
        return "vvar(" +
                "\"" + name + "\"" +
                ")";
    }
}
