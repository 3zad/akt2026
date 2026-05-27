package proovieksam;

import cma.*;
import proovieksam.ast.*;

import java.io.IOException;
import java.nio.file.Paths;

import static proovieksam.ast.EstologNode.*;

public class EstologCompiler {
    private final CMaProgramWriter pw = new CMaProgramWriter();

    public static CMaProgram compile(EstologProg prog) {
        EstologCompiler estologCompiler = new EstologCompiler();
        estologCompiler.compileNode(prog);
        return estologCompiler.pw.toProgram();
    }

    private void compileNode(EstologNode node) {
        throw new UnsupportedOperationException();
    }

    static void main() throws IOException {
        EstologProg prog = prog(
                kui(vordus(var("x"), var("y")), var("a"), var("b")),

                def("x", lit(false)),
                def("y", lit(true)),
                def("a", ja(var("x"), var("y"))),
                def("b", voi(var("x"), var("y")))
        );

        // väärtustame otse
        System.out.printf("eval: %b%n", EstologEvaluator.eval(prog));

        // kompileeri avaldist arvutav CMa programm
        CMaProgram program = compile(prog);

        // kirjuta programm faili, mida saab Vam-iga vaadata
        CMaStack initialStack = new CMaStack();
        program.toFile(Paths.get("cmas", "estolog.cma"), initialStack);

        // interpreteeri CMa programm
        CMaStack finalStack = CMaInterpreter.run(program, initialStack);
        System.out.printf("compiled: %d%n", finalStack.peek());
        System.out.printf("finalStack: %s%n", finalStack);
    }
}
