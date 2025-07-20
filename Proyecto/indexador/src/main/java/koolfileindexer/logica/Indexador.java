package koolfileindexer.logica;

import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.ArchivoConnector;
import koolfileindexer.modelo.Categoria;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Indexador recorre carpetas por lotes, aplica exclusiones
 * y delega persistencia en BD vía ArchivoConnector.
 */
public class Indexador {
    // ─── Singleton ───────────────────────────────────────────────
    private static final AtomicReference<Indexador> INSTANCIA = new AtomicReference<>();

    /**
     * Devuelve la instancia única de Indexador, inicializándola la primera vez.
     *
     * @param archivoExclusiones ruta al fichero de exclusiones
     * @param connector          implementación de ArchivoConnector
     */
    public static Indexador getInstance(String archivoExclusiones, ArchivoConnector connector) {
        INSTANCIA.compareAndSet(null, new Indexador(archivoExclusiones, connector));
        return INSTANCIA.get();
    }

    private final ArchivoConnector connector;

    // ─── Exclusiones ──────────────────────────────────────────────
    private final Set<Path> rutasExcluidas = new HashSet<>();
    private static final Set<String> EXT_PROHIBIDAS = Set.of("exe", "dll");
    private static final Set<String> PATRONES_PROTEGIDOS = Set.of("windows");

    // ─── Scheduler y batching ─────────────────────────────────────
    private ScheduledExecutorService scheduler;
    private Duration intervalo = Duration.ofMinutes(5);
    private int batchSize = 100;

    private Indexador(String archivoExclusiones, ArchivoConnector connector) {
        this.connector = Objects.requireNonNull(connector, "connector no puede ser null");
        cargarExclusiones(archivoExclusiones);
    }

    /**
     * Arranca la indexación periódica.
     *
     * @param raiz      carpeta raíz a indexar
     * @param batchSize número máximo de ficheros por pase
     * @param intervalo intervalo entre pasadas
     */
    public synchronized void iniciarIndexacionPeriodica(Path raiz, int batchSize, Duration intervalo) {
        // CAMBIO REQUERIDO: Añadido log de inicio de scheduler
        System.out.println("[SCHEDULER] Configurando indexación periódica:");
        System.out.println(" - Intervalo: cada " + intervalo.toMinutes() + " minutos");
        System.out.println(" - Tamaño de lote: " + batchSize + " archivos por ciclo");

        this.batchSize = batchSize;
        this.intervalo = intervalo;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        // CAMBIO REQUERIDO: Thread no-daemon y log de ciclo
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Indexador-Scheduler");
            t.setDaemon(false); // CAMBIO IMPORTANTE: Hilo de usuario
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n[SCHEDULER] Iniciando ciclo de indexación a las " + LocalDateTime.now());
            recorrerDirectorio(raiz, this.batchSize);
            System.out.println("[SCHEDULER] Ciclo completado a las " + LocalDateTime.now());
        }, 0, intervalo.toMillis(), TimeUnit.MILLISECONDS);
    }

    // ─── Exclusiones ──────────────────────────────────────────────

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

    /** Rutas excluidas (solo lectura). */
    public Set<Path> getRutasExcluidas() {
        return Collections.unmodifiableSet(rutasExcluidas);
    }

    /**
     * True si p está bajo alguna ruta excluida, oculto, protegido o con extensión
     * prohibida.
     */
    private boolean excluirArchivo(Path p) {
        Path norm = p.toAbsolutePath().normalize();

        for (Path excl : rutasExcluidas) {
            if (norm.startsWith(excl)) {
                return true;
            }
        }

        String nombre = norm.getFileName().toString().toLowerCase();

        try {
            if (Files.isHidden(norm))
                return true;
        } catch (IOException ignored) {
        }
        if (nombre.startsWith("."))
            return true;

        int idx = nombre.lastIndexOf('.');
        if (idx >= 0 && EXT_PROHIBIDAS.contains(nombre.substring(idx + 1))) {
            return true;
        }

        String rutaMin = norm.toString().toLowerCase();
        for (String pat : PATRONES_PROTEGIDOS) {
            if (rutaMin.contains(File.separator + pat)) {
                return true;
            }
        }

        return false;
    }

    // ─── Recorrido con batching ─────────────────────────────────────

    /**
     * Recorre recursivamente desde rutaBase e indexa hasta batchSize archivos.
     */
    public void recorrerDirectorio(Path rutaBase, int batchSize) {
        // CAMBIO REQUERIDO: Añadido log de inicio de recorrido
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
            Files.walkFileTree(base, new SimpleFileVisitor<>() {
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
                    System.err.println("⚠️ No se pudo acceder a: "
                            + file + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error recorriendo directorio: " + e.getMessage());
        }

        // CAMBIO REQUERIDO: Log de resumen del batch
        System.out.println("[BATCH] Procesados " + procesados.get() + " archivos en este ciclo");
    }

    // ─── Procesamiento / Persistencia ───────────────────────────────

    /**
     * Construye un objeto Archivo con los metadatos y lo inserta
     * o actualiza en BD según exista o no via connector.findByMetadata().
     */
    private void procesarArchivo(Path p, BasicFileAttributes attrs) {
        Archivo a = crearArchivoDesdePath(p, attrs);

        Optional<Archivo> opt = connector.findByMetadata(
                attrs.size(),
                attrs.creationTime().toMillis(),
                a.getExtension());

        if (opt.isPresent()) {
            Archivo existente = opt.get();
            existente.asignarCategoria(Categoria.clasificar(existente));
            existente.actualizarFechaModificacion(
                    LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));
            connector.update(existente);
            System.out.println("[ACTUALIZADO] " + existente.getRutaCompleta());
        } else {
            Long newId = connector.insert(a);
            a.setId(newId);
            System.out.println("[INSERTADO]  " + a.getRutaCompleta());
        }
    }

    /** Extrae metadatos y crea el objeto dominio Archivo. */
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
}