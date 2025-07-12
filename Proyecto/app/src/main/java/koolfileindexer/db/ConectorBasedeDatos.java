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
    private static Connection conexion_base_de_datos;

    public static synchronized Connection obtenerConexion() throws SQLException{
        try {
            if (conexion_base_de_datos == null || conexion_base_de_datos.isClosed()) {
                conexion_base_de_datos = DriverManager.getConnection(
                    JDBC_URL,
                    USUARIO,
                    CONTRASENA
                );
            }
        } catch (SQLException e) {
            throw new SQLException("Error al obtener la conexión", e);
        }
        return conexion_base_de_datos;
    }

    public static void terminarConexion() {
        try {
            if (
                conexion_base_de_datos != null &&
                !conexion_base_de_datos.isClosed()
            ) {
                conexion_base_de_datos.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean crearArchivo(Archivo nuevo_archivo) {
        CallableStatement statement_a_ejecutar = null;
        final String string_comando_a_ejecutar =
            "{CALL sp_crear_archivo(?, ?, ?, ?, ?, ?)}";

        try {
            statement_a_ejecutar = conexion_base_de_datos.prepareCall(
                string_comando_a_ejecutar,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            statement_a_ejecutar.setString(1, nuevo_archivo.nombre);
            statement_a_ejecutar.setLong(2, nuevo_archivo.tamanoBytes);

            java.sql.Date fecha_modificacion_en_sql = java.sql.Date.valueOf(
                nuevo_archivo.fechaCreacion.toLocalDate()
            );
            statement_a_ejecutar.setDate(3, fecha_modificacion_en_sql);

            statement_a_ejecutar.setString(4, nuevo_archivo.rutaCompleta);
            statement_a_ejecutar.setString(5, nuevo_archivo.extension);
            statement_a_ejecutar.setString(6, nuevo_archivo.categoria);

            statement_a_ejecutar.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public static boolean actualizarNombreArchivo(
        Archivo archivo_modificar,
        String viejo_nombre
    ) {
        CallableStatement statement_a_ejecutar = null;
        final String string_comando_a_ejecutar =
            "{CALL sp_actualizar_nombre_archivo (?, ?, ?, ?)}";

        try {
            statement_a_ejecutar = conexion_base_de_datos.prepareCall(
                string_comando_a_ejecutar,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            statement_a_ejecutar.setString(1, archivo_modificar.rutaCompleta);
            statement_a_ejecutar.setString(2, viejo_nombre);
            statement_a_ejecutar.setString(3, archivo_modificar.extension);
            statement_a_ejecutar.setString(4, archivo_modificar.nombre);

            statement_a_ejecutar.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public static boolean actualizarTamanoFechaModificacionArchivo(
        Archivo archivo_modificar
    ) {
        CallableStatement statement_a_ejecutar = null;
        final String string_comando_a_ejecutar =
            "{CALL sp_actualizar_tamano_fecha_modificacion_archivo (?, ?, ?, ?, ?)}";

        try {
            statement_a_ejecutar = conexion_base_de_datos.prepareCall(
                string_comando_a_ejecutar,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            statement_a_ejecutar.setString(1, archivo_modificar.rutaCompleta);
            statement_a_ejecutar.setString(2, archivo_modificar.nombre);
            statement_a_ejecutar.setString(3, archivo_modificar.extension);
            statement_a_ejecutar.setLong(4, archivo_modificar.tamanoBytes);

            java.sql.Date fecha_modificacion_en_sql = java.sql.Date.valueOf(
                archivo_modificar.fechaCreacion.toLocalDate()
            );
            statement_a_ejecutar.setDate(5, fecha_modificacion_en_sql);

            statement_a_ejecutar.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public static boolean actualizarCategoriaArchivo(
        Archivo archivo_modificar
    ) {
        CallableStatement statement_a_ejecutar = null;
        final String string_comando_a_ejecutar =
            "{CALL sp_actualizar_categoria_archivo (?, ?, ?, ?)}";

        try {
            statement_a_ejecutar = conexion_base_de_datos.prepareCall(
                string_comando_a_ejecutar,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            statement_a_ejecutar.setString(1, archivo_modificar.rutaCompleta);
            statement_a_ejecutar.setString(2, archivo_modificar.nombre);
            statement_a_ejecutar.setString(3, archivo_modificar.extension);
            statement_a_ejecutar.setString(4, archivo_modificar.categoria);

            statement_a_ejecutar.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public static boolean eliminarArchivo(Archivo archivo_eliminar) {
        CallableStatement statement_a_ejecutar = null;
        final String string_comando_a_ejecutar =
            "{CALL sp_eliminar_archivo (?, ?, ?)}";

        try {
            statement_a_ejecutar = conexion_base_de_datos.prepareCall(
                string_comando_a_ejecutar,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            statement_a_ejecutar.setString(1, archivo_eliminar.rutaCompleta);
            statement_a_ejecutar.setString(2, archivo_eliminar.nombre);
            statement_a_ejecutar.setString(3, archivo_eliminar.extension);

            statement_a_ejecutar.execute();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public static ResultSet buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
        Archivo lista_filtros,
        long tamano_minimo,
        long tamano_maximo
    ) {
        ResultSet resultado_consulta = null;
        PreparedStatement statement_a_ejecutar = null;
        boolean ocurrieron_errores = false, es_primer_comando = true;
        String string_comando_a_ejecutar =
            "SELECT * FROM ";

        if (lista_filtros.extension != null) {
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_extension (?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.rutaCompleta != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_ubicacion (?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.categoria != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_categoria (?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.etiquetas != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_etiqueta (?) ";
            es_primer_comando = false;
        }
        if ((tamano_minimo >= 0) & (tamano_maximo >= 0)) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_tamano (?, ?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.nombre != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar += "sp_buscar_archivos_segun_nombre (?) ";
            es_primer_comando = false;
        }

        if (lista_filtros.palabrasClave != null) {
            Iterator<String> iterador_palabras_clave = lista_filtros.palabrasClave.iterator();
            while(iterador_palabras_clave.hasNext()){
                string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
                string_comando_a_ejecutar +=
                "sp_buscar_archivos_con_una_palabra_clave_dada (?) ";
                iterador_palabras_clave.next();
            }
        }

        try {
            statement_a_ejecutar = conexion_base_de_datos.prepareCall(
                string_comando_a_ejecutar,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            int iterador_parametro = 1;
            if (lista_filtros.extension != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.extension
                );
                iterador_parametro++;
            }
            if (lista_filtros.rutaCompleta != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.rutaCompleta
                );
                iterador_parametro++;
            }
            if (lista_filtros.categoria != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.categoria
                );
                iterador_parametro++;
            }
            if (lista_filtros.etiquetas != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.etiquetas.get(0)
                );
                iterador_parametro++;
            }
            if ((tamano_minimo >= 0) & (tamano_maximo >= 0)) {
                statement_a_ejecutar.setLong(iterador_parametro, tamano_minimo);
                statement_a_ejecutar.setLong(iterador_parametro, tamano_maximo);
                iterador_parametro += 2;
            }
            if (lista_filtros.nombre != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.nombre
                );
                iterador_parametro++;
            }
            if (lista_filtros.palabrasClave != null) {
                Iterator<String> iterador_palabras_clave = lista_filtros.palabrasClave.iterator();
                while(iterador_palabras_clave.hasNext()){
                    statement_a_ejecutar.setString(
                        iterador_parametro++,
                        iterador_palabras_clave.next()
                    );
                }
            }

            resultado_consulta = statement_a_ejecutar.executeQuery();
        } catch (SQLException e) {
            ocurrieron_errores = true;
        }

        if (ocurrieron_errores) {
            return null;
        } else {
            return resultado_consulta;
        }
    }

    public static ResultSet buscarArchivosPorFiltroMinimoUnaPalabraClave(
        Archivo lista_filtros,
        long tamano_minimo,
        long tamano_maximo
    ) {
        ResultSet resultado_consulta = null;
        CallableStatement statement_a_ejecutar = null;
        boolean ocurrieron_errores = false, es_primer_comando = true;
        String string_comando_a_ejecutar =
            "SELECT * FROM ";
        int iterador_parametro = 1;

        if (lista_filtros.extension != null) {
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_extension (?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.rutaCompleta != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_ubicacion (?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.categoria != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_categoria (?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.etiquetas != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_etiqueta (?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.palabrasClave != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias (?) ";
            es_primer_comando = false;
        }
        if ((tamano_minimo >= 0) & (tamano_maximo >= 0)) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_tamano (?, ?) ";
            es_primer_comando = false;
        }
        if (lista_filtros.nombre != null) {
            string_comando_a_ejecutar += es_primer_comando ? "" : "INTERSECT SELECT * FROM ";
            string_comando_a_ejecutar += "sp_buscar_archivos_segun_nombre (?) ";
            es_primer_comando = false;
        }

        try {
            statement_a_ejecutar = conexion_base_de_datos.prepareCall(
                string_comando_a_ejecutar,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
            );

            if (lista_filtros.extension != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.extension
                );
                iterador_parametro++;
            }
            if (lista_filtros.rutaCompleta != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.rutaCompleta
                );
                iterador_parametro++;
            }
            if (lista_filtros.categoria != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.categoria
                );
                iterador_parametro++;
            }
            if (lista_filtros.etiquetas != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.etiquetas.get(0)
                );
                iterador_parametro++;
            }
            if (lista_filtros.palabrasClave != null) {
                Array palabras_clave = conexion_base_de_datos.createArrayOf(
                    "varchar",
                    lista_filtros.palabrasClave.toArray()
                );
                statement_a_ejecutar.setArray(
                    iterador_parametro,
                    palabras_clave
                );
                iterador_parametro++;
            }
            if ((tamano_minimo >= 0) & (tamano_maximo >= 0)) {
                statement_a_ejecutar.setLong(iterador_parametro, tamano_minimo);
                statement_a_ejecutar.setLong(iterador_parametro, tamano_maximo);
                iterador_parametro += 2;
            }
            if (lista_filtros.nombre != null) {
                statement_a_ejecutar.setString(
                    iterador_parametro,
                    lista_filtros.nombre
                );
                iterador_parametro++;
            }

            resultado_consulta = statement_a_ejecutar.executeQuery();
        } catch (SQLException e) {
            ocurrieron_errores = true;
        }

        if (ocurrieron_errores) {
            return null;
        } else {
            return resultado_consulta;
        }
    }
}
