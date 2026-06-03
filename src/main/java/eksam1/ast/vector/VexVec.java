package eksam1.ast.vector;

import eksam1.ast.scalar.VexScalarNode;

public record VexVec(VexScalarNode x, VexScalarNode y) implements VexVectorNode {
    @Override
    public String toString() {
        return "vec(" +
                "" + x +
                ", " + y +
                ")";
    }
}
