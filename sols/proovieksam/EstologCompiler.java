package proovieksam;

import cma.*;
import proovieksam.ast.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static cma.instruction.CMaBasicInstruction.Code.*;
import static cma.instruction.CMaIntInstruction.Code.*;
import static cma.instruction.CMaLabelInstruction.Code.JUMP;
import static cma.instruction.CMaLabelInstruction.Code.JUMPZ;
import static proovieksam.ast.EstologNode.*;

public class EstologCompiler {
    private final CMaProgramWriter pw = new CMaProgramWriter();
    private final List<String> variables = new ArrayList<>();

    public static CMaProgram compile(EstologProg prog) {
        EstologCompiler estologCompiler = new EstologCompiler();
        estologCompiler.compileNode(prog);
        return estologCompiler.pw.toProgram();
    }

    private void compileNode(EstologNode node) {
        switch (node) {
            case EstologLiteraal(boolean value) -> {
                pw.visit(LOADC, CMaUtils.bool2int(value));
            }
            case EstologMuutuja(String nimi) -> {
                int variableIndex = variables.indexOf(nimi);
                if (variableIndex < 0) throw new NoSuchElementException("Undefined variable " + node);
                pw.visit(LOADA, variableIndex);
            }
            case EstologJa(EstologNode left, EstologNode right) -> {
                compileNode(left);
                compileNode(right);
                pw.visit(AND);
            }
            case EstologVoi(EstologNode left, EstologNode right) -> {
                compileNode(left);
                compileNode(right);
                pw.visit(OR);
            }
            case EstologVordus(EstologNode left, EstologNode right) -> {
                compileNode(left);
                compileNode(right);
                pw.visit(EQ);
            }
            case EstologKui(EstologNode kui, EstologNode siis, EstologNode muidu) -> {
                CMaLabel _else = new CMaLabel();
                CMaLabel _endif = new CMaLabel();
                compileNode(kui);
                pw.visit(JUMPZ, _else);
                compileNode(siis);
                pw.visit(JUMP, _endif);
                pw.visit(_else);
                if (muidu != null)
                    compileNode(muidu);
                else
                    pw.visit(LOADC, CMaUtils.bool2int(true));
                pw.visit(_endif);
            }
            case EstologDef(String nimi, EstologNode avaldis) -> {
                compileNode(avaldis);

                int variableIndex = variables.indexOf(nimi);
                if (variableIndex >= 0) {
                    pw.visit(STOREA, variableIndex);
                    pw.visit(POP);
                } else {
                    variables.add(nimi);
                }
            }
            case EstologProg(EstologNode avaldis, List<EstologDef> defs) -> {
                for (EstologDef def : defs) {
                    compileNode(def);
                }
                compileNode(avaldis);
            }
        }
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
