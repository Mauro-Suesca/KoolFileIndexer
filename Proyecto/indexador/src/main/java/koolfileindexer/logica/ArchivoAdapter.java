package koolfileindexer.logica;

import java.time.LocalDateTime;
import java.util.List;
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
        // Usar el constructor con parámetros de la clase padre con valores seguros
        super("filtro_temp", 0, LocalDateTime.now(), "filtro_temp", "filtro_temp", "OTRO");
        this.nombre = "filtro_temp";
        this.rutaCompleta = "filtro_temp";
        this.extension = "filtro_temp";
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
        return null;
    }

    /**
     * Sobrescribe getNombre para usar nuestro propio valor
     */
    @Override
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Establece el nombre del filtro
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Sobrescribe getRutaCompleta para usar nuestro propio valor
     */
    @Override
    public String getRutaCompleta() {
        return this.rutaCompleta;
    }

    /**
     * Establece la ruta completa del filtro
     */
    public void setRutaCompleta(String rutaCompleta) {
        this.rutaCompleta = rutaCompleta;
    }

    /**
     * Sobrescribe getExtension para usar nuestro propio valor
     */
    @Override
    public String getExtension() {
        return this.extension;
    }

    /**
     * Establece la extensión del filtro
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }
}