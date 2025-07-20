package logica;

import modelo.Archivo;
import modelo.Categoria;
import modelo.Etiqueta;
import modelo.ValidadorEntrada;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Clase demo para verificar manualmente las funcionalidades del dominio:
 * — Normalización y validación de Archivo
 * — Etiquetas y palabras clave
 * — Igualdad de rutas y categorías
 */
public class DemoDominio {
    public static void main(String[] args) {
        System.out.println("=== DEMO DOMINIO: PARTE 1 ===\n");

        // 1) Crear Archivo y probar normalización de nombre y ruta
        Archivo a1 = new Archivo(
            "  MiDoc.TXT  ",
            "C:/Temp/../TempCarpeta/archivo.TXT",
            "TXT",
            2048,
            LocalDateTime.of(2025, 7, 1, 10, 0),
            LocalDateTime.of(2025, 7, 1, 10, 0)
        );
        System.out.println("Archivo creado:");
        System.out.println(a1);
        System.out.println("· Ruta normalizada:   " + a1.getRutaCompleta());
        System.out.println("· Nombre trimmed:     '" + a1.getNombre() + "'");
        System.out.println("· Categoría (auto):   " + a1.getCategoria());

        // 2) Ocultos y válidos
        Archivo oculto = new Archivo(
            ".oculto",
            "C:/ruta/oculto.txt",
            "txt",
            100,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        System.out.println("\n· esOculto('.oculto'): " + oculto.esOculto());
        System.out.println("· esValido('.oculto'): " + oculto.esValido());
        Archivo invalido = new Archivo(
            "   ",
            "C:/ruta/blank.txt",
            "txt",
            100,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        System.out.println("· esValido('   '):      " + invalido.esValido());

        // 3) Etiquetas
        System.out.println("\n== Etiquetas ==");
        Etiqueta t1 = Etiqueta.crear(" proyecto ");
        Etiqueta t2 = Etiqueta.crear("tarea_urgente");
        a1.agregarEtiqueta(t1);
        a1.agregarEtiqueta(t2);
        a1.agregarEtiqueta(t1); // duplicado
        System.out.println("Tras agregar t1, t2, t1: " + a1.getEtiquetas());
        t1.setNombre("informe 2025");
        System.out.println("Tras renombrar t1:        " + a1.getEtiquetas());
        a1.quitarEtiqueta(t2);
        System.out.println("Tras quitar t2:          " + a1.getEtiquetas());

        // etiquetas inválidas
        String[] etiquetasInvalidas = { "", " ", "mal  formato", "mal@formato" };
        for (String s : etiquetasInvalidas) {
            probarValidacionEtiqueta(s);
        }

        // 4) Palabras clave
        System.out.println("\n== Palabras clave ==");
        a1.agregarPalabraClave("clave1");
        a1.agregarPalabraClave("clave1"); // duplicado
        a1.agregarPalabraClave("clave2");
        System.out.println("Tras agregar:            " + a1.getPalabrasClave());
        a1.modificarPalabraClave("clave1", "nuevaClave");
        System.out.println("Tras modificar clave1:   " + a1.getPalabrasClave());
        a1.eliminarPalabraClave("clave2");
        System.out.println("Tras eliminar clave2:    " + a1.getPalabrasClave());

        // palabras inválidas
        String[] palabrasInvalidas = { "", "mal formato", "mal@formato" };
        for (String s : palabrasInvalidas) {
            probarValidacionPalabraClave(s);
        }

        // 5) Fecha modificación
        System.out.println("\nFecha modif tras cambios: " +
            a1.getFechaModificacion().truncatedTo(ChronoUnit.SECONDS));

        // 6) Comparación de rutas case-insensitive
        Archivo a2 = new Archivo(
            "archivo",
            a1.getRutaCompleta().toUpperCase(),
            "TXT",
            2048,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        System.out.println("\nRutas iguales ignoreCase: " +
            a1.getRutaCompleta().equalsIgnoreCase(a2.getRutaCompleta()));

        // 7) Igualdad de Categoría
        Categoria c1 = a1.getCategoria();
        Categoria c2 = Categoria.DOCUMENTO;
        System.out.println("\nCategoria.equals:        " +
            c1.equals(c2));

        System.out.println("\n=== FIN DEMO DOMINIO ===");
    }

    /** Helper para probar validación de etiquetas. */
    private static void probarValidacionEtiqueta(String s) {
        System.out.printf(
            "Probar etiqueta '%s': %s%n",
            s,
            ValidadorEntrada.esEtiquetaValida(s)
                ? "OK (pero debería FALLAR)"
                : "FALLA correctamente"
        );
    }

    /** Helper para probar validación de palabras clave. */
    private static void probarValidacionPalabraClave(String s) {
        System.out.printf(
            "Probar palabra  '%s': %s%n",
            s,
            ValidadorEntrada.esPalabraClaveValida(s)
                ? "OK (pero debería FALLAR)"
                : "FALLA correctamente"
        );
    }
}
