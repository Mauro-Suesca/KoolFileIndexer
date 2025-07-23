package koolfileindexer.modelo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestiona el ciclo de vida de las etiquetas:
 * - Eliminación de etiquetas huérfanas
 * - Creación garantizando unicidad
 */
public class GestorEtiquetas {
    private final List<Archivo> archivos;

    public GestorEtiquetas(Collection<Archivo> archivos) {
        this.archivos = new ArrayList<>(archivos);
    }

    /**
     * Elimina de todos los archivos cualquier etiqueta que no esté presente en
     * ningún Archivo.
     *
     * @return Set de etiquetas eliminadas
     */
    public Set<Etiqueta> eliminarHuérfanas() {
        // 1) Recolectar todas las etiquetas usadas
        Set<String> usadas = archivos.stream()
                .flatMap(a -> a.getEtiquetas().stream())
                .map(Etiqueta::getNombre)
                .collect(Collectors.toSet());

        // 2) Para cada archivo, quitar etiquetas cuyo nombre no esté en 'usadas'
        Set<Etiqueta> eliminadas = new HashSet<>();
        for (Archivo a : archivos) {
            List<Etiqueta> actuales = new ArrayList<>(a.getEtiquetas());
            for (Etiqueta e : actuales) {
                if (!usadas.contains(e.getNombre())) {
                    a.quitarEtiqueta(e);
                    eliminadas.add(e);
                }
            }
        }
        return eliminadas;
    }

    /**
     * Elimina etiquetas que no están en el conjunto de etiquetas válidas.
     *
     * @param etiquetasValidas Conjunto de nombres de etiquetas consideradas válidas
     * @return Set de etiquetas eliminadas
     */
    public Set<Etiqueta> eliminarHuerfanas(Set<String> etiquetasValidas) {
        Set<Etiqueta> eliminadas = new HashSet<>();
        for (Archivo a : archivos) {
            List<Etiqueta> actuales = new ArrayList<>(a.getEtiquetas());
            for (Etiqueta e : actuales) {
                if (!etiquetasValidas.contains(e.getNombre())) {
                    a.quitarEtiqueta(e);
                    eliminadas.add(e);
                }
            }
        }
        return eliminadas;
    }

    /**
     * Crea una nueva etiqueta con nombre único en todo el conjunto.
     * Si ya existe, devuelve la existente.
     */
    public Etiqueta crearUnica(String nombre) {
        if (nombre == null) {
            throw new IllegalArgumentException("nombre no puede ser null");
        }

        String normal = nombre.trim().toLowerCase();
        for (Archivo a : archivos) {
            for (Etiqueta e : a.getEtiquetas()) {
                if (e.getNombre().equals(normal)) {
                    return e;
                }
            }
        }
        return Etiqueta.crear(nombre);
    }
}
