package eksam1;

import eksam1.ast.vector.VexVectorNode;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static eksam1.ast.VexNode.*;
import static eksam1.ast.scalar.VexProj.Axis.X;
import static eksam1.ast.scalar.VexProj.Axis.Y;
import static org.junit.Assert.assertEquals;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VexEvaluatorTest {

    @Rule
    public TestRule globalTimeout = new DisableOnDebug(new Timeout(1000, TimeUnit.MILLISECONDS));

    private Map<String, Integer> scalarEnv;
    private Map<String, Vector> vectorEnv;

    @Before
    public void setUp() {
        scalarEnv = new HashMap<>();
        scalarEnv.put("a", 2);
        scalarEnv.put("b", -1);
        scalarEnv.put("foo", 42);
        scalarEnv = Collections.unmodifiableMap(scalarEnv);

        vectorEnv = new HashMap<>();
        vectorEnv.put("X", new Vector(2, 3));
        vectorEnv.put("Y", new Vector(-1, 5));
        vectorEnv.put("Z", new Vector(10, -7));
        vectorEnv = Collections.unmodifiableMap(vectorEnv);
    }

    @Test
    public void test01_vec_num_svar() {
        checkEval(vec(num(2), num(3)), new Vector(2, 3));
        checkEval(vec(num(-5), num(0)), new Vector(-5, 0));

        checkEval(vec(svar("a"), svar("b")), new Vector(2, -1));
        checkEval(vec(svar("foo"), num(0)), new Vector(42, 0));
    }

    @Test
    public void test02_binop() {
        checkEval(vec(add(num(2), num(5)), sub(num(10), num(6))), new Vector(7, 4));
        checkEval(vec(mul(num(10), num(6)), div(num(20), num(5))), new Vector(60, 4));
    }

    @Test
    public void test03_vvar() {
        checkEval(vvar("X"), new Vector(2, 3));
        checkEval(vvar("Z"), new Vector(10, -7));
    }

    @Test
    public void test04_proj() {
        checkEval(vec(proj(X, vvar("X")), proj(Y, vvar("X"))), new Vector(2, 3));
        checkEval(vec(proj(Y, vvar("Z")), proj(X, vvar("Y"))), new Vector(-7, -1));
        checkEval(vec(proj(X, vec(num(2), num(3))), proj(Y, vec(num(2), num(3)))), new Vector(2, 3));
    }

    @Test
    public void test05_scale() {
        checkEval(scale(num(2), vvar("X")), new Vector(4, 6));
        checkEval(scale(num(-3), vvar("Y")), new Vector(3, -15));
        checkEval(scale(svar("b"), vvar("Z")), new Vector(-10, 7));
        checkEval(scale(num(10), vec(num(2), num(3))), new Vector(20, 30));
    }

    @Test
    public void test06_plus() {
        checkEval(plus(vvar("X"), vvar("Y")), new Vector(1, 8));
        checkEval(plus(vec(num(5), num(-3)), vvar("Y")), new Vector(4, 2));
    }

    @Test
    public void test07_dot() {
        checkEval(vec(dot(vvar("X"), vvar("Y")), num(0)), new Vector(13, 0));
    }

    @Test
    public void test08_multiple() {
        checkEval(
                plus(
                        scale(
                                dot(vvar("X"), vvar("X")),
                                vec(proj(Y, vvar("Y")), mul(num(-1), proj(X, vvar("Y"))))),
                        vec(
                                div(proj(Y, vvar("X")), num(2)),
                                add(proj(X, vvar("Z")), num(10)))),
                new Vector(66, 33));
    }


    // Eraldi meetod, et VexEvaluatorOneSwitchTest saaks overload-ida.
    protected Vector eval(VexVectorNode vector, Map<String, Integer> scalarEnv, Map<String, Vector> vectorEnv) {
        return VexEvaluator.eval(vector, scalarEnv, vectorEnv);
    }

    private void checkEval(VexVectorNode vector, Vector expected) {
        assertEquals(expected, eval(vector, scalarEnv, vectorEnv));
    }
}
