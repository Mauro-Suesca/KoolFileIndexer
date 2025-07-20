package koolfileindexer.modelo;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EtiquetaTest {

    @Test
    void crear_normalizaMinusculasYEliminaEspacios() {
        Etiqueta e1 = Etiqueta.crear(" Proyecto ");
        Etiqueta e2 = Etiqueta.crear("proyecto");
        Etiqueta e3 = Etiqueta.crear("PROYECTO");

        // Deben ser todas iguales
        assertEquals(e1, e2);
        assertEquals(e1, e3);

        // hashCode también consistente
        assertEquals(e1.hashCode(), e2.hashCode());
        assertEquals(e1.hashCode(), e3.hashCode());
    }

    @Test
    void set_noPermiteDuplicados_enUnConjunto() {
        Etiqueta e1 = Etiqueta.crear("MiEtiqueta");
        Etiqueta e2 = Etiqueta.crear(" mietiqueta "); // mismo contenido, distinto case/spaces

        Set<Etiqueta> set = new HashSet<>();
        set.add(e1);
        set.add(e2);

        // Solo debe quedar una
        assertEquals(1, set.size());
    }
}
