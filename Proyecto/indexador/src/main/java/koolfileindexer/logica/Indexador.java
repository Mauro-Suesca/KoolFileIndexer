package koolfileindexer.logica;

import koolfileindexer.db.ConectorBasedeDatos;
import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.Categoria;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration; // Añadir esta importación
import java.io.IOException;

/**
 * Indexador recorre carpetas por lotes, aplica exclusiones
 * y delega persistencia en BD vía ConectorBasedeDatos.
 */
public class Indexador implements Runnable {
    private static final AtomicReference<Indexador> INSTANCIA = new AtomicReference<>();
    private final ConectorBasedeDatos connector;

    // ─── Exclusiones ──────────────────────────────────────────────
    private final Set<Path> rutasExcluidas = new HashSet<>();
    private static final Set<String> EXT_PROHIBIDAS = Set.of("exe", "dll");
    private static final Set<String> PATRONES_PROTEGIDOS = Set.of("windows");

    // ─── Scheduler y batching ─────────────────────────────────────
    private ScheduledExecutorService scheduler;
    private Duration intervalo = Duration.ofMinutes(5);
    private int batchSize = 100;

    // Nuevos campos para controlar el ciclo de vida
    private final List<Path> raicesAIndexar;
    private final int tamanoLote;
    private final Duration intervaloEjecucion;
    private volatile boolean ejecutando = true;

    // Constructor existente modificado para recibir parámetros de ejecución
    private Indexador(String archivoExclusiones, List<Path> raices, int tamanoLote, Duration intervalo) {
        this.connector = ConectorBasedeDatos.obtenerInstancia();
        this.raicesAIndexar = raices;
        this.tamanoLote = tamanoLote;
        this.intervaloEjecucion = intervalo;

        // Primero intentar con el archivo en $HOME/.config
        String userHome = System.getProperty("user.home");
        Path configPath = Paths.get(userHome, ".config", "koolfileindexer", "exclusiones.txt");

        if (Files.exists(configPath)) {
            cargarExclusiones(configPath.toString());
            System.out.println("[CONFIG] Cargadas exclusiones del usuario desde: " + configPath);
        } else {
            // Crear directorios y archivo si no existen
            try {
                Files.createDirectories(configPath.getParent());
                System.out.println("[CONFIG] Creado directorio de configuración en: " + configPath.getParent());

                // Si hay un archivo de exclusiones del proyecto, copiarlo al directorio del
                // usuario
                if (archivoExclusiones != null && !archivoExclusiones.isBlank()) {
                    Path proyectoExclusiones = Paths.get(archivoExclusiones);
                    if (Files.exists(proyectoExclusiones)) {
                        Files.copy(proyectoExclusiones, configPath);
                        System.out.println("[CONFIG] Copiado archivo de exclusiones del proyecto a: " + configPath);
                        cargarExclusiones(configPath.toString());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error creando configuración de usuario: " + e.getMessage());
            }
        }

        // Si no se cargaron exclusiones, intentar con el archivo pasado como parámetro
        if (rutasExcluidas.isEmpty() && archivoExclusiones != null && !archivoExclusiones.isBlank()) {
            cargarExclusiones(archivoExclusiones);
            System.out.println("[CONFIG] Cargadas exclusiones del proyecto desde: " + archivoExclusiones);
        }
    }

    // Método de fábrica modificado
    public static Indexador getInstance(String archivoExclusiones, List<Path> raices, int tamanoLote,
            Duration intervalo) {
        Indexador instance = INSTANCIA.get();
        if (instance == null) {
            instance = new Indexador(archivoExclusiones, raices, tamanoLote, intervalo);
            if (INSTANCIA.compareAndSet(null, instance)) {
                return instance;
            }
            return INSTANCIA.get();
        }
        return instance;
    }

    /**
     * Método de fábrica sobrecargado para mantener compatibilidad con tests
     */
    public static Indexador getInstance(String archivoExclusiones) {
        Path homePath = Paths.get(System.getProperty("user.home"));
        List<Path> raices = List.of(homePath);
        return getInstance(archivoExclusiones, raices, 100, Duration.ofMinutes(5));
    }

    /**
     * Arranca la indexación periódica.
     */
    public synchronized void iniciarIndexacionPeriodica(Path raiz, int batchSize, Duration intervalo) {
        // Validar parámetros
        Objects.requireNonNull(raiz, "La ruta base no puede ser null");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("El tamaño de lote debe ser positivo");
        }
        if (intervalo == null || intervalo.isNegative() || intervalo.isZero()) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }

        System.out.println("[SCHEDULER] Configurando indexación periódica:");
        System.out.println(" - Intervalo: cada " + intervalo.toMinutes() + " minutos");
        System.out.println(" - Tamaño de lote: " + batchSize + " archivos por ciclo");

        this.batchSize = batchSize;
        this.intervalo = intervalo;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Indexador-Scheduler");
            t.setDaemon(false);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n[SCHEDULER] Iniciando ciclo de indexación a las " + LocalDateTime.now());
            recorrerDirectorio(raiz, this.batchSize);
            System.out.println("[SCHEDULER] Ciclo completado a las " + LocalDateTime.now());
        }, 0, intervalo.toMinutes(), TimeUnit.MINUTES);
    }

    /**
     * Carga las exclusiones desde un archivo de texto.
     * Cada línea representa una ruta a excluir.
     */
    public void cargarExclusiones(String archivoExclusiones) {
        if (archivoExclusiones == null || archivoExclusiones.isBlank())
            return;
        Path path = Paths.get(archivoExclusiones);
        if (!Files.exists(path))
            return;

        try {
            // Limpiar exclusiones anteriores
            rutasExcluidas.clear();

            Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                    .map(Paths::get)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .forEach(rutasExcluidas::add);

            System.out.println("[CONFIG] Cargadas " + rutasExcluidas.size() + " exclusiones de " + path);
        } catch (IOException e) {
            System.err.println("Error leyendo exclusiones: " + e.getMessage());
        }

        System.out.println("[DEBUG] Exclusiones cargadas:");
        rutasExcluidas.forEach(p -> System.out.println("  - " + p));
    }

    public Set<Path> getRutasExcluidas() {
        return Collections.unmodifiableSet(rutasExcluidas);
    }

    // Verificar que este método siga conteniendo estas validaciones
    private boolean excluirArchivo(Path p) {
        Path norm = p.toAbsolutePath().normalize();

        // Exclusiones personalizadas del archivo
        for (Path excl : rutasExcluidas) {
            if (norm.startsWith(excl)) {
                return true;
            }
        }

        // Manejar el caso de rutas raíz donde getFileName() puede ser null
        Path fileName = norm.getFileName();
        if (fileName == null) {
            // Es una ruta raíz, usar solo la ruta completa para evaluación
            String rutaMin = norm.toString().toLowerCase();

            // Evaluar exclusiones basadas en la ruta
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                if (rutaMin.contains("\\windows\\") ||
                        rutaMin.contains("\\program files\\") ||
                        rutaMin.contains("\\archivos de programa\\")) {
                    return true;
                }
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                if (rutaMin.startsWith("/proc/") ||
                        rutaMin.startsWith("/sys/") ||
                        rutaMin.startsWith("/dev/")) {
                    return true;
                }
            }

            return false; // No excluir por defecto si es ruta raíz
        }

        // Procesar normalmente si no es una ruta raíz
        String nombre = fileName.toString().toLowerCase();
        String rutaMin = norm.toString().toLowerCase();

        // Archivos ocultos
        try {
            if (Files.isHidden(norm))
                return true;
        } catch (IOException ignored) {
        }

        // Archivos que empiezan con punto
        if (nombre.startsWith("."))
            return true;

        // Extensiones prohibidas
        int idx = nombre.lastIndexOf('.');
        if (idx >= 0 && EXT_PROHIBIDAS.contains(nombre.substring(idx + 1))) {
            return true;
        }

        // Rutas del sistema adaptadas para ser más específicas
        // En Windows
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            if (rutaMin.contains("\\windows\\") ||
                    rutaMin.contains("\\program files\\") ||
                    rutaMin.contains("\\archivos de programa\\")) {
                return true;
            }
        }
        // En Linux, excluir solo directorios específicos del sistema
        else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            if (rutaMin.startsWith("/proc/") ||
                    rutaMin.startsWith("/sys/") ||
                    rutaMin.startsWith("/dev/")) {
                return true;
            }
        }

        // Archivos específicos a excluir
        if (nombre.equals("thumbs.db")) {
            return true;
        }

        return false;
    }

    public void recorrerDirectorio(Path rutaBase, int batchSize) {
        System.out.println("[BATCH] Iniciando recorrido en: " + rutaBase);
        System.out.println("[BATCH] Máximo de archivos a procesar: " + batchSize);

        Path base;
        try {
            base = rutaBase.toAbsolutePath().normalize();
            if (!Files.isDirectory(base)) {
                System.err.println("No es un directorio válido: " + base);
                return;
            }
        } catch (Exception e) {
            System.err.println("Ruta inválida: " + rutaBase + " (" + e.getMessage() + ")");
            return;
        }

        AtomicInteger procesados = new AtomicInteger();

        try {
            Files.walkFileTree(base, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (excluirArchivo(dir) || procesados.get() >= batchSize) {
                        return procesados.get() >= batchSize
                                ? FileVisitResult.TERMINATE
                                : FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (procesados.get() >= batchSize) {
                        return FileVisitResult.TERMINATE;
                    }
                    try {
                        if (attrs.isRegularFile() && !excluirArchivo(file)) {
                            procesados.incrementAndGet();
                            procesarArchivo(file, attrs);
                        }
                    } catch (Exception e) {
                        // Capturar excepciones para evitar que falle todo el recorrido
                        System.err.println("Error al procesar " + file + ": " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.err.println("⚠️ No se pudo acceder a: " + file + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error recorriendo directorio: " + e.getMessage());
        }

        System.out.println("[BATCH] Procesados " + procesados.get() + " archivos en este ciclo");
    }

    private void procesarArchivo(Path p, BasicFileAttributes attrs) {
        Archivo archivoModelo = crearArchivoDesdePath(p, attrs);

        try {
            // Usar Archivo de db en lugar de ArchivoBD
            koolfileindexer.db.Archivo filtroBD = ArchivoConverter.toDbArchivo(archivoModelo);

            // Buscar en BD y manejar resultado
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
                    filtroBD, attrs.size(), attrs.size());
            if (rs != null) {
                try (rs) { // <-- Usar try-with-resources
                    if (rs.next()) {
                        actualizarArchivoExistente(archivoModelo, attrs, rs);
                    } else {
                        insertarNuevoArchivo(archivoModelo, attrs);
                    }
                }
            } else {
                insertarNuevoArchivo(archivoModelo, attrs);
            }
        } catch (Exception e) {
            System.err.println("Error al procesar archivo: " + p + " - " + e.getMessage());
        }
    }

    /**
     * Detiene el scheduler de indexación periódica si está activo.
     */
    public synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            System.out.println("[SCHEDULER] Detenido correctamente");
        }

        // Cerrar conexión si es necesario
        if (connector != null) {
            try {
                connector.terminarConexion();
                System.out.println("[DB] Conexión cerrada correctamente");
            } catch (Exception e) {
                System.err.println("[DB] Error al cerrar conexión: " + e.getMessage());
            }
        }
    }

    // Añadir este método para detectar cambios en archivos
    private void detectarCambiosArchivo(Path nuevaRuta, BasicFileAttributes attrs, ResultSet rs) throws SQLException {
        try {
            // Usar el enfoque de alternativas para obtener nombres de columnas
            String nombreActualEnBD = ArchivoConverter.getStringWithAlternatives(rs,
                    new String[] { "nombre", "arc_nombre", "name" });

            String rutaActualEnBD = ArchivoConverter.getStringWithAlternatives(rs,
                    new String[] { "path", "arc_path", "arc_ruta_completa", "ruta_completa" });

            // El resto del método sigue igual
            String nuevoNombre = nuevaRuta.getFileName().toString();
            String nuevaRutaCompleta = nuevaRuta.toAbsolutePath().normalize().toString();

            // Si el nombre cambió pero la ruta base es similar, actualizar el nombre
            if (!nuevoNombre.equals(nombreActualEnBD)) {
                ArchivoAdapter archivo = new ArchivoAdapter();
                archivo.setRutaCompleta(nuevaRutaCompleta);
                archivo.setNombre(nuevoNombre);
                String extension = ArchivoConverter.getStringWithAlternatives(rs,
                        new String[] { "extension", "ext_extension" });
                if (extension != null) {
                    archivo.setExtension(extension);
                }

                connector.actualizarNombreArchivo(archivo, nombreActualEnBD);
                System.out.println("[RENOMBRADO] " + nombreActualEnBD + " -> " + nuevoNombre);
            }

            // Si la ruta cambió pero el nombre es el mismo, actualizar la ubicación
            Path rutaActualPath = Paths.get(rutaActualEnBD);
            if (!rutaActualPath.getParent().equals(nuevaRuta.getParent())) {
                // Usar ArchivoAdapter en lugar de Archivo
                ArchivoAdapter archivo = new ArchivoAdapter();
                archivo.setRutaCompleta(nuevaRutaCompleta);
                archivo.setNombre(nuevoNombre);
                String extension = ArchivoConverter.getStringWithAlternatives(rs,
                        new String[] { "extension", "ext_extension" });
                if (extension != null) {
                    archivo.setExtension(extension);
                }

                connector.actualizarUbicacionArchivo(archivo, rutaActualEnBD);
                System.out.println("[MOVIDO] " + rutaActualEnBD + " -> " + nuevaRutaCompleta);
            }
        } catch (Exception e) {
            System.err.println("Error al detectar cambios en archivo: " + e.getMessage());
        }
    }

    private void actualizarArchivoExistente(Archivo archivoModelo, BasicFileAttributes attrs, ResultSet rs)
            throws Exception {
        archivoModelo.setId(rs.getLong("id"));
        archivoModelo.asignarCategoria(Categoria.clasificar(archivoModelo));
        archivoModelo.actualizarFechaModificacion(
                LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));

        try {
            // Detectar y manejar cambios de nombre o ubicación
            detectarCambiosArchivo(Paths.get(archivoModelo.getRutaCompleta()), attrs, rs);

            // Actualizar el resto de información
            koolfileindexer.db.Archivo archivoDb = ArchivoConverter.toDbArchivo(archivoModelo);
            connector.actualizarTamanoFechaModificacionArchivo(archivoDb);
            connector.actualizarCategoriaArchivo(archivoDb);
            System.out.println("[ACTUALIZADO] " + archivoModelo.getRutaCompleta());
        } catch (SQLException e) {
            System.err.println("Error al actualizar archivo: " + e.getMessage());
        }
    }

    private void insertarNuevoArchivo(Archivo archivoModelo, BasicFileAttributes attrs) throws Exception {
        // Usar ArchivoConverter para asegurar que se crea un objeto válido
        koolfileindexer.db.Archivo archivoDb = ArchivoConverter.toDbArchivo(archivoModelo);

        // Verificación adicional
        if (!(archivoDb instanceof ArchivoAdapter)) {
            // Si por alguna razón no es un ArchivoAdapter, convertirlo
            archivoDb = new ArchivoAdapter(
                    archivoDb.getNombre(),
                    archivoDb.getTamanoBytes(),
                    archivoDb.getFechaModificacion(),
                    archivoDb.getRutaCompleta(),
                    archivoDb.getExtension(),
                    archivoDb.getCategoria() != null ? archivoDb.getCategoria().getNombre() : "OTRO");
        }

        connector.crearArchivo(archivoDb);
    }

    private Archivo crearArchivoDesdePath(Path p, BasicFileAttributes attrs) {
        String nombre = p.getFileName().toString();
        String ruta = p.toAbsolutePath().normalize().toString();

        int idx = nombre.lastIndexOf('.');
        String ext = (idx > 0 && idx < nombre.length() - 1)
                ? nombre.substring(idx + 1).toLowerCase()
                : "";

        long tam = attrs.size();
        LocalDateTime cre = LocalDateTime.ofInstant(
                attrs.creationTime().toInstant(), ZoneId.systemDefault());
        LocalDateTime mod = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

        Archivo a = new Archivo(nombre, ruta, ext, tam, cre, mod);
        a.asignarCategoria(Categoria.clasificar(a));
        return a;
    }

    // Método run que ejecuta toda la lógica del indexador
    @Override
    public void run() {
        try {
            // Indexación inicial de todas las raíces
            System.out.println("\n=== Iniciando indexación inicial ===");
            for (Path raiz : raicesAIndexar) {
                System.out.println("\n→ Indexando (batch=" + tamanoLote + "): " + raiz);
                recorrerDirectorio(raiz, tamanoLote);
            }
            System.out.println("\n=== Indexación inicial completada ===");

            // Monitoreo periódico
            System.out.println("\n=== Iniciando monitor periódico ===");
            while (ejecutando && !Thread.currentThread().isInterrupted()) {
                try {
                    // Indexación periódica de HOME
                    Path homePath = Paths.get(System.getProperty("user.home"));
                    recorrerDirectorio(homePath, tamanoLote);

                    // Añadir limpieza periódica
                    limpiarArchivosNoExistentes();

                    // Esperar hasta el próximo intervalo
                    Thread.sleep(intervaloEjecucion.toMillis());
                } catch (InterruptedException e) {
                    System.out.println("Indexador interrumpido durante la espera");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error en ciclo de indexación: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Si es una interrupción, propagar
            if (e instanceof InterruptedException) {
                System.out.println("Indexador interrumpido");
                Thread.currentThread().interrupt();
            } else {
                System.err.println("Error fatal en el indexador: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Método para detener el indexador
    public void detener() {
        this.ejecutando = false;
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
    }

    public boolean agregarPalabraClave(String filePath, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            System.err.println("La palabra clave no puede estar vacía");
            return false;
        }

        if (!koolfileindexer.modelo.ValidadorEntrada.esPalabraClaveValida(keyword)) {
            System.err.println("Palabra clave inválida: " + keyword);
            return false;
        }

        try {
            // Lógica para agregar una palabra clave a un archivo existente
            // 1. Buscar el archivo por su ruta
            ArchivoAdapter filtro = new ArchivoAdapter();
            filtro.setRutaCompleta(filePath);
            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);
            if (rs != null) {
                try (rs) {
                    if (rs.next()) {
                        // 2. Obtener el ID del archivo
                        long archivoId = rs.getLong("id");

                        // 3. Asociar la nueva palabra clave al archivo
                        ArchivoAdapter archivo = new ArchivoAdapter();
                        archivo.setRutaCompleta(filePath);
                        archivo.setNombre(rs.getString("arc_nombre"));
                        String extension = rs.getString("ext_extension");
                        if (extension != null) {
                            archivo.setExtension(extension);
                        }
                        connector.asociarPalabraClaveArchivo(archivo, keyword.toLowerCase());
                        System.out.println("[PALABRA CLAVE AGREGADA] " + keyword + " a " + filePath);
                        return true;
                    } else {
                        System.err.println("Archivo no encontrado: " + filePath);
                        return false;
                    }
                }
            } else {
                System.err.println("Error al buscar el archivo en la base de datos");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error al agregar palabra clave: " + e.getMessage());
            return false;
        }
    }

    // Añadir este método a la clase Indexador
    public void limpiarArchivosNoExistentes() {
        System.out.println("[LIMPIEZA] Iniciando verificación de archivos indexados...");
        try {
            // Usar ArchivoAdapter en lugar de Archivo normal
            ArchivoAdapter filtro = new ArchivoAdapter();
            filtro.setNombre("%"); // Comodín SQL para buscar cualquier nombre

            ResultSet rs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, -1, -1);

            if (rs != null) {
                // El resto del código sigue igual
                int eliminados = 0;
                try (rs) {
                    while (rs.next()) {
                        String rutaCompleta = ArchivoConverter.getStringWithAlternatives(rs,
                                new String[] { "arc_ruta_completa", "ruta_completa", "path" });
                        Path path = Paths.get(rutaCompleta);

                        // Verificar si el archivo ya no existe
                        if (!Files.exists(path)) {
                            ArchivoAdapter archivoDb = new ArchivoAdapter(
                                    rs.getString("arc_nombre"),
                                    0, // El tamaño no es relevante para eliminar
                                    LocalDateTime.now(), // La fecha no es relevante para eliminar
                                    rutaCompleta,
                                    rs.getString("ext_extension"),
                                    rs.getString("cat_nombre") // Obtener la categoría del resultset
                            );

                            // Eliminar el archivo de la BD
                            connector.eliminarArchivo(archivoDb);
                            eliminados++;
                            System.out.println("[ELIMINADO] " + rutaCompleta + " (ya no existe en el sistema)");
                        }
                    }
                }
                System.out.println("[LIMPIEZA] Total de archivos eliminados: " + eliminados);
            }
        } catch (SQLException sqlEx) {
            System.err.println("Error de base de datos durante la limpieza: " + sqlEx.getMessage());
            sqlEx.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error inesperado durante la limpieza: " + e.getMessage());
            e.printStackTrace();
        }
    }
}