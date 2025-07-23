package koolfileindexer.logica;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import koolfileindexer.db.Etiqueta;

/**
 * Adaptador para evitar IndexOutOfBoundsException en ConectorBasedeDatos
 * cuando se trabaja con listas de etiquetas vacías.
 */
public class ArchivoAdapter extends koolfileindexer.db.Archivo {

    // Variables para almacenar los valores filtro
    private String nombre;
    private String rutaCompleta;
    private String extension;

    /**
     * Constructor por defecto para consultas de filtro
     */
    public ArchivoAdapter() {
        super("filtro_temp", 0, LocalDateTime.now(), "filtro_temp", "filtro_temp", "OTRO");
        // CAMBIAR ESTO:
        this.nombre = null;
        this.rutaCompleta = null;
        this.extension = null;
    }

    /**
     * Constructor completo con todos los parámetros
     */
    public ArchivoAdapter(String nombre, long tamanoBytes, LocalDateTime fechaModificacion,
            String rutaCompleta, String extension, String categoria) {
        super(nombre, tamanoBytes, fechaModificacion, rutaCompleta, extension, categoria);
        this.nombre = nombre;
        this.rutaCompleta = rutaCompleta;
        this.extension = extension;
    }

    /**
     * Sobrescribe getEtiquetas para retornar null en vez de una lista vacía
     */
    @Override
    public List<Etiqueta> getEtiquetas() {
        // Siempre devolver null para evitar problemas con listas vacías
        return null;
    }

    // Implementar correctamente estos setters
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setRutaCompleta(String rutaCompleta) {
        this.rutaCompleta = rutaCompleta;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    // Modificar los siguientes métodos:
    @Override
    public String getNombre() {
        return this.nombre; // Devuelve null si nombre es null
    }

    @Override
    public String getRutaCompleta() {
        return this.rutaCompleta; // Devuelve null si rutaCompleta es null
    }

    @Override
    public String getExtension() {
        return this.extension; // Devuelve null si extension es null
    }

    @Override
    public koolfileindexer.db.Categoria getCategoria() {
        // Devuelve null para evitar filtros por categoría en consultas
        return null;
    }

    /**
     * Sobrescribe getPalabrasClave para retornar null en vez de un conjunto vacío
     */
    @Override
    public Set<String> getPalabrasClave() {
        Set<String> claves = super.getPalabrasClave();
        return (claves == null || claves.isEmpty()) ? null : claves;
    }
}