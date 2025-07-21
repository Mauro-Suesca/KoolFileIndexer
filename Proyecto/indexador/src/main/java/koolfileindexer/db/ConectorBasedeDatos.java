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

    private final String JDBC_URL = "jdbc:postgresql://localhost:5432/KoolFileIndexer";
    private final String USUARIO = "kool_user";
    private final String CONTRASENA = "koolpass";
    private Connection conexion;

    public static ConectorBasedeDatos obtenerInstancia() {
        ConectorBasedeDatos resultado = instancia;

        if (resultado != null) {
            return resultado;
        }
        synchronized (ConectorBasedeDatos.class) {
            if (instancia == null) {
                instancia = new ConectorBasedeDatos();
            }
            return instancia;
        }
    }

    public synchronized Connection obtenerConexion() throws SQLException {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(
                        JDBC_URL,
                        USUARIO,
                        CONTRASENA);
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

    public void crearArchivo(Archivo nuevoArchivo) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_crear_archivo(?, ?, ?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, nuevoArchivo.getNombre());
        sentenciaEjecutable.setLong(2, nuevoArchivo.getTamanoBytes());

        java.sql.Date fechaModificacionParaSql = java.sql.Date.valueOf(
                nuevoArchivo.getFechaModificacion().toLocalDate());
        sentenciaEjecutable.setDate(3, fechaModificacionParaSql);

        sentenciaEjecutable.setString(4, nuevoArchivo.getRutaCompleta());
        sentenciaEjecutable.setString(5, nuevoArchivo.getExtension());
        sentenciaEjecutable.setString(6, nuevoArchivo.getCategoria().getNombre());

        sentenciaEjecutable.execute();
    }

    public void asociarPalabraClaveArchivo(
            Archivo archivoParaModificar,
            String nuevaPalabraClave) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_asociar_palabra_clave_archivo (?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaModificar.getRutaCompleta());
        sentenciaEjecutable.setString(2, archivoParaModificar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setString(4, nuevaPalabraClave);

        sentenciaEjecutable.execute();
    }

    public void asociarEtiquetaArchivo(
            Archivo archivoParaModificar,
            String nuevaEtiqueta) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_asociar_etiqueta_archivo (?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaModificar.getRutaCompleta());
        sentenciaEjecutable.setString(2, archivoParaModificar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setString(4, nuevaEtiqueta);

        sentenciaEjecutable.execute();
    }

    public ResultSet buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
            Archivo archivoFiltro,
            long tamanoMinimo,
            long tamanoMaximo) throws SQLException {

        boolean esPrimerComando = true;
        String consultaSQLDinamica = "SELECT * FROM ";

        if (archivoFiltro.getPalabrasClave() != null) {
            Iterator<String> iteradorPalabrasClave = archivoFiltro.getPalabrasClave().iterator();
            while (iteradorPalabrasClave.hasNext()) {
                consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSQLDinamica += "sp_buscar_archivos_con_una_palabra_clave_dada (?) ";
                esPrimerComando = false;
                iteradorPalabrasClave.next();
            }
        }

        CallableStatement sentenciaEjecutable = generarSentenciaEjecutableParaBuscarArchivos(
                archivoFiltro,
                tamanoMinimo,
                tamanoMaximo,
                esPrimerComando,
                consultaSQLDinamica);

        int indiceParametro = 1;

        if (archivoFiltro.getPalabrasClave() != null) {
            Iterator<String> iteradorPalabrasClave = archivoFiltro.getPalabrasClave().iterator();
            while (iteradorPalabrasClave.hasNext()) {
                sentenciaEjecutable.setString(
                        indiceParametro++,
                        iteradorPalabrasClave.next());
            }
        }

        return ejecutarConsultaSQLParaBuscarArchivos(
                archivoFiltro,
                tamanoMinimo,
                tamanoMaximo,
                sentenciaEjecutable,
                indiceParametro);
    }

    public ResultSet buscarArchivosPorFiltroMinimoUnaPalabraClave(
            Archivo archivoFiltro,
            long tamanoMinimo,
            long tamanoMaximo) throws SQLException {

        boolean esPrimerComando = true;
        String consultaSQLDinamica = "SELECT * FROM ";

        if (archivoFiltro.getPalabrasClave() != null) {
            consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSQLDinamica += "sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias (?) ";
            esPrimerComando = false;
        }

        CallableStatement sentenciaEjecutable = generarSentenciaEjecutableParaBuscarArchivos(
                archivoFiltro,
                tamanoMinimo,
                tamanoMaximo,
                esPrimerComando,
                consultaSQLDinamica);

        int indiceParametro = 1;

        if (archivoFiltro.getPalabrasClave() != null) {
            Array palabras_clave = conexion.createArrayOf(
                    "varchar",
                    archivoFiltro.getPalabrasClave().toArray());
            sentenciaEjecutable.setArray(
                    indiceParametro++,
                    palabras_clave);
        }

        return ejecutarConsultaSQLParaBuscarArchivos(
                archivoFiltro,
                tamanoMinimo,
                tamanoMaximo,
                sentenciaEjecutable,
                indiceParametro);
    }

    private CallableStatement generarSentenciaEjecutableParaBuscarArchivos(
            Archivo archivoFiltro,
            long tamanoMinimo,
            long tamanoMaximo,
            boolean esPrimerComando,
            String consultaSQLDinamica) throws SQLException {

        if (archivoFiltro.getExtension() != null) {
            consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSQLDinamica += "sp_buscar_archivos_segun_extension (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.getRutaCompleta() != null) {
            consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSQLDinamica += "sp_buscar_archivos_segun_ubicacion (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.getCategoria() != null) {
            consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSQLDinamica += "sp_buscar_archivos_segun_categoria (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.getEtiquetas() != null) {
            consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSQLDinamica += "sp_buscar_archivos_segun_etiqueta (?) ";
            esPrimerComando = false;
        }
        if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
            consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSQLDinamica += "sp_buscar_archivos_segun_tamano (?, ?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.getNombre() != null) {
            consultaSQLDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSQLDinamica += "sp_buscar_archivos_segun_nombre (?) ";
            esPrimerComando = false;
        }

        return conexion.prepareCall(
                consultaSQLDinamica,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
    }

    private ResultSet ejecutarConsultaSQLParaBuscarArchivos(
            Archivo archivoFiltro,
            long tamanoMinimo,
            long tamanoMaximo,
            CallableStatement sentenciaEjecutable,
            int indiceParametro) throws SQLException {

        if (archivoFiltro.getExtension() != null) {
            sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.getExtension());
        }
        if (archivoFiltro.getRutaCompleta() != null) {
            sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.getRutaCompleta());
        }
        if (archivoFiltro.getCategoria() != null) {
            sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.getCategoria().getNombre());
        }
        if (archivoFiltro.getEtiquetas() != null) {
            sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.getEtiquetas().get(0).getNombre());
        }
        if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
            sentenciaEjecutable.setLong(indiceParametro++, tamanoMinimo);
            sentenciaEjecutable.setLong(indiceParametro++, tamanoMaximo);
        }
        if (archivoFiltro.getNombre() != null) {
            sentenciaEjecutable.setString(
                    indiceParametro++,
                    archivoFiltro.getNombre());
        }

        return sentenciaEjecutable.executeQuery();
    }

    public void actualizarUbicacionConNombreNuevo(
            String viejaUbicacion,
            String nuevaUbicacion) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_actualizar_nombre_ubicacion (?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, viejaUbicacion);
        sentenciaEjecutable.setString(2, nuevaUbicacion);

        sentenciaEjecutable.execute();
    }

    public void actualizarUbicacionArchivo(
            Archivo archivoParaModificar,
            String viejaUbicacion) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_actualizar_archivo_con_nueva_ubicacion (?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, viejaUbicacion);
        sentenciaEjecutable.setString(2, archivoParaModificar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setString(4, archivoParaModificar.getRutaCompleta());

        sentenciaEjecutable.execute();
    }

    public void actualizarNombreArchivo(
            Archivo archivoParaModificar,
            String viejo_nombre) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_actualizar_nombre_archivo (?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaModificar.getRutaCompleta());
        sentenciaEjecutable.setString(2, viejo_nombre);
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setString(4, archivoParaModificar.getNombre());

        sentenciaEjecutable.execute();
    }

    public void actualizarTamanoFechaModificacionArchivo(
            Archivo archivoParaModificar) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_actualizar_tamano_fecha_modificacion_archivo (?, ?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaModificar.getRutaCompleta());
        sentenciaEjecutable.setString(2, archivoParaModificar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setLong(4, archivoParaModificar.getTamanoBytes());

        java.sql.Date fechaModificacionParaSql = java.sql.Date.valueOf(
                archivoParaModificar.getFechaModificacion().toLocalDate());
        sentenciaEjecutable.setDate(5, fechaModificacionParaSql);

        sentenciaEjecutable.execute();
    }

    public void actualizarCategoriaArchivo(
            Archivo archivoParaModificar) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_actualizar_categoria_archivo (?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaModificar.getRutaCompleta());
        sentenciaEjecutable.setString(2, archivoParaModificar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setString(4, archivoParaModificar.getCategoria().getNombre());

        sentenciaEjecutable.execute();
    }

    public void desasociarPalabraClaveArchivo(
            Archivo archivoParaModificar,
            String palabraClaveParaEliminar) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_desasociar_palabra_clave_archivo (?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaModificar.getRutaCompleta());
        sentenciaEjecutable.setString(2, archivoParaModificar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setString(4, palabraClaveParaEliminar);

        sentenciaEjecutable.execute();
    }

    public void desasociarEtiquetaArchivo(
            Archivo archivoParaModificar,
            String etiquetaParaEliminar) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_desasociar_etiqueta_archivo (?, ?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaModificar.getRutaCompleta());
        sentenciaEjecutable.setString(2, archivoParaModificar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaModificar.getExtension());
        sentenciaEjecutable.setString(4, etiquetaParaEliminar);

        sentenciaEjecutable.execute();
    }

    public void eliminarArchivo(Archivo archivoParaEliminar) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_eliminar_archivo (?, ?, ?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, archivoParaEliminar.getRutaCompleta());
        sentenciaEjecutable.setString(2, archivoParaEliminar.getNombre());
        sentenciaEjecutable.setString(3, archivoParaEliminar.getExtension());

        sentenciaEjecutable.execute();

    }

    public void eliminarArchivosEnUbicacion(String ubicacionParaEliminar) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_eliminar_archivos_en_ubicacion (?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, ubicacionParaEliminar);

        sentenciaEjecutable.execute();
    }

    public void eliminarEtiqueta(String etiquetaParaEliminar) throws SQLException {
        CallableStatement sentenciaEjecutable = null;
        final String stringComandoSql = "{CALL sp_eliminar_etiqueta (?)}";

        sentenciaEjecutable = conexion.prepareCall(
                stringComandoSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        sentenciaEjecutable.setString(1, etiquetaParaEliminar);

        sentenciaEjecutable.execute();
    }
}