package koolfileindexer.logica;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilitario para convertir entre objetos Archivo de diferentes paquetes
 */
public class ArchivoConverter {
    
    /**
     * Convierte un objeto del modelo a un objeto para la capa DB
     */
    public static koolfileindexer.db.Archivo toDbArchivo(koolfileindexer.modelo.Archivo modeloArchivo) {
        return new koolfileindexer.db.Archivo(
            modeloArchivo.getNombre(),
            modeloArchivo.getTamanoBytes(),
            modeloArchivo.getFechaModificacion(),
            modeloArchivo.getRutaCompleta(),
            modeloArchivo.getExtension(),
            modeloArchivo.getCategoria().name()
        );
    }
    
    /**
     * Convierte un objeto de la capa DB a un objeto del modelo
     * (Nota: en realidad solo devuelve el modelo interno)
     */
    public static koolfileindexer.modelo.Archivo toModelArchivo(koolfileindexer.db.Archivo dbArchivo) {
        // Si el dbArchivo fue creado con nuestro adaptador, podemos obtener el modelo directamente
        if (dbArchivo instanceof koolfileindexer.db.Archivo) {
            return ((koolfileindexer.db.Archivo) dbArchivo).getModelo();
        }
        
        // Si no, creamos uno nuevo
        koolfileindexer.modelo.Archivo modeloArchivo = new koolfileindexer.modelo.Archivo(
            dbArchivo.getNombre(),
            dbArchivo.getRutaCompleta(),
            dbArchivo.getExtension(),
            dbArchivo.getTamanoBytes(),
            LocalDateTime.now(),
            dbArchivo.getFechaModificacion()
        );
        
        // Copiar palabras clave
        Set<String> palabrasClave = dbArchivo.getPalabrasClave();
        if (palabrasClave != null) {
            for (String palabra : palabrasClave) {
                try {
                    modeloArchivo.agregarPalabraClave(palabra);
                } catch (IllegalArgumentException e) {
                    // Ignorar palabras clave inválidas
                }
            }
        }
        
        return modeloArchivo;
    }
}