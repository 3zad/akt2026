package eksam2.ast;

import java.util.List;

public record JadaVar(String name, JadaExpr index) implements JadaExpr {
    public JadaVar(String name) {
        this(name, null);
    }

    @Override
    public List<Object> getChildren() {
        // kui index == null, siis jätame selle pildile lisamata
        return (index == null
                ? List.of(name)
                : JadaExpr.super.getChildren());
    }

    @Override
    public String toString() {
        return "var(" +
                "\"" + name + "\"" +
                // kui index == null, siis jätame selle printimata
                (index == null
                        ? ""
                        : ", " + index
                ) +
                ")";
    }
}
