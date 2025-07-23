package koolfileindexer.logica;

import java.util.List;
import koolfileindexer.db.Etiqueta;

/**
 * Adaptador para evitar el problema de IndexOutOfBoundsException al usar etiquetas vacías.
 * Esta clase extiende Archivo pero sobrescribe getEtiquetas() para devolver null en vez de una lista vacía.
 */
public class ArchivoAdapter extends koolfileindexer.db.Archivo {
    
    /**
     * Constructor por defecto para búsquedas seguras.
     */
    public ArchivoAdapter() {
        super();
    }
    
    /**
     * Sobrescribe getEtiquetas para devolver null en vez de una lista vacía.
     * Esto previene el IndexOutOfBoundsException en ConectorBasedeDatos.ejecutarConsultaSQLParaBuscarArchivos()
     */
    @Override
    public List<Etiqueta> getEtiquetas() {
        return null;
    }
}