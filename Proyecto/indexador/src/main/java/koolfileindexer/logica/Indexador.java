package koolfileindexer.logica;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.Categoria;

public class Indexador {

    private final List<Archivo> archivosIndexados = new ArrayList<>();
    private final Set<String> rutasExcluidas = new HashSet<>();

    /** Carga rutas a excluir (una por línea) desde el .txt. */
    public Indexador(String archivoExclusiones) {
        if (archivoExclusiones != null && !archivoExclusiones.isBlank()) {
            Path path = Paths.get(archivoExclusiones);
            if (Files.exists(path)) {
                try {
                    Files.readAllLines(path)
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .forEach(rutasExcluidas::add);
                } catch (IOException e) {
                    System.err.println(
                        "Error leyendo exclusiones: " + e.getMessage()
                    );
                }
            }
        }
    }

    /** Sólo lectura de los patrones de exclusión. */
    public Set<String> getRutasExcluidas() {
        return Collections.unmodifiableSet(rutasExcluidas);
    }

    /** Recorre recursivamente el árbol, indexando ficheros válidos. */
    public void recorrerDirectorio(Path rutaBase) {
        Path base;
        try {
            base = rutaBase.toAbsolutePath().normalize();
            if (!Files.isDirectory(base)) {
                System.err.println("No es un directorio válido: " + base);
                return;
            }
        } catch (Exception e) {
            System.err.println(
                "Ruta inválida: " + rutaBase + " (" + e.getMessage() + ")"
            );
            return;
        }

        try {
            Files.walkFileTree(
                base,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(
                        Path dir,
                        BasicFileAttributes attrs
                    ) {
                        if (
                            estaExcluido(dir) || esOcultoOSistema(dir)
                        ) return FileVisitResult.SKIP_SUBTREE;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attrs
                    ) {
                        if (
                            attrs.isRegularFile() &&
                            !yaIndexado(file) &&
                            !estaExcluido(file) &&
                            !esOcultoOSistema(file)
                        ) {
                            indexarArchivo(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(
                        Path file,
                        IOException exc
                    ) {
                        // Si no podemos leer ese archivo o carpeta, lo reportamos y seguimos
                        System.err.println(
                            "⚠️ No se pudo acceder a: " +
                            file +
                            " (" +
                            exc.getMessage() +
                            ")"
                        );
                        return FileVisitResult.CONTINUE;
                    }
                }
            );
        } catch (IOException e) {
            System.err.println(
                "Error recorriendo directorio: " + e.getMessage()
            );
        }
    }

    /** Comprueba si la ruta contiene alguno de los patrones de exclusión. */
    private boolean estaExcluido(Path p) {
        String s = p.toAbsolutePath().normalize().toString().toLowerCase();
        return rutasExcluidas.stream().anyMatch(s::contains);
    }

    /** Ocultos de SO, extensiones bloqueadas y carpeta Windows. */
    private boolean esOcultoOSistema(Path p) {
        try {
            if (Files.isHidden(p)) return true;
        } catch (IOException ignored) {}

        String name = p.getFileName().toString().toLowerCase();
        if (name.startsWith(".")) return true;
        if (name.endsWith(".exe") || name.endsWith(".dll")) return true;

        String path = p.toAbsolutePath().normalize().toString().toLowerCase();
        if (path.contains(File.separator + "windows")) return true;

        return false;
    }

    /** Extrae metadatos, crea el objeto Archivo y lo añade a la lista. */
    public void indexarArchivo(Path p) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(
                p,
                BasicFileAttributes.class
            );

            String nombre = p.getFileName().toString();
            String ruta = p.toAbsolutePath().normalize().toString();

            // obtengo extensión
            String ext = "";
            int idx = nombre.lastIndexOf('.');
            if (idx > 0 && idx < nombre.length() - 1) {
                ext = nombre.substring(idx + 1).toLowerCase();
            }

            long tam = attrs.size();
            LocalDateTime cre = LocalDateTime.ofInstant(
                attrs.creationTime().toInstant(),
                ZoneId.systemDefault()
            );
            LocalDateTime mod = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(),
                ZoneId.systemDefault()
            );

            Archivo a = new Archivo(nombre, ruta, ext, tam, cre, mod);
            a.asignarCategoria(Categoria.clasificar(a));
            archivosIndexados.add(a);

            System.out.println("Indexado → " + a);
        } catch (IOException e) {
            System.err.println(
                "No se pudo indexar " + p + ": " + e.getMessage()
            );
        }
    }

    /** Evita indexar dos veces el mismo archivo. */
    private boolean yaIndexado(Path p) {
        String rut = p.toAbsolutePath().normalize().toString();
        return archivosIndexados
            .stream()
            .anyMatch(a -> a.getRutaCompleta().equals(rut));
    }

    /** Devuelve la lista final de archivos indexados. */
    public List<Archivo> getArchivosIndexados() {
        return Collections.unmodifiableList(archivosIndexados);
    }
}
