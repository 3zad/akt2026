package eksam2;

import eksam2.ast.*;

import java.util.*;

public class JadaEvaluator {
    private final Map<String, List<Integer>> env;

    private JadaEvaluator(Map<String, List<Integer>> initialEnv) {
        this.env = new HashMap<>(initialEnv); // teeme koopia, sest initialEnv pole muudetav
    }

    public static Map<String, List<Integer>> eval(JadaStmt stmt, Map<String, List<Integer>> initialEnv) {
        JadaEvaluator jadaEvaluator = new JadaEvaluator(initialEnv);
        jadaEvaluator.evalStmt(stmt);
        return jadaEvaluator.env;
    }

    private void evalStmt(JadaStmt node) {
        throw new UnsupportedOperationException();
    }
}
