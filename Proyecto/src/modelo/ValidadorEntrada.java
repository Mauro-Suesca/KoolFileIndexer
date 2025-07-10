package modelo;

import java.util.regex.Pattern;

public class ValidadorEntrada {
    private static final int MIN = 1;
    private static final int MAX = 50;

    /**
     * Permite letras, dígitos, guiones y guion bajo, con un único espacio
     * interno (para etiquetas).
     */
    private static final Pattern PATRON_ETIQUETA = Pattern.compile("^[a-z0-9_\\-]+( [a-z0-9_\\-]+)*$");

    /**
     * Solo letras, dígitos, guiones y guion bajo, SIN espacios (para palabras
     * clave).
     */
    private static final Pattern PATRON_PALABRA = Pattern.compile("^[a-z0-9_\\-]+$");

    public static boolean esEtiquetaValida(String etiqueta) {
        return esLongitudValida(etiqueta)
                && PATRON_ETIQUETA.matcher(etiqueta.toLowerCase()).matches();
    }

    public static boolean esPalabraClaveValida(String palabra) {
        return esLongitudValida(palabra)
                && PATRON_PALABRA.matcher(palabra.toLowerCase()).matches();
    }

    private static boolean esLongitudValida(String texto) {
        if (texto == null)
            return false;
        int len = texto.trim().length();
        return len >= MIN && len <= MAX;
    }
}
