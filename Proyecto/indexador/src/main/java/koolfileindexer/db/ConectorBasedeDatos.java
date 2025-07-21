package koolfileindexer.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConectorBasedeDatos {
    private static volatile ConectorBasedeDatos instancia;
    private ScheduledExecutorService connectionMonitor;

    // Configuración de conexión (cargada desde properties)
    private String jdbcUrl;
    private String usuario;
    private String contrasena;

    // Conexión a la base de datos
    private volatile Connection conexion;
    private volatile boolean conexionActiva = false;

    // Configuración de timeouts
    private static final int CONNECTION_TIMEOUT = 5; // segundos
    private static final int VALIDATION_TIMEOUT = 2; // segundos
    private static final int MONITOR_INTERVAL = 30; // segundos
    private static final int MAX_RETRIES = 3;

    /**
     * Obtiene la instancia singleton del conector
     */
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

    private ConectorBasedeDatos() {
        cargarConfiguracion();
        inicializarConexion();
        iniciarMonitorConexion();
    }

    /**
     * Carga la configuración desde un archivo properties
     */
    private void cargarConfiguracion() {
        Properties prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream("dbconfig.properties"));
            jdbcUrl = prop.getProperty("db.url", "jdbc:postgresql://localhost:5432/KoolFileIndexer");
            usuario = prop.getProperty("db.user", "kool_user");
            contrasena = prop.getProperty("db.password", "koolpass");
        } catch (Exception e) {
            System.err.println("[BD] Error al cargar configuración, usando valores por defecto: " + e.getMessage());
            jdbcUrl = "jdbc:postgresql://localhost:5432/KoolFileIndexer";
            usuario = "kool_user";
            contrasena = "koolpass";
        }
    }

    /**
     * Inicializa la conexión a la base de datos
     */
    private void inicializarConexion() {
        try {
            Class.forName("org.postgresql.Driver");
            establecerNuevaConexion();
        } catch (ClassNotFoundException e) {
            System.err.println("[BD] Driver PostgreSQL no encontrado: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[BD] Error inicial de conexión: " + e.getMessage());
        }
    }

    /**
     * Obtiene una conexión válida a la base de datos
     */
    public synchronized Connection obtenerConexion() throws SQLException {
        if (!validarConexion()) {
            establecerNuevaConexion();
        }
        return conexion;
    }

    /**
     * Verifica si la conexión actual es válida
     */
    public synchronized boolean validarConexion() {
        if (conexion == null || !conexionActiva) {
            return false;
        }

        try {
            return conexion.isValid(VALIDATION_TIMEOUT);
        } catch (SQLException e) {
            System.err.println("[BD] Error al validar conexión: " + e.getMessage());
            conexionActiva = false;
            return false;
        }
    }

    /**
     * Establece una nueva conexión a la base de datos
     */
    private synchronized void establecerNuevaConexion() throws SQLException {
        int intentos = 0;
        SQLException ultimoError = null;

        while (intentos < MAX_RETRIES) {
            try {
                cerrarConexionSilenciosamente();

                Properties props = new Properties();
                props.setProperty("user", usuario);
                props.setProperty("password", contrasena);
                props.setProperty("socketTimeout", String.valueOf(CONNECTION_TIMEOUT));
                props.setProperty("connectTimeout", String.valueOf(CONNECTION_TIMEOUT));
                props.setProperty("tcpKeepAlive", "true");

                conexion = DriverManager.getConnection(jdbcUrl, props);
                conexion.setAutoCommit(true);

                // Corrección: Usar Executor en lugar de ExecutorService y convertir a int
                Executor executor = Executors.newSingleThreadExecutor();
                conexion.setNetworkTimeout(executor, (int) TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT));

                conexionActiva = true;
                System.out.println("[BD] Conexión establecida correctamente");
                return;

            } catch (SQLException e) {
                ultimoError = e;
                System.err.println("[BD] Intento " + (intentos + 1) + " de conexión fallido: " + e.getMessage());
                intentos++;

                try {
                    TimeUnit.SECONDS.sleep(1 << intentos); // Backoff exponencial
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupción durante reconexión", ie);
                }
            }
        }

        conexionActiva = false;
        throw new SQLException("No se pudo establecer conexión después de " + MAX_RETRIES + " intentos", ultimoError);
    }

    /**
     * Cierra la conexión actual de manera segura
     */
    private void cerrarConexionSilenciosamente() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("[BD] Error al cerrar conexión: " + e.getMessage());
            } finally {
                conexion = null;
                conexionActiva = false;
            }
        }
    }

    /**
     * Inicia el monitor que verifica periódicamente la conexión
     */
    private void iniciarMonitorConexion() {
        connectionMonitor = Executors.newSingleThreadScheduledExecutor();
        connectionMonitor.scheduleAtFixedRate(() -> {
            try {
                if (!validarConexion()) {
                    System.out.println("[BD] Monitor: Intentando reconexión...");
                    establecerNuevaConexion();
                }
            } catch (SQLException e) {
                System.err.println("[BD] Monitor: Error en reconexión: " + e.getMessage());
            }
        }, MONITOR_INTERVAL, MONITOR_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Cierra todos los recursos del conector
     */
    public void shutdown() {
        if (connectionMonitor != null) {
            connectionMonitor.shutdown();
            try {
                if (!connectionMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                    connectionMonitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                connectionMonitor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        cerrarConexionSilenciosamente();
    }

    // Métodos auxiliares para conversión de fechas
    private Timestamp localDateTimeToTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null)
            return null;
        return Timestamp.valueOf(localDateTime);
    }

    private Timestamp instantToTimestamp(Instant instant) {
        if (instant == null)
            return null;
        return Timestamp.from(instant);
    }

    // Métodos CRUD con correcciones aplicadas
    public boolean crearArchivo(ArchivoBD archivo) {
        try (Connection conn = obtenerConexion();
                CallableStatement stmt = conn.prepareCall(
                        "{CALL sp_crear_archivo(?, ?, ?, ?, ?, ?)}",
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY)) {

            stmt.setString(1, archivo.getNombre());
            stmt.setLong(2, archivo.getTamanoBytes());
            stmt.setTimestamp(3,
                    archivo.getFechaCreacion() != null ? Timestamp.valueOf(archivo.getFechaCreacion()) : null);
            stmt.setString(4, archivo.getRutaCompleta());
            stmt.setString(5, archivo.getExtension());
            stmt.setString(6, archivo.getCategoria());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BD] Error al crear archivo: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarTamanoFechaModificacionArchivo(ArchivoBD archivo) {
        try (Connection conn = obtenerConexion();
                CallableStatement stmt = conn.prepareCall(
                        "{CALL sp_actualizar_tamano_fecha_modificacion_archivo (?, ?, ?, ?, ?)}",
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY)) {

            stmt.setString(1, archivo.getRutaCompleta());
            stmt.setString(2, archivo.getNombre());
            stmt.setString(3, archivo.getExtension());
            stmt.setLong(4, archivo.getTamanoBytes());
            stmt.setTimestamp(5,
                    archivo.getFechaCreacion() != null ? Timestamp.valueOf(archivo.getFechaCreacion()) : null);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BD] Error al actualizar tamaño/fecha: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarNombreArchivo(ArchivoBD archivo, String viejoNombre) {
        try (Connection conn = obtenerConexion();
                CallableStatement stmt = conn.prepareCall(
                        "{CALL sp_actualizar_nombre_archivo (?, ?, ?, ?)}",
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY)) {

            stmt.setString(1, archivo.getRutaCompleta());
            stmt.setString(2, viejoNombre);
            stmt.setString(3, archivo.getExtension());
            stmt.setString(4, archivo.getNombre());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BD] Error al actualizar nombre: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarCategoriaArchivo(ArchivoBD archivo) {
        try (Connection conn = obtenerConexion();
                CallableStatement stmt = conn.prepareCall(
                        "{CALL sp_actualizar_categoria_archivo (?, ?, ?, ?)}",
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY)) {

            stmt.setString(1, archivo.getRutaCompleta());
            stmt.setString(2, archivo.getNombre());
            stmt.setString(3, archivo.getExtension());
            stmt.setString(4, archivo.getCategoria());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BD] Error al actualizar categoría: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarArchivo(ArchivoBD archivo) {
        try (Connection conn = obtenerConexion();
                CallableStatement stmt = conn.prepareCall(
                        "{CALL sp_eliminar_archivo (?, ?, ?)}",
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY)) {

            stmt.setString(1, archivo.getRutaCompleta());
            stmt.setString(2, archivo.getNombre());
            stmt.setString(3, archivo.getExtension());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BD] Error al eliminar archivo: " + e.getMessage());
            return false;
        }
    }

    public ResultSet buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
            ArchivoBD archivo, long tamanoMinimo, long tamanoMaximo) {

        try (Connection conn = obtenerConexion()) {
            boolean esPrimerComando = true;
            StringBuilder consultaSqlDinamica = new StringBuilder("SELECT * FROM ");

            // Construcción dinámica usando getters
            if (archivo.getExtension() != null) {
                consultaSqlDinamica.append("sp_buscar_archivos_segun_extension (?) ");
                esPrimerComando = false;
            }
            if (archivo.getRutaCompleta() != null) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_ubicacion (?) ");
                esPrimerComando = false;
            }
            if (archivo.getCategoria() != null) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_categoria (?) ");
                esPrimerComando = false;
            }
            if (archivo.getEtiquetas() != null && !archivo.getEtiquetas().isEmpty()) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_etiqueta (?) ");
                esPrimerComando = false;
            }
            if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_tamano (?, ?) ");
                esPrimerComando = false;
            }
            if (archivo.getNombre() != null) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_nombre (?) ");
                esPrimerComando = false;
            }
            if (archivo.getPalabrasClave() != null && !archivo.getPalabrasClave().isEmpty()) {
                for (String palabra : archivo.getPalabrasClave()) {
                    consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                            .append("sp_buscar_archivos_con_una_palabra_clave_dada (?) ");
                    esPrimerComando = false;
                }
            }

            try (PreparedStatement stmt = conn.prepareCall(
                    consultaSqlDinamica.toString(),
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY)) {

                int indiceParametro = 1;
                if (archivo.getExtension() != null) {
                    stmt.setString(indiceParametro++, archivo.getExtension());
                }
                if (archivo.getRutaCompleta() != null) {
                    stmt.setString(indiceParametro++, archivo.getRutaCompleta());
                }
                if (archivo.getCategoria() != null) {
                    stmt.setString(indiceParametro++, archivo.getCategoria());
                }
                if (archivo.getEtiquetas() != null && !archivo.getEtiquetas().isEmpty()) {
                    stmt.setString(indiceParametro++, archivo.getEtiquetas().get(0));
                }
                if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                    stmt.setLong(indiceParametro++, tamanoMinimo);
                    stmt.setLong(indiceParametro++, tamanoMaximo);
                }
                if (archivo.getNombre() != null) {
                    stmt.setString(indiceParametro++, archivo.getNombre());
                }
                if (archivo.getPalabrasClave() != null && !archivo.getPalabrasClave().isEmpty()) {
                    for (String palabra : archivo.getPalabrasClave()) {
                        stmt.setString(indiceParametro++, palabra);
                    }
                }

                return stmt.executeQuery();
            }
        } catch (SQLException e) {
            System.err.println("[BD] Error en búsqueda por varias palabras clave: " + e.getMessage());
            return null;
        }
    }

    public ResultSet buscarArchivosPorFiltroMinimoUnaPalabraClave(
            ArchivoBD archivoFiltro, long tamanoMinimo, long tamanoMaximo) {

        try (Connection conn = obtenerConexion()) {
            boolean esPrimerComando = true;
            StringBuilder consultaSqlDinamica = new StringBuilder("SELECT * FROM ");

            // Construcción dinámica de la consulta
            if (archivoFiltro.getExtension() != null) {
                consultaSqlDinamica.append("sp_buscar_archivos_segun_extension (?) ");
                esPrimerComando = false;
            }
            if (archivoFiltro.getRutaCompleta() != null) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_ubicacion (?) ");
                esPrimerComando = false;
            }
            if (archivoFiltro.getCategoria() != null) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_categoria (?) ");
                esPrimerComando = false;
            }
            if (archivoFiltro.getEtiquetas() != null && !archivoFiltro.getEtiquetas().isEmpty()) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_etiqueta (?) ");
                esPrimerComando = false;
            }
            if (archivoFiltro.getPalabrasClave() != null && !archivoFiltro.getPalabrasClave().isEmpty()) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias (?) ");
                esPrimerComando = false;
            }
            if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_tamano (?, ?) ");
                esPrimerComando = false;
            }
            if (archivoFiltro.getNombre() != null) {
                consultaSqlDinamica.append(esPrimerComando ? "" : "INTERSECT SELECT * FROM ")
                        .append("sp_buscar_archivos_segun_nombre (?) ");
                esPrimerComando = false;
            }

            try (CallableStatement stmt = conn.prepareCall(
                    consultaSqlDinamica.toString(),
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY)) {

                int indiceParametro = 1;
                if (archivoFiltro.getExtension() != null) {
                    stmt.setString(indiceParametro++, archivoFiltro.getExtension());
                }
                if (archivoFiltro.getRutaCompleta() != null) {
                    stmt.setString(indiceParametro++, archivoFiltro.getRutaCompleta());
                }
                if (archivoFiltro.getCategoria() != null) {
                    stmt.setString(indiceParametro++, archivoFiltro.getCategoria());
                }
                if (archivoFiltro.getEtiquetas() != null && !archivoFiltro.getEtiquetas().isEmpty()) {
                    stmt.setString(indiceParametro++, archivoFiltro.getEtiquetas().get(0));
                }
                if (archivoFiltro.getPalabrasClave() != null && !archivoFiltro.getPalabrasClave().isEmpty()) {
                    Array palabrasClaveArray = conn.createArrayOf(
                            "varchar",
                            archivoFiltro.getPalabrasClave().toArray());
                    stmt.setArray(indiceParametro++, palabrasClaveArray);
                }
                if ((tamanoMinimo >= 0) && (tamanoMaximo >= 0)) {
                    stmt.setLong(indiceParametro++, tamanoMinimo);
                    stmt.setLong(indiceParametro++, tamanoMaximo);
                }
                if (archivoFiltro.getNombre() != null) {
                    stmt.setString(indiceParametro++, archivoFiltro.getNombre());
                }

                return stmt.executeQuery();
            }
        } catch (SQLException e) {
            System.err.println("[BD] Error en búsqueda por mínimo una palabra clave: " + e.getMessage());
            return null;
        }
    }
}