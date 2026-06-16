package week10;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
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

import static org.objectweb.asm.Opcodes.*;


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

        // Klassi attribuudid
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
        cw.visitSource(null, null);

        // main meetod
        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC + ACC_STATIC,                     // modifikaatorid
                "main",                                        // meetodi nimi
                "([Ljava/lang/String;)V",                    // meetodi kirjeldaja
                null,                                         // geneerikute info
                new String[] { "java/io/IOException" });
        mv.visitCode();
        // terve AKTK programm tuleb kompileerida main meetodi sisse
        new AktkCompiler(mv).compileStatement(statement);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // klassi lõpetamine
        cw.visitEnd();

        // klassi baidijada genereerimine
        return cw.toByteArray();
    }

    protected final MethodVisitor mv;
    protected final Map<VariableBinding, Integer> variableIndices = new HashMap<>();

    public AktkCompiler(MethodVisitor mv) {
        this.mv = mv;
    }

    protected int getVariableIndex(VariableBinding binding) {
        // Kasuta teadaolevat indeksit, kui see on olemas,
        // või leia järgmine vaba indeks ja salvesta see.
        // Esimene muutuja saab indeksi 1, sest indeksil 0 on main meetodi parameeter (String args[]).
        return variableIndices.computeIfAbsent(binding, ignoreBinding -> variableIndices.size() + 1);
    }

    protected void compileStatement(Statement statement) {
        switch (statement) {
            case Assignment assignment -> {
                compileExpression(assignment.getExpression());
                mv.visitVarInsn(ISTORE, getVariableIndex(assignment.getBinding()));
            }
            case Block(List<Statement> statements) -> {
                for (Statement stmt : statements) {
                    compileStatement(stmt);
                }
            }
            case ExpressionStatement(Expression expression) -> {
                compileExpression(expression);
                mv.visitInsn(POP); // Eraldiseisval avaldisel on ka väärtus, aga ignoreerime seda
            }
            case FunctionDefinition _ ->
                    throw new UnsupportedOperationException("cannot compile function definitions");
            case IfStatement(Expression condition, Block thenBranch, Block elseBranch) -> {
                Label doneLabel = new Label();
                Label elseLabel = new Label();

                // Tingimus
                compileExpression(condition);
                mv.visitJumpInsn(IFEQ, elseLabel);
                // True/then haru
                compileStatement(thenBranch);
                mv.visitJumpInsn(GOTO, doneLabel); // Hüppa else haru koodist üle
                // False/else haru
                mv.visitLabel(elseLabel);
                compileStatement(elseBranch);
                // Lõpp
                mv.visitLabel(doneLabel);
            }
            case ReturnStatement _ -> throw new UnsupportedOperationException("cannot compile return statements");
            case VariableDeclaration variableDeclaration -> {
                // TODO: 03.06.20 test no initializer
                if (variableDeclaration.getInitializer() != null) {
                    compileExpression(variableDeclaration.getInitializer());
                    mv.visitVarInsn(ISTORE, getVariableIndex(variableDeclaration));
                }
            }
            case WhileStatement(Expression condition, Block body) -> {
                Label startLabel = new Label();
                Label doneLabel = new Label();

                // Tingimus
                mv.visitLabel(startLabel);
                compileExpression(condition);
                mv.visitJumpInsn(IFEQ, doneLabel);
                // True haru/tsükli keha
                compileStatement(body);
                mv.visitJumpInsn(GOTO, startLabel);
                // False haru/lõpp
                mv.visitLabel(doneLabel);
            }
        }
    }

    protected void compileExpression(Expression expression) {
        switch (expression) {
            case FunctionCall functionCall -> {
                // genereeri argumentide väärtustamise kood
                for (Expression arg : functionCall.getArguments()) {
                    compileExpression(arg);
                }

                if (functionCall.isArithmeticOperation()) {
                    compileArithmeticOperation(functionCall);
                } else if (functionCall.isComparisonOperation()) {
                    compileComparisonOperation(functionCall);
                } else {
                    compileBuiltinFunction(functionCall);
                }
            }
            case IntegerLiteral(Integer value) -> mv.visitLdcInsn(value);
            case StringLiteral(String value) -> mv.visitLdcInsn(value);
            case Variable variable -> mv.visitVarInsn(ILOAD, getVariableIndex(variable.getBinding()));
        }
    }

    protected void compileArithmeticOperation(FunctionCall functionCall) {
        switch (functionCall.getArguments().size()) {
            case 1 -> { // unaarne operaator
                switch (functionCall.getFunctionName()) {
                    case "-" -> mv.visitInsn(INEG);
                    default -> throw new UnsupportedOperationException("unknown unary operator called");
                }
            }

            case 2 -> { // binaarne operaator
                switch (functionCall.getFunctionName()) {
                    case "+" -> mv.visitInsn(IADD);
                    case "-" -> mv.visitInsn(ISUB);
                    case "*" -> mv.visitInsn(IMUL);
                    case "/" -> mv.visitInsn(IDIV);
                    case "%" -> mv.visitInsn(IREM);
                    default -> throw new UnsupportedOperationException("unknown binary operator called");
                }
            }

            default ->
                    throw new UnsupportedOperationException("arithmetic operator called with invalid number of arguments");
        }
    }

    protected void compileComparisonOperation(FunctionCall call) {
        if (call.getArguments().size() != 2) {
            throw new UnsupportedOperationException("comparison operator called with invalid number of arguments");
        }

        // Rikkalik võrdlusoperatsioonide valik on seotud jumpidega,
        // aga kuna ma ei taha avaldise väärtustamise koodi siduda if-lausega
        // siis ma kasutan jumpe lihtsalt selleks, et tekitada stacki tippu
        // kas 0 või 1
        Label trueLabel = new Label();
        Label doneLabel = new Label();

        // Tingimus
        switch (call.getFunctionName()) {
            case ">" -> mv.visitJumpInsn(IF_ICMPGT, trueLabel);
            case "<" -> mv.visitJumpInsn(IF_ICMPLT, trueLabel);
            case ">=" -> mv.visitJumpInsn(IF_ICMPGE, trueLabel);
            case "<=" -> mv.visitJumpInsn(IF_ICMPLE, trueLabel);
            case "==" -> mv.visitJumpInsn(IF_ICMPEQ, trueLabel);
            case "!=" -> mv.visitJumpInsn(IF_ICMPNE, trueLabel);
            default -> throw new UnsupportedOperationException("unknown comparison operator called");
        }
        // False haru
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, doneLabel); // Hüppa üle true haru
        // True haru
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);
        // Lõpp
        mv.visitLabel(doneLabel);
    }

    protected void compileBuiltinFunction(FunctionCall functionCall) {
        // Koosta argumentide osa meetodi tüübi descriptor'ist
        String argsDescriptor = "I".repeat(functionCall.getArguments().size());

        mv.visitMethodInsn(
                INVOKESTATIC, // Kõik builtin meetodid on staatilised
                Type.getInternalName(AktkCompilerBuiltins.class), // "week10/AktkCompilerBuiltins"
                functionCall.getFunctionName(),
                "(" + argsDescriptor + ")I", // Kõik builtin meetodid tagastavad int-i
                false);
    }
}
