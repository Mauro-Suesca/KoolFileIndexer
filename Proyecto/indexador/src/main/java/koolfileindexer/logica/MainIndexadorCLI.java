package koolfileindexer.logica;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainIndexadorCLI {

    public static void main(String[] args) {
        // 1) Inicializa Indexador con tu fichero de exclusiones:
        Indexador idx = new Indexador("exclusiones.txt");
        System.out.println(
            ">>> Excluyendo rutas/patrones: " + idx.getRutasExcluidas()
        );

        // 2) Decide raíces según el modo:
        String modo = args.length > 0 ? args[0].toLowerCase() : "demo";
        List<Path> rootsToScan;

        switch (modo) {
            case "demo":
                System.out.println("[Demo] Solo HOME:");
                rootsToScan = List.of(
                    Paths.get(System.getProperty("user.home"))
                );
                break;
            case "docs":
                System.out.println("[Docs] Solo Documents:");
                rootsToScan = List.of(
                    Paths.get(System.getProperty("user.home"), "Documents")
                );
                break;
            case "full":
                System.out.println("[Full] Todas las raíces + HOME:");
                rootsToScan = new ArrayList<>();

                // 3a) Todas las unidades montadas (C:\, D:\… o "/" en Unix)
                for (File disco : File.listRoots()) {
                    Path p = disco.toPath();
                    if (p.toFile().exists() && p.toFile().isDirectory()) {
                        rootsToScan.add(p);
                    }
                }
                // 3b) Aseguramos también tu carpeta de usuario
                rootsToScan.add(Paths.get(System.getProperty("user.home")));
                break;
            default:
                System.err.println(
                    "Modo desconocido “" + modo + "”, usando demo."
                );
                rootsToScan = List.of(
                    Paths.get(System.getProperty("user.home"))
                );
        }

        // 3) Recorre cada raíz
        for (Path root : rootsToScan) {
            System.out.println("→ Indexando: " + root);
            idx.recorrerDirectorio(root);
        }

        // 4) Resumen final
        System.out.println(
            "\n=== ÍNDICE FINAL: " +
            idx.getArchivosIndexados().size() +
            " archivos indexados ==="
        );
    }
}
