package koolfileindexer.modelo;

import java.util.Objects;

/**
 * Representa una etiqueta de archivo.
 * Instancias con mismo texto (insensible a mayúsculas) son iguales.
 */
public class Etiqueta {
    private final String nombre;

    private Etiqueta(String nombre) {
        if (nombre == null) {
            throw new IllegalArgumentException("nombre no puede ser null");
        }
        String normalizado = nombre.trim().toLowerCase();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException("nombre no puede estar vacío");
        }
        if (normalizado.length() > 50) {
            throw new IllegalArgumentException("nombre no puede exceder 50 caracteres");
        }
        if (!esNombreValido(normalizado)) {
            throw new IllegalArgumentException("nombre contiene caracteres inválidos");
        }
        this.nombre = normalizado;
    }

    private static boolean esNombreValido(String nombre) {
        // Permitir letras, dígitos, guiones, guiones bajos y espacios simples
        return nombre.matches("[a-z0-9_\\-]+(\\s[a-z0-9_\\-]+)*");
    }

    /**
     * Crea y valida una nueva Etiqueta.
     *
     * @param nombre texto de la etiqueta; no nulo, 1–50 chars,
     *               letras/dígitos/guión/espacio simple.
     * @return una instancia nueva con nombre en minúsculas.
     * @throws IllegalArgumentException si el nombre no cumple las reglas.
     */
    public static Etiqueta crear(String nombre) {
        return new Etiqueta(nombre);
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Etiqueta that))
            return false;
        return nombre.equals(that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
