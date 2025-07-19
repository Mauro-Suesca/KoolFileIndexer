package modelo;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Representa un archivo con metadatos, categoría automática,
 * etiquetas y palabras clave.
 */
public class Archivo {
    private final String nombre;
    private final String rutaCompleta;
    private final String extension;
    private final long tamanoBytes;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private Categoria categoria;
    private final List<Etiqueta> etiquetas = new ArrayList<>();
    private final Set<String> palabrasClave = new HashSet<>();

    public Archivo(
            String nombre,
            String rutaCompleta,
            String extension,
            long tamanoBytes,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaModificacion) {
        this.nombre = Objects.requireNonNull(nombre, "Nombre no puede ser null")
                .trim();
        this.rutaCompleta = Paths.get(rutaCompleta)
                .toAbsolutePath()
                .normalize()
                .toString();
        this.extension = extension != null
                ? extension.toLowerCase()
                : "";
        this.tamanoBytes = tamanoBytes;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.categoria = Categoria.clasificar(this);
    }

    /** Un archivo oculto comienza con punto. */
    public boolean esOculto() {
        return nombre.startsWith(".");
    }

    /** Valida nombre con reglas centralizadas en ValidadorEntrada. */
    public boolean esValido() {
        return !esOculto()
                && ValidadorEntrada.esNombreArchivoValido(nombre);
    }

    public void agregarEtiqueta(Etiqueta etiqueta) {
        if (etiqueta == null) {
            throw new IllegalArgumentException("Etiqueta nula");
        }
        if (!etiquetas.contains(etiqueta)) {
            etiquetas.add(etiqueta);
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    public void quitarEtiqueta(Etiqueta etiqueta) {
        if (etiquetas.remove(etiqueta)) {
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    public void agregarPalabraClave(String palabra) {
        String token = normalizeToken(palabra);
        if (!ValidadorEntrada.esPalabraClaveValida(token)) {
            throw new IllegalArgumentException(
                    String.format("Palabra clave inválida: '%s'", palabra));
        }
        if (palabrasClave.add(token)) {
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    public void eliminarPalabraClave(String palabra) {
        String token = normalizeToken(palabra);
        if (palabrasClave.remove(token)) {
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    public boolean modificarPalabraClave(String antigua, String nueva) {
        String orig = normalizeToken(antigua);
        String neu = normalizeToken(nueva);
        if (!palabrasClave.contains(orig)) {
            return false;
        }
        if (!ValidadorEntrada.esPalabraClaveValida(neu)) {
            throw new IllegalArgumentException(
                    String.format("Nueva palabra clave inválida: '%s'", nueva));
        }
        palabrasClave.remove(orig);
        palabrasClave.add(neu);
        actualizarFechaModificacion(LocalDateTime.now());
        return true;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void asignarCategoria(Categoria categoria) {
        if (categoria != null) {
            this.categoria = categoria;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Archivo))
            return false;
        Archivo that = (Archivo) o;
        return rutaCompleta.equalsIgnoreCase(that.rutaCompleta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rutaCompleta.toLowerCase());
    }

    public String getNombre() {
        return nombre;
    }

    public String getRutaCompleta() {
        return rutaCompleta;
    }

    public String getExtension() {
        return extension;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public List<Etiqueta> getEtiquetas() {
        return Collections.unmodifiableList(etiquetas);
    }

    public Set<String> getPalabrasClave() {
        return Collections.unmodifiableSet(palabrasClave);
    }

    /** Normaliza tokens: trim + toLowerCase. */
    private String normalizeToken(String token) {
        return Objects.requireNonNull(token, "Token no puede ser null")
                .toLowerCase()
                .trim();
    }

    public void actualizarFechaModificacion(LocalDateTime nuevaFecha) {
        this.fechaModificacion = Objects.requireNonNull(nuevaFecha);
    }

    @Override
    public String toString() {
        // Línea única para logs
        return String.format(
                "Archivo[nombre=%s, ruta=%s, ext=%s, tam=%dB, mod=%s, cat=%s, tags=%s, keys=%s]",
                nombre, rutaCompleta, extension,
                tamanoBytes, fechaModificacion,
                categoria.getNombre(),
                etiquetas, palabrasClave);
    }
}
