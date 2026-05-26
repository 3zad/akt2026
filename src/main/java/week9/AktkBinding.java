package week9;

import week7.ast.*;
import java.util.*;

public class AktkBinding {

    /**
     * Assigns each variable reference (week7.ast.Variable) its binding element
     * (week7.ast.VariableBinding).
     * Binds assignments, function calls, and return statements as well.
     */
    public static void bind(AstNode node) {
        new Binder().bindNode(node);
    }

    private static class Binder {
        private Deque<Map<String, VariableBinding>> env = new ArrayDeque<>();

        private Map<String, FunctionDefinition> functions = new HashMap<>();

        private Deque<FunctionDefinition> currentFunctions = new ArrayDeque<>();

        public Binder() {
            env.push(new HashMap<>());
        }

        public void bindNode(AstNode node) {
            if (node == null) return;

            if (node instanceof Block) {
                env.push(new HashMap<>());

                for (Object child : node.getChildren()) {
                    if (child instanceof FunctionDefinition fd) {
                        functions.put(fd.name(), fd);
                    }
                }

                // 2nd Pass in block: Traverse statements
                for (Object child : node.getChildren()) {
                    if (child instanceof AstNode) {
                        bindNode((AstNode) child);
                    }
                }

                env.pop();

            } else if (node instanceof VariableDeclaration decl) {
                bindNode(decl.getInitializer());
                assert env.peek() != null;
                env.peek().put(decl.variableName(), decl);

            } else if (node instanceof FunctionDefinition fd) {
                functions.put(fd.name(), fd);

                env.push(new HashMap<>());
                currentFunctions.push(fd);

                for (FunctionParameter param : fd.params()) {
                    assert env.peek() != null;
                    env.peek().put(param.variableName(), param);
                }

                bindNode(fd.body());

                currentFunctions.pop();
                env.pop();

            } else if (node instanceof Variable var) {
                VariableBinding binding = lookupVariable(var.getName());
                if (binding != null) {
                    var.setBinding(binding);
                }

            } else if (node instanceof Assignment assign) {
                bindNode(assign.getExpression());

                VariableBinding binding = lookupVariable(assign.getVariableName());
                if (binding != null) {
                    assign.setBinding(binding);
                }

            } else if (node instanceof FunctionCall call) {
                for (Expression arg : call.getArguments()) {
                    bindNode(arg);
                }
                FunctionDefinition fd = functions.get(call.getFunctionName());
                if (fd != null) {
                    call.setFunctionBinding(fd);
                }

            } else if (node instanceof ReturnStatement ret) {
                bindNode(ret.getExpression());
                if (!currentFunctions.isEmpty()) {
                    ret.setFunctionBinding(currentFunctions.peek());
                }

            } else if (node instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) node;
                bindNode(ifStmt.condition());
                bindNode(ifStmt.thenBranch());
                bindNode(ifStmt.elseBranch());

            } else if (node instanceof WhileStatement) {
                WhileStatement whileStmt = (WhileStatement) node;
                bindNode(whileStmt.condition());
                bindNode(whileStmt.body());

            } else if (node instanceof ExpressionStatement) {
                // Adjust to .getExpression() if your AST uses 'get' prefixes here!
                ExpressionStatement exprStmt = (ExpressionStatement) node;
                bindNode(exprStmt.expression());
            }
            else {
                for (Object child : node.getChildren()) {
                    if (child instanceof AstNode) {
                        bindNode((AstNode) child);
                    }
                }
            }
        }

        private VariableBinding lookupVariable(String name) {
            for (Map<String, VariableBinding> scope : env) {
                if (scope.containsKey(name)) {
                    return scope.get(name);
                }
            }
            return null; // Undefined variable
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

        bind(ast);
        System.out.println(ast);

        ExpressionStatement lastStmt = (ExpressionStatement) ast.getChildren().getLast();
        FunctionCall printCall = (FunctionCall) lastStmt.expression();
        Variable lastY = (Variable) printCall.getArguments().getFirst();

        System.out.println("last 'y': " + lastY.getBinding());
    }
}