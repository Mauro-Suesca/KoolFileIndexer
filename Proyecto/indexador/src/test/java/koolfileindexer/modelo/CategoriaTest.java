package koolfileindexer.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaTest {

    @Test
    void clasificarPorExtensionReconoceImagenYDocumento() {
        Archivo img = new Archivo("pic.png", "/any/pic.png", "png", 0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        Archivo doc = new Archivo("doc.pdf", "/any/doc.pdf", "pdf", 0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        assertEquals(Categoria.IMAGEN, Categoria.clasificar(img));
        assertEquals(Categoria.DOCUMENTO, Categoria.clasificar(doc));
    }

    @Test
    void extensionDesconocidaDevuelveOtro() {
        Archivo desconocido = new Archivo("foo.xyz", "/tmp/foo.xyz", "xyz", 0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        assertEquals(Categoria.OTRO, Categoria.clasificar(desconocido));
    }

    @Test
    void clasificar_reconoceArchivosComprimidos() {
        // Verificar que se reconocen los tipos de archivos comprimidos
        Archivo zip = new Archivo("archivo.zip", "/tmp/archivo.zip", "zip", 1000,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        Archivo rar = new Archivo("archivo.rar", "/tmp/archivo.rar", "rar", 1000,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());

        assertEquals(Categoria.COMPRIMIDO, Categoria.clasificar(zip));
        assertEquals(Categoria.COMPRIMIDO, Categoria.clasificar(rar));
    }
}
