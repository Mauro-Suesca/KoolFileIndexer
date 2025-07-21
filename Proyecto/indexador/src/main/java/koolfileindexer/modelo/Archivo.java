package koolfileindexer.modelo;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Representa un archivo con metadatos, categoría automática,
 * etiquetas y palabras clave, y un identificador de BD.
 */
public class Archivo {
    private Long id;
    private final String nombre;
    private final String rutaCompleta;
    private final String extension;
    private final long tamanoBytes;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private Categoria categoria;
    private final Set<Etiqueta> etiquetas = new HashSet<>();
    private final Set<String> palabrasClave = new HashSet<>();

    public Archivo(String nombre, String rutaCompleta, String extension,
            long tamanoBytes, LocalDateTime fechaCreacion,
            LocalDateTime fechaModificacion) {

        // Validar nombre
        if (nombre == null) {
            throw new IllegalArgumentException("nombre no puede ser null");
        }
        if (nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("nombre no puede estar vacío");
        }
        this.nombre = nombre.trim();

        // Validar ruta
        if (rutaCompleta == null) {
            throw new IllegalArgumentException("rutaCompleta no puede ser null");
        }
        if (rutaCompleta.trim().isEmpty()) {
            throw new IllegalArgumentException("rutaCompleta no puede estar vacía");
        }
        this.rutaCompleta = Paths.get(rutaCompleta)
                .toAbsolutePath()
                .normalize()
                .toString();

        // Validar extensión
        if (extension == null) {
            throw new IllegalArgumentException("extension no puede ser null");
        }
        this.extension = extension.trim().toLowerCase();

        // Validar tamaño
        if (tamanoBytes < 0) {
            throw new IllegalArgumentException("tamanoBytes no puede ser negativo");
        }
        this.tamanoBytes = tamanoBytes;

        // Validar fechas
        if (fechaCreacion == null) {
            throw new IllegalArgumentException("fechaCreacion no puede ser null");
        }
        if (fechaModificacion == null) {
            throw new IllegalArgumentException("fechaModificacion no puede ser null");
        }
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;

        this.categoria = Categoria.clasificar(this);
    }

    /** PK de la tabla en BD. */
    public Long getId() {
        return id;
    }

    /** Sólo debe llamarse tras insert() en la BD. */
    public void setId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id debe ser positivo");
        }
        this.id = id;
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

    /** Añade una etiqueta (si pasa validación y no existe). */
    public void agregarEtiqueta(Etiqueta etiqueta) {
        if (etiqueta == null) {
            throw new IllegalArgumentException("Etiqueta nula");
        }
        if (!etiquetas.contains(etiqueta)) {
            etiquetas.add(etiqueta);
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    /** Quita una etiqueta existente. */
    public void quitarEtiqueta(Etiqueta etiqueta) {
        if (etiquetas.remove(etiqueta)) {
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    /**
     * Asociación manual de etiqueta:
     * sólo si es válida y no está ya.
     */
    public void asociarEtiquetaManual(Etiqueta e) {
        if (ValidadorEntrada.esEtiquetaValida(e.getNombre())
                && !etiquetas.contains(e)) {
            etiquetas.add(e);
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    /** Añade palabra clave validada y normalizada. */
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

    /** Elimina una palabra clave. */
    public void eliminarPalabraClave(String palabra) {
        String token = normalizeToken(palabra);
        if (palabrasClave.remove(token)) {
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    /**
     * Asociación manual de palabra clave:
     * sólo si es válida y no está ya.
     */
    public void asociarPalabraClaveManual(String palabra) {
        String token = normalizeToken(palabra);
        if (ValidadorEntrada.esPalabraClaveValida(token)
                && !palabrasClave.contains(token)) {
            palabrasClave.add(token);
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    /** Modifica una palabra clave existente. */
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

    // ─── Categoría ──────────────────────────────────────────────────────────────

    public Categoria getCategoria() {
        return categoria;
    }

    public void asignarCategoria(Categoria categoria) {
        if (categoria != null) {
            this.categoria = categoria;
        }
    }

    // ─── equals()/hashCode() basados SOLO en id si existe ────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Archivo))
            return false;
        Archivo that = (Archivo) o;
        if (this.id != null && that.id != null) {
            return this.id.equals(that.id);
        }
        // fallback: ruta única (case‐insensitive)
        return this.rutaCompleta.equalsIgnoreCase(that.rutaCompleta);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hashCode(id);
        }
        return rutaCompleta.toLowerCase().hashCode();
    }

    // ─── Getters de metadatos ───────────────────────────────────────────────────

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

    public Set<Etiqueta> getEtiquetas() {
        return Collections.unmodifiableSet(etiquetas);
    }

    public Set<String> getPalabrasClave() {
        return Collections.unmodifiableSet(palabrasClave);
    }

    /** Actualiza la fecha de modificación. */
    public void actualizarFechaModificacion(LocalDateTime nuevaFecha) {
        this.fechaModificacion = Objects.requireNonNull(nuevaFecha);
    }

    /** Normaliza tokens: trim + toLowerCase. */
    private String normalizeToken(String token) {
        return Objects.requireNonNull(token, "Token no puede ser null")
                .toLowerCase()
                .trim();
    }

    // ─── toString() incluyendo el id ───────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
                "Archivo[id=%s, nombre=%s, ruta=%s, ext=%s, tam=%dB, mod=%s, cat=%s, etiquetas=%s, palabras claves=%s]",
                id, nombre, rutaCompleta, extension,
                tamanoBytes, fechaModificacion,
                categoria.getNombre(),
                etiquetas, palabrasClave);
    }
}
