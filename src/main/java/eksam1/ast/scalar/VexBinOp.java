package eksam1.ast.scalar;

import cma.instruction.CMaBasicInstruction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public record VexBinOp(Op op, VexScalarNode left, VexScalarNode right) implements VexScalarNode {

    public enum Op {
        // IntelliJ soovitab siin Integer::sum kujule viia
        Add("add", "+", (x, y) -> x + y, CMaBasicInstruction.Code.ADD),
        Sub("sub", "-", (x, y) -> x - y, CMaBasicInstruction.Code.SUB),
        Mul("mul", "*", (x, y) -> x * y, CMaBasicInstruction.Code.MUL),
        Div("div", "/", (x, y) -> x / y, CMaBasicInstruction.Code.DIV);

        private final String nodeInfo;
        private final String symbol;
        private final BinaryOperator<Integer> javaOperator;
        private final CMaBasicInstruction.Code cmaCode;

        Op(String nodeInfo, String symbol, BinaryOperator<Integer> javaOperator, CMaBasicInstruction.Code cmaCode) {
            this.nodeInfo = nodeInfo;
            this.symbol = symbol;
            this.javaOperator = javaOperator;
            this.cmaCode = cmaCode;
        }

        public String getSymbol() {
            return symbol;
        }

        public BinaryOperator<Integer> toJava() {
            return javaOperator;
        }

        public CMaBasicInstruction.Code toCMa() {
            return cmaCode;
        }

        private final static Map<String, Op> symbolMap =
                Arrays.stream(Op.values()).collect(toMap(Op::getSymbol, identity()));

        public static Op fromSymbol(String symbol) {
            return symbolMap.get(symbol);
        }

    }

    public VexBinOp(String symbol, VexScalarNode left, VexScalarNode right) {
        this(Op.fromSymbol(symbol), left, right);
    }

    @Override
    public List<Object> getChildren() {
        return List.of(left, right);
    }

    @Override
    public String getNodeLabel() {
        return op.nodeInfo;
    }

    @Override
    public String toString() {
        return op.nodeInfo + "(" +
                "" + left +
                ", " + right +
                ")";
    }
}
