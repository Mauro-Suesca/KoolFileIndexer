package koolfileindexer.db;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

public class BaseDeDatosTest{
    
    @Test
    public void probarCreacionArchivo(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");
        conector.obtenerConexion();
        conector.crearArchivo(archivoPrueba);

        Archivo archivoFiltro = new Archivo("documento", -1, null, "/home/docs", "pdf", null);

        ResultSet resultados = conector.buscarArchivosPorFiltroMinimoUnaPalabraClave(archivoFiltro, -1, -1);

        assertEquals(resultados.getString(1), "/home/docs/documento.pdf");

        conector.eliminarArchivo(archivoPrueba);
    }

    @Test
    public void probarEliminacionArchivo(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");
        conector.obtenerConexion();
        conector.crearArchivo(archivoPrueba);
        conector.eliminarArchivo(archivoPrueba);

        Archivo archivoFiltro = new Archivo("documento", -1, null, "/home/docs", "pdf", null);

        ResultSet resultados = conector.buscarArchivosPorFiltroMinimoUnaPalabraClave(archivoFiltro, -1, -1);

        assertNotEquals(resultados.getString(1), "/home/docs/documento.pdf");
    }

    @Test
    public void probarBusquedaExitosaPorVariasPalabrasClave(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");
        conector.obtenerConexion();
        conector.crearArchivo(archivoPrueba);
        conector.asociarPalabraClaveArchivo(archivoPrueba, "odwubnwdialdyeapow");
        conector.asociarPalabraClaveArchivo(archivoPrueba, "pbuapihewanipxhwai");
        conector.asociarPalabraClaveArchivo(archivoPrueba, "biyuvreuhujvvwxqol");

        Archivo archivoFiltro = new Archivo(null, -1, null, null, null, null);
        conector.asociarPalabraClaveArchivo(archivoFiltro, "odwubnwdialdyeapow");
        conector.asociarPalabraClaveArchivo(archivoFiltro, "pbuapihewanipxhwai");
        conector.asociarPalabraClaveArchivo(archivoFiltro, "biyuvreuhujvvwxqol");

        ResultSet resultados = conector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(archivoFiltro, -1, -1);

        assertEquals(resultados.getString(1), "/home/docs/documento.pdf");

        conector.eliminarArchivo(archivoPrueba);
    }

    @Test
    public void probarBusquedaSinResultadosPorVariasPalabrasClave(){
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        Archivo archivoPrueba = new Archivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");
        conector.obtenerConexion();
        conector.crearArchivo(archivoPrueba);
        conector.asociarPalabraClaveArchivo(archivoPrueba, "odwubnwdialdyeapow");

        Archivo archivoFiltro = new Archivo(null, -1, null, null, null, null);
        conector.asociarPalabraClaveArchivo(archivoFiltro, "odwubnwdialdyeapow");
        conector.asociarPalabraClaveArchivo(archivoFiltro, "pbuapihewanipxhwai");
        conector.asociarPalabraClaveArchivo(archivoFiltro, "biyuvreuhujvvwxqol");

        ResultSet resultados = conector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(archivoFiltro, -1, -1);

        assertNotEquals(resultados.getString(1), "/home/docs/documento.pdf");

        conector.eliminarArchivo(archivoPrueba);
    }
}