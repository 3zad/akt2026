package eksam1;

import cma.CMaInterpreter;
import cma.CMaProgram;
import cma.CMaStack;
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
public class VexCompilerTest {

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
        checkCompile(vec(num(2), num(3)), new Vector(2, 3));
        checkCompile(vec(num(-5), num(0)), new Vector(-5, 0));

        checkCompile(vec(svar("a"), svar("b")), new Vector(2, -1));
        checkCompile(vec(svar("foo"), num(0)), new Vector(42, 0));
    }

    @Test
    public void test02_binop() {
        checkCompile(vec(add(num(2), num(5)), sub(num(10), num(6))), new Vector(7, 4));
        checkCompile(vec(mul(num(10), num(6)), div(num(20), num(5))), new Vector(60, 4));
    }

    @Test
    public void test03_vvar() {
        checkCompile(vvar("X"), new Vector(2, 3));
        checkCompile(vvar("Z"), new Vector(10, -7));
    }

    @Test
    public void test04_proj() {
        checkCompile(vec(proj(X, vvar("X")), proj(Y, vvar("X"))), new Vector(2, 3));
        checkCompile(vec(proj(Y, vvar("Z")), proj(X, vvar("Y"))), new Vector(-7, -1));
        checkCompile(vec(proj(X, vec(num(2), num(3))), proj(Y, vec(num(2), num(3)))), new Vector(2, 3));
    }

    @Test
    public void test05_scale() {
        checkCompile(scale(num(2), vvar("X")), new Vector(4, 6));
        checkCompile(scale(num(-3), vvar("Y")), new Vector(3, -15));
        checkCompile(scale(svar("b"), vvar("Z")), new Vector(-10, 7));
        checkCompile(scale(num(10), vec(num(2), num(3))), new Vector(20, 30));
    }

    @Test
    public void test06_plus() {
        checkCompile(plus(vvar("X"), vvar("Y")), new Vector(1, 8));
        checkCompile(plus(vec(num(5), num(-3)), vvar("Y")), new Vector(4, 2));
    }

    @Test
    public void test07_dot() {
        checkCompile(vec(dot(vvar("X"), vvar("Y")), num(0)), new Vector(13, 0));
    }

    @Test
    public void test08_multiple() {
        checkCompile(
                plus(
                        scale(
                                dot(vvar("X"), vvar("X")),
                                vec(proj(Y, vvar("Y")), mul(num(-1), proj(X, vvar("Y"))))),
                        vec(
                                div(proj(Y, vvar("X")), num(2)),
                                add(proj(X, vvar("Z")), num(10)))),
                new Vector(66, 33));
    }

    private void checkCompile(VexVectorNode vector, Vector expected) {
        Map<String, Integer> variables = new HashMap<>();
        CMaStack initialStack = new CMaStack();
        int stackPointer = 0;
        for (Map.Entry<String, Integer> entry : scalarEnv.entrySet()) {
            variables.put(entry.getKey(), stackPointer);
            initialStack.push(entry.getValue());
            stackPointer++;
        }
        for (Map.Entry<String, Vector> entry : vectorEnv.entrySet()) {
            variables.put(entry.getKey(), stackPointer);
            initialStack.push(entry.getValue().x());
            initialStack.push(entry.getValue().y());
            stackPointer += 2;
        }

        CMaProgram program = VexCompiler.compile(vector, variables);
        CMaStack finalStack = CMaInterpreter.run(program, initialStack);

        CMaStack expectedStack = new CMaStack(initialStack);
        expectedStack.push(expected.x());
        expectedStack.push(expected.y());
        assertEquals(expectedStack, finalStack);
    }
}
