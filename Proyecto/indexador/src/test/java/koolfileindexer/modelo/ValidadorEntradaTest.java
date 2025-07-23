package koolfileindexer.modelo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorEntradaTest {

    @Test
    void normalizar_procesaCorrectamenteInputs() {
        // Normalizar texto con espacios y mayúsculas
        assertEquals("texto", ValidadorEntrada.normalizar("  Texto  "));
        
        // Manejo de null
        assertNull(ValidadorEntrada.normalizar(null));
        
        // Cadena vacía después de trim
        assertEquals("", ValidadorEntrada.normalizar("   "));
    }
    
    @Test
    void esNombreArchivoValido_validaLongitudYCaracteres() {
        // Nombres válidos
        assertTrue(ValidadorEntrada.esNombreArchivoValido("archivo.txt"));
        assertTrue(ValidadorEntrada.esNombreArchivoValido("nombre con espacios"));
        
        // Nombres inválidos
        assertFalse(ValidadorEntrada.esNombreArchivoValido("")); // vacío
        assertFalse(ValidadorEntrada.esNombreArchivoValido("   ")); // solo espacios
        assertFalse(ValidadorEntrada.esNombreArchivoValido("a".repeat(51))); // muy largo
        assertFalse(ValidadorEntrada.esNombreArchivoValido("nombre?invalido")); // caracter inválido
    }
}