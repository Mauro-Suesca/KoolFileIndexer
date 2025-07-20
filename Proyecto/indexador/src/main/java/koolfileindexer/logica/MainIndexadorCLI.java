package logica;

import modelo.ArchivoConnector;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI para invocar el Indexador en distintos modos:
 * - demo: sólo tu carpeta HOME
 * - docs: sólo tu carpeta Documents
 * - full: todas las unidades montadas + HOME
 *
 * Uso:
 * java -cp bin logica.MainIndexadorCLI [modo]
 */
public class MainIndexadorCLI {
    /** Tamaño de lote por defecto (puedes ajustar aquí) */
    private static final int DEFAULT_BATCH = 100;
    /** Intervalo por defecto entre pasadas (para periodic) */
    private static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(1);

    public static void main(String[] args) {
        // 1) Creamos el conector stub y la instancia singleton
        ArchivoConnector connector = new InMemoryArchivoConnector();
        Indexador idx = Indexador.getInstance("exclusiones.txt", connector);

        System.out.println(">>> Excluyendo rutas/patrones:");
        idx.getRutasExcluidas().forEach(r -> System.out.println("  - " + r));

        // 2) Selección de raíces según el modo
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
                for (File disco : File.listRoots()) {
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

        // 3) Indexación por lotes (batch) SIN scheduler (un solo pase)
        for (Path root : rootsToScan) {
            System.out.println("\n→ Indexando (batch=" + DEFAULT_BATCH + "): " + root);
            idx.recorrerDirectorio(root, DEFAULT_BATCH);
        }

        // 4) Resumen final (solo con el stub sabemos el conteo)
        if (connector instanceof InMemoryArchivoConnector mem) {
            System.out.println("\n=== ÍNDICE FINAL: "
                    + mem.getAll().size() + " archivos indexados ===");
        } else {
            System.out.println("\n=== Índice completado. (Resumen no disponible) ===");
        }

        // 5) Arranque periódico
        idx.iniciarIndexacionPeriodica(
                Paths.get(System.getProperty("user.home")),
                DEFAULT_BATCH,
                DEFAULT_INTERVAL);

        // Nota: el scheduler usa un hilo NO-DAEMON, así que la JVM
        // seguirá viva hasta que hagas Ctrl+C.
    }
}
