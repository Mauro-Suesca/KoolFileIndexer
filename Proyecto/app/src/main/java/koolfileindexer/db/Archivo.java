package koolfileindexer.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class Archivo {

    String nombre;
    long tamanoBytes;
    LocalDateTime fechaCreacion;
    String rutaCompleta;
    String extension;
    String categoria;
    List<String> etiquetas;
    Set<String> palabrasClave;
}
