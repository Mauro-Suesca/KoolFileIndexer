package koolfileindexer.modelo;

import java.util.Objects;
import java.util.Set;

/**
 * Representa una categoría de archivo.
 * Instancias automáticas según extensión; "Sin categoría" si no coincide.
 */
public class Categoria {
    public static final Categoria IMAGEN = new Categoria("Imagen", true);
    public static final Categoria DOCUMENTO = new Categoria("Documento", true);
    public static final Categoria MUSICA = new Categoria("Música", true);
    public static final Categoria VIDEO = new Categoria("Video", true);
    public static final Categoria SIN_CATEGORIA = new Categoria("Sin categoría", true);

    // Extensiones por categoría
    private static final Set<String> EXT_IMAGEN = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    private static final Set<String> EXT_DOCUMENTO = Set.of("docx", "pdf", "txt");
    private static final Set<String> EXT_MUSICA = Set.of("mp3", "wav", "aac");
    private static final Set<String> EXT_VIDEO = Set.of("mp4", "avi", "mkv");

    private final String nombre;
    private final boolean esAutomatica;

    private Categoria(String nombre, boolean esAutomatica) {
        this.nombre = Validator.validarNombreCategoria(nombre);
        this.esAutomatica = esAutomatica;
    }

    /** Clasificación “oficial” usada desde Archivo. Sólo por extensión. */
    public static Categoria clasificar(Archivo archivo) {
        return clasificarPorExtension(archivo);
    }

    /** Sólo por extensión del archivo. */
    public static Categoria clasificarPorExtension(Archivo archivo) {
        String ext = archivo.getExtension().toLowerCase();
        if (EXT_IMAGEN.contains(ext))
            return IMAGEN;
        if (EXT_DOCUMENTO.contains(ext))
            return DOCUMENTO;
        if (EXT_MUSICA.contains(ext))
            return MUSICA;
        if (EXT_VIDEO.contains(ext))
            return VIDEO;
        return SIN_CATEGORIA;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esAutomatica() {
        return esAutomatica;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Categoria that))
            return false;
        return esAutomatica == that.esAutomatica
                && nombre.equalsIgnoreCase(that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre.toLowerCase(), esAutomatica);
    }

    @Override
    public String toString() {
        return nombre + (esAutomatica ? " (auto)" : "");
    }
}
