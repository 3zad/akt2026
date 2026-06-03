package eksam1.ast;

import eksam1.ast.scalar.*;
import eksam1.ast.vector.*;
import toylangs.AbstractNode;

import static eksam1.ast.scalar.VexBinOp.Op.*;

public interface VexNode extends AbstractNode {

    static VexNum num(int value) {
        return new VexNum(value);
    }

    static VexSVar svar(String name) {
        return new VexSVar(name);
    }

    static VexBinOp add(VexScalarNode left, VexScalarNode right) {
        return new VexBinOp(Add, left, right);
    }

    static VexBinOp sub(VexScalarNode left, VexScalarNode right) {
        return new VexBinOp(Sub, left, right);
    }

    static VexBinOp mul(VexScalarNode left, VexScalarNode right) {
        return new VexBinOp(Mul, left, right);
    }

    static VexBinOp div(VexScalarNode left, VexScalarNode right) {
        return new VexBinOp(Div, left, right);
    }

    static VexBinOp binop(VexBinOp.Op op, VexScalarNode left, VexScalarNode right) {
        return new VexBinOp(op, left, right);
    }

    static VexBinOp binop(String symbol, VexScalarNode left, VexScalarNode right) {
        return new VexBinOp(symbol, left, right);
    }


    static VexVec vec(VexScalarNode x, VexScalarNode y) {
        return new VexVec(x, y);
    }

    static VexVVar vvar(String name) {
        return new VexVVar(name);
    }

    static VexProj proj(VexProj.Axis axis, VexVectorNode vector) {
        return new VexProj(axis, vector);
    }

    static VexScale scale(VexScalarNode scalar, VexVectorNode vector) {
        return new VexScale(scalar, vector);
    }

    static VexPlus plus(VexVectorNode left, VexVectorNode right) {
        return new VexPlus(left, right);
    }

    static VexDot dot(VexVectorNode left, VexVectorNode right) {
        return new VexDot(left, right);
    }
}
