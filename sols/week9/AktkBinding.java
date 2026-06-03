package week9;

import week7.ast.*;
import week8.Environment;

import java.util.List;


public class AktkBinding {
    /**
     * Määra iga antud programmis olevale muutujaviitele (week7.ast.Variable)
     * teda siduv element (week7.ast.VariableBinding,
     * st week7.ast.VariableDeclaration või week7.ast.FunctionParameter)
     * Kasuta selleks meetodit week7.ast.Variable#setBinding.
     * <p>
     * Kui muutuja kasutusele ei vasta ühtegi deklaratsiooni ega parameetrit,
     * siis jäta binding määramata.
     */
    public static void bind(Statement statement) {
        new AktkBinding().bindStatement(statement);
    }

    // Spetsiaalne muutujanimi, mida kasutame funktsiooni definitsiooni hoidmiseks keskkonnas
    private static final String FUNCTION_BINDING_NAME = "__function__";

    // Kuna hoiame samas keskkonnas nii VariableDeclaration kui ka FunctionDefinition objekte,
    // siis kasutame siin üldist AstNode ja cast'ime nii nagu vaja on
    private final Environment<AstNode> env = new Environment<>();

    // Siin pole vaja eraldi Statement ja Expression funktsioone (mõlemad tagastavad void),
    // aga nii on organiseeritum ega pea FunctionParameter-it käsitlema
    private void bindStatement(Statement statement) {
        switch (statement) {
            case Assignment assignment -> {
                assignment.setBinding((VariableBinding) env.get(assignment.getVariableName()));
                bindExpression(assignment.getExpression());
            }
            case Block(List<Statement> statements) -> {
                env.enterBlock();
                for (Statement stmt : statements) {
                    bindStatement(stmt);
                }
                env.exitBlock();
            }
            case ExpressionStatement(Expression expression) -> bindExpression(expression);
            case FunctionDefinition functionDefinition -> {
                env.declareAssign(functionDefinition.name(), functionDefinition);

                env.enterBlock();
                for (FunctionParameter param : functionDefinition.params()) {
                    env.declareAssign(param.variableName(), param);
                }
                env.declareAssign(FUNCTION_BINDING_NAME, functionDefinition);
                bindStatement(functionDefinition.body());
                env.exitBlock();
            }
            case IfStatement(Expression condition, Block thenBranch, Block elseBranch) -> {
                bindExpression(condition);
                bindStatement(thenBranch);
                bindStatement(elseBranch);
            }
            case ReturnStatement returnStatement -> {
                returnStatement.setFunctionBinding((FunctionDefinition) env.get(FUNCTION_BINDING_NAME));
                bindExpression(returnStatement.getExpression());
            }
            case VariableDeclaration variableDeclaration -> {
                if (variableDeclaration.getInitializer() != null)
                    bindExpression(variableDeclaration.getInitializer());
                env.declareAssign(variableDeclaration.variableName(), variableDeclaration);
            }
            case WhileStatement(Expression condition, Block body) -> {
                bindExpression(condition);
                bindStatement(body);
            }
        }
    }

    private void bindExpression(Expression expression) {
        switch (expression) {
            case FunctionCall functionCall -> {
                functionCall.setFunctionBinding((FunctionDefinition) env.get(functionCall.getFunctionName()));

                for (Expression arg : functionCall.getArguments()) {
                    bindExpression(arg);
                }
            }
            case IntegerLiteral _, StringLiteral _ -> {}
            case Variable variable -> variable.setBinding((VariableBinding) env.get(variable.getName()));
        }
    }
}
