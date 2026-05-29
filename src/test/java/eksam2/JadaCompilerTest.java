package eksam2;

import cma.CMaInterpreter;
import cma.CMaProgram;
import cma.CMaStack;
import eksam2.ast.JadaStmt;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static eksam2.ast.JadaNode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JadaCompilerTest {

    @Rule
    public TestRule globalTimeout = new DisableOnDebug(new Timeout(1000, TimeUnit.MILLISECONDS));

    private Map<String, List<Integer>> initialEnv;

    @Before
    public void setUp() {
        initialEnv = new LinkedHashMap<>(); // linked to fix variable order!
        initialEnv.put("a", List.of(5,5,5));
        initialEnv.put("b", List.of(11, 22, 33));
        initialEnv.put("c", List.of(100, 200));
        initialEnv.put("x", List.of(1));
        initialEnv.put("y", List.of(-1));
        initialEnv.put("z", List.of(0));
        initialEnv.put("i", List.of(0));

        initialEnv = Collections.unmodifiableMap(initialEnv);
    }

    @Test
    public void test01_numvar() {
        checkCompile(assign("x", num(10)), Map.of("x", List.of(10)));
        checkCompile(assign("z", num(-10)), Map.of("z", List.of(-10)));

        // loeme keskkonnast (setup'is defineeritud)
        checkCompile(assign("i", var("x")), Map.of("i", List.of(1)));
        checkCompile(assign("i", var("y")), Map.of("i", List.of(-1)));
    }

    @Test
    public void test02_ops() {
        checkCompile(assign("x", add(num(2), num(3))), Map.of("x", List.of(5)));
        checkCompile(assign("x", add(add(num(5), num(6)), num(7))), Map.of("x", List.of(18)));

        checkCompile(assign("x", var("b", num(0))), Map.of("x", List.of(11)));
        checkCompile(assign("x", var("b", num(1))), Map.of("x", List.of(22)));
        checkCompile(assign("x", var("b", num(2))), Map.of("x", List.of(33)));
    }

    @Test
    public void test03_assign() {
        checkCompile(assign("a", num(0), var("b", num(2))), Map.of("a", List.of(33, 5, 5)));
        checkCompile(assign("a", num(1), var("b", num(1))), Map.of("a", List.of(5, 22, 5)));
        checkCompile(assign("a", num(2), var("b", num(0))), Map.of("a", List.of(5, 5, 11)));
    }

    @Test
    public void test04_blocks() {
        checkCompile(
                block(
                        assign("x", num(1)),
                        assign("y", num(2))),
                Map.of("x", List.of(1), "y", List.of(2)));
    }

    @Test
    public void test05_foreach() {
        checkCompile(
                foreach("i", "b", // b = [11,22,33]
                        assign("z", add(var("z"), var("i")))),
                Map.of("z", List.of(66), "i", List.of(33)));
    }

    @Test
    public void test06_arrays() {
        // a = [5,5,5], b = [11,22,33], c = [100,200]
        checkCompile(assign("b", var("a")), Map.of("b", List.of(5,5,5)));
        checkCompile(assign("a", var("b")), Map.of("a", List.of(11,22,33)));
        checkCompile(assign("a", add(var("a"), var("b"))), Map.of("a", List.of(16,27,38)));

        checkCompile(assign("a", from(10)), Map.of("a", List.of(10,11,12)));
        checkCompile(assign("c", from(10)), Map.of("c", List.of(10,11)));
        checkCompile(assign("i", from(10)), Map.of("i", List.of(10)));

        checkCompile(assign("a", add(var("a"), from(0))), Map.of("a", List.of(5,6,7)));
        checkCompile(assign("a", add(var("b"), from(0))), Map.of("a", List.of(11,23,35)));
        checkCompile(assign("c", add(var("c"), from(0))), Map.of("c", List.of(100,201)));

    }

    @Test
    public void test07_constarray() {
        fail("See test avalikustatakse pärast eksamit! Testib literaalide venitamist konstantseks jadaks.");
    }

    @Test
    public void test08_multiple() {
        fail("See test avalikustatakse pärast eksamit! Lihtsalt keerulisemad avaldised.");
    }


    private void checkCompile(JadaStmt stmt, Map<String, List<Integer>> expectedEnvDiff) {
        List<String> variables = new ArrayList<>();
        CMaStack initialStack = new CMaStack();
        Map<String, Integer> lengths = new LinkedHashMap<>();

        for (Map.Entry<String, List<Integer>> entry : initialEnv.entrySet()) {
            List<Integer> array = entry.getValue();
            for (int value : array) initialStack.push(value);
            variables.add(entry.getKey());
            lengths.put(entry.getKey(), array.size());
        }

        CMaProgram program = JadaCompiler.compile(stmt, lengths);
        CMaStack finalStack = CMaInterpreter.run(program, initialStack);

        Map<String, List<Integer>> expected = new HashMap<>(initialEnv);
        expected.putAll(expectedEnvDiff);
        CMaStack expectedStack = new CMaStack();
        for (String variable : variables) {
            List<Integer> integers = expected.get(variable);
            for (int value : integers) expectedStack.push(value);
        }
        assertEquals(expectedStack, finalStack);
    }
}
