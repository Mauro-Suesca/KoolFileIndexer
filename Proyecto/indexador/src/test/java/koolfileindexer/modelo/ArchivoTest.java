package koolfileindexer.modelo;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArchivoTest {

    @Test
    void equalsYHashCode_seBasanEnIdOEnRuta() {
        LocalDateTime now = LocalDateTime.now();
        Archivo a1 = new Archivo("f.txt", "/tmp/f.txt", "txt", 100, now, now);
        Archivo a2 = new Archivo("f.txt", "/tmp/f.txt", "txt", 100, now, now);

        // Sin ID: igualdad por ruta
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        // Ambos con mismo ID
        a1.setId(1L);
        a2.setId(1L);
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        // Diferente ID => no iguales
        a2.setId(2L);
        assertNotEquals(a1, a2);
    }

    @Test
    void asociarEtiquetaManual_validaYNoDuplica() {
        Archivo a = new Archivo("x", "/tmp/x", "ext", 0, LocalDateTime.now(), LocalDateTime.now());
        Etiqueta e = Etiqueta.crear("proyecto");
        a.asociarEtiquetaManual(e);
        assertTrue(a.getEtiquetas().contains(e));
        // segundo intento no duplica
        a.asociarEtiquetaManual(e);
        assertEquals(1, a.getEtiquetas().size());
    }

    @Test
    void asociarPalabraClaveManual_validaYNormaliza() {
        Archivo a = new Archivo("x", "/tmp/x", "ext", 0, LocalDateTime.now(), LocalDateTime.now());
        a.asociarPalabraClaveManual(" clave1 ");
        assertTrue(a.getPalabrasClave().contains("clave1"));
        // mayúsculas o espacios no duplican
        a.asociarPalabraClaveManual("Clave1");
        assertEquals(1, a.getPalabrasClave().size());
    }
}
