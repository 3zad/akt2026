package week8;

import java.util.*;

public class Environment<T> {
    private final Deque<Map<String, T>> scopes = new ArrayDeque<>();

    /**
     * Esialgu peaks olemas olema globaalne skoop, kuhu saab muutujaid deklareerida enne ühtegi skoopi (plokki) sisenemist.
     */
    public Environment() {
        scopes.push(new LinkedHashMap<>());
    }

    /**
     * Deklareerib praeguses skoobis uue muutuja.
     */
    public void declare(String variable) {
        assert scopes.peek() != null;
        scopes.peek().put(variable, null);
    }

    /**
     * Omistab muutujale uue väärtuse kõige sisemises skoobis, kus see muutuja deklareeritud on.
     */
    public void assign(String variable, T value) {
        for (Map<String, T> scope : scopes) {
            if (scope.containsKey(variable)) {
                scope.put(variable, value);
                return;
            }
        }
        throw new RuntimeException("Undeclared variable: " + variable);
    }

    /**
     * Deklareerib praeguses skoobis uue muutuja ja omistab sellele väärtuse.
     */
    public void declareAssign(String variable, T value) {
        assert scopes.peek() != null;
        scopes.peek().put(variable, value);
    }

    /**
     * Tagastab muutuja praeguse väärtuse kõige sisemises skoobis, kus see muutuja deklareeritud on.
     * Deklareerimata või väärtustamata muutujate korral peaks tagastama {@code null}.
     */
    public T get(String variable) {
        for (Map<String, T> scope : scopes) {
            if (scope.containsKey(variable)) {
                return scope.get(variable);
            }
        }
        return null;
    }

    /**
     * Tähistab uude skoopi (plokki) sisenemist.
     * Uues skoobis võib üle deklareerida välimiste välimise skoobi muutujaid.
     */
    public void enterBlock() {
        scopes.push(new LinkedHashMap<>());
    }

    /**
     * Tähistab praegusest skoobist (plokist) väljumist.
     * Unustama peaks kõik sisemises skoobis deklareeritud muutujad.
     */
    public void exitBlock() {
        if (scopes.size() <= 1) {
            throw new RuntimeException("Cannot exit global scope");
        }
        scopes.pop();
    }
}
