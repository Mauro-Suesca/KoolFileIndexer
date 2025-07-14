package koolfileindexer.modelo;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

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
        LocalDateTime fechaModificacion
    ) {
        // RN-001: nombre trim(), rutas absolutas y normalizadas
        this.nombre = Objects.requireNonNull(nombre).trim();
        this.rutaCompleta = Paths.get(rutaCompleta)
            .toAbsolutePath()
            .normalize()
            .toString();
        this.extension = (extension != null ? extension.toLowerCase() : "");
        this.tamanoBytes = tamanoBytes;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.categoria = Categoria.clasificar(this);
    }

    public boolean esOculto() {
        return nombre.startsWith(".");
    }

    public boolean esValido() {
        return !esOculto() && !nombre.isEmpty();
    }

    public void agregarEtiqueta(Etiqueta etiqueta) {
        if (etiqueta == null) throw new IllegalArgumentException(
            "Etiqueta nula"
        );
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
        if (!ValidadorEntrada.esPalabraClaveValida(palabra)) {
            throw new IllegalArgumentException(
                "Palabra clave inválida: '" + palabra + "'"
            );
        }
        if (palabrasClave.add(palabra.toLowerCase().trim())) {
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    public void eliminarPalabraClave(String palabra) {
        if (palabrasClave.remove(palabra.toLowerCase().trim())) {
            actualizarFechaModificacion(LocalDateTime.now());
        }
    }

    public boolean modificarPalabraClave(String antigua, String nueva) {
        String orig = Objects.requireNonNull(antigua).toLowerCase().trim();
        String neu = Objects.requireNonNull(nueva).toLowerCase().trim();
        if (!palabrasClave.contains(orig)) return false;
        if (!ValidadorEntrada.esPalabraClaveValida(nueva)) {
            throw new IllegalArgumentException(
                "Nueva palabra clave inválida: '" + nueva + "'"
            );
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
        if (categoria != null) this.categoria = categoria;
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

    private void actualizarFechaModificacion(LocalDateTime nuevaFecha) {
        this.fechaModificacion = Objects.requireNonNull(nuevaFecha);
    }

    @Override
    public String toString() {
        return String.format(
            "Archivo{\n" +
            "  nombre       : '%s',\n" +
            "  ruta         : '%s',\n" +
            "  extensión    : '%s',\n" +
            "  tamaño       : %d bytes,\n" +
            "  modificado   : %s,\n" +
            "  categoría    : %s,\n" +
            "  etiquetas    : %s,\n" +
            "  palabrasClave: %s\n" +
            "}",
            nombre,
            rutaCompleta,
            extension,
            tamanoBytes,
            fechaModificacion,
            categoria.getNombre(),
            etiquetas,
            palabrasClave
        );
    }
}
