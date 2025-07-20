package koolfileindexer.modelo;

import java.util.Optional;

public interface ArchivoConnector {
    /** Busca un Archivo por tamaño, timestamp de creación y extensión */
    Optional<Archivo> findByMetadata(long size, long creationTimeMillis, String extension);

    /** Inserta un nuevo Archivo y devuelve su ID asignado */
    Long insert(Archivo a);

    /** Actualiza un Archivo existente */
    void update(Archivo a);
}
