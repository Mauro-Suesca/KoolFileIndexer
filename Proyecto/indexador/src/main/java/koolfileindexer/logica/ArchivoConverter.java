package koolfileindexer.logica;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;

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
        return new ArchivoAdapter(
                modeloArchivo.getNombre(),
                modeloArchivo.getTamanoBytes(),
                modeloArchivo.getFechaModificacion(),
                modeloArchivo.getRutaCompleta(),
                modeloArchivo.getExtension(),
                modeloArchivo.getCategoria().name());
    }

    /**
     * Convierte un ResultSet de la base de datos a un Archivo del modelo
     */
    public static Archivo fromResultSet(ResultSet rs) throws SQLException {
        // Intentar obtener el nombre con diferentes formatos posibles
        String nombre = getStringWithAlternatives(rs, new String[] { "arc_nombre", "nombre", "name" });
        String rutaCompleta = getStringWithAlternatives(rs,
                new String[] { "arc_ruta_completa", "ruta_completa", "path" });
        String extension = getStringWithAlternatives(rs, new String[] { "ext_extension", "extension", "ext" });
        long tamanoBytes = getLongWithAlternatives(rs, new String[] { "arc_tamano_bytes", "tamano_bytes", "size" });

        // Para timestamps también manejamos alternativas
        LocalDateTime fechaCreacion = getTimestampWithAlternatives(rs,
                new String[] { "arc_fecha_creacion", "fecha_creacion", "created_at" });
        LocalDateTime fechaModificacion = getTimestampWithAlternatives(rs,
                new String[] { "arc_fecha_modificacion", "fecha_modificacion", "modified_at" });

        Archivo archivo = new Archivo(nombre, rutaCompleta, extension, tamanoBytes, fechaCreacion, fechaModificacion);

        // Intentar establecer ID si está disponible
        try {
            archivo.setId(getLongWithAlternatives(rs, new String[] { "id", "arc_id", "archivo_id" }));
        } catch (Exception e) {
            // ID podría no estar disponible, lo cual es aceptable
            System.out.println("Nota: No se encontró ID para el archivo " + nombre);
        }

        return archivo;
    }

    /**
     * Convierte un objeto DB a un objeto modelo
     */
    public static koolfileindexer.modelo.Archivo toModelArchivo(koolfileindexer.db.Archivo dbArchivo) {
        // Si el dbArchivo fue creado con nuestro adaptador, podemos obtener el modelo
        // directamente
        if (dbArchivo instanceof ArchivoAdapter) {
            // Procesamiento especial para ArchivoAdapter si es necesario
        }

        // Creamos uno nuevo
        koolfileindexer.modelo.Archivo modeloArchivo = new koolfileindexer.modelo.Archivo(
                dbArchivo.getNombre(),
                dbArchivo.getRutaCompleta(),
                dbArchivo.getExtension(),
                dbArchivo.getTamanoBytes(),
                LocalDateTime.now(),
                dbArchivo.getFechaModificacion());

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

    // Métodos auxiliares para manejar nombres de columna alternativos
    public static String getStringWithAlternatives(ResultSet rs, String[] columnNames) throws SQLException {
        for (String colName : columnNames) {
            try {
                return rs.getString(colName);
            } catch (SQLException e) {
                // Intentar con el siguiente nombre
            }
        }
        throw new SQLException(
                "No se encontró ninguna columna entre las alternativas: " + String.join(", ", columnNames));
    }

    private static long getLongWithAlternatives(ResultSet rs, String[] columnNames) throws SQLException {
        for (String colName : columnNames) {
            try {
                return rs.getLong(colName);
            } catch (SQLException e) {
                // Intentar con el siguiente nombre
            }
        }
        throw new SQLException(
                "No se encontró ninguna columna entre las alternativas: " + String.join(", ", columnNames));
    }

    private static LocalDateTime getTimestampWithAlternatives(ResultSet rs, String[] columnNames) throws SQLException {
        for (String colName : columnNames) {
            try {
                java.sql.Timestamp ts = rs.getTimestamp(colName);
                if (ts != null) {
                    return ts.toLocalDateTime();
                }
            } catch (SQLException e) {
                // Intentar con el siguiente nombre
            }
        }
        throw new SQLException(
                "No se encontró ninguna columna entre las alternativas: " + String.join(", ", columnNames));
    }
}