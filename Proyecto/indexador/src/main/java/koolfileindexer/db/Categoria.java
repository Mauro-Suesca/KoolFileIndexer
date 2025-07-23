package koolfileindexer.db;

import java.util.Objects; // Añadir esta importación

public class Categoria {
    private final String nombre;

    public Categoria(String nombre) {
        Objects.requireNonNull(nombre, "nombre no puede ser null");
        String limpio = nombre.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("nombre no puede estar vacío");
        }
        this.nombre = limpio;
    }

    public String getNombre() {
        return nombre;
    }

    // Método para convertir a Categoria del modelo
    public koolfileindexer.modelo.Categoria toModelCategoria() {
        try {
            return koolfileindexer.modelo.Categoria.valueOf(nombre.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si no existe, retornamos OTRO
            return koolfileindexer.modelo.Categoria.OTRO;
        }
    }
}