package eksam2.ast;

public record JadaForeach(String element, String array, JadaStmt body) implements JadaStmt {
    @Override
    public String toString() {
        return "foreach(" +
                "\"" + element + "\"" +
                ", \"" + array + "\"" +
                ", " + body +
                ")";
    }
}
