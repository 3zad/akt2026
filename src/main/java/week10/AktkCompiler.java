package week10;

import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import week7.AktkAst;
import week7.ast.*;
import week9.AktkBinding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class AktkCompiler {

    static void main(String[] args) throws IOException {
        // lihtsam viis "käsurea parameetrite andmiseks":
        //args = new String[] {"inputs/yks_pluss_yks.aktk"};

        if (args.length != 1) {
            throw new IllegalArgumentException("Sellele programmile tuleb anda parameetriks kompileeritava AKTK faili nimi");
        }

        Path sourceFile = Paths.get(args[0]);
        if (!Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("Ei leia faili nimega '" + sourceFile + "'");
        }

        String className = sourceFile.getFileName().toString().replace(".aktk", "");
        Path classFile = sourceFile.toAbsolutePath().getParent().resolve(className + ".class");

        createClassFile(sourceFile, className, classFile);
    }

    private static void createClassFile(Path sourceFile, String className, Path classFile) throws IOException {
        // loen faili sisu muutujasse
        String source = Files.readString(sourceFile);

        // parsin ja moodustan AST'i
        Statement ast = AktkAst.createAst(source);

        // seon muutujad
        AktkBinding.bind(ast);

        // kompileerin
        byte[] bytes = createClass(ast, className);
        Files.write(classFile, bytes);
    }

    public static byte[] createClass(Statement statement, String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, "java/lang/Object", null);
        cw.visitSource(null, null);

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC + ACC_STATIC,                     // modifikaatorid
                "main",                                        // meetodi nimi
                "([Ljava/lang/String;)V",                    // meetodi kirjeldaja
                null,                                         // geneerikute info
                new String[] { "java/io/IOException" });
        mv.visitCode();

        // maps each variable to its local slot index and slot 0 is reserved for String[] args.
        Map<VariableBinding, Integer> varMap = new HashMap<>();
        int[] nextSlot = {1}; // slot 0 = String[] args

        compileStatement(statement, mv, varMap, nextSlot, className);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    static void compileStatement(Statement stmt, MethodVisitor mv,
                                 Map<VariableBinding, Integer> varMap, int[] nextSlot, String className) {
        switch (stmt) {
            case Block(var statements) -> {
                for (Statement s : statements) {
                    compileStatement(s, mv, varMap, nextSlot, className);
                }
            }

            case VariableDeclaration vd -> {
                int slot = nextSlot[0]++;
                varMap.put(vd, slot);
                if (vd.getInitializer() != null) {
                    compileExpression(vd.getInitializer(), mv, varMap, className);
                } else {
                    mv.visitLdcInsn(0);
                }
                mv.visitVarInsn(Opcodes.ISTORE, slot);
            }

            case Assignment a -> {
                compileExpression(a.getExpression(), mv, varMap, className);
                mv.visitVarInsn(Opcodes.ISTORE, varMap.get(a.getBinding()));
            }

            case ExpressionStatement(Expression e) -> {
                compileExpression(e, mv, varMap, className);
                mv.visitInsn(Opcodes.POP);
            }

            case IfStatement(Expression cond, Block thenBranch, Block elseBranch) -> {
                Label elseLabel = new Label();
                Label endLabel = new Label();
                compileExpression(cond, mv, varMap, className);
                mv.visitJumpInsn(Opcodes.IFEQ, elseLabel);
                compileStatement(thenBranch, mv, varMap, nextSlot, className);
                mv.visitJumpInsn(Opcodes.GOTO, endLabel);
                mv.visitLabel(elseLabel);
                if (elseBranch != null) {
                    compileStatement(elseBranch, mv, varMap, nextSlot, className);
                }
                mv.visitLabel(endLabel);
            }

            case WhileStatement(Expression cond, Block body) -> {
                Label start = new Label();
                Label end = new Label();
                mv.visitLabel(start);
                compileExpression(cond, mv, varMap, className);
                mv.visitJumpInsn(Opcodes.IFEQ, end);
                compileStatement(body, mv, varMap, nextSlot, className);
                mv.visitJumpInsn(Opcodes.GOTO, start);
                mv.visitLabel(end);
            }

            default -> {}
        }
    }

    static void compileExpression(Expression expr, MethodVisitor mv,
                                  Map<VariableBinding, Integer> varMap, String className) {
        switch (expr) {
            case IntegerLiteral il -> mv.visitLdcInsn(il.value());

            case Variable v -> mv.visitVarInsn(Opcodes.ILOAD, varMap.get(v.getBinding()));

            case FunctionCall fc -> {
                String name = fc.getFunctionName();
                List<Expression> args = fc.getArguments();

                if (fc.isArithmeticOperation()) {
                    compileExpression(args.get(0), mv, varMap, className);
                    compileExpression(args.get(1), mv, varMap, className);
                    switch (name) {
                        case "+" -> mv.visitInsn(Opcodes.IADD);
                        case "-" -> mv.visitInsn(Opcodes.ISUB);
                        case "*" -> mv.visitInsn(Opcodes.IMUL);
                        case "/" -> mv.visitInsn(Opcodes.IDIV);
                        case "%" -> mv.visitInsn(Opcodes.IREM);
                        default  -> throw new IllegalStateException(name);
                    }
                } else if (fc.isComparisonOperation()) {
                    compileExpression(args.get(0), mv, varMap, className);
                    compileExpression(args.get(1), mv, varMap, className);
                    Label trueLabel = new Label();
                    Label endLabel = new Label();
                    switch (name) {
                        case "==" -> mv.visitJumpInsn(Opcodes.IF_ICMPEQ, trueLabel);
                        case "!=" -> mv.visitJumpInsn(Opcodes.IF_ICMPNE, trueLabel);
                        case "<"  -> mv.visitJumpInsn(Opcodes.IF_ICMPLT, trueLabel);
                        case "<=" -> mv.visitJumpInsn(Opcodes.IF_ICMPLE, trueLabel);
                        case ">"  -> mv.visitJumpInsn(Opcodes.IF_ICMPGT, trueLabel);
                        case ">=" -> mv.visitJumpInsn(Opcodes.IF_ICMPGE, trueLabel);
                        default   -> throw new IllegalStateException(name);
                    }
                    mv.visitLdcInsn(0);
                    mv.visitJumpInsn(Opcodes.GOTO, endLabel);
                    mv.visitLabel(trueLabel);
                    mv.visitLdcInsn(1);
                    mv.visitLabel(endLabel);
                } else {
                    for (Expression arg : args) {
                        compileExpression(arg, mv, varMap, className);
                    }
                    String desc = "(" + "I".repeat(args.size()) + ")I";
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "week10/AktkCompilerBuiltins", name, desc, false);
                }
            }

            case StringLiteral sl -> throw new UnsupportedOperationException("String literals not supported");
        }
    }
}
