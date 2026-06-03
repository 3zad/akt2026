package eksam1.ast.vector;

import eksam1.ast.scalar.VexScalarNode;

public record VexScale(VexScalarNode scalar, VexVectorNode vector) implements VexVectorNode {
    @Override
    public String toString() {
        return "scale(" +
                "" + scalar +
                ", " + vector +
                ")";
    }
}
