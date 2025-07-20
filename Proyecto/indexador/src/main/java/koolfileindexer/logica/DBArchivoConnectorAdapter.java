package koolfileindexer.logica;

import koolfileindexer.db.ConectorBasedeDatos;
import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.ArchivoConnector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public class DBArchivoConnectorAdapter implements ArchivoConnector {
    private final ConectorBasedeDatos db = ConectorBasedeDatos.obtenerInstancia();

    @Override
    public Optional<Archivo> findByMetadata(long size, long creationTimeMillis, String extension) {
        // Construyo un filtro ligero:
        Archivo filtro = new Archivo(
            null,
            null,
            extension,
            size,
            LocalDateTime.ofInstant(Instant.ofEpochMilli(creationTimeMillis), ZoneId.systemDefault()),
            null
        );

        try {
            // Uso el SP de "misma metadata exacta" para localizar el registro
            ResultSet rs = db.buscarArchivosPorFiltroVariasPalabrasClaveMismoArchivo(filtro, size, size);
            if (rs != null && rs.next()) {
                // Mapeo columnas a un objeto Archivo
                Archivo encontrado = new Archivo(
                    rs.getString("nombre"),
                    rs.getString("ruta_completa"),
                    rs.getString("extension"),
                    rs.getLong("tamano_bytes"),
                    rs.getDate("fecha_creacion").toLocalDate().atStartOfDay(),
                    rs.getTimestamp("fecha_modificacion").toLocalDateTime()
                );
                encontrado.setId(rs.getLong("id")); // si tu BD devuelve un id
                return Optional.of(encontrado);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Long insert(Archivo a) {
        // insertar y luego recuperar el id
        if (db.crearArchivo(a)) {
            // Si SP no devuelve ID, haz una consulta extra:
            return findByMetadata(a.getTamanoBytes(),
                                  a.getFechaCreacion().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                  a.getExtension())
                   .map(Archivo::getId)
                   .orElse(null);
        }
        return null;
    }

    @Override
    public void update(Archivo a) {
        // Dependiendo de qué cambió, invoca el SP adecuado:
        db.actualizarNombreArchivo(a, /*viejoNombre*/ a.getNombreAnterior());
        db.actualizarTamanoFechaModificacionArchivo(a);
        db.actualizarCategoriaArchivo(a);
    }
}
