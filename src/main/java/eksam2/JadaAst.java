package eksam2;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import eksam2.ast.JadaStmt;
import utils.ExceptionErrorListener;

import static eksam2.ast.JadaNode.*;

public class JadaAst {

    public static JadaStmt makeJadaAst(String input) {
        JadaLexer lexer = new JadaLexer(CharStreams.fromString(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ExceptionErrorListener());

        JadaParser parser = new JadaParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());

        ParseTree tree = parser.init();
        return parseTreeToAst(tree);
    }

    private static JadaStmt parseTreeToAst(ParseTree tree) {
        throw new UnsupportedOperationException();
    }
}
