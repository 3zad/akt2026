package week8;

import week7.AktkAst;
import week7.ast.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AktkInterpreter {
    public static void main(String[] args) {
        run("print(2)");
    }

    // user-defined functions
    private final Map<String, FunctionDefinition> functions = new HashMap<>();
    // Variable environment
    private final Environment<Object> env = new Environment<>();

    // entry point
    public static void run(String program) {
        Statement ast = AktkAst.createAst(program);
        new AktkInterpreter().execute(ast);
    }

    private Object execute(Statement stmt) {
        if (stmt instanceof Block block) {
            return executeBlock(block, false);
        }
        if (stmt instanceof VariableDeclaration decl) {
            Object value = decl.getInitializer() != null ? evaluate(decl.getInitializer()) : null;
            env.declareAssign(decl.variableName(), value);
            return null;
        }
        if (stmt instanceof Assignment asgn) {
            Object value = evaluate(asgn.getExpression());
            env.assign(asgn.getVariableName(), value);
            return null;
        }
        if (stmt instanceof IfStatement ifStmt) {
            Object cond = evaluate(ifStmt.condition());
            if (isTruthy(cond)) {
                return executeBlock(ifStmt.thenBranch(), false);
            } else if (ifStmt.elseBranch() != null) {
                return executeBlock(ifStmt.elseBranch(), false);
            }
            return null;
        }
        if (stmt instanceof WhileStatement whileStmt) {
            while (isTruthy(evaluate(whileStmt.condition()))) {
                executeBlock(whileStmt.body(), false);
            }
            return null;
        }
        if (stmt instanceof FunctionDefinition funDef) {
            functions.put(funDef.name(), funDef);
            return null;
        }
        if (stmt instanceof ReturnStatement ret) {
            return new ReturnValue(evaluate(ret.getExpression()));
        }
        if (stmt instanceof ExpressionStatement exprStmt) {
            evaluate(exprStmt.expression());
            return null;
        }
        throw new RuntimeException("Unknown statement type: " + stmt.getClass().getSimpleName());
    }

    private Object executeBlock(Block block, boolean newScope) {
        if (newScope) env.enterBlock();
        try {
            for (Statement s : block.statements()) {
                Object result = execute(s);
                if (result instanceof ReturnValue) {
                    return result; // propagate upward
                }
            }
            return null;
        } finally {
            if (newScope) env.exitBlock();
        }
    }

    private Object evaluate(Expression expr) {
        if (expr instanceof IntegerLiteral lit) {
            return lit.value();
        }
        if (expr instanceof StringLiteral lit) {
            return lit.value();
        }
        if (expr instanceof Variable var) {
            return env.get(var.getName());
        }
        if (expr instanceof FunctionCall call) {
            return evaluateFunctionCall(call);
        }
        throw new RuntimeException("Unknown expression type: " + expr.getClass().getSimpleName());
    }

    private Object evaluateFunctionCall(FunctionCall call) {
        String name = call.getFunctionName();
        List<Expression> args = call.getArguments();

        if (args.size() == 1 && name.equals("-")) {
            Object operand = evaluate(args.get(0));
            if (operand instanceof Integer i) return -i;
            throw new RuntimeException("Unary minus requires an integer");
        }

        if (call.isArithmeticOperation() && args.size() == 2) {
            Object left  = evaluate(args.get(0));
            Object right = evaluate(args.get(1));
            return applyArithmetic(name, left, right);
        }

        if (call.isComparisonOperation() && args.size() == 2) {
            Object left  = evaluate(args.get(0));
            Object right = evaluate(args.get(1));
            return applyComparison(name, left, right);
        }

        if (functions.containsKey(name)) {
            return callUserFunction(functions.get(name), args);
        }

        return callBuiltin(name, args);
    }

    private Object applyArithmetic(String op, Object left, Object right) {
        if (left instanceof String l && right instanceof String r) {
            if (op.equals("+")) return l + r;
            throw new RuntimeException("Unsupported string operation: " + op);
        }
        // Integer arithmetic
        if (left instanceof Integer l && right instanceof Integer r) {
            return switch (op) {
                case "+"  -> l + r;
                case "-"  -> l - r;
                case "*"  -> l * r;
                case "/"  -> l / r;
                case "%"  -> l % r;
                default   -> throw new RuntimeException("Unknown arithmetic op: " + op);
            };
        }
        throw new RuntimeException("Type mismatch for operator " + op
                + ": " + left.getClass().getSimpleName()
                + " and " + right.getClass().getSimpleName());
    }

    private Integer applyComparison(String op, Object left, Object right) {
        if (left instanceof Integer l && right instanceof Integer r) {
            return switch (op) {
                case "==" -> l.equals(r)  ? 1 : 0;
                case "!=" -> !l.equals(r) ? 1 : 0;
                case "<"  -> l < r        ? 1 : 0;
                case "<=" -> l <= r       ? 1 : 0;
                case ">"  -> l > r        ? 1 : 0;
                case ">=" -> l >= r       ? 1 : 0;
                default   -> throw new RuntimeException("Unknown comparison op: " + op);
            };
        }
        if (left instanceof String l && right instanceof String r) {
            int cmp = l.compareTo(r);
            return switch (op) {
                case "==" -> l.equals(r)  ? 1 : 0;
                case "!=" -> !l.equals(r) ? 1 : 0;
                case "<"  -> cmp < 0      ? 1 : 0;
                case "<=" -> cmp <= 0     ? 1 : 0;
                case ">"  -> cmp > 0      ? 1 : 0;
                case ">=" -> cmp >= 0     ? 1 : 0;
                default   -> throw new RuntimeException("Unknown comparison op: " + op);
            };
        }
        throw new RuntimeException("Type mismatch for comparison " + op);
    }

    private Object callUserFunction(FunctionDefinition def, List<Expression> argExprs) {
        // Evaluate arguments in the current scope before entering function scope
        List<Object> argValues = new ArrayList<>();
        for (Expression arg : argExprs) {
            argValues.add(evaluate(arg));
        }

        env.enterBlock();
        try {
            List<FunctionParameter> params = def.params();
            for (int i = 0; i < params.size(); i++) {
                env.declareAssign(params.get(i).variableName(), argValues.get(i));
            }
            Object result = executeBlock(def.body(), false); // scope already entered above
            if (result instanceof ReturnValue rv) {
                return rv.value();
            }
            return null;
        } finally {
            env.exitBlock();
        }
    }

    private Object callBuiltin(String name, List<Expression> argExprs) {
        List<Object> argValues = new ArrayList<>();
        for (Expression arg : argExprs) {
            argValues.add(evaluate(arg));
        }

        Class<?>[] types = new Class<?>[argValues.size()];
        for (int i = 0; i < argValues.size(); i++) {
            types[i] = argValues.get(i) == null ? Object.class : argValues.get(i).getClass();
        }

        try {
            Method method = AktkInterpreterBuiltins.class.getDeclaredMethod(name, types);
            return method.invoke(null, argValues.toArray());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unknown function: " + name + " with arg types " + java.util.Arrays.toString(types));
        } catch (Exception e) {
            throw new RuntimeException("Error calling builtin '" + name + "': " + e.getMessage(), e);
        }
    }

    private boolean isTruthy(Object value) {
        return value instanceof Integer i && i != 0;
    }

    private record ReturnValue(Object value) {}
}