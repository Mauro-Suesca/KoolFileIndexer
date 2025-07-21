package koolfileindexer.logica;

import koolfileindexer.db.ConectorBasedeDatos;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.SQLException;

public class MainIndexadorCLI {
    private static final int DEFAULT_BATCH = 100;
    private static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(1);
    private static ConectorBasedeDatos connector;

    public static void main(String[] args) {
        Indexador idx = null;
        try {
            // 0) Inicializar y verificar conexión BD
            connector = ConectorBasedeDatos.obtenerInstancia();
            verificarConexionBD();

            // 1) Instancia del indexador con archivo de exclusiones
            idx = Indexador.getInstance("exclusiones.txt");
            mostrarConfiguracion(idx);

            // 2) Selección de raíces según el modo
            List<Path> rootsToScan = seleccionarRaices(args);

            // 3) Indexación por lotes
            ejecutarIndexacion(idx, rootsToScan);

            // 4) Arranque del monitor periódico
            iniciarMonitorPeriodico(idx);

            // 5) Esperar comando de salida
            esperarComandoSalida();

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Asegurar limpieza de recursos
            if (idx != null) {
                idx.shutdown();
            }
            if (connector != null) {
                // Usar terminarConexion() en lugar de shutdown()
                connector.terminarConexion();
            }
        }
    }

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
        return rootsToScan;
    }

    private static void ejecutarIndexacion(Indexador idx, List<Path> roots) {
        System.out.println("\n=== Iniciando indexación inicial ===");
        for (Path root : roots) {
            System.out.println("\n→ Indexando (batch=" + DEFAULT_BATCH + "): " + root);
            idx.recorrerDirectorio(root, DEFAULT_BATCH);
        }
        System.out.println("\n=== Indexación inicial completada ===");
    }

    private static void iniciarMonitorPeriodico(Indexador idx) {
        System.out.println("\n=== Iniciando monitor periódico ===");
        idx.iniciarIndexacionPeriodica(
                Paths.get(System.getProperty("user.home")),
                DEFAULT_BATCH,
                DEFAULT_INTERVAL);
        System.out.println("Monitor activo - presiona 'q' + ENTER para salir");
    }

    private static void esperarComandoSalida() {
        System.out.println("Servicio iniciado. Presiona Ctrl+C para terminar.");

        // Agregar un gancho de apagado para limpiar recursos al terminar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Apagando servicios...");
            // El código de limpieza ya está en el bloque finally del método main
        }));

        // Mantener el programa en ejecución sin esperar entrada interactiva
        try {
            // Esperar por un tiempo específico (30 minutos) o hasta que el programa sea
            // terminado
            Thread.sleep(30 * 60 * 1000); // 30 minutos en milisegundos
            System.out.println("Tiempo máximo de ejecución alcanzado. Terminando servicios...");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Ejecución interrumpida.");
        }
    }
}