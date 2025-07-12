package koolfileindexer.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public class ConectorBasedeDatos {
    private static final String JDBC_URL =
        "jdbc:postgresql://localhost:5432/KoolFileIndexer";
    private static final String USUARIO = "kool_user";
    private static final String CONTRASENA = "koolpass";
    private static Connection conexion;

    public static synchronized Connection obtenerConexion() throws SQLException{
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

    public static void terminarConexion() {
        try {
            if (
                conexion != null &&
                !conexion.isClosed()
            ) {
                conexion.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean crearArchivo(Archivo nuevoArchivo) {
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

    public static boolean actualizarNombreArchivo(
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

    public static boolean actualizarTamanoFechaModificacionArchivo(
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

    public static boolean actualizarCategoriaArchivo(
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

    public static boolean eliminarArchivo(Archivo archivoParaEliminar) {
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

    public static ResultSet buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
        Archivo archivoFiltro,
        long tamanoMinimo,
        long tamanoMaximo
    ) {
        ResultSet resultadoConsulta = null;
        PreparedStatement sentenciaEjecutable = null;
        boolean ocurrieronErrores = false, esPrimerComando = true;
        String consultaSqlDinamica =
            "SELECT * FROM ";

        if (archivoFiltro.extension != null) {
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_extension (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.rutaCompleta != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_ubicacion (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.categoria != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_categoria (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.etiquetas != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_etiqueta (?) ";
            esPrimerComando = false;
        }
        if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_tamano (?, ?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.nombre != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica += "sp_buscar_archivos_segun_nombre (?) ";
            esPrimerComando = false;
        }

        if (archivoFiltro.palabrasClave != null) {
            Iterator<String> iteradorPalabrasClave = archivoFiltro.palabrasClave.iterator();
            while(iteradorPalabrasClave.hasNext()){
                consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
                consultaSqlDinamica +=
                "sp_buscar_archivos_con_una_palabra_clave_dada (?) ";
                iteradorPalabrasClave.next();
            }
        }

        try {
            sentenciaEjecutable = conexion.prepareCall(
                consultaSqlDinamica,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            int indiceParametro = 1;
            if (archivoFiltro.extension != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.extension
                );
                indiceParametro++;
            }
            if (archivoFiltro.rutaCompleta != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.rutaCompleta
                );
                indiceParametro++;
            }
            if (archivoFiltro.categoria != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.categoria
                );
                indiceParametro++;
            }
            if (archivoFiltro.etiquetas != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.etiquetas.get(0)
                );
                indiceParametro++;
            }
            if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                sentenciaEjecutable.setLong(indiceParametro, tamanoMinimo);
                sentenciaEjecutable.setLong(indiceParametro, tamanoMaximo);
                indiceParametro += 2;
            }
            if (archivoFiltro.nombre != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.nombre
                );
                indiceParametro++;
            }
            if (archivoFiltro.palabrasClave != null) {
                Iterator<String> iteradorPalabrasClave = archivoFiltro.palabrasClave.iterator();
                while(iteradorPalabrasClave.hasNext()){
                    sentenciaEjecutable.setString(
                        indiceParametro++,
                        iteradorPalabrasClave.next()
                    );
                }
            }

            resultadoConsulta = sentenciaEjecutable.executeQuery();
        } catch (SQLException e) {
            ocurrieronErrores = true;
        }

        if (ocurrieronErrores) {
            return null;
        } else {
            return resultadoConsulta;
        }
    }

    public static ResultSet buscarArchivosPorFiltroMinimoUnaPalabraClave(
        Archivo archivoFiltro,
        long tamanoMinimo,
        long tamanoMaximo
    ) {
        ResultSet resultadoConsulta = null;
        CallableStatement sentenciaEjecutable = null;
        boolean ocurrieronErrores = false, esPrimerComando = true;
        String consultaSqlDinamica =
            "SELECT * FROM ";
        int indiceParametro = 1;

        if (archivoFiltro.extension != null) {
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_extension (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.rutaCompleta != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_ubicacion (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.categoria != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_categoria (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.etiquetas != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_etiqueta (?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.palabrasClave != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias (?) ";
            esPrimerComando = false;
        }
        if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica +=
                "sp_buscar_archivos_segun_tamano (?, ?) ";
            esPrimerComando = false;
        }
        if (archivoFiltro.nombre != null) {
            consultaSqlDinamica += esPrimerComando ? "" : "INTERSECT SELECT * FROM ";
            consultaSqlDinamica += "sp_buscar_archivos_segun_nombre (?) ";
            esPrimerComando = false;
        }

        try {
            sentenciaEjecutable = conexion.prepareCall(
                consultaSqlDinamica,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            if (archivoFiltro.extension != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.extension
                );
                indiceParametro++;
            }
            if (archivoFiltro.rutaCompleta != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.rutaCompleta
                );
                indiceParametro++;
            }
            if (archivoFiltro.categoria != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.categoria
                );
                indiceParametro++;
            }
            if (archivoFiltro.etiquetas != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.etiquetas.get(0)
                );
                indiceParametro++;
            }
            if (archivoFiltro.palabrasClave != null) {
                Array palabras_clave = conexion.createArrayOf(
                    "varchar",
                    archivoFiltro.palabrasClave.toArray()
                );
                sentenciaEjecutable.setArray(
                    indiceParametro,
                    palabras_clave
                );
                indiceParametro++;
            }
            if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                sentenciaEjecutable.setLong(indiceParametro, tamanoMinimo);
                sentenciaEjecutable.setLong(indiceParametro, tamanoMaximo);
                indiceParametro += 2;
            }
            if (archivoFiltro.nombre != null) {
                sentenciaEjecutable.setString(
                    indiceParametro,
                    archivoFiltro.nombre
                );
                indiceParametro++;
            }

            resultadoConsulta = sentenciaEjecutable.executeQuery();
        } catch (SQLException e) {
            ocurrieronErrores = true;
        }

        if (ocurrieronErrores) {
            return null;
        } else {
            return resultadoConsulta;
        }
    }
}
