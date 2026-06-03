package eksam1;

import eksam1.ast.scalar.VexScalarNode;
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

    private static final VexVisitor<VexScalarNode> scalarVisitor = null; // implementeeri mind!
    private static final VexVisitor<VexVectorNode> vectorVisitor = null; // implementeeri mind!
}
