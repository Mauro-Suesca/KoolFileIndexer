package koolfileindexer.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BaseDeDatosTest{
    
    @Test
    public void probarCreacionArchivo(){
        MockConectorBasedeDatos conector = MockConectorBasedeDatos.obtenerInstancia();
        MockArchivo archivoPrueba = new MockArchivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");

        try{

            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);

            MockArchivo archivoFiltro = new MockArchivo("documento", -1, null, "/home/docs", "pdf", null);

            ResultSet resultados = conector.buscarArchivosPorFiltroMinimoUnaPalabraClave(archivoFiltro, -1, -1);

            Assertions.assertEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");

            conector.eliminarArchivo(archivoPrueba);

        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }

    @Test
    public void probarEliminacionArchivo(){
        MockConectorBasedeDatos conector = MockConectorBasedeDatos.obtenerInstancia();
        MockArchivo archivoPrueba = new MockArchivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");
            
        try{
            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);
            conector.eliminarArchivo(archivoPrueba);

            MockArchivo archivoFiltro = new MockArchivo("documento", -1, null, "/home/docs", "pdf", null);

            ResultSet resultados = conector.buscarArchivosPorFiltroMinimoUnaPalabraClave(archivoFiltro, -1, -1);

            Assertions.assertNotEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");
        
        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }

    @Test
    public void probarBusquedaExitosaPorVariasPalabrasClave(){
        MockConectorBasedeDatos conector = MockConectorBasedeDatos.obtenerInstancia();
        MockArchivo archivoPrueba = new MockArchivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");

        try{
            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);
            conector.asociarPalabraClaveArchivo(archivoPrueba, "odwubnwdialdyeapow");
            conector.asociarPalabraClaveArchivo(archivoPrueba, "pbuapihewanipxhwai");
            conector.asociarPalabraClaveArchivo(archivoPrueba, "biyuvreuhujvvwxqol");

            MockArchivo archivoFiltro = new MockArchivo(null, -1, null, null, null, null);
            conector.asociarPalabraClaveArchivo(archivoFiltro, "odwubnwdialdyeapow");
            conector.asociarPalabraClaveArchivo(archivoFiltro, "pbuapihewanipxhwai");
            conector.asociarPalabraClaveArchivo(archivoFiltro, "biyuvreuhujvvwxqol");

            ResultSet resultados = conector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(archivoFiltro, -1, -1);

            Assertions.assertEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");

            conector.eliminarArchivo(archivoPrueba);
        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }

    @Test
    public void probarBusquedaSinResultadosPorVariasPalabrasClave(){
        MockConectorBasedeDatos conector = MockConectorBasedeDatos.obtenerInstancia();
        MockArchivo archivoPrueba = new MockArchivo("documento", 1024, java.time.LocalDateTime.now(), "/home/docs", "pdf", "Texto");
        try{
            conector.obtenerConexion();
            conector.crearArchivo(archivoPrueba);
            conector.asociarPalabraClaveArchivo(archivoPrueba, "odwubnwdialdyeapow");

            MockArchivo archivoFiltro = new MockArchivo(null, -1, null, null, null, null);
            conector.asociarPalabraClaveArchivo(archivoFiltro, "odwubnwdialdyeapow");
            conector.asociarPalabraClaveArchivo(archivoFiltro, "pbuapihewanipxhwai");
            conector.asociarPalabraClaveArchivo(archivoFiltro, "biyuvreuhujvvwxqol");

            ResultSet resultados = conector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(archivoFiltro, -1, -1);

            Assertions.assertNotEquals(resultados.getString(2) + "/" + resultados.getString(3) + "." + resultados.getString(4), "/home/docs/documento.pdf");

            conector.eliminarArchivo(archivoPrueba);

        }catch(SQLException e){
            Assertions.fail("Ocurrió una excepción SQL: " + e.getMessage());
        }

        conector.terminarConexion();
    }

    private static class MockConectorBasedeDatos {
        private static volatile MockConectorBasedeDatos instancia;

        private final String JDBC_URL =
            "jdbc:postgresql://localhost:5432/KoolFileIndexer";
        private final String USUARIO = "kool_user";
        private final String CONTRASENA = "koolpass";
        private Connection conexion;

        public static MockConectorBasedeDatos obtenerInstancia(){
            MockConectorBasedeDatos resultado = instancia;
            
            if(resultado != null){
                return resultado;
            }
            synchronized(MockConectorBasedeDatos.class){
                if(instancia == null){
                    instancia = new MockConectorBasedeDatos();
                }
                return instancia;
            }
        }

        public synchronized Connection obtenerConexion() throws SQLException{
            try {
                if (conexion == null || conexion.isClosed()) {
                    conexion = DriverManager.getConnection(
                        JDBC_URL,
                        USUARIO,
                        CONTRASENA
                    );
                }
            } catch (SQLException e) {
                throw new SQLException("Error al obtener la conexión", e);
            }

            return conexion;
        }

        public void terminarConexion() {
            try {
                if (conexion != null && !conexion.isClosed()) {
                    conexion.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void crearArchivo(MockArchivo nuevoArchivo) throws SQLException{
            CallableStatement sentenciaEjecutable = null;
            final String stringComandoSql =
                "{CALL sp_crear_archivo(?, ?, ?, ?, ?, ?)}";

            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, nuevoArchivo.nombre);
            sentenciaEjecutable.setLong(2, nuevoArchivo.tamanoBytes);

            java.sql.Date fechaModificacionParaSql = java.sql.Date.valueOf(
                nuevoArchivo.fechaModificacion.toLocalDate()
            );
            sentenciaEjecutable.setDate(3, fechaModificacionParaSql);

            sentenciaEjecutable.setString(4, nuevoArchivo.rutaCompleta);
            sentenciaEjecutable.setString(5, nuevoArchivo.extension);
            sentenciaEjecutable.setString(6, nuevoArchivo.categoria);

            sentenciaEjecutable.execute();
        }

        public void asociarPalabraClaveArchivo(
            MockArchivo archivoParaModificar,
            String nuevaPalabraClave
        ) throws SQLException{
            CallableStatement sentenciaEjecutable = null;
            final String stringComandoSql =
                "{CALL sp_asociar_palabra_clave_archivo (?, ?, ?, ?)}";

            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaModificar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaModificar.nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setString(4, nuevaPalabraClave);

            sentenciaEjecutable.execute();
        }

        public ResultSet buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
            MockArchivo archivoFiltro,
            long tamanoMinimo,
            long tamanoMaximo
        ) throws SQLException{

            boolean esPrimerComando = true;
            String consultaSQLDinamica = "SELECT * FROM ";

            if (archivoFiltro.palabrasClave != null) {
                Iterator<String> iteradorPalabrasClave = archivoFiltro.palabrasClave.iterator();
                while(iteradorPalabrasClave.hasNext()){
                    consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                    consultaSQLDinamica +=
                        "sp_buscar_archivos_con_una_palabra_clave_dada (?) ";
                    esPrimerComando = false;
                    iteradorPalabrasClave.next();
                }
            }

            CallableStatement sentenciaEjecutable = generarSentenciaEjecutableParaBuscarArchivos(
                archivoFiltro,
                tamanoMinimo, 
                tamanoMaximo, 
                esPrimerComando,
                consultaSQLDinamica
            );

            int indiceParametro = 1;

            if (archivoFiltro.palabrasClave != null) {
                Iterator<String> iteradorPalabrasClave = archivoFiltro.palabrasClave.iterator();
                while(iteradorPalabrasClave.hasNext()){
                    sentenciaEjecutable.setString(
                        indiceParametro++,
                        iteradorPalabrasClave.next()
                    );
                }
            }

            return ejecutarConsultaSQLParaBuscarArchivos(
                archivoFiltro, 
                tamanoMinimo, 
                tamanoMaximo, 
                sentenciaEjecutable, 
                indiceParametro
            );
        }

        public ResultSet buscarArchivosPorFiltroMinimoUnaPalabraClave(
            MockArchivo archivoFiltro,
            long tamanoMinimo,
            long tamanoMaximo
        ) throws SQLException{

            boolean esPrimerComando = true;
            String consultaSQLDinamica = "SELECT * FROM ";

            if (archivoFiltro.palabrasClave != null) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica +=
                    "sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias (?) ";
                esPrimerComando = false;
            }

            CallableStatement sentenciaEjecutable = generarSentenciaEjecutableParaBuscarArchivos(
                archivoFiltro,
                tamanoMinimo, 
                tamanoMaximo,
                esPrimerComando,
                consultaSQLDinamica
            );

            int indiceParametro = 1;

            if (archivoFiltro.palabrasClave != null) {
                Array palabras_clave = conexion.createArrayOf(
                    "varchar",
                    archivoFiltro.palabrasClave.toArray()
                );
                sentenciaEjecutable.setArray(
                    indiceParametro++,
                    palabras_clave
                );
            }

            return ejecutarConsultaSQLParaBuscarArchivos(
                archivoFiltro, 
                tamanoMinimo, 
                tamanoMaximo, 
                sentenciaEjecutable, 
                indiceParametro
            );
        }

        private CallableStatement generarSentenciaEjecutableParaBuscarArchivos(
            MockArchivo archivoFiltro,
            long tamanoMinimo,
            long tamanoMaximo,
            boolean esPrimerComando,
            String consultaSQLDinamica
        ) throws SQLException{

            if (archivoFiltro.extension != null) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica +=
                    "sp_buscar_archivos_segun_extension (?) ";
                esPrimerComando = false;
            }
            if (archivoFiltro.rutaCompleta != null) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica +=
                    "sp_buscar_archivos_segun_ubicacion (?) ";
                esPrimerComando = false;
            }
            if (archivoFiltro.categoria != null) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica +=
                    "sp_buscar_archivos_segun_categoria (?) ";
                esPrimerComando = false;
            }
            if (archivoFiltro.etiquetas != null) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica +=
                    "sp_buscar_archivos_segun_etiqueta (?) ";
                esPrimerComando = false;
            }
            if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica +=
                    "sp_buscar_archivos_segun_tamano (?, ?) ";
                esPrimerComando = false;
            }
            if (archivoFiltro.nombre != null) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica += "sp_buscar_archivos_segun_nombre (?) ";
                esPrimerComando = false;
            }
            
            return conexion.prepareCall(
                consultaSQLDinamica,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );
        }

        private ResultSet ejecutarConsultaSQLParaBuscarArchivos(
            MockArchivo archivoFiltro, 
            long tamanoMinimo,
            long tamanoMaximo,
            CallableStatement sentenciaEjecutable,
            int indiceParametro
        ) throws SQLException{

            if (archivoFiltro.extension != null) {
                sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.extension
                );
            }
            if (archivoFiltro.rutaCompleta != null) {
                sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.rutaCompleta
                );
            }
            if (archivoFiltro.categoria != null) {
                sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.categoria
                );
            }
            if (archivoFiltro.etiquetas != null) {
                sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.etiquetas.get(0)
                );
            }
            if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                sentenciaEjecutable.setLong(indiceParametro++, tamanoMinimo);
                sentenciaEjecutable.setLong(indiceParametro++, tamanoMaximo);
            }
            if (archivoFiltro.nombre != null) {
                sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.nombre
                );
            }

            return sentenciaEjecutable.executeQuery();
        }

        public void eliminarArchivo(MockArchivo archivoParaEliminar) throws SQLException{
            CallableStatement sentenciaEjecutable = null;
            final String stringComandoSql =
                "{CALL sp_eliminar_archivo (?, ?, ?)}";

            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaEliminar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaEliminar.nombre);
            sentenciaEjecutable.setString(3, archivoParaEliminar.extension);

            sentenciaEjecutable.execute();

        }
    }

    private class MockArchivo {
        public String nombre;
        public long tamanoBytes;
        public LocalDateTime fechaModificacion;
        public String rutaCompleta;
        public String extension;
        public String categoria;
        public List<String> etiquetas;
        public Set<String> palabrasClave;

        MockArchivo(String nombre, long tamanoBytes, LocalDateTime fechaModificacion, String rutaCompleta, String extension, String categoria){
            this.nombre = nombre;
            this.tamanoBytes = tamanoBytes;
            this.fechaModificacion = fechaModificacion;
            this.rutaCompleta = rutaCompleta;
            this.extension = extension;
            this.categoria = categoria;
        }
    }
}