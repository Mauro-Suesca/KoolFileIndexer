package modelo;

import java.util.Objects;

public class Etiqueta {
    private String nombre;

    private Etiqueta(String nombre) {
        this.nombre = nombre;
    }

    public static Etiqueta crear(String nombre) {
        String limpio = Objects.requireNonNull(nombre).trim();
        if (!ValidadorEntrada.esEtiquetaValida(limpio)) {
            throw new IllegalArgumentException("El nombre de la etiqueta es inválido");
        }
        return new Etiqueta(limpio.toLowerCase());
    }

    public void setNombre(String nuevoNombre) {
        String limpio = Objects.requireNonNull(nuevoNombre).trim();
        if (!ValidadorEntrada.esEtiquetaValida(limpio)) {
            throw new IllegalArgumentException("El nuevo nombre de la etiqueta es inválido");
        }
        this.nombre = limpio.toLowerCase();
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Etiqueta))
            return false;
        Etiqueta e = (Etiqueta) o;
        return nombre.equals(e.nombre);
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
