package koolfileindexer.modelo;

import java.util.Objects;

/**
 * Helpers para validaciones comunes.
 */
public class Validator {
    /** Valida longitud (1–50) y trim + not null. */
    public static String validarNombreCategoria(String nombre) {
        Objects.requireNonNull(nombre, "Nombre de categoría no puede ser null");
        String limpio = nombre.trim();
        if (limpio.length() < 1 || limpio.length() > 50) {
            throw new IllegalArgumentException(
                    "Nombre de categoría inválido (1-50 chars): " + limpio);
        }
        return limpio;
    }
}
