package modelo;

import java.util.Set;
import java.util.Objects;

public class Categoria {
    public static final Categoria SIN_CATEGORIA = new Categoria("Sin categoría", true);

    private final String nombre;
    private final boolean esAutomatica;

    private Categoria(String nombre, boolean esAutomatica) {
        String limpio = Objects.requireNonNull(nombre).trim();
        if (limpio.length() < 1 || limpio.length() > 50) {
            throw new IllegalArgumentException("Nombre de categoría inválido (1–50 chars)");
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

    public static Categoria clasificar(Archivo archivo) {
        String ext = archivo.getExtension().toLowerCase();
        Set<String> imagenes = Set.of("jpg", "jpeg", "png", "gif", "bmp");
        Set<String> documentos = Set.of("docx", "pdf", "txt");
        Set<String> musica = Set.of("mp3", "wav", "aac");
        Set<String> videos = Set.of("mp4", "avi", "mkv");

        if (imagenes.contains(ext))
            return new Categoria("Imagen", true);
        if (documentos.contains(ext))
            return new Categoria("Documento", true);
        if (musica.contains(ext))
            return new Categoria("Música", true);
        if (videos.contains(ext))
            return new Categoria("Video", true);
        return SIN_CATEGORIA;
    }

    @Override
    public String toString() {
        return nombre + (esAutomatica ? " (auto)" : "");
    }
}
