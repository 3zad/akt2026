package eksam1.ast.scalar;

import eksam1.ast.vector.VexVectorNode;

public record VexProj(Axis projAxis, VexVectorNode vector) implements VexScalarNode {
    public enum Axis {
        X, Y
    }

    @Override
    public String toString() {
        return "proj(" +
                "" + projAxis +
                ", " + vector +
                ")";
    }
}
