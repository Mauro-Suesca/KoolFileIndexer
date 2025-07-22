package koolfileindexer.logica;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.Categoria;

/**
 * Utilitario para convertir entre objetos Archivo de diferentes paquetes
 */
public class ArchivoConverter {

    /**
     * Convierte un objeto del modelo a un objeto para la capa DB
     */
    public static koolfileindexer.db.Archivo toDbArchivo(koolfileindexer.modelo.Archivo modeloArchivo) {
        // Crear una nueva instancia de db.Archivo usando el constructor
        return new koolfileindexer.db.Archivo(
                modeloArchivo.getNombre(),
                modeloArchivo.getTamanoBytes(),
                modeloArchivo.getFechaModificacion(),
                modeloArchivo.getRutaCompleta(),
                modeloArchivo.getExtension(),
                modeloArchivo.getCategoria().getNombre());
    }

    /**
     * Convierte un ResultSet de la base de datos a un Archivo del modelo
     */
    public static Archivo fromResultSet(ResultSet rs) throws SQLException {
        String nombre = rs.getString("arc_nombre");
        String rutaCompleta = rs.getString("arc_ruta_completa");
        String extension = rs.getString("ext_extension");
        long tamanoBytes = rs.getLong("arc_tamano_bytes");
        LocalDateTime fechaCreacion = rs.getTimestamp("arc_fecha_creacion").toLocalDateTime();
        LocalDateTime fechaModificacion = rs.getTimestamp("arc_fecha_modificacion").toLocalDateTime();

        Archivo archivo = new Archivo(nombre, rutaCompleta, extension, tamanoBytes, fechaCreacion, fechaModificacion);
        archivo.setId(rs.getLong("id"));

        // La categoría se puede determinar automáticamente después
        return archivo;
    }
}