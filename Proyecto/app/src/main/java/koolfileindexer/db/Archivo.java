package koolfileindexer.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class Archivo {
    //Esqueleto básico de la clase Archivo que es necesario para las funciones del conector a la base de datos, será reemplazado por una clase completa eventualmente
    String nombre;
    long tamanoBytes;
    LocalDateTime fechaCreacion;
    String rutaCompleta;
    String extension;
    String categoria;
    List<String> etiquetas;
    Set<String> palabrasClave;
}
