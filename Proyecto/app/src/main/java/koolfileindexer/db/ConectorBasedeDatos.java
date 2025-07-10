package koolfileindexer.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConectorBasedeDatos {

    private static final String JDBC_URL =
        "jdbc:postgresql://localhost:5432/KoolFileIndexer";
    private static final String USUARIO = "";
    private static final String CONTRASENA = "";
    private static Connection conexion_base_de_datos;

    public static String iniciar_conexion() {
        String mensajeError = "";

        try {
            Class.forName("org.postgresql.Driver");

            conexion_base_de_datos = DriverManager.getConnection(
                JDBC_URL,
                USUARIO,
                CONTRASENA
            );
        } catch (ClassNotFoundException e) {
            System.err.println(
                "No se encontró el driver de JDBC de PostgreSQL: " +
                e.getMessage()
            );
        } catch (SQLException e) {
            System.err.println(
                "Error al conectar a la base de datos: " + e.getMessage()
            );

            if (e.getErrorCode() == 0) {
                mensajeError = "El servidor no ha sido inicializado";
            } else {
                mensajeError = e.getMessage();
            }
        }
        return mensajeError;
    }

    public static void terminar_conexion() {
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
            "{PERFORM sp_crear_archivo(?, ?, ?, ?, ?, ?)}";

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
            "{PERFORM sp_actualizar_nombre_archivo (?, ?, ?, ?)}";

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
            "{PERFORM sp_actualizar_tamano_fecha_modificacion_archivo (?, ?, ?, ?, ?)}";

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
            "{PERFORM sp_actualizar_categoria_archivo (?, ?, ?, ?)}";

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
            "{PERFORM sp_eliminar_archivo (?, ?, ?)}";

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

    public static ResultSet buscarArchivosPorFiltro(
        Archivo lista_filtros,
        long tamano_minimo,
        long tamano_maximo
    ) {
        ResultSet resultado_consulta = null;
        CallableStatement statement_a_ejecutar = null;
        boolean ocurrieron_errores = false;
        String string_comando_a_ejecutar =
            "{SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension FROM ";
        int iterador_parametro = 1;

        if (lista_filtros.extension != null) {
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_extension (?) ";
        }
        if (lista_filtros.rutaCompleta != null) {
            string_comando_a_ejecutar += "INTERSECT ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_ubicacion (?) ";
        }
        if (lista_filtros.categoria != null) {
            string_comando_a_ejecutar += "INTERSECT ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_categoria (?) ";
        }
        if (lista_filtros.etiquetas != null) {
            string_comando_a_ejecutar += "INTERSECT ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_etiqueta (?) ";
        }
        if (lista_filtros.palabrasClave != null) {
            string_comando_a_ejecutar += "INTERSECT ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_palabra_clave (?) ";
        }
        if ((tamano_minimo >= 0) & (tamano_maximo >= 0)) {
            string_comando_a_ejecutar += "INTERSECT ";
            string_comando_a_ejecutar +=
                "sp_buscar_archivos_segun_tamano (?, ?) ";
        }
        if (lista_filtros.nombre != null) {
            string_comando_a_ejecutar += "INTERSECT ";
            string_comando_a_ejecutar += "sp_buscar_archivos_segun_nombre (?) ";
        }

        string_comando_a_ejecutar += "}";

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

            statement_a_ejecutar.execute();

            resultado_consulta = statement_a_ejecutar.getResultSet();
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
