package logica;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public static void main(String[] args) {
        // 1) Obtenemos la instancia singleton (constructor privado)
        Indexador idx = Indexador.getInstance("exclusiones.txt");
        System.out.println(">>> Excluyendo rutas/patrones:");
        idx.getRutasExcluidas().forEach(r -> System.out.println("  - " + r));

        // 2) Selección de raíces según el modo
        String modo = args.length > 0 ? args[0].toLowerCase() : "demo";
        List<Path> rootsToScan;
        switch (modo) {
            case "demo":
                System.out.println("\n[Demo] Índice de tu carpeta HOME:");
                rootsToScan = List.of(Paths.get(System.getProperty("user.home")));
                break;

            case "docs":
                System.out.println("\n[Docs] Índice de tu carpeta DOCUMENTS:");
                rootsToScan = List.of(
                        Paths.get(System.getProperty("user.home"), "Documents"));
                break;

            case "full":
                System.out.println("\n[Full] Índice de todas las unidades montadas + HOME:");
                rootsToScan = new ArrayList<>();
                for (File disco : File.listRoots()) {
                    Path p = disco.toPath();
                    if (p.toFile().exists() && p.toFile().isDirectory()) {
                        rootsToScan.add(p);
                    }
                }
                // además la carpeta HOME
                rootsToScan.add(Paths.get(System.getProperty("user.home")));
                break;

            default:
                System.err.println("\nModo desconocido \"" + modo + "\"; usando demo.");
                rootsToScan = List.of(Paths.get(System.getProperty("user.home")));
        }

        // 3) Recorremos cada raíz e indexamos
        for (Path root : rootsToScan) {
            System.out.println("\n→ Indexando: " + root);
            idx.recorrerDirectorio(root);
        }

        // 4) Resumen final
        int total = idx.getArchivosIndexados().size();
        System.out.println("\n=== ÍNDICE FINAL: " + total + " archivos indexados ===");
    }
}
