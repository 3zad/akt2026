package eksam2;

import cma.CMaInterpreter;
import cma.CMaProgram;
import cma.CMaProgramWriter;
import cma.CMaStack;
import eksam2.ast.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cma.instruction.CMaBasicInstruction.Code.*;
import static cma.instruction.CMaIntInstruction.Code.*;
import static eksam2.ast.JadaNode.*;

public class JadaCompiler {
    private final CMaProgramWriter pw = new CMaProgramWriter();
    private final Map<String, Integer> lengths;
    private final Map<String, Integer> addresses = new HashMap<>();

    public JadaCompiler(Map<String, Integer> lengths) {
        this.lengths = lengths;

        // Järgmine abistav kood arvutab ettantud massiivide algusaadressid CMa stack'il
        int stackPointer = 0;
        for (Map.Entry<String, Integer> entry : lengths.entrySet()) {
            addresses.put(entry.getKey(), stackPointer);
            stackPointer += entry.getValue();
        }
    }

    public static CMaProgram compile(JadaStmt stmt, Map<String, Integer> lengths) {
        JadaCompiler jadaCompiler = new JadaCompiler(lengths);
        jadaCompiler.compileStmt(stmt);
        return jadaCompiler.pw.toProgram();
    }

    private void compileStmt(JadaStmt node) {
        throw new UnsupportedOperationException();
    }

    static void main() throws IOException {
        JadaStmt prog = foreach("i", "b",
                assign("z", add(var("z"), var("i"))));

        Map<String, List<Integer>> initialEnv =
                Map.of("b", List.of(11, 22, 33), "z", List.of(0), "i", List.of(0));

        // teisenda väärtuskeskkond CMa masina jaoks sobivaks
        CMaStack initialStack = new CMaStack();
        Map<String, Integer> lengths = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entry : initialEnv.entrySet()) {
            List<Integer> array = entry.getValue();
            for (int value : array) initialStack.push(value);
            lengths.put(entry.getKey(), array.size());
        }

        // trüki interpretaatori tulemus võrdluseks
        System.out.println("eval: " + JadaEvaluator.eval(prog, initialEnv));

        // trüki etteantud muutujate järjekord ja pikkused
        System.out.println("lengths: " + lengths);

        // kompileeri lauset täitev CMa programm
        CMaProgram program = compile(prog, lengths);

        // trüki algne CMa stack
        System.out.println("initial: " + initialStack);

        // kirjuta programm faili, mida saab Vam-iga vaadata
        program.toFile(Paths.get("cmas", "jada.cma"), initialStack);

        // interpreteeri CMa programm
        CMaStack finalStack = CMaInterpreter.run(program, initialStack);
        System.out.println("compiled: " + finalStack);
    }
}
