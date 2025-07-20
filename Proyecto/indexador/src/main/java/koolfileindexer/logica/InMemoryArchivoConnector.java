package logica;

import modelo.Archivo;
import modelo.ArchivoConnector;
import java.util.*;

public class InMemoryArchivoConnector implements ArchivoConnector {
    private final Map<Long, Archivo> store = new LinkedHashMap<>();
    private long nextId = 1L;

    @Override
    public Optional<Archivo> findByMetadata(long size, long creationTimeMillis, String extension) {
        return store.values().stream()
                .filter(a -> a.getTamanoBytes() == size &&
                        a.getFechaCreacion()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli() == creationTimeMillis
                        &&
                        a.getExtension().equals(extension))
                .findFirst();
    }

    @Override
    public Long insert(Archivo a) {
        long id = nextId++;
        a.setId(id);
        store.put(id, a);
        return id;
    }

    @Override
    public void update(Archivo a) {
        store.put(a.getId(), a);
    }

    public Collection<Archivo> getAll() {
        return Collections.unmodifiableCollection(store.values());
    }
}
