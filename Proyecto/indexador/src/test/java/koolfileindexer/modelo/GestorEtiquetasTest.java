package koolfileindexer.modelo;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GestorEtiquetasTest {

    @Test
    void eliminarHuerfanas_conListaValida_eliminaEtiquetasNoIncluidas() {
        // Crear un archivo con etiquetas
        LocalDateTime now = LocalDateTime.now();
        Archivo a = new Archivo("file.txt", "/tmp/file.txt", "txt", 100, now, now);
        
        // Agregar tres etiquetas
        a.agregarEtiqueta(Etiqueta.crear("tag1"));
        a.agregarEtiqueta(Etiqueta.crear("tag2"));
        a.agregarEtiqueta(Etiqueta.crear("tag3"));
        
        // Crear conjunto con solo dos etiquetas válidas
        Set<String> etiquetasValidas = new HashSet<>();
        etiquetasValidas.add("tag1");
        etiquetasValidas.add("tag3");
        
        // Crear gestor de etiquetas
        List<Archivo> archivos = Collections.singletonList(a);
        GestorEtiquetas gestor = new GestorEtiquetas(archivos);
        
        // Eliminar etiquetas no válidas
        Set<Etiqueta> eliminadas = gestor.eliminarHuerfanas(etiquetasValidas);
        
        // Verificar que se eliminó la etiqueta correcta
        assertEquals(1, eliminadas.size());
        assertTrue(eliminadas.stream().anyMatch(e -> e.getNombre().equals("tag2")));
        
        // Verificar que el archivo tiene solo las etiquetas válidas
        Set<String> etiquetasRestantes = a.getEtiquetas().stream()
                .map(Etiqueta::getNombre)
                .collect(Collectors.toSet());
        assertEquals(2, etiquetasRestantes.size());
        assertTrue(etiquetasRestantes.contains("tag1"));
        assertTrue(etiquetasRestantes.contains("tag3"));
        assertFalse(etiquetasRestantes.contains("tag2"));
    }
    
    @Test
    void crearUnica_conNombreNulo_lanzaExcepcion() {
        // Crear gestor con lista vacía
        GestorEtiquetas gestor = new GestorEtiquetas(Collections.emptyList());
        
        // Verificar que se lanza excepción al pasar nombre nulo
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gestor.crearUnica(null));
        
        // Verificar mensaje de error
        assertTrue(exception.getMessage().contains("nombre no puede ser null"));
    }
}