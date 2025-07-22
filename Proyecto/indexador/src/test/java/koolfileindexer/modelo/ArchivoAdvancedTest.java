package koolfileindexer.modelo;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArchivoAdvancedTest {

    @Test
    void modificarPalabraClave_noExistente_retornaFalse() {
        LocalDateTime now = LocalDateTime.now();
        Archivo a = new Archivo("f.txt", "/tmp/f.txt", "txt", 100, now, now);
        // intentar modificar una clave que no existe
        boolean resultado = a.modificarPalabraClave("noExiste", "nueva");
        assertFalse(resultado, "debería devolver false si la palabra original no existía");
        assertTrue(a.getPalabrasClave().isEmpty(), "no debe haber palabras clave");
    }

    @Test
    void eliminarPalabraClave_actualizaFechaModificacion() throws Exception {
        LocalDateTime cre = LocalDateTime.now().minusDays(1);
        LocalDateTime mod = LocalDateTime.now().minusDays(1);
        Archivo a = new Archivo("f.txt", "/tmp/f.txt", "txt", 100, cre, mod);
        a.agregarPalabraClave("clave1");
        LocalDateTime antes = a.getFechaModificacion();

        // Duerme unos milisegundos para que el timestamp cambie
        Thread.sleep(10);
        a.eliminarPalabraClave("clave1");

        assertTrue(a.getFechaModificacion().isAfter(antes),
                "la fecha de modificación debe actualizarse al eliminar clave");
    }

    @Test
    void agregarPalabraClaveVacia_lanzaExcepcion() {
        LocalDateTime now = LocalDateTime.now();
        Archivo a = new Archivo("f.txt", "/tmp/f.txt", "txt", 100, now, now);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> a.agregarPalabraClave(""));

        assertTrue(exception.getMessage().contains("inválida"),
                "El mensaje debe indicar que la palabra clave es inválida");
    }

    @Test
    void agregarPalabraClaveSoloEspacios_lanzaExcepcion() {
        LocalDateTime now = LocalDateTime.now();
        Archivo a = new Archivo("f.txt", "/tmp/f.txt", "txt", 100, now, now);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> a.agregarPalabraClave("   "));

        assertTrue(exception.getMessage().contains("inválida"),
                "El mensaje debe indicar que la palabra clave es inválida");
    }
}
