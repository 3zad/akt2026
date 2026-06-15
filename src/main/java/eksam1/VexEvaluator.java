package eksam1;

import eksam1.ast.scalar.*;
import eksam1.ast.vector.*;

import java.util.Map;

import static eksam1.ast.scalar.VexProj.Axis.X;

public class VexEvaluator {
    private final Map<String, Integer> scalarEnv;
    private final Map<String, Vector> vectorEnv;

    private VexEvaluator(Map<String, Integer> scalarEnv, Map<String, Vector> vectorEnv) {
        this.scalarEnv = scalarEnv;
        this.vectorEnv = vectorEnv;
    }

    public static Vector eval(VexVectorNode vectorNode, Map<String, Integer> scalarEnv, Map<String, Vector> vectorEnv) {
        return new VexEvaluator(scalarEnv, vectorEnv).evalVector(vectorNode);
    }

    // Skalaaravaldised ja vektoravaldised on erinevat tüüpi väärtustega ning võivad teineteist vastastikuselt sisaldada.

    private Vector evalVector(VexVectorNode vectorNode) {
        switch (vectorNode) {
            case VexPlus(VexVectorNode left, VexVectorNode right) -> {
                var vLeft = evalVector(left);
                var vRight = evalVector(right);
                return new Vector(vLeft.x() + vRight.x(), vLeft.y() + vRight.y());
            }
            case VexScale(VexScalarNode scalar, VexVectorNode vector) -> {
                var vec = evalVector(vector);
                var scal = evalScalar(scalar);
                return new Vector(vec.x()*scal, vec.y()*scal);
            }
            case VexVVar(String name) -> {
                return vectorEnv.get(name);
            }
            case VexVec(VexScalarNode x, VexScalarNode y) -> {
                int a = evalScalar(x);
                int b = evalScalar(y);
                return new Vector(a,b);
            }
        }
    }

    private int evalScalar(VexScalarNode scalarNode) {
        switch (scalarNode) {
            case VexBinOp(VexBinOp.Op op, VexScalarNode left, VexScalarNode right) -> {
                return evalExpression(op, evalScalar(left), evalScalar(right));
            }
            case VexDot(VexVectorNode left, VexVectorNode right) -> {
                var vLeft = evalVector(left);
                var vRight = evalVector(right);
                return vLeft.x() * vRight.x() + vLeft.y() * vRight.y();
            }
            case VexNum(int value) -> {
                return value;
            }
            case VexProj(VexProj.Axis projAxis, VexVectorNode vector) -> {
                var vec = evalVector(vector);
                switch (projAxis) {
                    case X -> {
                        return vec.x();
                    }
                    case Y -> {
                        return vec.y();
                    }
                }
            }
            case VexSVar(String name) -> {
                return scalarEnv.get(name);
            }
        }

        // Error
        return Integer.MIN_VALUE;
    }

    private int evalExpression(VexBinOp.Op op, int a, int b)
    {
        switch (op)
        {
            case Add -> {
                return a + b;
            }
            case Sub -> {
                return a - b;
            }
            case Mul -> {
                return a * b;
            }
            case Div -> {
                return a / b;
            }
        }

        // Error
        return Integer.MIN_VALUE;
    }
}
