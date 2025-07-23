package koolfileindexer.modelo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representa una categoría de archivo.
 * Instancias automáticas según extensión; "Sin categoría" si no coincide.
 */
public enum Categoria {
    DOCUMENTO("Documento"),
    IMAGEN("Imagen"),
    VIDEO("Video"),
    MUSICA("Música"),
    COMPRIMIDO("Comprimido"),
    OTRO("Sin categoría");

    private final String nombre;

    Categoria(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    private static final Map<String, Categoria> extensiones = new HashMap<>();
    static {
        extensiones.put("doc", DOCUMENTO);
        extensiones.put("pdf", DOCUMENTO);
        extensiones.put("txt", DOCUMENTO);
        extensiones.put("jpg", IMAGEN);
        extensiones.put("jpeg", IMAGEN);
        extensiones.put("png", IMAGEN);
        extensiones.put("gif", IMAGEN);
        extensiones.put("bmp", IMAGEN);
        extensiones.put("mp3", MUSICA);
        extensiones.put("wav", MUSICA);
        extensiones.put("aac", MUSICA);
        extensiones.put("mp4", VIDEO);
        extensiones.put("avi", VIDEO);
        extensiones.put("mkv", VIDEO);

        // Agregar las extensiones para COMPRIMIDO
        extensiones.put("zip", COMPRIMIDO);
        extensiones.put("rar", COMPRIMIDO);
        extensiones.put("7z", COMPRIMIDO);
        extensiones.put("tar", COMPRIMIDO);
        extensiones.put("gz", COMPRIMIDO);
    }

    /** Clasificación “oficial” usada desde Archivo. Sólo por extensión. */
    public static Categoria clasificar(Archivo archivo) {
        if (archivo == null) {
            throw new IllegalArgumentException("archivo no puede ser null");
        }
        String ext = archivo.getExtension();
        if (ext == null || ext.trim().isEmpty()) {
            return OTRO;
        }
        return extensiones.getOrDefault(ext.toLowerCase(), OTRO);
    }
}
