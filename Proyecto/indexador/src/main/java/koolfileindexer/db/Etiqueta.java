package koolfileindexer.db;

import java.util.Objects;

public class Etiqueta {
    private final String nombre;
    private final koolfileindexer.modelo.Etiqueta modelo;

    public Etiqueta(String nombre) {
        // Validación de null
        Objects.requireNonNull(nombre, "nombre no puede ser null");

        try {
            // Crear primero el modelo (que hace su propia validación)
            koolfileindexer.modelo.Etiqueta modeloCreado = koolfileindexer.modelo.Etiqueta.crear(nombre);

            // Usar el nombre normalizado que devuelve el modelo
            // para garantizar 100% de consistencia
            this.nombre = modeloCreado.getNombre();
            this.modelo = modeloCreado;
        } catch (IllegalArgumentException ex) {
            // Captura específica de IllegalArgumentException
            throw new IllegalArgumentException("Nombre de etiqueta inválido: " + nombre, ex);
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