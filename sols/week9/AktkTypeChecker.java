package week9;

import week7.ast.*;
import week8.AktkInterpreterBuiltins;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class AktkTypeChecker {

    public static void check(Statement statement) {
        // Meetod peab viskama RuntimeException-i (või mõne selle alamklassi erindi), kui:
        // 1) programmis kasutatakse deklareerimata muutujaid või funktsioone,
        // mida pole defineeritud ei antud programmis ega "standardteegis"
        //    (vt. interpretaatori koduülesannet)
        // 2) programmis kasutatakse mõnd lihttüüpi, mis pole ei String ega Integer
        // 3) leidub muutuja deklaratsioon, milles pole antud ei tüüpi ega algväärtusavaldist
        // 4) programmis on mõnel avaldisel vale tüüp
        AktkBinding.bind(statement);
        new AktkTypeChecker().checkStatement(statement);
    }

    // TODO: enum?
    private static final String INTEGER_TYPE = "Integer";
    private static final String STRING_TYPE = "String";

    private void checkStatement(Statement statement) {
        switch (statement) {
            case Assignment assignment -> {
                if (assignment.getBinding() == null) {
                    throw new RuntimeException("unbound variable '%s'".formatted(assignment.getVariableName()));
                }

                String variableType = assignment.getBinding().type();
                String expressionType = checkExpression(assignment.getExpression());
                if (!variableType.equals(expressionType)) {
                    throw new RuntimeException("assignment expression type differs from variable type");
                }
            }
            case Block(List<Statement> statements) -> {
                for (Statement stmt : statements) {
                    checkStatement(stmt);
                }
            }
            case ExpressionStatement(Expression expression) -> checkExpression(expression); // Eraldiseisval avaldisel on ka tüüp, kontrollime, aga ignoreerime seda
            case FunctionDefinition functionDefinition -> {
                // Parameetrite tüübid peavad olema toetatud
                for (FunctionParameter parameter : functionDefinition.params()) {
                    if (isInvalidType(parameter.type())) {
                        throw new RuntimeException("unknown parameter type");
                    }
                }
                // Tagastustüüp peab olema toetatud, kui see on olemas
                String returnType = functionDefinition.returnType();
                if (returnType != null && isInvalidType(returnType)) {
                    throw new RuntimeException("unknown return type");
                }

                checkStatement(functionDefinition.body());
            }
            case IfStatement(Expression condition, Block thenBranch, Block elseBranch) -> {
                String conditionType = checkExpression(condition);
                if (!conditionType.equals(INTEGER_TYPE)) {
                    throw new RuntimeException("invalid condition type");
                }
                checkStatement(thenBranch);
                checkStatement(elseBranch);
            }
            case ReturnStatement returnStatement -> {
                String expressionType = checkExpression(returnStatement.getExpression());
                String returnType = returnStatement.getFunctionBinding().returnType();
                if (!expressionType.equals(returnType)) {
                    throw new RuntimeException("return expression type differs from function return type");
                }
            }
            case VariableDeclaration variableDeclaration -> {
                // Muutuja deklaratsioonis peab olema kas tüüp või algväärtusavaldis või mõlemad.
                // Kui on mõlemad, siis nende tüübid peavad klappima.

                // Algväärtusavaldis ei näe seda muutujat, mida parasjagu deklareeritakse
                // st. lause var x = x + 1 omab mõtet vaid siis, kui ka mõnes ülevalpool olevas skoobis
                // on deklareeritud muutuja x.

                String declarationType = variableDeclaration.type();
                if (variableDeclaration.getInitializer() != null) {
                    String initializerType = checkExpression(variableDeclaration.getInitializer());
                    if (declarationType != null) {
                        if (!declarationType.equals(initializerType)) {
                            throw new RuntimeException("variable declaration type differs from initializer expression type");
                        }
                    }
                    else {
                        variableDeclaration.setType(initializerType);
                    }
                }
                else {
                    if (declarationType == null) {
                        throw new RuntimeException("missing variable type");
                    }
                }

                // Muutuja tüüp (deklareeritud või tuletatud) peab olema toetatud
                if (isInvalidType(variableDeclaration.type())) {
                    throw new RuntimeException("unknown variable type");
                }
            }
            case WhileStatement(Expression condition, Block body) -> {
                String conditionType = checkExpression(condition);
                if (!conditionType.equals(INTEGER_TYPE)) {
                    throw new RuntimeException("invalid condition type");
                }
                checkStatement(body);
            }
        }
    }

    private String checkExpression(Expression expression) {
        return switch (expression) {
            case FunctionCall functionCall -> {
                String name = functionCall.getFunctionName();
                List<String> argTypes = new ArrayList<>();
                for (Expression arg : functionCall.getArguments()) {
                    argTypes.add(checkExpression(arg));
                }

                switch (name) {
                    case "+":
                        if (argTypes.get(0).equals(argTypes.get(1))) yield argTypes.get(0);
                        else throw new IllegalArgumentException("Plus types must match!");
                    case "==":
                    case "!=":
                        if (argTypes.get(0).equals(argTypes.get(1))) yield INTEGER_TYPE;
                        else throw new IllegalArgumentException("Equality types must match!");
                    default:
                        if (functionCall.isArithmeticOperation() || functionCall.isComparisonOperation()) {
                            for (String t : argTypes) {
                                if (!t.equals(INTEGER_TYPE)) {
                                    throw new IllegalArgumentException("Operation needs integer arguments!");
                                }
                            }
                            yield INTEGER_TYPE;
                        }
                        FunctionDefinition functionBinding = functionCall.getFunctionBinding();
                        if (functionBinding != null)
                            yield checkAktkFunction(functionBinding, argTypes);
                        yield checkBuiltinFunction(name, argTypes);
                }
            }
            case IntegerLiteral _ -> INTEGER_TYPE;
            case StringLiteral _ -> STRING_TYPE;
            case Variable variable -> {
                if (variable.getBinding() == null) {
                    throw new RuntimeException("unbound variable '%s'".formatted(variable.getName()));
                }

                yield variable.getBinding().type();
            }
        };
    }

    private String checkAktkFunction(FunctionDefinition functionDefinition, List<String> argTypes) {
        List<String> expected = new ArrayList<>();
        for (FunctionParameter functionParameter : functionDefinition.params()) {
            expected.add(functionParameter.type());
        }
        if (!expected.equals(argTypes)) throw new IllegalArgumentException("Type error");
        return functionDefinition.returnType();
    }


    private String checkBuiltinFunction(String name, List<String> argTypes) {
        // Leia argumendite tüüpide klassid, et reflection õige meetodi üles leiaks
        Class<?>[] argClasses = new Class[argTypes.size()];
        for (int i = 0; i < argTypes.size(); i++) {
            argClasses[i] = getClassForType(argTypes.get(i));
        }

        try {
            Method method = AktkInterpreterBuiltins.class.getDeclaredMethod(name, argClasses);
            return getTypeForClass(method.getReturnType());
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("unknown builtin function", e);
        }
    }

    private static boolean isInvalidType(String type) {
        return !switch (type) {
            case INTEGER_TYPE, STRING_TYPE -> true;
            default -> false;
        };
    }

    private static String getTypeForClass(Class<?> clazz) {
        if (clazz == Integer.class) {
            return INTEGER_TYPE;
        }
        else if (clazz == String.class) {
            return STRING_TYPE;
        }
        else if (clazz == void.class) {
            return null;
        }
        else {
            throw new UnsupportedOperationException("unsupported Java type " + clazz);
        }
    }

    private static Class<?> getClassForType(String type) {
        return switch (type) {
            case INTEGER_TYPE -> Integer.class;
            case STRING_TYPE -> String.class;
            default -> throw new UnsupportedOperationException("unsupported AKTK type " + type);
        };
    }
}
