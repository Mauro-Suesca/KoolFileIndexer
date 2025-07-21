package koolfileindexer.db;

import java.util.Objects;

public class Etiqueta {
    private final String nombre;
    private final koolfileindexer.modelo.Etiqueta modelo;

    public Etiqueta(String nombre) {
        this.nombre = nombre;
        try {
            // Intenta crear un objeto Etiqueta del modelo
            this.modelo = koolfileindexer.modelo.Etiqueta.crear(nombre);
        } catch (Exception e) {
            // Si falla, dejamos modelo como null
            throw new IllegalArgumentException("Nombre de etiqueta inválido: " + nombre);
        }
    }

    public String getNombre() {
        return nombre;
    }

    // Acceso al modelo subyacente
    public koolfileindexer.modelo.Etiqueta getModelo() {
        return modelo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Etiqueta etiqueta = (Etiqueta) o;
        return Objects.equals(nombre.toLowerCase(), etiqueta.nombre.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre.toLowerCase());
    }
}