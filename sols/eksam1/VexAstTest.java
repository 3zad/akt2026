package eksam1;

import eksam1.ast.vector.VexVectorNode;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static eksam1.ast.VexNode.*;
import static eksam1.ast.scalar.VexProj.Axis.X;
import static eksam1.ast.scalar.VexProj.Axis.Y;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VexAstTest {

    @Test
    public void test01_vec_num() {
        legal("<0,0>", vec(num(0), num(0)));
        legal("< 0  , 0 >", vec(num(0), num(0)));

        legal("<123,0>", vec(num(123), num(0)));
        legal("<-123,0>", vec(num(-123), num(0)));
        legal("<123,456>", vec(num(123), num(456)));

        illegal("0");
        illegal("0,0");
        illegal("<0>");
        illegal("<0,0,0>");
        illegal("<0,0");
        illegal("0,0>");
        illegal("<0 0>");

        illegal("<0123,0>");
        illegal("<0, 0123>");
        illegal("<--1, 0>");
        illegal("<0, - 1>");
    }

    @Test
    public void test02_var() {
        legal("<a,b>", vec(svar("a"), svar("b")));
        legal("<abba, kala>", vec(svar("abba"), svar("kala")));

        legal("X", vvar("X"));
        legal("Kala", vvar("Kala"));

        illegal("<X,a>");
        illegal("<a, Kala>");
        illegal("<aX, a>");
        illegal("KaLa");
        illegal("aAa");
        illegal("<x1,y2>");
        illegal("X1");
    }

    @Test
    public void test03_binop() {
        legal("<a + b, a-b>", vec(add(svar("a"), svar("b")), sub(svar("a"), svar("b"))));
        legal("<a * b, a / b>", vec(mul(svar("a"), svar("b")), div(svar("a"), svar("b"))));

        legal("<a + b * a, a - b / a>", vec(add(svar("a"), mul(svar("b"), svar("a"))), sub(svar("a"), div(svar("b"), svar("a")))));
        legal("<(a + b) * a, (a - b) / a>", vec(mul(add(svar("a"), svar("b")), svar("a")), div(sub(svar("a"), svar("b")), svar("a"))));
        legal("<a * b + a, a / b - a>", vec(add(mul(svar("a"), svar("b")), svar("a")), sub(div(svar("a"), svar("b")), svar("a"))));
        legal("<(a * b) + a, (a / b) - a>", vec(add(mul(svar("a"), svar("b")), svar("a")), sub(div(svar("a"), svar("b")), svar("a"))));

        legal("<a - b - c, a / b / c>", vec(sub(sub(svar("a"), svar("b")), svar("c")), div(div(svar("a"), svar("b")), svar("c"))));
        legal("<a - b - c, a / b / c>", vec(sub(sub(svar("a"), svar("b")), svar("c")), div(div(svar("a"), svar("b")), svar("c"))));

        legal("<a + b - c, a - b + c>", vec(sub(add(svar("a"), svar("b")), svar("c")), add(sub(svar("a"), svar("b")), svar("c"))));
        legal("<a * b / c, a / b * c>", vec(div(mul(svar("a"), svar("b")), svar("c")), mul(div(svar("a"), svar("b")), svar("c"))));
    }

    @Test
    public void test04_proj_dot() {
        legal("<X|y, X|x>", vec(proj(Y, vvar("X")), proj(X, vvar("X"))));
        legal("<<1,2>|y, <3,4>|x>", vec(proj(Y, vec(num(1), num(2))), proj(X, vec(num(3), num(4)))));
        legal("<X|y, <1,2>|x>", vec(proj(Y, vvar("X")), proj(X, vec(num(1), num(2)))));

        legal("<X . Y, Y . Z>", vec(dot(vvar("X"), vvar("Y")), dot(vvar("Y"), vvar("Z"))));
        legal("<X . Y, <1,2> . Z>", vec(dot(vvar("X"), vvar("Y")), dot(vec(num(1), num(2)), vvar("Z"))));
        legal("<X . Y, <1,2> . <3,4>>", vec(dot(vvar("X"), vvar("Y")), dot(vec(num(1), num(2)), vec(num(3), num(4)))));

        legal("<X|x, X . Y>", vec(proj(X, vvar("X")), dot(vvar("X"), vvar("Y"))));
        legal("<<1,2> . <3,4>, <3,4>|y>", vec(dot(vec(num(1), num(2)), vec(num(3), num(4))), proj(Y, vec(num(3), num(4)))));

        illegal("X . Y");
        illegal("X|x");
        illegal("<X . Y . Z, 0>");
    }

    @Test
    public void test05_rotate() {
        legal("XR", vec(proj(Y, vvar("X")), mul(num(-1), proj(X, vvar("X")))));
        legal("X R", vec(proj(Y, vvar("X")), mul(num(-1), proj(X, vvar("X")))));
        legal("XL", vec(mul(num(-1), proj(Y, vvar("X"))), proj(X, vvar("X"))));
        legal("X L", vec(mul(num(-1), proj(Y, vvar("X"))), proj(X, vvar("X"))));
        legal("XRL", vec(mul(num(-1), proj(Y, vec(proj(Y, vvar("X")), mul(num(-1), proj(X, vvar("X")))))), proj(X, vec(proj(Y, vvar("X")), mul(num(-1), proj(X, vvar("X")))))));

        illegal("xR");
        illegal("x L");
    }

    @Test
    public void test06_plus_scale() {
        legal("X + Y", plus(vvar("X"), vvar("Y")));
        legal("<1,2> + <3,4>", plus(vec(num(1), num(2)), vec(num(3), num(4))));
        legal("X + Y + Z", plus(plus(vvar("X"), vvar("Y")), vvar("Z")));
        legal("X + (Y + Z)", plus(vvar("X"), plus(vvar("Y"), vvar("Z"))));

        legal("4 * X", scale(num(4), vvar("X")));
        legal("4 * <1,2>", scale(num(4), vec(num(1), num(2))));

        legal("4 * X + Y", plus(scale(num(4), vvar("X")), vvar("Y")));
        legal("4 * (X + Y)", scale(num(4), plus(vvar("X"), vvar("Y"))));
        legal("2 * X + 4*Y", plus(scale(num(2), vvar("X")), scale(num(4), vvar("Y"))));
        legal("(X . X) * Y", scale(dot(vvar("X"), vvar("X")), vvar("Y")));

        legal("(X|x) * X", scale(proj(X, vvar("X")), vvar("X")));
        legal("(1+2)*X", scale(add(num(1), num(2)), vvar("X")));

        illegal("X . X * Y");
        illegal("Y * X . X");
        illegal("4 * 4 * X");
        illegal("X|x * X");
        illegal("1+2*X");
    }

    @Test
    public void test07_keywordvars() {
        legal("RR", vec(proj(Y, vvar("R")), mul(num(-1), proj(X, vvar("R")))));
        legal("R R", vec(proj(Y, vvar("R")), mul(num(-1), proj(X, vvar("R")))));
        legal("Rr", vvar("Rr"));
        legal("<X|x, x>", vec(proj(X, vvar("X")), svar("x")));

        illegal("rR");
    }

    @Test
    public void test08_multiple() {
        legal("<(X + Y)|y, (X + Y)|x>", vec(proj(Y, plus(vvar("X"), vvar("Y"))), proj(X, plus(vvar("X"), vvar("Y")))));
        legal("(4 * a) * (X + Y)", scale(mul(num(4), svar("a")), plus(vvar("X"), vvar("Y"))));

        legal("(X.X)*YR + <X|y/2, Z|x+10>",
                plus(
                        scale(
                                dot(vvar("X"), vvar("X")),
                                vec(proj(Y, vvar("Y")), mul(num(-1), proj(X, vvar("Y"))))),
                        vec(
                                div(proj(Y, vvar("X")), num(2)),
                                add(proj(X, vvar("Z")), num(10))))

        );
    }


    private void legal(String input, VexVectorNode expectedAst) {
        VexVectorNode actualAst = VexAst.makeVexAst(input);
        assertEquals(expectedAst, actualAst);
    }

    private void illegal(String input) {
        try {
            VexAst.makeVexAst(input);
            fail("expected parse error: " + input);
        } catch (Exception _) {

        }
    }
}
