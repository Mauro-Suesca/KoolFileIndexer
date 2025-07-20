package modelo;

import java.util.Objects;

/**
 * Representa una etiqueta de archivo.
 * Instancias con mismo texto (insensible a mayúsculas) son iguales.
 */
public class Etiqueta {
    private String nombre;

    private Etiqueta(String nombre) {
        this.nombre = nombre;
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
        return new Etiqueta(validarYNormalizar(nombre, "Etiqueta"));
    }

    /**
     * Renombra esta etiqueta.
     *
     * @param nuevoNombre mismo formato y reglas que crear().
     * @throws IllegalArgumentException si el nuevo nombre no cumple las reglas.
     */
    public void setNombre(String nuevoNombre) {
        this.nombre = validarYNormalizar(nuevoNombre, "Nuevo nombre de etiqueta");
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

    // ───────────────────────────────────────────────────
    // Helpers privados

    /**
     * Valida y normaliza un texto según las reglas de etiquetas.
     *
     * @param input    texto original
     * @param contexto descripción para mensajes de error
     * @return texto normalizado (trim + toLowerCase)
     * @throws NullPointerException     si input es null
     * @throws IllegalArgumentException si input no cumple el patrón
     */
    private static String validarYNormalizar(String input, String contexto) {
        Objects.requireNonNull(input, contexto + " no puede ser null");
        String limpio = input.trim();
        if (!ValidadorEntrada.esEtiquetaValida(limpio)) {
            throw new IllegalArgumentException(
                    String.format("%s inválido: '%s'", contexto, limpio));
        }
        return limpio.toLowerCase();
    }
}
