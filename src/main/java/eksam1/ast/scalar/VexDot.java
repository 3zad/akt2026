package eksam1.ast.scalar;

import eksam1.ast.vector.VexVectorNode;

public record VexDot(VexVectorNode left, VexVectorNode right) implements VexScalarNode {
    @Override
    public String toString() {
        return "dot(" +
                "" + left +
                ", " + right +
                ")";
    }
}
