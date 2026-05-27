package proovieksam;

import cma.*;
import cma.instruction.CMaBasicInstruction;
import cma.instruction.CMaIntInstruction;
import cma.instruction.CMaLabelInstruction;
import proovieksam.ast.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static cma.instruction.CMaBasicInstruction.Code.*;
import static cma.instruction.CMaIntInstruction.Code.*;
import static cma.instruction.CMaLabelInstruction.Code.*;
import static proovieksam.ast.EstologNode.*;

public class EstologCompiler {
    private final CMaProgramWriter pw = new CMaProgramWriter();
    private final Map<String, Integer> variableMap = new HashMap<>();

    public static CMaProgram compile(EstologProg prog) {
        EstologCompiler estologCompiler = new EstologCompiler();
        estologCompiler.compileNode(prog);
        return estologCompiler.pw.toProgram();
    }

    private void compileNode(EstologNode node) {
        switch (node) {
            case EstologBinOp estologBinOp -> {
                switch (estologBinOp)
                {
                    case EstologJa estologJa -> {
                        compileNode(estologJa.left());
                        compileNode(estologJa.right());
                        pw.visit(AND);
                    }
                    case EstologVoi estologVoi -> {
                        compileNode(estologVoi.left());
                        compileNode(estologVoi.right());
                        pw.visit(OR);
                    }
                    case EstologVordus estologVordus -> {
                        compileNode(estologVordus.left());
                        compileNode(estologVordus.right());
                        pw.visit(EQ);
                    }
                }
            }
            case EstologDef estologDef -> {
                var varName = estologDef.nimi();
                compileNode(estologDef.avaldis());
                if (variableMap.containsKey(varName))
                {
                    pw.visit(STOREA, variableMap.get(varName));
                    pw.visit(POP);
                } else {
                    variableMap.put(varName, variableMap.size());
                }

            }
            case EstologKui(EstologNode kui, EstologNode siis, EstologNode muidu) -> {
                CMaLabel elseLabel = new CMaLabel();
                CMaLabel endLabel = new CMaLabel();
                compileNode(kui);
                pw.visit(JUMPZ, elseLabel);
                compileNode(siis);
                pw.visit(JUMP, endLabel);
                pw.visit(elseLabel);
                if (muidu == null) {
                    pw.visit(LOADC, 1);
                } else {
                    compileNode(muidu);
                }
                pw.visit(endLabel);
            }
            case EstologLiteraal estologLiteraal -> {
                boolean value = estologLiteraal.value();
                //                      1 = true, 0 = false
                pw.visit(LOADC, value ? 1 : 0);
            }
            case EstologMuutuja estologMuutuja -> {
                var varName = estologMuutuja.nimi();

                pw.visit(LOADA, variableMap.get(varName));
            }
            case EstologProg estologProg -> {
                compileNode(estologProg.avaldis());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        EstologProg prog = prog(
                kui(vordus(var("x"), var("y")), var("a"), var("b")),

                def("x", lit(false)),
                def("y", lit(true)),
                def("a", ja(var("x"), var("y"))),
                def("b", voi(var("x"), var("y")))
        );

        // väärtustame otse
        // System.out.printf("eval: %b%n", EstologEvaluator.eval(prog));

        // kompileeri avaldist arvutav CMa programm
        CMaProgram program = compile(prog);

        // kirjuta programm faili, mida saab Vam-iga vaadata
        CMaStack initialStack = new CMaStack();
        program.toFile(Paths.get("cmas", "estolog.cma"), initialStack);

        // interpreteeri CMa programm
        CMaStack finalStack = CMaInterpreter.run(program, initialStack);
        // System.out.printf("compiled: %d%n", finalStack.peek());
        System.out.printf("finalStack: %s%n", finalStack);
    }
}
