package koolfileindexer.db;

public class Categoria {
    private final String nombre;

    public Categoria(String nombre) {
        this.nombre = nombre;
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