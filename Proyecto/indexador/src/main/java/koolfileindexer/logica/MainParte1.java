package koolfileindexer.logica;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.Categoria;
import koolfileindexer.modelo.Etiqueta;
import koolfileindexer.modelo.ValidadorEntrada;

public class MainParte1 {

    public static void main(String[] args) {
        System.out.println("=== TEST PARTE 1: DOMINIO ===\n");

        // 1) Crear Archivo con ruta y nombre sin normalizar
        Archivo a1 = new Archivo(
            "  MiDoc.TXT  ",
            "C:/Temp/../TempCarpeta/archivo.txt",
            "TXT",
            2048,
            LocalDateTime.of(2025, 7, 1, 10, 0),
            LocalDateTime.of(2025, 7, 1, 10, 0)
        );
        System.out.println("Archivo creado:");
        System.out.println(a1);

        // RN-001: ruta normalizada y trim de nombre
        System.out.println("\n· Ruta normalizada: " + a1.getRutaCompleta());
        System.out.println("· Nombre trimmed: '" + a1.getNombre() + "'");

        // RN-004: categoría automática
        System.out.println(
            "\n· Categoría automática (debe ser Documento (auto)): " +
            a1.getCategoria()
        );

        // 2) Pruebas de esOculto() y esValido()
        Archivo oculto = new Archivo(
            ".oculto",
            "C:/ruta/oculto.txt",
            "txt",
            100,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        System.out.println("\n· esOculto() de '.oculto': " + oculto.esOculto());
        System.out.println("· esValido() de '.oculto': " + oculto.esValido());
        Archivo invalido = new Archivo(
            "   ",
            "C:/ruta/blank.txt",
            "txt",
            100,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        System.out.println(
            "· esValido() de nombre en blanco: " + invalido.esValido()
        );

        // 3) Etiquetas: validas, duplicados, longitud, patrón y renombrado (RN-002,
        // RN-003)
        System.out.println("\n== Etiquetas ==");
        try {
            Etiqueta t1 = Etiqueta.crear(" proyecto ");
            Etiqueta t2 = Etiqueta.crear("tarea_urgente");
            a1.agregarEtiqueta(t1);
            a1.agregarEtiqueta(t2);
            a1.agregarEtiqueta(t1); // duplicado, no se añade
            System.out.println(
                "Tras agregar t1 y t2 (y t1 duplicada): " + a1.getEtiquetas()
            );

            // Renombrado válido
            t1.setNombre("informe 2025");
            System.out.println(
                "Tras renombrar t1 a 'informe 2025': " + a1.getEtiquetas()
            );

            // Longitud >50 -> excepción
            String largo = "x".repeat(51);
            try {
                Etiqueta.crear(largo);
                System.out.println(
                    "ERROR: etiqueta de longitud >50 no lanzó excepción"
                );
            } catch (IllegalArgumentException ex) {
                System.out.println(
                    "  [OK] Detectada etiqueta demasiado larga: " +
                    ex.getMessage()
                );
            }

            // Espacio doble -> inválido
            try {
                Etiqueta.crear("mal  formato");
                System.out.println(
                    "ERROR: etiqueta con espacio doble no lanzó excepción"
                );
            } catch (IllegalArgumentException ex) {
                System.out.println(
                    "  [OK] Detectada etiqueta con espacio doble: " +
                    ex.getMessage()
                );
            }

            // Caracter especial -> inválido
            try {
                Etiqueta.crear("mal@formato");
                System.out.println(
                    "ERROR: etiqueta con carácter especial no lanzó excepción"
                );
            } catch (IllegalArgumentException ex) {
                System.out.println(
                    "  [OK] Detectada etiqueta con carácter especial: " +
                    ex.getMessage()
                );
            }

            // Quitar etiqueta y verificar
            a1.quitarEtiqueta(t2);
            System.out.println("Tras quitar t2: " + a1.getEtiquetas());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4) Palabras clave: validas, duplicados, longitud, patrón (RN-003)
        System.out.println("\n== Palabras clave ==");
        System.out.println("Iniciales: " + a1.getPalabrasClave());

        a1.agregarPalabraClave("clave1");
        a1.agregarPalabraClave("clave1"); // duplicado
        a1.agregarPalabraClave("clave2");
        System.out.println(
            "Tras agregar 'clave1' y 'clave2': " + a1.getPalabrasClave()
        );

        // Modificación
        boolean mod = a1.modificarPalabraClave("clave1", "nuevaClave");
        System.out.println("Modificar 'clave1'→'nuevaClave': " + mod);
        System.out.println("Palabras clave ahora: " + a1.getPalabrasClave());

        // Longitud >50
        String larga = "k".repeat(51);
        try {
            a1.agregarPalabraClave(larga);
            System.out.println("ERROR: palabra clave >50 no lanzó excepción");
        } catch (Exception ex) {
            System.out.println(
                "  [OK] Detectada palabra clave demasiado larga"
            );
        }

        // Espacio simple válido? No permitido en palabras clave
        try {
            a1.agregarPalabraClave("mal formato");
            System.out.println(
                "ERROR: palabra clave con espacio no lanzó excepción"
            );
        } catch (Exception ex) {
            System.out.println(
                "  [OK] Detectada palabra clave con espacio: " + ex.getMessage()
            );
        }

        // Eliminar palabra clave
        a1.eliminarPalabraClave("clave2");
        System.out.println("Tras eliminar 'clave2': " + a1.getPalabrasClave());

        // 5) Fecha de modificación se actualiza
        System.out.println(
            "\nFecha de modificación actualizada: " +
            a1.getFechaModificacion().truncatedTo(ChronoUnit.SECONDS)
        );

        // 6) Comprobar comparaciones de rutas case-insensitive
        Archivo a2 = new Archivo(
            "archivo",
            a1.getRutaCompleta().toUpperCase(),
            "txt",
            2048,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        System.out.println("\nComparación de rutas case-insensitive:");
        System.out.println(
            "Rutas iguales ignoreCase: " +
            a1.getRutaCompleta().equalsIgnoreCase(a2.getRutaCompleta())
        );

        System.out.println("\n=== FIN TEST PARTE 1 ===");
    }
}
