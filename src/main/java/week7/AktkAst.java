package week7;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import utils.ExceptionErrorListener;
import week7.ast.*;
import week7.AktkParser.*;
import week7.AktkLexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class AktkAst {

    // Ise testimiseks soovitame kasutada selline meetod: inputs/sample.aktk failis sisend muuta.
    // Kui testid sinna kopeerida, siis äkki võtab IDE escape sümbolid ära ja on selgem,
    // milline see kood tegelikult välja näeb.
    public static void main() throws IOException {
        String program = Files.readString(Paths.get("inputs", "sample.aktk"));
        AstNode ast = createAst(program);
        System.out.println(ast);
    }

    // Automaattestide jaoks vajalik meetod.
    public static Statement createAst(String program) {
        AktkLexer lexer = new AktkLexer(CharStreams.fromString(program));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ExceptionErrorListener());

        AktkParser parser = new AktkParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());

        ParseTree tree = parser.program();
        return parseTreeToAst(tree);
    }

    // Põhimeetod, mida tuleks implementeerida:
    private static Statement parseTreeToAst(ParseTree tree) {

        if (tree instanceof ProgramContext ctx) {
            return parseTreeToAst(ctx.statements());
        }

        if (tree instanceof StatementsContext ctx) {
            List<Statement> statements = new ArrayList<>();
            for (StatementContext statement : ctx.statement()) {
                statements.add((Statement) parseTreeToAst(statement));
            }
            return new Block(statements);
        }

        if (tree instanceof StatementContext ctx) {
            if (ctx.varDecl() != null)    return parseTreeToAst(ctx.varDecl());
            if (ctx.assignment() != null) return parseTreeToAst(ctx.assignment());
            if (ctx.ifStmt() != null)     return parseTreeToAst(ctx.ifStmt());
            if (ctx.whileStmt() != null)  return parseTreeToAst(ctx.whileStmt());
            if (ctx.funDecl() != null)    return parseTreeToAst(ctx.funDecl());
            if (ctx.returnStmt() != null) return parseTreeToAst(ctx.returnStmt());
            if (ctx.block() != null)      return parseTreeToAst(ctx.block());
            if (ctx.expr() != null)       return new ExpressionStatement(parseExpr(ctx.expr()));
        }

        if (tree instanceof BlockContext ctx) {
            Statement inner = parseTreeToAst(ctx.statements());
            return inner;
        }

        if (tree instanceof VarDeclContext ctx) {
            String name = ctx.NAME().toString();
            String type = null;
            Expression initializer = null;

            if (ctx.NAME().size() == 2) {
                type = ctx.NAME(1).getText();
            }
            if (ctx.expr() != null) {
                initializer = parseExpr(ctx.expr());
            }
            return new VariableDeclaration(name, type, initializer);
        }

        if (tree instanceof AssignmentContext ctx) {
            String name = ctx.NAME().getText();
            Expression expr = parseExpr(ctx.expr());
            return new Assignment(name, expr);
        }

        if (tree instanceof IfStmtContext ctx) {
            Expression condition = parseExpr(ctx.expr());
            Block thenBranch = stmtToBlock(ctx.statement(0));
            Block elseBranch = stmtToBlock(ctx.statement(1));
            return new IfStatement(condition, thenBranch, elseBranch);
        }

        if (tree instanceof WhileStmtContext ctx) {
            Expression condition = parseExpr(ctx.expr());
            Block body = stmtToBlock(ctx.statement());
            return new WhileStatement(condition, body);
        }

        if (tree instanceof FunDeclContext ctx) {
            String name = ctx.NAME(0).getText();
            String returnType = null;
            if (ctx.NAME().size() == 2) {
                returnType = ctx.NAME(1).getText();
            }
            List<FunctionParameter> params = new ArrayList<>();
            if (ctx.paramList() != null) {
                for (ParamContext p : ctx.paramList().param()) {
                    String pName = p.NAME(0).getText();
                    String pType = p.NAME(1).getText();
                    params.add(new FunctionParameter(pName, pType));
                }
            }
            Block body = (Block) parseTreeToAst(ctx.block());
            return new FunctionDefinition(name, params, returnType, body);
        }

        if (tree instanceof ReturnStmtContext ctx) {
            Expression expr = parseExpr(ctx.expr());
            return new ReturnStatement(expr);
        }

        return null;
    }

    private static Block stmtToBlock(StatementContext ctx) {
        Statement s = (Statement) parseTreeToAst(ctx);
        if (s instanceof Block b) return b;
        return new Block(List.of(s));
    }

    private static Expression parseExpr(ExprContext ctx) {
        if (ctx.compOp() != null) {
            Expression left  = parseAddExpr(ctx.addExpr(0));
            Expression right = parseAddExpr(ctx.addExpr(1));
            String op = ctx.compOp().getText();
            return new FunctionCall(op, left, right);
        }
        return parseAddExpr(ctx.addExpr(0));
    }

    private static Expression parseAddExpr(AddExprContext ctx) {
        List<MulExprContext> mulExprs = ctx.mulExpr();
        Expression result = parseMulExpr(mulExprs.get(0));
        for (int i = 1; i < mulExprs.size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Expression right = parseMulExpr(mulExprs.get(i));
            result = new FunctionCall(op, result, right);
        }
        return result;
    }

    private static Expression parseMulExpr(MulExprContext ctx) {
        List<UnaryExprContext> unaryExprs = ctx.unaryExpr();
        Expression result = parseUnaryExpr(unaryExprs.get(0));
        for (int i = 1; i < unaryExprs.size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Expression right = parseUnaryExpr(unaryExprs.get(i));
            result = new FunctionCall(op, result, right);
        }
        return result;
    }

    private static Expression parseUnaryExpr(UnaryExprContext ctx) {
        if (ctx.unaryExpr() != null) {
            Expression operand = parseUnaryExpr(ctx.unaryExpr());
            return new FunctionCall("-", operand);
        }
        return parseAtom(ctx.atom());
    }

    private static Expression parseAtom(AtomContext ctx) {
        if (ctx.INT_LIT() != null) {
            return new IntegerLiteral(Integer.parseInt(ctx.INT_LIT().getText()));
        }
        if (ctx.STR_LIT() != null) {
            String raw = ctx.STR_LIT().getText();
            String value = raw.substring(1, raw.length() - 1);
            return new StringLiteral(value);
        }
        if (ctx.NAME() != null && ctx.argList() != null) {
            String name = ctx.NAME().getText();
            List<Expression> args = new ArrayList<>();
            for (ExprContext arg : ctx.argList().expr()) {
                args.add(parseExpr(arg));
            }
            return new FunctionCall(name, args);
        }
        if (ctx.NAME() != null && ctx.getChildCount() == 1) {
            return new Variable(ctx.NAME().getText());
        }
        if (ctx.expr() != null) {
            return parseExpr(ctx.expr());
        }
        throw new IllegalArgumentException("Unknown atom: " + ctx.getText());
    }
}
