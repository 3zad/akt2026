package eksam2;

import eksam2.ast.JadaNode;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static eksam2.ast.JadaNode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JadaAstTest {

    @Test
    public void test01_numvar() {
        legal("x <- 123", assign("x", num(123)));
        legal("x <- -123", assign("x", num(-123)));
        legal("x <- 0", assign("x", num(0)));

        legal("x <- x", assign("x", var("x")));
        legal("ChEcK <- 10", assign("ChEcK", num(10)));
        legal("A <- 10", assign("A", num(10)));

        illegal("x <- 0123");
        illegal("x <- --1");
        illegal("x <- - 1");
        illegal("x <- -0");
        illegal("x <- -01");
        illegal("x <- 1.0");
        illegal("x <- x0");
        illegal("x <- 00");
    }

    @Test
    public void test02_ops() {
        legal("x <- x + y", assign("x", add(var("x"), var("y"))));
        legal("x <- 5 +6", assign("x", add(num(5), num(6))));
        legal("x <- 5 +-6", assign("x", add(num(5), num(-6))));
        legal("x <- 5 + -6", assign("x", add(num(5), num(-6))));

        legal("x <- 5 + 6 + 7", assign("x", add(add(num(5), num(6)), num(7))));
        legal("x <- (5 + 6) + 7", assign("x", add(add(num(5), num(6)), num(7))));
        legal("x <- 5 + (6 + 7)", assign("x", add(num(5), add(num(6), num(7)))));

        illegal("x <- 3 - 10");
        illegal("x <- 3 ++ 10");
        illegal("x <- 3 + + 10");
        illegal("x <- 5 -");
    }

    @Test
    public void test03_idx() {
        legal("x <- a_10", assign("x", var("a", num(10))));
        legal("x <- a_b ", assign("x", var("a", var("b"))));

        legal("z @ 5 <- 10", assign("z", num(5), num(10)));
        legal("z @ 5 <- a_3", assign("z", num(5), var("a", num(3))));

        illegal("x <- 10_x");
        illegal("a @ <- 10");
        illegal("x <-");
    }

    @Test
    public void test04_prio() {
        legal("x <- a_i + 1", assign("x", add(var("a", var("i")), num(1))));
        legal("x <- a_(i + 1)", assign("x", var("a", add(var("i"), num(1)))));

        legal("x <- a_1 + a_2", assign("x", add(var("a", num(1)), var("a", num(2)))));
        legal("x <- a_(1 + a_2)", assign("x", var("a", add(num(1), var("a", num(2))))));
        legal("x <- a_a_i", assign("x", var("a", var("a", var("i")))));
        legal("x <- a_(a_i)", assign("x", var("a", var("a", var("i")))));

        legal("x @ i + 1 <- 10", assign("x", add(var("i"), num(1)), num(10)));
        legal("x @ x_i + 1 <- 10", assign("x", add(var("x", var("i")), num(1)), num(10)));

        illegal("x <- (a+b)_i");
        illegal("(x @ i) + 1 <- 10");
        illegal("x <- a + b_");
        illegal("x <- + 6");
    }

    @Test
    public void test05_foreach_range() {
        legal("for i in arr do x <- x + i", foreach("i", "arr", assign("x", add(var("x"), var("i")))));
        legal("for idx in arr do barr @ idx <- 10", foreach("idx", "arr", assign("barr", var("idx"), num(10))));

        legal("x <- [1..]", assign("x", from(1)));
        legal("x <- [0 .. ] + [1 ..]", assign("x", add(from(0), from(1))));

        illegal("for i in arr do");
        illegal("for i in arr do x <- x + i for i in arr do x <- x + i");
        illegal("x <- [1..");
        illegal("x <- 1.. ");
        illegal("x <- [i..]");
    }

    @Test
    public void test06_blocks() {
        legal("{ x <- 10 }", block(assign("x", num(10))));
        legal("{ x <- 10; y <- 20 }", block(assign("x", num(10)), assign("y", num(20))));
        legal("{ x <- 10; y <- 20; z <- 30 }", block(assign("x", num(10)), assign("y", num(20)), assign("z", num(30))));
        legal("{ x <- 10; { y <- 20} ; z <- 30 }", block(assign("x", num(10)), block(assign("y", num(20))), assign("z", num(30))));
        legal("{ x <- 10; { y <- 20; z <- 30} }", block(assign("x", num(10)), block(assign("y", num(20)), assign("z", num(30)))));
        legal("{ x <- 10; { {y <- 20}; z <- 30} }", block(assign("x", num(10)), block(block(assign("y", num(20))), assign("z", num(30)))));

        legal("for i in arr do { x <- x + i; y <- y + i }", foreach("i", "arr", block(assign("x", add(var("x"), var("i"))), assign("y", add(var("y"), var("i"))))));
        legal("{ for i in arr do x <- x + i; for i in arr do y <- y + i }", block(foreach("i", "arr", assign("x", add(var("x"), var("i")))), foreach("i", "arr", assign("y", add(var("y"), var("i"))))));
        legal("{ x <- 10; for i in arr do x <- x + i; z <- 30 }", block(assign("x", num(10)), foreach("i", "arr", assign("x", add(var("x"), var("i")))), assign("z", num(30))));

        illegal("");
        illegal("10");
        illegal("{ }");
        illegal("{ x <- 10");
        illegal("x <- 10 ; ");
        illegal(" ; x <- 10 ");
        illegal("{ x <- 10; { y <- 20; z <- 30} ; }");
    }

    @Test
    public void test07_nested_for() {
        fail("See test avalikustatakse pärast eksamit! Ei tohi lubada itereerimise kehas teist itereerimist.");
    }

    @Test
    public void test08_multiple() {
        fail("See test avalikustatakse pärast eksamit! Lihtsalt natuke rohkem avaldiste kombinatsioone.");
    }


    private void legal(String input, JadaNode expectedAst) {
        JadaNode actualAst = JadaAst.makeJadaAst(input);
        assertEquals(expectedAst, actualAst);
    }

    private void illegal(String input) {
        try {
            JadaAst.makeJadaAst(input);
            fail("expected parse error: " + input);
        } catch (Exception _) {

        }
    }
}
