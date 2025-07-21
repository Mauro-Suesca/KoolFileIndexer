package koolfileindexer.db;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArchivoBD {
    private String nombre;
    private long tamanoBytes;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private String rutaCompleta;
    private String extension;
    private String categoria;
    private List<String> etiquetas = new ArrayList<>();
    private Set<String> palabrasClave = new HashSet<>();

    // Getters y setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getRutaCompleta() {
        return rutaCompleta;
    }

    public void setRutaCompleta(String rutaCompleta) {
        this.rutaCompleta = rutaCompleta;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas != null ? etiquetas : new ArrayList<>();
    }

    public Set<String> getPalabrasClave() {
        return palabrasClave;
    }

    public void setPalabrasClave(Set<String> palabrasClave) {
        this.palabrasClave = palabrasClave != null ? palabrasClave : new HashSet<>();
    }
}
