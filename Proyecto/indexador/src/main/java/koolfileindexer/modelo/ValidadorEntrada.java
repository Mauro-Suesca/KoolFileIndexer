package koolfileindexer.modelo;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Valida nombres de archivo, etiquetas y palabras clave.
 * Longitud válida: entre MIN y MAX caracteres.
 */
public class ValidadorEntrada {
    private static final int MIN = 1;
    private static final int MAX = 50;

    /** Letras, dígitos, guiones/guion bajo, un único espacio interno. */
    private static final Pattern PATRON_ETIQUETA = Pattern.compile("^[a-z0-9_\\-]+( [a-z0-9_\\-]+)*$");

    /** Letras, dígitos, guiones/guion bajo, sin espacios. */
    private static final Pattern PATRON_PALABRA = Pattern.compile("^[a-z0-9_\\-]+$");

    /** Caracteres inválidos en nombres de archivo. */
    private static final Pattern INVALID_FILENAME_CHARS_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * Normaliza un string: trim + toLowerCase.
     * 
     * @param input String a normalizar (puede ser null)
     * @return String normalizado, o null si el input era null
     */
    public static String normalizar(String input) {
        return input == null ? null : input.trim().toLowerCase();
    }

    /**
     * Valida un nombre de archivo básico:
     * — No nulo.
     * — Longitud entre MIN y MAX (tras trim).
     */
    public static boolean esNombreArchivoValido(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }

        // Verificar longitud
        String trimmed = nombre.trim();
        if (trimmed.length() < MIN || trimmed.length() > MAX) {
            return false;
        }

        // Verificar caracteres inválidos en nombres de archivo
        return !INVALID_FILENAME_CHARS_PATTERN.matcher(trimmed).find();
    }

    /**
     * @return true si la etiqueta cumple longitud y patrón (permite un único
     *         espacio interno).
     */
    public static boolean esEtiquetaValida(String etiqueta) {
        return normalize(etiqueta)
                .filter(s -> s.length() >= MIN && s.length() <= MAX)
                .filter(s -> PATRON_ETIQUETA.matcher(s).matches())
                .isPresent();
    }

    /**
     * @return true si la palabra clave cumple longitud y patrón (sin espacios).
     */
    public static boolean esPalabraClaveValida(String palabra) {
        return normalize(palabra)
                .filter(s -> s.length() >= MIN && s.length() <= MAX)
                .filter(s -> PATRON_PALABRA.matcher(s).matches())
                .isPresent();
    }

    /**
     * Normaliza un string y lo devuelve como Optional.
     * 
     * @param input String a normalizar (puede ser null)
     * @return Optional con el string normalizado, o empty si el input era null o
     *         vacío
     */
    private static Optional<String> normalize(String input) {
        String normalizado = normalizar(input);
        return normalizado == null || normalizado.isEmpty() ? Optional.empty() : Optional.of(normalizado);
    }

    // Añadir método de Validator
    public static String validarNombreCategoria(String nombre) {
        Objects.requireNonNull(nombre, "Nombre de categoría no puede ser null");
        String limpio = nombre.trim();
        if (limpio.length() < MIN || limpio.length() > MAX) {
            throw new IllegalArgumentException(
                    String.format("Categoría debe tener %d–%d caracteres (recibidos %d): '%s'",
                            MIN, MAX, limpio.length(), limpio));
        }
        return limpio;
    }
}
