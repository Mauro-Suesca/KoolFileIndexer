package koolfileindexer.logica;

import koolfileindexer.db.ConectorBasedeDatos;
import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.Etiqueta;
// Importaciones para la API de sockets
import koolfileindexer.common.model.GenericList;
import koolfileindexer.common.model.Search;
import koolfileindexer.common.model.Tag;
// Usar nombre completo o renombrar para evitar conflicto
import koolfileindexer.common.model.File;
import koolfileindexer.common.model.ErrorMessage;
import koolfileindexer.common.protocol.Request;
import koolfileindexer.common.protocol.Response;
import koolfileindexer.common.protocol.v1.SocketServer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

public class MainIndexadorCLI {
    private static final int DEFAULT_BATCH = 100;
    private static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(1);
    private static ConectorBasedeDatos connector;
    private static SocketServer socketServer;
    private static Future<?> indexadorFuture;
    private static volatile boolean running = true; // Añadir variable de control

    public static void main(String[] args) {
        try {
            inicializarBaseDeDatos();
            Indexador indexador = configurarIndexador(args);
            iniciarServidorSocket(indexador);
            ejecutarBucleAceptacion();
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            limpiarRecursos();
        }
    }

    private static void inicializarBaseDeDatos() throws SQLException {
        connector = ConectorBasedeDatos.obtenerInstancia();
        verificarConexionBD();
    }

    private static Indexador configurarIndexador(String[] args) {
        List<Path> rootsToScan = seleccionarRaices(args);
        Indexador indexador = Indexador.getInstance(
                Paths.get("src", "main", "resources", "indexador", "exclusiones.txt").toString(),
                rootsToScan, DEFAULT_BATCH, DEFAULT_INTERVAL);
        mostrarConfiguracion(indexador);
        return indexador;
    }

    private static void iniciarServidorSocket(Indexador indexador) throws IOException {
        socketServer = SocketServer.createServer(10);
        registrarAccionesAPI(socketServer, indexador);
        indexadorFuture = socketServer.submit(indexador);
        System.out.println("Servidor socket iniciado. Esperando conexiones...");
    }

    private static void ejecutarBucleAceptacion() {
        while (running) {
            try {
                socketServer.accept();
            } catch (Exception e) {
                System.err.println("Error en bucle de aceptación: " + e.getMessage());
                if (Thread.currentThread().isInterrupted()) {
                    running = false;
                }
            }
        }
    }

    private static void limpiarRecursos() {
        if (indexadorFuture != null) {
            indexadorFuture.cancel(true);
        }

        if (socketServer != null) {
            try {
                System.out.println("Cerrando servidor socket...");
                socketServer.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el servidor socket: " + e.getMessage());
            }
        }

        if (connector != null) {
            connector.terminarConexion();
        }
    }

    private static void registrarAccionesAPI(SocketServer server, Indexador indexador) {
        // Registrar acción para buscar archivos
        server.registerAction("search", (Request req) -> {
            try {
                Search search = req.build(Search.stringFactory());
                String[] keywords = search.getKeywords();
                String[] tagNames = search.getTags();
                String[] filters = search.getFilters(); // Obtener los filtros

                // Procesar filtros para búsqueda por nombre
                if (filters != null && filters.length > 0) {
                    for (String filter : filters) {
                        if (filter.startsWith("name:") || filter.startsWith("nombre:")) {
                            // Reemplazar las keywords con el nombre a buscar
                            String nombreBusqueda = filter.substring(filter.indexOf(":") + 1).trim();
                            keywords = new String[] { nombreBusqueda };
                            System.out.println("Búsqueda por nombre: " + nombreBusqueda);
                            break;
                        }
                    }
                }

                System.out.println("Búsqueda recibida - Keywords: " +
                        String.join(", ", keywords) + " - Tags: " +
                        (tagNames != null && tagNames.length > 0 ? String.join(", ", tagNames) : "ninguna") +
                        " - Filtros: "
                        + (filters != null && filters.length > 0 ? String.join(", ", filters) : "ninguno"));

                // El método buscarArchivos ya está implementado para usar la primera keyword
                // como nombre
                List<Archivo> resultados = buscarArchivos(keywords, tagNames);
                GenericList<File> listaArchivos = convertirArchivos(resultados);

                return Response.ok(listaArchivos);
            } catch (Exception e) {
                e.printStackTrace();
                return Response.err(new ErrorMessage("Error en búsqueda: " + e.getMessage()));
            }
        });

        // Registrar acción para agregar etiqueta
        server.registerAction("addTag", (Request req) -> {
            try {
                // Ya que Tag no tiene filePath, extraemos del mensaje completo
                String rawData = req.getRawData();
                String[] lines = rawData.split("\r\n");

                if (lines.length < 2) {
                    return Response.err(new ErrorMessage("Formato incorrecto: se esperan tag y filePath"));
                }

                String[] tagParts = lines[0].split(":", 2);
                String[] pathParts = lines[1].split(":", 2);

                if (tagParts.length != 2 || pathParts.length != 2) {
                    return Response.err(new ErrorMessage("Formato incorrecto: formato esperado 'clave: valor'"));
                }

                String tagName = tagParts[1].trim();
                String filePath = pathParts[1].trim();

                if (tagName.isEmpty() || filePath.isEmpty()) {
                    return Response.err(new ErrorMessage("Tag y filePath no pueden estar vacíos"));
                }

                boolean success = agregarEtiqueta(filePath, tagName);
                if (success) {
                    return Response.ok("Etiqueta agregada correctamente");
                } else {
                    return Response.err(new ErrorMessage("No se pudo agregar la etiqueta"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.err(new ErrorMessage("Error al agregar etiqueta: " + e.getMessage()));
            }
        });

        // Registrar acción para agregar palabra clave
        server.registerAction("addKeyword", (Request req) -> {
            try {
                String rawData = req.getRawData();
                String[] lines = rawData.split("\r\n");

                if (lines.length < 2) {
                    return Response.err(new ErrorMessage("Formato incorrecto: se esperan keyword y filePath"));
                }

                String keyword = lines[0].split(": ")[1];
                String filePath = lines[1].split(": ")[1];

                boolean success = agregarPalabraClave(filePath, keyword);
                if (success) {
                    return Response.ok("Palabra clave agregada correctamente");
                } else {
                    return Response.err(new ErrorMessage("No se pudo agregar la palabra clave"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.err(new ErrorMessage("Error al agregar palabra clave: " + e.getMessage()));
            }
        });

        // Registrar acción para eliminar palabra clave
        server.registerAction("removeKeyword", (Request req) -> {
            try {
                String rawData = req.getRawData();
                String[] lines = rawData.split("\r\n");

                if (lines.length < 2) {
                    return Response.err(new ErrorMessage("Formato incorrecto: se esperan keyword y filePath"));
                }

                String keyword = lines[0].split(": ")[1];
                String filePath = lines[1].split(": ")[1];

                boolean success = eliminarPalabraClave(filePath, keyword);
                if (success) {
                    return Response.ok("Palabra clave eliminada correctamente");
                } else {
                    return Response.err(new ErrorMessage("No se pudo eliminar la palabra clave"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.err(new ErrorMessage("Error al eliminar palabra clave: " + e.getMessage()));
            }
        });

        // Registrar acción para recargar exclusiones
        server.registerAction("reloadExclusions", (Request req) -> {
            try {
                String rawData = req.getRawData();
                String[] lines = rawData.split("\r\n");

                // Si se proporciona una ruta específica
                String filePath = null;
                if (lines.length > 0 && lines[0].startsWith("path: ")) {
                    filePath = lines[0].split(": ")[1];
                }

                // Si no se proporciona ruta, usar la ubicación predeterminada en $HOME
                if (filePath == null || filePath.isEmpty()) {
                    String userHome = System.getProperty("user.home");
                    filePath = Paths.get(userHome, ".config", "koolfileindexer", "exclusiones.txt").toString();
                }

                // Validar que el archivo existe y es accesible
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    return Response.err(new ErrorMessage("El archivo de exclusiones no existe: " + filePath));
                }
                if (!Files.isReadable(path)) {
                    return Response.err(new ErrorMessage("El archivo de exclusiones no es legible: " + filePath));
                }

                // Cargar las exclusiones desde el archivo
                indexador.cargarExclusiones(filePath);

                return Response.ok("Exclusiones recargadas correctamente desde: " + filePath);
            } catch (Exception e) {
                e.printStackTrace();
                return Response.err(new ErrorMessage("Error al recargar exclusiones: " + e.getMessage()));
            }
        });

        // Registrar acción para listar exclusiones actuales
        server.registerAction("getExclusions", (Request req) -> {
            try {
                Set<Path> exclusiones = indexador.getRutasExcluidas();

                StringBuilder sb = new StringBuilder();
                sb.append("Exclusiones actuales:\n");

                int i = 1;
                for (Path exclusion : exclusiones) {
                    sb.append(i++).append(". ").append(exclusion).append("\n");
                }

                return Response.ok(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
                return Response.err(new ErrorMessage("Error al obtener exclusiones: " + e.getMessage()));
            }
        });
    }

    // Métodos auxiliares para la API

    // Modificar el método buscarArchivos para usar el método más adecuado según el
    // caso
    private static List<Archivo> buscarArchivos(String[] keywords, String[] tagNames) {
        List<Archivo> resultados = new ArrayList<>();
        try {
            ArchivoAdapter filtro = new ArchivoAdapter();

            // Si hay palabras clave, intentar usarlas primero como nombre
            if (keywords != null && keywords.length > 0 && keywords[0] != null && !keywords[0].trim().isEmpty()) {
                // Solo establecer el nombre si no está vacío
                filtro.setNombre(keywords[0].trim());

                ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

                // Si encontramos resultados por nombre, los devolvemos
                if (rs != null) {
                    boolean hayResultados = false;
                    try (rs) {
                        while (rs.next()) {
                            Archivo archivo = ArchivoConverter.fromResultSet(rs);
                            resultados.add(archivo);
                            hayResultados = true;
                        }
                    }

                    // Si encontramos resultados por nombre, no necesitamos buscar por palabras
                    // clave
                    if (hayResultados) {
                        return resultados;
                    }
                }

                // Si no encontramos por nombre, intentamos con palabras clave
                filtro = new ArchivoAdapter(); // Reiniciar el filtro con nuestro adaptador seguro
                filtro.setNombre(null);
                Set<String> palabrasClave = new HashSet<>(Arrays.asList(keywords));
                filtro.setPalabrasClave(palabrasClave);
            }

            // Si hay etiquetas, crear una lista de etiquetas para el filtro
            if (tagNames != null && tagNames.length > 0) {
                List<koolfileindexer.db.Etiqueta> etiquetas = new ArrayList<>();
                for (String tagName : tagNames) {
                    etiquetas.add(new koolfileindexer.db.Etiqueta(tagName));
                }
                // No hay método setEtiquetas pero asumimos que se maneja por separado
            }

            // Elegir el método más apropiado según la cantidad de keywords
            try {
                // Declaración e inicialización en una sola línea usando operador ternario
                ResultSet rs = (keywords != null && keywords.length > 1)
                        ? connector.buscarArchivosPorFiltroMinimoUnaPalabraClave(filtro, -1, -1)
                        : connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

                if (rs != null) {
                    try (rs) { // Ahora rs es efectivamente final
                        while (rs.next()) {
                            Archivo archivo = ArchivoConverter.fromResultSet(rs);
                            resultados.add(archivo);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al buscar archivos: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error al buscar archivos: " + e.getMessage());
            e.printStackTrace();
        }

        return resultados;
    }

    private static GenericList<koolfileindexer.common.model.File> convertirArchivos(List<Archivo> archivos) {
        GenericList<koolfileindexer.common.model.File> resultado = new GenericList<>();
        for (Archivo archivo : archivos) {
            // Convertir etiquetas a array
            String[] etiquetas = archivo.getEtiquetas().stream()
                    .map(Etiqueta::getNombre)
                    .toArray(String[]::new);

            koolfileindexer.common.model.File fileUI = new koolfileindexer.common.model.File(
                    archivo.getNombre(),
                    archivo.getExtension(),
                    archivo.getRutaCompleta(),
                    archivo.getFechaModificacion().toString(),
                    (int) archivo.getTamanoBytes(),
                    etiquetas);
            resultado.add(fileUI);
        }
        return resultado;
    }

    // Implementar el método agregarEtiqueta
    private static boolean agregarEtiqueta(String filePath, String tagName) {
        try {
            // Verificar que la ruta no es nula ni vacía
            if (filePath == null || filePath.trim().isEmpty()) {
                System.err.println("La ruta del archivo no puede estar vacía");
                return false;
            }

            // Crear un filtro para buscar el archivo por ruta
            ArchivoAdapter filtro = new ArchivoAdapter();
            filtro.setRutaCompleta(filePath.trim());

            // Buscar el archivo
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

            if (rs != null) {
                try (rs) {
                    if (rs.next()) {
                        // Crear un archivo para modificar con los datos obtenidos - usar
                        // getStringWithAlternatives
                        ArchivoAdapter archivo = new ArchivoAdapter(
                                ArchivoConverter.getStringWithAlternatives(rs, new String[] { "nombre", "arc_nombre" }),
                                ArchivoConverter.getLongWithAlternatives(rs, new String[] { "tamano", "arc_tamano" }),
                                ArchivoConverter.getTimestampWithAlternatives(rs,
                                        new String[] { "fecha_modificacion", "arc_fecha_modificacion" }),
                                ArchivoConverter.getStringWithAlternatives(rs, new String[] { "path", "arc_path" }),
                                ArchivoConverter.getStringWithAlternatives(rs,
                                        new String[] { "extension", "ext_extension" }),
                                ArchivoConverter.getStringWithAlternatives(rs,
                                        new String[] { "categoria", "cat_nombre" }));

                        // Asociar la etiqueta al archivo
                        connector.asociarEtiquetaArchivo(archivo, tagName);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al agregar etiqueta: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // Implementar el método agregarPalabraClave
    private static boolean agregarPalabraClave(String filePath, String keyword) {
        try {
            // Validar palabra clave
            if (keyword == null || keyword.trim().isEmpty()) {
                System.err.println("La palabra clave no puede estar vacía");
                return false;
            }

            // Validar ruta del archivo
            if (filePath == null || filePath.trim().isEmpty()) {
                System.err.println("La ruta del archivo no puede estar vacía");
                return false;
            }

            ArchivoAdapter filtro = new ArchivoAdapter();
            filtro.setRutaCompleta(filePath.trim());

            // Buscar el archivo por ruta completa
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

            if (rs != null) {
                try (rs) {
                    if (rs.next()) {
                        // Crear objeto archivo con los datos necesarios para asociar
                        ArchivoAdapter archivo = new ArchivoAdapter();
                        archivo.setRutaCompleta(filePath);
                        archivo.setNombre(ArchivoConverter.getStringWithAlternatives(rs,
                                new String[] { "nombre", "arc_nombre" }));
                        archivo.setExtension(ArchivoConverter.getStringWithAlternatives(rs,
                                new String[] { "extension", "ext_extension" }));

                        // Asociar la palabra clave
                        connector.asociarPalabraClaveArchivo(archivo, keyword.toLowerCase());
                        return true;
                    } else {
                        System.err.println("No se encontró el archivo: " + filePath);
                        return false;
                    }
                }
            } else {
                System.err.println("Error al buscar el archivo");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error al agregar palabra clave: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Implementar el método eliminarPalabraClave
    private static boolean eliminarPalabraClave(String filePath, String keyword) {
        try {
            // Validar parámetros
            if (keyword == null || keyword.trim().isEmpty()) {
                System.err.println("La palabra clave no puede estar vacía");
                return false;
            }

            if (filePath == null || filePath.trim().isEmpty()) {
                System.err.println("La ruta del archivo no puede estar vacía");
                return false;
            }

            // CAMBIAR ESTO:
            // koolfileindexer.db.Archivo filtro = new koolfileindexer.db.Archivo();
            // filtro.setRutaCompleta(filePath);

            // POR ESTO:
            ArchivoAdapter filtro = new ArchivoAdapter();
            filtro.setRutaCompleta(filePath.trim());

            // Y MÁS ABAJO, CAMBIAR TAMBIÉN:
            // koolfileindexer.db.Archivo archivo = new koolfileindexer.db.Archivo();

            // POR ESTO:
            ArchivoAdapter archivo = new ArchivoAdapter();

            // Buscar el archivo por ruta completa
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

            if (rs != null) {
                try (rs) {
                    if (rs.next()) {
                        // Crear objeto archivo con los datos necesarios
                        archivo.setRutaCompleta(filePath);
                        archivo.setNombre(rs.getString("arc_nombre"));
                        archivo.setExtension(rs.getString("ext_extension"));

                        // Desasociar la palabra clave
                        connector.desasociarPalabraClaveArchivo(archivo, keyword.toLowerCase());
                        System.out.println("[PALABRA CLAVE ELIMINADA] " + keyword + " de " + filePath);
                        return true;
                    } else {
                        System.err.println("Archivo no encontrado: " + filePath);
                        return false;
                    }
                }
            } else {
                System.err.println("Error al buscar el archivo");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error al eliminar palabra clave: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Mantener métodos existentes de verificación y configuración
    private static void verificarConexionBD() throws SQLException {
        try (Connection conn = connector.obtenerConexion()) {
            if (conn == null || conn.isClosed()) {
                throw new SQLException("No se pudo establecer la conexión a la base de datos");
            }
            System.out.println("✓ Conexión a base de datos establecida correctamente");
        }
    }

    private static void mostrarConfiguracion(Indexador idx) {
        System.out.println("\n=== Configuración del Indexador ===");
        System.out.println(">>> Rutas/patrones excluidos:");
        idx.getRutasExcluidas().forEach(r -> System.out.println("  - " + r));
    }

    private static List<Path> seleccionarRaices(String[] args) {
        // Siempre usar modo FULL independientemente de los argumentos
        System.out.println("\n[Full] Índice de todas las unidades + HOME:");
        List<Path> rootsToScan = new ArrayList<>();

        // Añadir todos los discos/raíces
        for (java.io.File disco : java.io.File.listRoots()) {
            Path p = disco.toPath();
            if (p.toFile().isDirectory()) {
                rootsToScan.add(p);
            }
        }

        // Añadir HOME
        rootsToScan.add(Paths.get(System.getProperty("user.home")));

        return rootsToScan;
    }

    private static Optional<ArchivoAdapter> buscarArchivoPorRuta(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                System.err.println("La ruta del archivo no puede estar vacía");
                return Optional.empty();
            }

            ArchivoAdapter filtro = new ArchivoAdapter();
            filtro.setRutaCompleta(filePath.trim());

            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);
            if (rs != null) {
                try (rs) {
                    if (rs.next()) {
                        // Usar nombres de columna correctos para las funciones SQL
                        ArchivoAdapter archivo = new ArchivoAdapter(
                                ArchivoConverter.getStringWithAlternatives(rs, new String[] { "nombre", "arc_nombre" }),
                                ArchivoConverter.getLongWithAlternatives(rs, new String[] { "tamano", "arc_tamano" }),
                                ArchivoConverter.getTimestampWithAlternatives(rs,
                                        new String[] { "fecha_modificacion", "arc_fecha_modificacion" }),
                                ArchivoConverter.getStringWithAlternatives(rs, new String[] { "path", "arc_path" }),
                                ArchivoConverter.getStringWithAlternatives(rs,
                                        new String[] { "extension", "ext_extension" }),
                                ArchivoConverter.getStringWithAlternatives(rs,
                                        new String[] { "categoria", "cat_nombre" }));
                        return Optional.of(archivo);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al buscar archivo: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private static ResultSet obtenerResultSetSegunKeywords(ArchivoAdapter filtro, String[] keywords)
            throws SQLException {
        return (keywords != null && keywords.length > 1)
                ? connector.buscarArchivosPorFiltroMinimoUnaPalabraClave(filtro, -1, -1)
                : connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);
    }
}