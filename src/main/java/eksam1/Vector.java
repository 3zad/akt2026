package eksam1;

/**
 * 2D täisarvuline vektor.
 */
public record Vector(int x, int y) {

    /**
     * Korruta vektor skalaariga (punktiviisiliselt).
     */
    public Vector scale(int scalar) {
        return new Vector(scalar * x, scalar * y);
    }

    /**
     * Liida teise vektoriga (punktiviisiliselt).
     */
    public Vector add(Vector other) {
        return new Vector(x + other.x, y + other.y);
    }

    /**
     * Skalaarkorrutis (ingl. dot product) teise vektoriga.
     */
    public int dot(Vector other) {
        return x * other.x + y * other.y;
    }
}
