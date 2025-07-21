package koolfileindexer.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BaseDeDatosTest{
    
    @Test
    public void probarCreacionArchivo(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Documento");

        try{

            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);

            Archivo archivoFiltro = new Archivo("documento", -1, null, "/home/docs", "pdf", null);

            ResultSet resultados = conector.buscarArchivosPorFiltroMinimoUnaPalabraClave(archivoFiltro, -1, -1);

            if(resultados.next()){
                Assertions.assertEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");
            }

            conector.eliminarArchivo(archivoPrueba);

        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }

    @Test
    public void probarEliminacionArchivo(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Documento");
            
        try{
            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);
            conector.eliminarArchivo(archivoPrueba);

            Archivo archivoFiltro = new Archivo("documento", -1, null, "/home/docs", "pdf", null);

            ResultSet resultados = conector.buscarArchivosPorFiltroMinimoUnaPalabraClave(archivoFiltro, -1, -1);

            if(resultados.next()){
                Assertions.assertNotEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");
            }
        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }

    @Test
    public void probarBusquedaExitosaPorVariasPalabrasClave(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Documento");

        try{
            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);
            conector.asociarPalabraClaveArchivo(archivoPrueba, "odwubnwdialdyeapow");
            conector.asociarPalabraClaveArchivo(archivoPrueba, "pbuapihewanipxhwai");
            conector.asociarPalabraClaveArchivo(archivoPrueba, "biyuvreuhujvvwxqol");

            Archivo archivoFiltro = new Archivo(null, -1, null, null, null, null);
            HashSet<String> palabrasFiltro = new HashSet<>();
            palabrasFiltro.add("odwubnwdialdyeapow");
            palabrasFiltro.add("pbuapihewanipxhwai");
            palabrasFiltro.add("biyuvreuhujvvwxqol");

            ResultSet resultados = conector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(archivoFiltro, -1, -1);

            if(resultados.next()){
                Assertions.assertEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");
            }

            conector.eliminarArchivo(archivoPrueba);
        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }

    @Test
    public void probarBusquedaSinResultadosPorVariasPalabrasClave(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Documento");
        try{
            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);
            conector.asociarPalabraClaveArchivo(archivoPrueba, "odwubnwdialdyeapow");

            Archivo archivoFiltro = new Archivo(null, -1, null, null, null, null);
            HashSet<String> palabrasFiltro = new HashSet<>();
            palabrasFiltro.add("odwubnwdialdyeapow");
            palabrasFiltro.add("pbuapihewanipxhwai");
            palabrasFiltro.add("biyuvreuhujvvwxqol");

            ResultSet resultados = conector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(archivoFiltro, -1, -1);

            if(resultados.next()){
                Assertions.assertNotEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");
            }

            conector.eliminarArchivo(archivoPrueba);

        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }
}