package eksam1;

import cma.CMaInterpreter;
import cma.CMaProgram;
import cma.CMaProgramWriter;
import cma.CMaStack;
import eksam1.ast.scalar.*;
import eksam1.ast.vector.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static cma.instruction.CMaBasicInstruction.Code.*;
import static cma.instruction.CMaIntInstruction.Code.LOADA;
import static cma.instruction.CMaIntInstruction.Code.LOADC;
import static eksam1.ast.VexNode.*;
import static eksam1.ast.scalar.VexProj.Axis.*;

public class VexCompiler {
    private final CMaProgramWriter pw = new CMaProgramWriter();
    private final Map<String, Integer> variables;

    private VexCompiler(Map<String, Integer> variables) {
        this.variables = variables;
    }

    public static CMaProgram compile(VexVectorNode vectorNode, Map<String, Integer> variables) {
        VexCompiler compiler = new VexCompiler(variables);

        // kompileeri X-koordinaadi kood
        compiler.compileVector(vectorNode, X);
        // kompileeri Y-koordinaadi kood
        compiler.compileVector(vectorNode, Y);

        return compiler.pw.toProgram();
    }

    /**
     * @param axis kumba koordinaati parasjagu kompileeritakse
     */
    private void compileVector(VexVectorNode vectorNode, VexProj.Axis axis) {
        throw new UnsupportedOperationException();
    }

    static void main() throws IOException {
        VexVectorNode prog =
                plus(
                        scale(
                                dot(vvar("X"), vvar("X")),
                                vec(proj(Y, vvar("Y")), mul(num(-1), proj(X, vvar("Y"))))),
                        vec(
                                div(proj(Y, vvar("X")), num(2)),
                                add(proj(X, vvar("Z")), num(10))));

        Map<String, Integer> scalarEnv =
                Map.of("a", 2, "b", -1, "foo", 42);
        Map<String, Vector> vectorEnv =
                Map.of("X", new Vector(2, 3), "Y", new Vector(-1, 5), "Z", new Vector(10, -7));


        // teisenda väärtuskeskkond CMa masina jaoks sobivaks
        Map<String, Integer> variables = new HashMap<>();
        CMaStack initialStack = new CMaStack();
        int stackPointer = 0;
        for (Map.Entry<String, Integer> entry : scalarEnv.entrySet()) {
            variables.put(entry.getKey(), stackPointer);
            initialStack.push(entry.getValue());
            stackPointer++;
        }
        for (Map.Entry<String, Vector> entry : vectorEnv.entrySet()) {
            variables.put(entry.getKey(), stackPointer);
            initialStack.push(entry.getValue().x());
            initialStack.push(entry.getValue().y());
            stackPointer += 2;
        }

        // trüki interpretaatori tulemus võrdluseks
        System.out.println("eval: " + VexEvaluator.eval(prog, scalarEnv, vectorEnv));

        // kompileeri lauset täitev CMa programm
        CMaProgram program = compile(prog, variables);

        // trüki algne CMa stack
        System.out.println("initial: " + initialStack);

        // kirjuta programm faili, mida saab Vam-iga vaadata
        program.toFile(Paths.get("cmas", "vex.cma"), initialStack);

        // interpreteeri CMa programm
        CMaStack finalStack = CMaInterpreter.run(program, initialStack);
        System.out.println("compiled: " + finalStack);
    }
}
