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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
            indexador = Indexador.getInstance("exclusiones.txt", rootsToScan, DEFAULT_BATCH, DEFAULT_INTERVAL);
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

                System.out.println("Búsqueda recibida - Keywords: " +
                        String.join(", ", keywords) + " - Tags: " +
                        (tagNames != null && tagNames.length > 0 ? String.join(", ", tagNames) : "ninguna"));

                // Realizar búsqueda usando el conector de BD
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
    }

    // Métodos auxiliares para la API

    // Implementar el método buscarArchivos
    private static List<Archivo> buscarArchivos(String[] keywords, String[] tagNames) {
        List<Archivo> resultados = new ArrayList<>();
        try {
            // Construir consulta SQL basada en keywords y tags
            StringBuilder where = new StringBuilder();
            List<Object> params = new ArrayList<>();

            if (keywords != null && keywords.length > 0) {
                where.append(" AND (");
                for (int i = 0; i < keywords.length; i++) {
                    if (i > 0)
                        where.append(" OR ");
                    where.append("LOWER(a.arc_nombre) LIKE LOWER(?)");
                    params.add("%" + keywords[i] + "%");
                }
                where.append(")");
            }

            // Si hay etiquetas, agregar condición
            if (tagNames != null && tagNames.length > 0) {
                where.append(
                        " AND a.id IN (SELECT arc_id FROM Archivo_Etiqueta ae JOIN Etiqueta e ON ae.eti_id = e.id WHERE ");
                for (int i = 0; i < tagNames.length; i++) {
                    if (i > 0)
                        where.append(" OR ");
                    where.append("LOWER(e.eti_nombre) = LOWER(?)");
                    params.add(tagNames[i]);
                }
                where.append(")");
            }

            // Ejecutar consulta utilizando algún método existente de connector
            // Modificar esto para usar métodos disponibles en tu ConectorBasedeDatos
            ResultSet rs = connector.ejecutarConsultaPersonalizada(
                    "SELECT * FROM Archivo a WHERE 1=1" + where.toString(),
                    params.toArray());

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
            // Buscar el archivo por ruta
            ResultSet rs = connector.ejecutarConsultaPersonalizada(
                    "SELECT id FROM Archivo WHERE arc_ruta_completa = ?",
                    new Object[] { filePath });

            if (rs != null && rs.next()) {
                long archivoId = rs.getLong("id");
                rs.close();

                // Buscar o crear la etiqueta
                ResultSet rsTag = connector.ejecutarConsultaPersonalizada(
                        "SELECT id FROM Etiqueta WHERE eti_nombre = ?",
                        new Object[] { tagName });

                long etiquetaId;
                if (rsTag != null && rsTag.next()) {
                    etiquetaId = rsTag.getLong("id");
                    rsTag.close();
                } else {
                    // Crear etiqueta si no existe
                    ResultSet rsInsert = connector.ejecutarConsultaPersonalizada(
                            "INSERT INTO Etiqueta(eti_nombre) VALUES (?) RETURNING id",
                            new Object[] { tagName });
                    if (rsInsert != null && rsInsert.next()) {
                        etiquetaId = rsInsert.getLong("id");
                        rsInsert.close();
                    } else {
                        return false;
                    }
                }

                // Asociar etiqueta a archivo si no existe la relación
                connector.ejecutarConsultaPersonalizada(
                        "INSERT INTO Archivo_Etiqueta(arc_id, eti_id) " +
                                "SELECT ?, ? WHERE NOT EXISTS (" +
                                "SELECT 1 FROM Archivo_Etiqueta WHERE arc_id = ? AND eti_id = ?)",
                        new Object[] { archivoId, etiquetaId, archivoId, etiquetaId });

                return true;
            }
        } catch (Exception e) {
            System.err.println("Error al agregar etiqueta: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
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