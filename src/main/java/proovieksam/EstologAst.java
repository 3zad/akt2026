package proovieksam;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import proovieksam.ast.EstologDef;
import proovieksam.ast.EstologNode;
import utils.ExceptionErrorListener;
import java.util.*;

import java.io.IOException;
import java.nio.file.Paths;

import static proovieksam.EstologParser.*;
import static proovieksam.ast.EstologNode.*;

public class EstologAst {

    public static void main() throws IOException {
        EstologNode ast = makeEstologAst("""
                        x := 0;
                        y := 1;
                        a := (x JA y);
                        b := (x VOI y);
                
                        (KUI (x = y) SIIS a MUIDU b)""");
        System.out.println(ast);
        ast.renderPngFile(Paths.get("graphs", "estolog.png"));
    }

    public static EstologNode makeEstologAst(String input) {
        EstologLexer lexer = new EstologLexer(CharStreams.fromString(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ExceptionErrorListener());

        EstologParser parser = new EstologParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());

        ParseTree tree = parser.init();
        System.out.println(tree.toStringTree(parser));
        return parseTreeToAst(tree);
    }

    // Implementeeri see meetod.
    private static EstologNode parseTreeToAst(ParseTree tree) {
        if (tree instanceof InitContext ctx) {
            return parseTreeToAst(ctx.prog());
        }

        if (tree instanceof ProgContext ctx) {
            List<EstologDef> defs = ctx.def().stream()
                    .map(d -> (EstologDef) parseTreeToAst(d))
                    .toList();
            EstologNode finalExpr = parseTreeToAst(ctx.expr());
            return prog(finalExpr, defs);
        }

        if (tree instanceof DefContext ctx) {
            return def(ctx.ID().getText(), parseTreeToAst(ctx.expr()));
        }

        if (tree instanceof IfExprContext ctx) {
            EstologNode cond = parseTreeToAst(ctx.expr(0));
            EstologNode then = parseTreeToAst(ctx.expr(1));
            if (ctx.expr().size() == 3) {
                return kui(cond, then, parseTreeToAst(ctx.expr(2)));
            }
            return kui(cond, then);
        }

        if (tree instanceof ToNingContext ctx) {
            return parseTreeToAst(ctx.ningExpr());
        }

        if (tree instanceof NingOpContext ctx) {
            EstologNode result = parseTreeToAst(ctx.voiExpr(0));
            for (int i = 1; i < ctx.voiExpr().size(); i++) {
                result = ja(result, parseTreeToAst(ctx.voiExpr(i)));
            }
            return result;
        }

        if (tree instanceof VoiOpContext ctx) {
            EstologNode result = parseTreeToAst(ctx.jaExpr(0));
            for (int i = 1; i < ctx.jaExpr().size(); i++) {
                result = voi(result, parseTreeToAst(ctx.jaExpr(i)));
            }
            return result;
        }

        if (tree instanceof JaOpContext ctx) {
            EstologNode result = parseTreeToAst(ctx.eqExpr(0));
            for (int i = 1; i < ctx.eqExpr().size(); i++) {
                result = ja(result, parseTreeToAst(ctx.eqExpr(i)));
            }
            return result;
        }

        if (tree instanceof EqOpContext ctx) {
            EstologNode left = parseTreeToAst(ctx.atom(0));
            if (ctx.atom().size() == 2) {
                return vordus(left, parseTreeToAst(ctx.atom(1)));
            }
            return left;
        }

        if (tree instanceof ParenContext ctx) {
            return parseTreeToAst(ctx.expr());
        }

        if (tree instanceof VarContext ctx) {
            return var(ctx.ID().getText());
        }

        if (tree instanceof TrueLitContext ctx) {
            return lit(true);
        }

        if (tree instanceof FalseLitContext ctx) {
            return lit(false);
        }

        throw new IllegalArgumentException("Unknown parse tree node: " + tree.getClass().getSimpleName());
    }
}