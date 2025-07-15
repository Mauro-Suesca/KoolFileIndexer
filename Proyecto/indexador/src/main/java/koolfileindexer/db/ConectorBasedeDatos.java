package koolfileindexer.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public class ConectorBasedeDatos {
    private static volatile ConectorBasedeDatos instancia;

    private final String JDBC_URL =
        "jdbc:postgresql://localhost:5432/KoolFileIndexer";
    private final String USUARIO = "kool_user";
    private final String CONTRASENA = "koolpass";
    private Connection conexion;

    public static ConectorBasedeDatos obtenerInstancia(){
        ConectorBasedeDatos resultado = instancia;
        
        if(resultado != null){
            return resultado;
        }
        synchronized(ConectorBasedeDatos.class){
            if(instancia == null){
                instancia = new ConectorBasedeDatos();
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

    public boolean crearArchivo(Archivo nuevoArchivo) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_crear_archivo(?, ?, ?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, nuevoArchivo.nombre);
            sentenciaEjecutable.setLong(2, nuevoArchivo.tamanoBytes);

            java.sql.Date fechaModificacionParaSql = java.sql.Date.valueOf(
                nuevoArchivo.fechaCreacion.toLocalDate()
            );
            sentenciaEjecutable.setDate(3, fechaModificacionParaSql);

            sentenciaEjecutable.setString(4, nuevoArchivo.rutaCompleta);
            sentenciaEjecutable.setString(5, nuevoArchivo.extension);
            sentenciaEjecutable.setString(6, nuevoArchivo.categoria);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean asociarPalabraClaveArchivo(
        Archivo archivoParaModificar,
        String nuevaPalabraClave
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_asociar_palabra_clave_archivo (?, ?, ?, ?)}";

        try {
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
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean asociarEtiquetaArchivo(
        Archivo archivoParaModificar,
        String nuevaEtiqueta
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_asociar_etiqueta_archivo (?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaModificar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaModificar.nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setString(4, nuevaEtiqueta);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public ResultSet buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
        Archivo archivoFiltro,
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
        Archivo archivoFiltro,
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
        Archivo archivoFiltro,
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
        Archivo archivoFiltro, 
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

    public boolean actualizarUbicacionConNombreNuevo(
        String viejaUbicacion,
        String nuevaUbicacion
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_actualizar_nombre_ubicacion (?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, viejaUbicacion);
            sentenciaEjecutable.setString(2, nuevaUbicacion);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean actualizarUbicacionArchivo(
        Archivo archivoParaModificar,
        String viejaUbicacion
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_actualizar_archivo_con_nueva_ubicacion (?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, viejaUbicacion);
            sentenciaEjecutable.setString(2, archivoParaModificar.nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setString(4, archivoParaModificar.rutaCompleta);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean actualizarNombreArchivo(
        Archivo archivoParaModificar,
        String viejo_nombre
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_actualizar_nombre_archivo (?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaModificar.rutaCompleta);
            sentenciaEjecutable.setString(2, viejo_nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setString(4, archivoParaModificar.nombre);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean actualizarTamanoFechaModificacionArchivo(
        Archivo archivoParaModificar
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_actualizar_tamano_fecha_modificacion_archivo (?, ?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaModificar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaModificar.nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setLong(4, archivoParaModificar.tamanoBytes);

            java.sql.Date fechaModificacionParaSql = java.sql.Date.valueOf(
                archivoParaModificar.fechaCreacion.toLocalDate()
            );
            sentenciaEjecutable.setDate(5, fechaModificacionParaSql);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean actualizarCategoriaArchivo(
        Archivo archivoParaModificar
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_actualizar_categoria_archivo (?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaModificar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaModificar.nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setString(4, archivoParaModificar.categoria);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean desasociarPalabraClaveArchivo(
        Archivo archivoParaModificar,
        String palabraClaveParaEliminar
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_desasociar_palabra_clave_archivo (?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaModificar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaModificar.nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setString(4, palabraClaveParaEliminar);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean desasociarEtiquetaArchivo(
        Archivo archivoParaModificar,
        String etiquetaParaEliminar
    ) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_desasociar_etiqueta_archivo (?, ?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaModificar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaModificar.nombre);
            sentenciaEjecutable.setString(3, archivoParaModificar.extension);
            sentenciaEjecutable.setString(4, etiquetaParaEliminar);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean eliminarArchivo(Archivo archivoParaEliminar) {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_eliminar_archivo (?, ?, ?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, archivoParaEliminar.rutaCompleta);
            sentenciaEjecutable.setString(2, archivoParaEliminar.nombre);
            sentenciaEjecutable.setString(3, archivoParaEliminar.extension);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean eliminarArchivosEnUbicacion(String ubicacionParaEliminar){
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_eliminar_archivos_en_ubicacion (?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, ubicacionParaEliminar);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public boolean eliminarEtiqueta(String etiquetaParaEliminar){
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql =
            "{CALL sp_eliminar_etiqueta (?)}";

        try {
            sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            sentenciaEjecutable.setString(1, etiquetaParaEliminar);

            sentenciaEjecutable.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }
}