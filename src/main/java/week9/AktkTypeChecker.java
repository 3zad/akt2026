package week9;

import week7.ast.*;
import java.util.List;
import java.util.Objects;

public class AktkTypeChecker {

    public static void check(AstNode ast) {
        AktkBinding.bind(ast);
        new Checker().checkType(ast);
    }

    private static class Checker {

        public String checkType(AstNode node) {
            if (node == null) return null;

            if (node instanceof IntegerLiteral) {
                return "Integer";
            }
            else if (node instanceof StringLiteral) {
                return "String";
            }
            else if (node instanceof Variable) {
                Variable var = (Variable) node;
                if (var.getBinding() == null) {
                    throw new RuntimeException("Undefined variable used: " + var.getName());
                }
                return var.getBinding().type();
            }
            else if (node instanceof VariableDeclaration) {
                VariableDeclaration decl = (VariableDeclaration) node;
                validateAllowedType(decl.type());

                if (decl.type() == null && decl.getInitializer() == null) {
                    throw new RuntimeException("Variable declaration for '" + decl.variableName() + "' must have a type or an initializer.");
                }

                if (decl.getInitializer() != null) {
                    String initType = checkType(decl.getInitializer());

                    if (decl.type() == null) {
                        decl.setType(initType);
                    } else if (!decl.type().equals(initType)) {
                        throw new RuntimeException("Type mismatch: Cannot initialize variable of type " + decl.type() + " with " + initType);
                    }
                }
                return null;
            }
            else if (node instanceof Assignment) {
                Assignment assign = (Assignment) node;
                if (assign.getBinding() == null) {
                    throw new RuntimeException("Assigning to undefined variable: " + assign.getVariableName());
                }

                String expectedType = assign.getBinding().type();
                String actualType = checkType(assign.getExpression());

                if (!expectedType.equals(actualType)) {
                    throw new RuntimeException("Assignment type mismatch: Variable is " + expectedType + " but assigned " + actualType);
                }
                return null;
            }
            else if (node instanceof FunctionDefinition) {
                FunctionDefinition fd = (FunctionDefinition) node;
                validateAllowedType(fd.returnType());

                for (FunctionParameter param : fd.params()) {
                    validateAllowedType(param.type());
                }

                checkType(fd.body());
                return null;
            }
            else if (node instanceof ReturnStatement) {
                ReturnStatement ret = (ReturnStatement) node;
                if (ret.getFunctionBinding() == null) {
                    throw new RuntimeException("Return statement found outside of a function.");
                }

                String expectedType = ret.getFunctionBinding().returnType();
                String actualType = ret.getExpression() == null ? null : checkType(ret.getExpression());

                if (!Objects.equals(expectedType, actualType)) {
                    throw new RuntimeException("Return type mismatch: Expected " + expectedType + " but got " + actualType);
                }
                return null;
            }
            else if (node instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) node;
                String condType = checkType(ifStmt.condition());
                if (!"Integer".equals(condType)) {
                    throw new RuntimeException("'if' condition must be an Integer.");
                }

                checkType(ifStmt.thenBranch());
                checkType(ifStmt.elseBranch());
                return null;
            }
            else if (node instanceof WhileStatement) {
                WhileStatement whileStmt = (WhileStatement) node;
                String condType = checkType(whileStmt.condition());
                if (!"Integer".equals(condType)) {
                    throw new RuntimeException("'while' condition must be an Integer.");
                }

                checkType(whileStmt.body());
                return null;
            }
            else if (node instanceof FunctionCall) {
                return validateFunctionCall((FunctionCall) node);
            }
            else {
                for (Object child : node.getChildren()) {
                    if (child instanceof AstNode) {
                        checkType((AstNode) child);
                    }
                }
                return null;
            }
        }

        private String validateFunctionCall(FunctionCall call) {
            String name = call.getFunctionName();
            List<Expression> args = call.getArguments();

            switch (name) {
                case "+":
                    assertArgCount(name, args, 2);
                    String t1 = checkType(args.get(0));
                    String t2 = checkType(args.get(1));
                    if ("Integer".equals(t1) && "Integer".equals(t2)) return "Integer";
                    if ("String".equals(t1) && "String".equals(t2)) return "String";
                    throw new RuntimeException("Operator '+' requires two Integers or two Strings.");

                case "-": case "*": case "/": case "%":
                    if (args.size() == 1) {
                        assertSameType(checkType(args.get(0)), "Integer", name);
                    } else {
                        assertArgCount(name, args, 2);
                        assertSameType(checkType(args.get(0)), "Integer", name);
                        assertSameType(checkType(args.get(1)), "Integer", name);
                    }
                    return "Integer";

                case "==": case "!=": case "<": case "<=": case ">": case ">=":
                    assertArgCount(name, args, 2);
                    String comp1 = checkType(args.get(0));
                    String comp2 = checkType(args.get(1));
                    if (comp1 == null || !comp1.equals(comp2)) {
                        throw new RuntimeException("Comparison operators require both sides to be of the same type.");
                    }
                    return "Integer";

                case "printInt":
                    assertArgCount(name, args, 1);
                    assertSameType(checkType(args.get(0)), "Integer", name);
                    return null;

                case "printString":
                    assertArgCount(name, args, 1);
                    assertSameType(checkType(args.get(0)), "String", name);
                    return null;

                case "print":
                    assertArgCount(name, args, 1);
                    checkType(args.get(0));
                    return null;

                case "readInt":
                    assertArgCount(name, args, 0);
                    return "Integer";

                case "readString":
                    assertArgCount(name, args, 0);
                    return "String";
            }

            FunctionDefinition fd = call.getFunctionBinding();
            if (fd == null) {
                throw new RuntimeException("Call to undefined function: " + name);
            }
            if (fd.params().size() != args.size()) {
                throw new RuntimeException("Argument count mismatch for function " + name);
            }

            for (int i = 0; i < args.size(); i++) {
                String actualArgType = checkType(args.get(i));
                String expectedParamType = fd.params().get(i).type();
                if (!Objects.equals(actualArgType, expectedParamType)) {
                    throw new RuntimeException("Argument type mismatch in call to " + name);
                }
            }

            return fd.returnType();
        }

        private void validateAllowedType(String type) {
            if (type != null && !type.equals("Integer") && !type.equals("String")) {
                throw new RuntimeException("Unsupported type: " + type + ". Only Integer and String are allowed.");
            }
        }

        private void assertArgCount(String op, List<Expression> args, int expected) {
            if (args.size() != expected) {
                throw new RuntimeException("Operator/Function '" + op + "' expects " + expected + " arguments, got " + args.size());
            }
        }

        private void assertSameType(String actual, String expected, String context) {
            if (!expected.equals(actual)) {
                throw new RuntimeException("Type mismatch in '" + context + "'. Expected " + expected + ", got " + actual);
            }
        }
    }

    public static void main(String[] args) {
        AstNode ast = week7.AktkAst.createAst("\n" +
                "var x = 5;\n" +
                " \n" +
                "if x / 2 == 0 then {\n" +
                "    var y = 0;\n" +
                "    printInt(x)\n" +
                "} else {\n" +
                "    var y = 1;\n" +
                "    printInt(y)\n" +
                "};\n" +
                " \n" +
                "printInt(y)");

        check(ast);
        System.out.println(ast);
    }
}