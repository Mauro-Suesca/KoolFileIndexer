package koolfileindexer.logica;

import koolfileindexer.db.ConectorBasedeDatos;
import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.Categoria;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import koolfileindexer.db.ArchivoBD;

/**
 * Indexador recorre carpetas por lotes, aplica exclusiones
 * y delega persistencia en BD vía ConectorBasedeDatos.
 */
public class Indexador {
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

    private Indexador(String archivoExclusiones) {
        this.connector = ConectorBasedeDatos.obtenerInstancia();
        cargarExclusiones(archivoExclusiones);
    }

    public static Indexador getInstance(String archivoExclusiones) {
        INSTANCIA.compareAndSet(null, new Indexador(archivoExclusiones));
        return INSTANCIA.get();
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
            ArchivoBD filtroBD = new ArchivoBD();
            filtroBD.setNombre(archivoModelo.getNombre());
            filtroBD.setTamanoBytes(attrs.size());
            filtroBD.setFechaCreacion(
                    LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault()));
            filtroBD.setRutaCompleta(archivoModelo.getRutaCompleta());
            filtroBD.setExtension(archivoModelo.getExtension());

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

        // Preparar datos para actualización
        ArchivoBD archivoBD = new ArchivoBD();
        archivoBD.setNombre(archivoModelo.getNombre());
        archivoBD.setTamanoBytes(archivoModelo.getTamanoBytes());
        archivoBD.setFechaCreacion(archivoModelo.getFechaCreacion());
        archivoBD.setRutaCompleta(archivoModelo.getRutaCompleta());
        archivoBD.setExtension(archivoModelo.getExtension());
        archivoBD.setCategoria(
                archivoModelo.getCategoria() != null ? archivoModelo.getCategoria().getNombre() : null);

        connector.actualizarTamanoFechaModificacionArchivo(archivoBD);
        connector.actualizarCategoriaArchivo(archivoBD);
        System.out.println("[ACTUALIZADO] " + archivoModelo.getRutaCompleta());
    }

    private void insertarNuevoArchivo(Archivo archivoModelo, BasicFileAttributes attrs) throws Exception {
        ArchivoBD archivoBD = new ArchivoBD();
        archivoBD.setNombre(archivoModelo.getNombre());
        archivoBD.setTamanoBytes(archivoModelo.getTamanoBytes());
        archivoBD.setFechaCreacion(archivoModelo.getFechaCreacion());
        archivoBD.setRutaCompleta(archivoModelo.getRutaCompleta());
        archivoBD.setExtension(archivoModelo.getExtension());
        archivoBD.setCategoria(
                archivoModelo.getCategoria() != null ? archivoModelo.getCategoria().getNombre() : null);

        if (connector.crearArchivo(archivoBD)) {
            // Obtener ID del nuevo archivo
            ResultSet nuevoRs = connector.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(
                    archivoBD, archivoModelo.getTamanoBytes(), archivoModelo.getTamanoBytes());
            if (nuevoRs != null) {
                try (nuevoRs) {
                    if (nuevoRs.next()) {
                        archivoModelo.setId(nuevoRs.getLong("id"));
                    }
                }
            }
            System.out.println("[INSERTADO]  " + archivoModelo.getRutaCompleta());
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
}