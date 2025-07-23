package koolfileindexer.db;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

class ArchivoTest {

    @Test
    void constructorPorDefecto_creaFiltroValido() {
        // Crear archivo con constructor por defecto (filtro)
        Archivo filtro = new Archivo();

        // Verificar que los getters devuelven null para los campos de filtro
        assertNull(filtro.getNombre());
        assertNull(filtro.getRutaCompleta());
        assertNull(filtro.getExtension());
        assertNull(filtro.getCategoria());
    }

    @Test
    void actualizarModelo_mantieneTodasLasPropiedades() {
        // Crear archivo con valores iniciales - usar ruta independiente del SO
        String rutaTest = "/ruta/test.txt";
        Archivo archivo = new Archivo("test.txt", 100, LocalDateTime.now(),
                rutaTest, "txt", "DOCUMENTO");
        archivo.setId(1L);

        // Agregar palabras clave
        Set<String> palabrasClave = new HashSet<>();
        palabrasClave.add("clave1");
        palabrasClave.add("clave2");
        archivo.setPalabrasClave(palabrasClave);

        // Modificar nombre usando el setter refactorizado
        archivo.setNombre("nuevo.txt");

        // Verificar que las propiedades se mantuvieron
        assertEquals(1L, archivo.getModelo().getId());
        assertEquals("nuevo.txt", archivo.getNombre());

        // Comprobar solo el nombre del archivo, no la ruta completa
        assertTrue(archivo.getRutaCompleta().endsWith("ruta/test.txt") ||
                archivo.getRutaCompleta().endsWith("ruta\\test.txt"),
                "La ruta debe terminar con 'ruta/test.txt' o 'ruta\\test.txt'");

        assertEquals("txt", archivo.getExtension());
        assertTrue(archivo.getModelo().getPalabrasClave().contains("clave1"));
        assertTrue(archivo.getModelo().getPalabrasClave().contains("clave2"));
    }

    @Test
    void gettersOptional_devuelvenValoresCorrectos() {
        // Usar ruta independiente del SO
        String rutaTest = "/ruta/test.txt";
        Archivo archivo = new Archivo("test.txt", 100, LocalDateTime.now(),
                rutaTest, "txt", "DOCUMENTO");

        // Verificar que los Optional están presentes
        assertTrue(archivo.getNombreOptional().isPresent());
        assertEquals("test.txt", archivo.getNombreOptional().get());
        assertTrue(archivo.getRutaCompletaOptional().isPresent());

        // Comprobar solo el final de la ruta
        String rutaObtenida = archivo.getRutaCompletaOptional().get();
        assertTrue(rutaObtenida.endsWith("ruta/test.txt") ||
                rutaObtenida.endsWith("ruta\\test.txt"),
                "La ruta debe terminar con 'ruta/test.txt' o 'ruta\\test.txt'");

        assertTrue(archivo.getExtensionOptional().isPresent());
        assertEquals("txt", archivo.getExtensionOptional().get());
        assertTrue(archivo.getCategoriaOptional().isPresent());
    }

    @Test
    void gettersOptional_filtro_devuelvenEmpty() {
        // Archivo filtro
        Archivo filtro = new Archivo();

        // Verificar que los Optional están vacíos
        assertFalse(filtro.getNombreOptional().isPresent());
        assertFalse(filtro.getRutaCompletaOptional().isPresent());
        assertFalse(filtro.getExtensionOptional().isPresent());
        assertFalse(filtro.getCategoriaOptional().isPresent());
    }
}