package eksam1.ast.vector;

public record VexPlus(VexVectorNode left, VexVectorNode right) implements VexVectorNode {
    @Override
    public String toString() {
        return "plus(" +
                "" + left +
                ", " + right +
                ")";
    }
}
