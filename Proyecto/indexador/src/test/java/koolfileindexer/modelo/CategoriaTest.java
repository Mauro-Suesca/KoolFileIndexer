package koolfileindexer.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaTest {

    @Test
    void clasificarPorExtensionReconoceImagenYDocumento() {
        Archivo img = new Archivo("pic.png","/any/pic.png","png",0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        Archivo doc = new Archivo("doc.pdf","/any/doc.pdf","pdf",0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        assertEquals(Categoria.IMAGEN, Categoria.clasificar(img));
        assertEquals(Categoria.DOCUMENTO, Categoria.clasificar(doc));
    }

    @Test
    void extensionDesconocidaDevuelveSinCategoria() {
        Archivo raro = new Archivo("foo.xyz","/any/foo.xyz","xyz",0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        assertEquals(Categoria.SIN_CATEGORIA, Categoria.clasificar(raro));
    }
}
