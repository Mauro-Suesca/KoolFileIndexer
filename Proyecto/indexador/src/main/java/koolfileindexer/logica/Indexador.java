package logica;

import modelo.Archivo;
import modelo.Categoria;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Indexador recorre carpetas, aplica exclusiones y mantiene un índice
 * en memoria de archivos, detectando duplicados/renombres vía fileKey.
 */
public class Indexador {
    // --- Singleton ---
    private static Indexador instancia;

    private Indexador(String archivoExclusiones) {
        cargarExclusiones(archivoExclusiones);
    }

    public static synchronized Indexador getInstance(String archivoExclusiones) {
        if (instancia == null) {
            instancia = new Indexador(archivoExclusiones);
        }
        return instancia;
    }

    // --- Índice en memoria por key (thread-safe) ---
    private final Map<Object, Archivo> archivosPorKey = new ConcurrentHashMap<>();

    // --- Exclusiones ---
    private final Set<Path> rutasExcluidas = new HashSet<>();

    private void cargarExclusiones(String archivoExclusiones) {
        if (archivoExclusiones == null || archivoExclusiones.isBlank())
            return;
        Path path = Paths.get(archivoExclusiones);
        if (!Files.exists(path))
            return;
        try {
            Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#")) // ignorar líneas vacías y comentarios
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

    /** Recorre recursivamente desde rutaBase e indexa cada archivo válido. */
    public void recorrerDirectorio(Path rutaBase) {
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

        try {
            Files.walkFileTree(base, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (estaExcluido(dir) || esOcultoOSistema(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()
                            && !estaExcluido(file)
                            && !esOcultoOSistema(file)) {
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
    }

    /** True si p está bajo alguna ruta excluida. */
    private boolean estaExcluido(Path p) {
        Path norm = p.toAbsolutePath().normalize();
        return rutasExcluidas.stream().anyMatch(norm::startsWith);
    }

    /** Ocultos/SO/Windows/.exe/.dll. */
    private boolean esOcultoOSistema(Path p) {
        try {
            if (Files.isHidden(p))
                return true;
        } catch (IOException ignored) {
        }
        String name = p.getFileName().toString().toLowerCase();
        if (name.startsWith("."))
            return true;
        if (name.endsWith(".exe") || name.endsWith(".dll"))
            return true;
        String full = p.toAbsolutePath().normalize().toString().toLowerCase();
        return full.contains(File.separator + "windows");
    }

    /**
     * Crea/actualiza un Archivo en el índice, usando fileKey (o fallback).
     */
    private void procesarArchivo(Path p, BasicFileAttributes attrs) {
        // 1) Extraer fileKey nativo o generar fallback
        Object key = obtenerFileKey(attrs, p);
        if (key == null) {
            key = generarFallbackKey(p, attrs);
        }

        // 2) Insertar o actualizar según exista la clave
        if (archivosPorKey.containsKey(key)) {
            Archivo existente = archivosPorKey.get(key);
            existente.asignarCategoria(Categoria.clasificar(existente));
            existente.actualizarFechaModificacion(
                    LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));
            System.out.println("[Actualizado] " + existente.getRutaCompleta());
        } else {
            Archivo a = crearArchivoDesdePath(p, attrs);
            archivosPorKey.put(key, a);
            imprimirArchivo(a);
        }
    }

    private Archivo crearArchivoDesdePath(Path p, BasicFileAttributes attrs) {
        String nombre = p.getFileName().toString();
        String ruta = p.toAbsolutePath().normalize().toString();
        String ext = "";
        int idx = nombre.lastIndexOf('.');
        if (idx > 0 && idx < nombre.length() - 1) {
            ext = nombre.substring(idx + 1).toLowerCase();
        }

        long tam = attrs.size();
        LocalDateTime cre = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
        LocalDateTime mod = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

        Archivo a = new Archivo(nombre, ruta, ext, tam, cre, mod);
        a.asignarCategoria(Categoria.clasificar(a));
        return a;
    }

    private void imprimirArchivo(Archivo a) {
        System.out.println();
        System.out.println("=== Archivo indexado ===");
        System.out.println("Nombre        : " + a.getNombre());
        System.out.println("Ruta          : " + a.getRutaCompleta());
        System.out.println("Extensión     : " + a.getExtension());
        System.out.println("Tamaño        : " + a.getTamanoBytes() + " bytes");
        System.out.println("Última modif. : " + a.getFechaModificacion());
        System.out.println("Categoría     : " + a.getCategoria().getNombre());
        System.out.println("Etiquetas     : " + a.getEtiquetas());
        System.out.println("Palabras clave: " + a.getPalabrasClave());
        System.out.println("========================");
        System.out.println();
    }

    /** Extrae fileKey del FS (requiere attrs ya leídos). */
    protected Object obtenerFileKey(BasicFileAttributes attrs, Path p) {
        try {
            return attrs.fileKey();
        } catch (Exception e) {
            return null;
        }
    }

    /** Fallback: hash de ruta + tamaño + fecha de creación. */
    private Object generarFallbackKey(Path p, BasicFileAttributes attrs) {
        String ruta = p.toAbsolutePath().normalize().toString();
        return Objects.hash(ruta, attrs.size(), attrs.creationTime().toMillis());
    }

    /** Devuelve el índice actual (solo lectura). */
    public Collection<Archivo> getArchivosIndexados() {
        return Collections.unmodifiableCollection(archivosPorKey.values());
    }
}
