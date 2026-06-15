package eksam1;

import eksam1.ast.scalar.VexScalarNode;
import eksam1.ast.vector.VexVec;
import eksam1.ast.vector.VexVectorNode;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import utils.ExceptionErrorListener;

import static eksam1.ast.VexNode.*;
import static eksam1.ast.scalar.VexProj.Axis.X;
import static eksam1.ast.scalar.VexProj.Axis.Y;

public class VexAst {

    public static VexVectorNode makeVexAst(String input) {
        VexLexer lexer = new VexLexer(CharStreams.fromString(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ExceptionErrorListener());

        VexParser parser = new VexParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());

        ParseTree tree = parser.init();
        return parseTreeToAst(tree);
    }

    private static VexVectorNode parseTreeToAst(ParseTree tree) {
        return vectorVisitor.visit(tree);
    }

    // Kuna skalaaravaldised ja vektoravaldised on erinevat tüüpi ning võivad teineteist vastastikuselt sisaldada, siis kahe visitori kasutamiseks tuleb need implementeerida isendiväljades.

    private static final VexVisitor<VexScalarNode> scalarVisitor = new VexBaseVisitor<>() {
        @Override
        public VexScalarNode visitExpr(VexParser.ExprContext ctx)
        {
            if (ctx.op != null) {
                VexScalarNode l = scalarVisitor.visit(ctx.expr(0));
                VexScalarNode r = scalarVisitor.visit(ctx.expr(1));
                return switch (ctx.op.getText())
                {
                    case "*" -> mul(l, r);
                    case "/" -> div(l, r);
                    case "+" -> add(l, r);
                    case "-" -> sub(l, r);
                    default -> null;
                };
            }

            if (ctx.AXIS() != null) {
                return svar(ctx.AXIS().getText());
            }

            if (ctx.SCALAR() != null) {
                return num(Integer.parseInt(ctx.SCALAR().getText()));
            }

            if (ctx.SCALAR_ID() != null) {
                return svar(ctx.SCALAR_ID().getText());
            }

            if (ctx.proj() != null) {
                return scalarVisitor.visit(ctx.proj());
            }

            if (ctx.dot() != null) {
                return scalarVisitor.visit(ctx.dot());
            }

            else {
                return scalarVisitor.visit(ctx.expr(0));
            }

        }

        @Override
        public VexScalarNode visitProj(VexParser.ProjContext ctx)
        {
            VexVectorNode vec = vectorVisitor.visit(ctx.simpleVector());
            var axis = ctx.AXIS().getText().endsWith("x") ? X : Y;
            return proj(axis, vec);

        }

        @Override
        public VexScalarNode visitDot(VexParser.DotContext ctx)
        {
            VexVectorNode l = vectorVisitor.visit(ctx.simpleVector(0));
            VexVectorNode r = vectorVisitor.visit(ctx.simpleVector(1));
            return dot(l, r);
        }

        @Override
        public VexScalarNode visitSimpleScalar(VexParser.SimpleScalarContext ctx)
        {
            if (ctx.SCALAR() != null) {
                return num(Integer.parseInt(ctx.SCALAR().getText()));
            }

            if (ctx.SCALAR_ID() != null) {
                return svar(ctx.SCALAR_ID().getText());
            }

            else {
                return scalarVisitor.visit(ctx.expr());
            }

        }
    };

    private static final VexVisitor<VexVectorNode> vectorVisitor = new VexBaseVisitor<>() {
        @Override
        public VexVectorNode visitInit(VexParser.InitContext ctx) {
            return visitVector(ctx.vector());
        }

        @Override
        public VexVectorNode visitSimpleVector(VexParser.SimpleVectorContext ctx)
        {
            if (ctx.VECTOR_ID() != null) {
                return vvar(ctx.VECTOR_ID().getText());
            }
            if (ctx.expr().size() == 2) {
                VexScalarNode l = scalarVisitor.visit(ctx.expr(0));
                VexScalarNode r = scalarVisitor.visit(ctx.expr(1));
                return vec(l, r);
            }
            if (ctx.vector() != null) {
                return visitVector(ctx.vector());
            }

            // case: simpleVector ('L' | 'R')
            // ZL ≡ <-1*Z|y, Z|x>
            // ZR ≡ <Z|y, -1*Z|x>
            VexVectorNode inner = visitSimpleVector(ctx.simpleVector());
            if (ctx.getChild(1).getText().equals("L")) {
                return vec(
                  mul(num(-1), proj(Y, inner)),
                        proj(X, inner)
                );
            }

            if (ctx.getChild(1).getText().equals("R")) {
                return vec(
                        proj(Y, inner),
                        mul(num(-1), proj(X, inner))
                );
            }

            if (ctx.ROTATION() != null) {
                return vvar(ctx.ROTATION().getText());
            }

            return null;
        }

        @Override
        public VexVectorNode visitVector(VexParser.VectorContext ctx)
        {
            if (ctx.vector().size() == 2) {
                VexVectorNode left = visitVector(ctx.vector(0));
                VexVectorNode right = visitVector(ctx.vector(1));
                return plus(left, right);
            }
            if (ctx.simpleScalar() != null) {
                VexScalarNode scalar = scalarVisitor.visit(ctx.simpleScalar());
                VexVectorNode vector = vectorVisitor.visit(ctx.simpleVector());
                return scale(scalar, vector);
            }

            return visitSimpleVector(ctx.simpleVector());
        }
    };
}
