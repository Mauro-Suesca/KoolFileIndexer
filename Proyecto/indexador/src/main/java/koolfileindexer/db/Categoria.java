package koolfileindexer.db;

public class Categoria{
    //Esqueleto básico de la clase Categoria que es necesario para las funciones del conector a la base de datos, será reemplazado por una clase completa eventualmente
    private final String nombre;

    Categoria(String nombre){
        this.nombre = nombre;
    }

    public String getNombre(){
        return nombre;
    }
}