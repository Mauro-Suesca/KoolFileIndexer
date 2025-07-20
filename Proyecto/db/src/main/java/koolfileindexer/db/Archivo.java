package koolfileindexer.db;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Archivo {
    //Esqueleto básico de la clase Archivo que es necesario para las funciones del conector a la base de datos, será reemplazado por una clase completa eventualmente
    private String nombre;
    private long tamanoBytes;
    private LocalDateTime fechaModificacion;
    private String rutaCompleta;
    private String extension;
    private Categoria categoria;
    private List<Etiqueta> etiquetas;
    private Set<String> palabrasClave;

    Archivo(String nombre, long tamanoBytes, LocalDateTime fechaModificacion, String rutaCompleta, String extension, String categoria){
        this.nombre = nombre;
        this.tamanoBytes = tamanoBytes;
        this.fechaModificacion = fechaModificacion;
        this.rutaCompleta = rutaCompleta;
        this.extension = extension;
        this.categoria = new Categoria(categoria);
    }

    public Categoria getCategoria() {
        return categoria;
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

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public List<Etiqueta> getEtiquetas() {
        return Collections.unmodifiableList(etiquetas);
    }

    public Set<String> getPalabrasClave() {
        return Collections.unmodifiableSet(palabrasClave);
    }
}
