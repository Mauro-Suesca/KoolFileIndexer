package koolfileindexer.db;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptador para usar ArchivoModelo desde el paquete DB
 */
public class Archivo {
    // Usamos el nombre completo en lugar de "as"
    private final koolfileindexer.modelo.Archivo modelo;

    public Archivo(String nombre, long tamanoBytes, LocalDateTime fechaModificacion,
            String rutaCompleta, String extension, String categoria) {
        // Crear el objeto modelo
        this.modelo = new koolfileindexer.modelo.Archivo(
                nombre,
                rutaCompleta,
                extension,
                tamanoBytes,
                LocalDateTime.now(), // fecha creación
                fechaModificacion // fecha modificación
        );

        // Asignar categoría correctamente
        try {
            // Intenta usar el valor del enum
            koolfileindexer.modelo.Categoria cat = koolfileindexer.modelo.Categoria.valueOf(categoria.toUpperCase());
            this.modelo.asignarCategoria(cat);
        } catch (IllegalArgumentException e) {
            // Si falla, usa OTRO
            this.modelo.asignarCategoria(koolfileindexer.modelo.Categoria.OTRO);
        }
    }

    // Getters para ConectorBaseDatos
    public String getNombre() {
        return modelo.getNombre();
    }

    public long getTamanoBytes() {
        return modelo.getTamanoBytes();
    }

    public LocalDateTime getFechaModificacion() {
        return modelo.getFechaModificacion();
    }

    public String getRutaCompleta() {
        return modelo.getRutaCompleta();
    }

    public String getExtension() {
        return modelo.getExtension();
    }

    public Categoria getCategoria() {
        return new Categoria(modelo.getCategoria().name());
    }

    public List<Etiqueta> getEtiquetas() {
        // Implementación mínima para compatibilidad
        return null;
    }

    public Set<String> getPalabrasClave() {
        return modelo.getPalabrasClave();
    }

    public void setPalabrasClave(Set<String> palabrasClave) {
        // Clone the set to avoid modifying an unmodifiable collection
        Set<String> palabrasClaveModificables = new HashSet<>();
        if (palabrasClave != null) {
            palabrasClaveModificables.addAll(palabrasClave);
        }

        // Clear existing and add new
        Set<String> existentes = modelo.getPalabrasClave();
        for (String palabra : existentes) {
            modelo.eliminarPalabraClave(palabra);
        }

        for (String palabra : palabrasClaveModificables) {
            modelo.agregarPalabraClave(palabra);
        }
    }

    // Método para acceder directamente al modelo
    public koolfileindexer.modelo.Archivo getModelo() {
        return modelo;
    }
}