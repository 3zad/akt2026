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
        throw new UnsupportedOperationException();
    }
}
