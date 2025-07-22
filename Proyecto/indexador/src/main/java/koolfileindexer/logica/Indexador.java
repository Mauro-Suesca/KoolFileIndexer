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
        cargarExclusiones(archivoExclusiones);
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

    private void cargarExclusiones(String archivoExclusiones) {
        if (archivoExclusiones == null || archivoExclusiones.isBlank())
            return;
        Path path = Paths.get(archivoExclusiones);
        if (!Files.exists(path))
            return;

        try {
            Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                    .map(Paths::get)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .forEach(rutasExcluidas::add);
        } catch (IOException e) {
            System.err.println("Error leyendo exclusiones: " + e.getMessage());
        }
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

        String nombre = norm.getFileName().toString().toLowerCase();
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

        // Rutas del sistema (Windows, Program Files, etc.)
        if (rutaMin.contains("\\windows\\") || rutaMin.contains("/windows/")) {
            return true;
        }
        if (rutaMin.contains("\\program files\\") || rutaMin.contains("/program files/")) {
            return true;
        }
        if (rutaMin.contains("\\archivos de programa\\") || rutaMin.contains("/archivos de programa/")) {
            return true;
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
                    if (attrs.isRegularFile() && !excluirArchivo(file)) {
                        procesados.incrementAndGet();
                        procesarArchivo(file, attrs);
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
                try (rs) {
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
    }

    private void actualizarArchivoExistente(Archivo archivoModelo, BasicFileAttributes attrs, ResultSet rs)
            throws Exception {
        archivoModelo.setId(rs.getLong("id"));
        archivoModelo.asignarCategoria(Categoria.clasificar(archivoModelo));
        archivoModelo.actualizarFechaModificacion(
                LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));

        try {
            // Usar Archivo de db en lugar de ArchivoBD
            koolfileindexer.db.Archivo archivoDb = ArchivoConverter.toDbArchivo(archivoModelo);
            connector.actualizarTamanoFechaModificacionArchivo(archivoDb);
            connector.actualizarCategoriaArchivo(archivoDb);
            System.out.println("[ACTUALIZADO] " + archivoModelo.getRutaCompleta());
        } catch (SQLException e) {
            System.err.println("Error al actualizar archivo: " + e.getMessage());
        }
    }

    private void insertarNuevoArchivo(Archivo archivoModelo, BasicFileAttributes attrs) throws Exception {
        try {
            // Usar Archivo de db en lugar de ArchivoBD
            koolfileindexer.db.Archivo archivoDb = ArchivoConverter.toDbArchivo(archivoModelo);
            connector.crearArchivo(archivoDb);
            System.out.println("[INSERTADO]  " + archivoModelo.getRutaCompleta());

            // Buscar usando el mismo archivoDb para la consulta
            ResultSet nuevoRs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
                    archivoDb, archivoModelo.getTamanoBytes(), archivoModelo.getTamanoBytes());
            if (nuevoRs != null) {
                try (nuevoRs) {
                    if (nuevoRs.next()) {
                        archivoModelo.setId(nuevoRs.getLong("id"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar archivo: " + e.getMessage());
        }
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
            while (ejecutando) {
                try {
                    // Indexación periódica de HOME
                    Path homePath = Paths.get(System.getProperty("user.home"));
                    recorrerDirectorio(homePath, tamanoLote);

                    // Esperar hasta el próximo intervalo
                    Thread.sleep(intervaloEjecucion.toMillis());
                } catch (InterruptedException e) {
                    System.out.println("Indexador interrumpido durante la espera");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error durante la indexación periódica: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fatal en el indexador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para detener el indexador
    public void detener() {
        this.ejecutando = false;
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
    }
}