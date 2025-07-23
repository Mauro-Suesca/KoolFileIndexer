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
import java.util.Set;
import java.util.concurrent.Future;

public class MainIndexadorCLI {
    private static final int DEFAULT_BATCH = 100;
    private static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(1);
    private static ConectorBasedeDatos connector;
    private static SocketServer socketServer;
    private static Future<?> indexadorFuture;

    public static void main(String[] args) {
        Indexador indexador = null;
        try {
            // 0) Inicializar y verificar conexión BD
            connector = ConectorBasedeDatos.obtenerInstancia();
            verificarConexionBD();

            // 1) Selección de raíces según el modo
            List<Path> rootsToScan = seleccionarRaices(args);

            // 2) Crear el indexador (ahora sin iniciar)
            indexador = Indexador.getInstance(
                    Paths.get("src", "main", "resources", "indexador", "exclusiones.txt").toString(),
                    rootsToScan, DEFAULT_BATCH, DEFAULT_INTERVAL);
            mostrarConfiguracion(indexador);

            // 3) Iniciar el servidor de sockets
            socketServer = SocketServer.createServer(10);

            // 4) Registrar las acciones en el servidor
            registrarAccionesAPI(socketServer, indexador);

            // 5) Enviar el indexador como tarea al servidor
            indexadorFuture = socketServer.submit(indexador);

            // 6) Iniciar bucle de aceptación de conexiones
            System.out.println("Servidor socket iniciado. Esperando conexiones...");

            // Bucle principal
            while (true) {
                socketServer.accept();
            }

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Asegurar limpieza de recursos
            if (indexador != null) {
                indexador.detener();
            }

            // Este es el código que necesitas para cerrar correctamente el socket server
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

                String tagName = lines[0].split(": ")[1];
                String filePath = lines[1].split(": ")[1];

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
            // CAMBIAR ESTO:
            // koolfileindexer.db.Archivo filtro = new koolfileindexer.db.Archivo();

            // POR ESTO:
            ArchivoAdapter filtro = new ArchivoAdapter();

            // Si hay palabras clave, intentar usarlas primero como nombre
            if (keywords != null && keywords.length > 0) {
                // Usar la primera palabra clave como nombre para búsqueda
                filtro.setNombre(keywords[0]);

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
            ResultSet rs;
            if (keywords != null && keywords.length > 1) {
                // Con múltiples palabras clave, usar búsqueda flexible (al menos una
                // coincidencia)
                rs = connector.buscarArchivosPorFiltroMinimoUnaPalabraClave(filtro, -1, -1);
            } else {
                // Con una sola palabra o sin palabras, usar búsqueda exacta
                rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);
            }

            if (rs != null) {
                while (rs.next()) {
                    Archivo archivo = ArchivoConverter.fromResultSet(rs);
                    resultados.add(archivo);
                }
                rs.close();
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
            // Crear un filtro para buscar el archivo por ruta
            ArchivoAdapter filtro = new ArchivoAdapter();
            filtro.setRutaCompleta(filePath);

            // Buscar el archivo
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

            if (rs != null && rs.next()) {
                // Crear un archivo para modificar con los datos obtenidos
                ArchivoAdapter archivo = new ArchivoAdapter(
                        rs.getString("arc_nombre"),
                        rs.getLong("arc_tamano_bytes"),
                        rs.getTimestamp("arc_fecha_modificacion").toLocalDateTime(),
                        rs.getString("arc_ruta_completa"),
                        rs.getString("ext_extension"),
                        rs.getString("cat_nombre"));
                rs.close();

                // Asociar la etiqueta al archivo
                connector.asociarEtiquetaArchivo(archivo, tagName);
                return true;
            }

            if (rs != null) {
                rs.close();
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

            // Buscar el archivo por ruta completa
            koolfileindexer.db.Archivo filtro = new koolfileindexer.db.Archivo();
            filtro.setRutaCompleta(filePath);
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

            if (rs != null) {
                try (rs) {
                    if (rs.next()) {
                        // Crear objeto archivo con los datos necesarios para asociar
                        koolfileindexer.db.Archivo archivo = new koolfileindexer.db.Archivo();
                        archivo.setRutaCompleta(filePath);
                        archivo.setNombre(rs.getString("arc_nombre"));
                        archivo.setExtension(rs.getString("ext_extension"));

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
            // Buscar el archivo por ruta completa
            koolfileindexer.db.Archivo filtro = new koolfileindexer.db.Archivo();
            filtro.setRutaCompleta(filePath);
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

            if (rs != null) {
                try (rs) {
                    if (rs.next()) {
                        // Crear objeto archivo con los datos necesarios
                        koolfileindexer.db.Archivo archivo = new koolfileindexer.db.Archivo();
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
        String modo = args.length > 0 ? args[0].toLowerCase() : "demo";
        List<Path> rootsToScan;

        switch (modo) {
            case "docs" -> {
                System.out.println("\n[Docs] Índice de tu carpeta DOCUMENTS:");
                rootsToScan = List.of(Paths.get(System.getProperty("user.home"), "Documents"));
            }
            case "full" -> {
                System.out.println("\n[Full] Índice de todas las unidades + HOME:");
                rootsToScan = new ArrayList<>();
                for (java.io.File disco : java.io.File.listRoots()) {
                    Path p = disco.toPath();
                    if (p.toFile().isDirectory()) {
                        rootsToScan.add(p);
                    }
                }
                rootsToScan.add(Paths.get(System.getProperty("user.home")));
            }
            default -> {
                System.out.println("\n[Demo] Índice de tu carpeta HOME:");
                rootsToScan = List.of(Paths.get(System.getProperty("user.home")));
            }
        }
        return rootsToScan;
    }
}