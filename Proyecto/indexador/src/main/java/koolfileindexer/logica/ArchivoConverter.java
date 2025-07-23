package koolfileindexer.logica;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
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
        // Crear el objeto usando ArchivoAdapter
        ArchivoAdapter dbArchivo = new ArchivoAdapter(
                modeloArchivo.getNombre(),
                modeloArchivo.getTamanoBytes(),
                modeloArchivo.getFechaModificacion(),
                modeloArchivo.getRutaCompleta(),
                modeloArchivo.getExtension(),
                modeloArchivo.getCategoria().name());

        // No transferir palabras clave o etiquetas si están vacías
        if (modeloArchivo.getPalabrasClave() != null && !modeloArchivo.getPalabrasClave().isEmpty()) {
            Set<String> palabrasClave = new HashSet<>(modeloArchivo.getPalabrasClave());
            dbArchivo.setPalabrasClave(palabrasClave);
        }

        return dbArchivo;
    }

    /**
     * Convierte un ResultSet de la base de datos a un Archivo del modelo
     */
    public static Archivo fromResultSet(ResultSet rs) throws SQLException {
        // Priorizar los nombres de columnas usados en las funciones SQL
        String nombre = getStringWithAlternatives(rs, new String[] { "nombre", "arc_nombre", "name" });
        String rutaCompleta = getStringWithAlternatives(rs,
                new String[] { "path", "arc_path", "ruta_completa", "arc_ruta_completa" });
        String extension = getStringWithAlternatives(rs, new String[] { "extension", "ext_extension", "ext" });
        long tamanoBytes = getLongWithAlternatives(rs, new String[] { "tamano", "arc_tamano", "size", "tamano_bytes" });

        // Para timestamps también manejamos alternativas
        LocalDateTime fechaModificacion = getTimestampWithAlternatives(rs,
                new String[] { "fecha_modificacion", "arc_fecha_modificacion", "modified_at" });

        // Intentar obtener fecha de creación, si no está disponible usar la fecha de
        // modificación
        LocalDateTime fechaCreacion;
        try {
            fechaCreacion = getTimestampWithAlternatives(rs,
                    new String[] { "fecha_creacion", "arc_fecha_creacion", "created_at" });
        } catch (SQLException e) {
            // Si no hay fecha de creación, usar la fecha de modificación
            fechaCreacion = fechaModificacion;
        }

        Archivo archivo = new Archivo(nombre, rutaCompleta, extension, tamanoBytes, fechaCreacion, fechaModificacion);

        // Intentar establecer ID si está disponible
        try {
            archivo.setId(getLongWithAlternatives(rs, new String[] { "id", "arc_id", "archivo_id" }));
        } catch (Exception e) {
            // ID podría no estar disponible
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
        // Agregar las columnas de los procedimientos almacenados
        if (columnNames.length > 0 && columnNames[0].contains("ruta")) {
            columnNames = new String[] { "path", "arc_path", "arc_ruta_completa", "ruta_completa" };
        } else if (columnNames.length > 0 && columnNames[0].contains("nombre")) {
            columnNames = new String[] { "nombre", "arc_nombre", "name" };
        } else if (columnNames.length > 0 && columnNames[0].contains("extension")) {
            columnNames = new String[] { "extension", "ext_extension", "ext" };
        } else if (columnNames.length > 0 && columnNames[0].contains("categoria")) {
            columnNames = new String[] { "categoria", "cat_nombre", "category" };
        }

        for (String name : columnNames) {
            try {
                return rs.getString(name);
            } catch (SQLException e) {
                // Intentar con el siguiente nombre
            }
        }

        throw new SQLException("No se encontró ninguna columna entre las alternativas: " +
                String.join(", ", columnNames));
    }

    /**
     * Obtiene un valor Long de un ResultSet probando varios nombres de columna
     * posibles
     */
    public static long getLongWithAlternatives(ResultSet rs, String[] alternatives) throws SQLException {
        // Añadir los nombres que realmente tienen las columnas en las funciones SQL
        String[] updatedAlternatives = new String[] {
                "tamano", // Nombre en el resultado de la función SQL
                "arc_tamano", // Nombre real en la tabla
                "arc_tamano_bytes", // Nombre anterior que se buscaba en el código
                "tamano_bytes", // Nombre alternativo
                "size" // Nombre en inglés
        };

        // Si se proporcionaron alternativas adicionales, combinarlas
        if (alternatives != null && alternatives.length > 0) {
            String[] combined = new String[updatedAlternatives.length + alternatives.length];
            System.arraycopy(updatedAlternatives, 0, combined, 0, updatedAlternatives.length);
            System.arraycopy(alternatives, 0, combined, updatedAlternatives.length, alternatives.length);
            updatedAlternatives = combined;
        }

        // Intentar cada nombre de columna
        for (String alt : updatedAlternatives) {
            try {
                return rs.getLong(alt);
            } catch (SQLException e) {
                // Intentar con la siguiente alternativa
            }
        }

        // Si no se encontró ninguna columna, lanzar excepción
        throw new SQLException("No se encontró ninguna columna entre las alternativas: " +
                String.join(", ", updatedAlternatives));
    }

    public static LocalDateTime getTimestampWithAlternatives(ResultSet rs, String[] alternatives) throws SQLException {
        // Añadir los nombres que realmente tienen las columnas en las funciones SQL
        String[] updatedAlternatives = new String[] {
                "fecha_modificacion", // Nombre en el resultado de la función SQL
                "arc_fecha_modificacion", // Nombre en la tabla
                "fecha_creacion", // Otro nombre posible
                "arc_fecha_creacion", // Otro nombre posible
                "modified_at", // Nombre en inglés
                "created_at" // Nombre en inglés
        };

        // Si se proporcionaron alternativas adicionales, combinarlas
        if (alternatives != null && alternatives.length > 0) {
            String[] combined = new String[updatedAlternatives.length + alternatives.length];
            System.arraycopy(updatedAlternatives, 0, combined, 0, updatedAlternatives.length);
            System.arraycopy(alternatives, 0, combined, updatedAlternatives.length, alternatives.length);
            updatedAlternatives = combined;
        }

        // Intentar cada nombre de columna
        for (String alt : updatedAlternatives) {
            try {
                java.sql.Timestamp ts = rs.getTimestamp(alt);
                if (ts != null) {
                    return ts.toLocalDateTime();
                }
            } catch (SQLException e) {
                // Intentar con la siguiente alternativa
            }
        }

        // Si no se encontró ninguna columna, lanzar excepción
        throw new SQLException("No se encontró ninguna columna entre las alternativas: " +
                String.join(", ", updatedAlternatives));
    }

    public static class ArchivoAdapter extends koolfileindexer.db.Archivo {
        private String nombre;
        private String rutaCompleta;
        private String extension;

        public ArchivoAdapter(String nombre, long tamanoBytes, LocalDateTime fechaModificacion, String rutaCompleta,
                String extension, String categoria) {
            super(nombre, tamanoBytes, fechaModificacion, rutaCompleta, extension, categoria);
            this.nombre = nombre;
            this.rutaCompleta = rutaCompleta;
            this.extension = extension;
        }

        public ArchivoAdapter() {
            super("irrelevant", 0L, LocalDateTime.now(), "irrelevant", "irrelevant", "OTRO");
            this.nombre = null;
            this.rutaCompleta = null;
            this.extension = null;
        }

        @Override
        public String getNombre() {
            return nombre;
        }

        @Override
        public String getRutaCompleta() {
            return rutaCompleta;
        }

        @Override
        public String getExtension() {
            return extension;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
    }
}