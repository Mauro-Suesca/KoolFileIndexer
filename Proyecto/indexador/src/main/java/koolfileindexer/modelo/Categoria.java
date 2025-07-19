package modelo;

import java.util.Objects;
import java.util.Set;

/**
 * Representa una categoría de archivo.
 * Instancias automáticas según extensión; sin categoría si no coincide.
 */
public class Categoria {
    public static final Categoria IMAGEN = new Categoria("Imagen", true);
    public static final Categoria DOCUMENTO = new Categoria("Documento", true);
    public static final Categoria MUSICA = new Categoria("Música", true);
    public static final Categoria VIDEO = new Categoria("Video", true);
    public static final Categoria SIN_CATEGORIA = new Categoria("Sin categoría", true);

    private static final Set<String> EXT_IMAGEN = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    private static final Set<String> EXT_DOCUMENTO = Set.of("docx", "pdf", "txt");
    private static final Set<String> EXT_MUSICA = Set.of("mp3", "wav", "aac");
    private static final Set<String> EXT_VIDEO = Set.of("mp4", "avi", "mkv");

    private final String nombre;
    private final boolean esAutomatica;

    private Categoria(String nombre, boolean esAutomatica) {
        String limpio = Objects.requireNonNull(nombre, "Nombre no puede ser null").trim();
        if (limpio.length() < 1 || limpio.length() > 50) {
            throw new IllegalArgumentException("Nombre de categoría inválido (1–50 chars): " + limpio);
        }
        this.nombre = limpio;
        this.esAutomatica = esAutomatica;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esAutomatica() {
        return esAutomatica;
    }

    /**
     * Clasifica un archivo en una categoría automática según su extensión.
     * 
     * @param archivo el archivo a clasificar
     * @return IMAGEN, DOCUMENTO, MUSICA, VIDEO o SIN_CATEGORIA
     */
    public static Categoria clasificar(Archivo archivo) {
        String ext = archivo.getExtension().toLowerCase();
        if (EXT_IMAGEN.contains(ext)) {
            return IMAGEN;
        }
        if (EXT_DOCUMENTO.contains(ext)) {
            return DOCUMENTO;
        }
        if (EXT_MUSICA.contains(ext)) {
            return MUSICA;
        }
        if (EXT_VIDEO.contains(ext)) {
            return VIDEO;
        }
        return SIN_CATEGORIA;
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
