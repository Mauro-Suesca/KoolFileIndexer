package koolfileindexer.db;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptador para usar ArchivoModelo desde el paquete DB
 */
public class Archivo {
    // Usamos el nombre completo en lugar de "as"
    private koolfileindexer.modelo.Archivo modelo;

    // Añadir constante para el valor de filtro
    private static final String FILTER_VALUE = "filtro_temp";

    public Archivo(String nombre, long tamanoBytes, LocalDateTime fechaModificacion,
            String rutaCompleta, String extension, String categoria) {
        // Crear el objeto modelo
        this.modelo = new koolfileindexer.modelo.Archivo(
                nombre,
                rutaCompleta,
                extension,
                tamanoBytes,
                LocalDateTime.now(), // fecha creación
                fechaModificacion // fecha modificación
        );

        // Asignar categoría correctamente
        try {
            // Intenta usar el valor del enum
            koolfileindexer.modelo.Categoria cat = koolfileindexer.modelo.Categoria.valueOf(categoria.toUpperCase());
            this.modelo.asignarCategoria(cat);
        } catch (IllegalArgumentException e) {
            // Si falla, usa OTRO
            this.modelo.asignarCategoria(koolfileindexer.modelo.Categoria.OTRO);
        }
    }

    // Constructor por defecto modificado para filtros
    public Archivo() {
        this.modelo = new koolfileindexer.modelo.Archivo(
                "filtro_temp", // Usar texto específico en lugar de espacio
                "filtro_temp",
                "filtro_temp",
                0,
                LocalDateTime.now(),
                LocalDateTime.now());

        // No asignamos categoría para que no filtre por categoría
    }

    // Getters para ConectorBaseDatos
    public String getNombre() {
        String nombre = modelo.getNombre();
        return "filtro_temp".equals(nombre) ? null : nombre;
    }

    public long getTamanoBytes() {
        return modelo.getTamanoBytes();
    }

    public LocalDateTime getFechaModificacion() {
        return modelo.getFechaModificacion();
    }

    public String getRutaCompleta() {
        String ruta = modelo.getRutaCompleta();
        return "filtro_temp".equals(ruta) ? null : ruta;
    }

    public String getExtension() {
        String ext = modelo.getExtension();
        return "filtro_temp".equals(ext) ? null : ext;
    }

    public Categoria getCategoria() {
        // si es la instancia de filtro temporal, devolvemos null:
        if ("filtro_temp".equals(modelo.getExtension())
                && "filtro_temp".equals(modelo.getNombre())
                && "filtro_temp".equals(modelo.getRutaCompleta())) {
            return null;
        }
        // en caso contrario, devolvemos la categoría real:
        return new Categoria(modelo.getCategoria().name());
    }

    public List<Etiqueta> getEtiquetas() {
        return Collections.emptyList(); // Nunca devuelve null
    }

    public Set<String> getPalabrasClave() {
        return modelo.getPalabrasClave();
    }

    public void setPalabrasClave(Set<String> palabrasClave) {
        // Clone the set to avoid modifying an unmodifiable collection
        Set<String> palabrasClaveModificables = new HashSet<>();
        if (palabrasClave != null) {
            palabrasClaveModificables.addAll(palabrasClave);
        }

        // Clear existing and add new
        Set<String> existentes = modelo.getPalabrasClave();
        for (String palabra : existentes) {
            modelo.eliminarPalabraClave(palabra);
        }

        for (String palabra : palabrasClaveModificables) {
            modelo.agregarPalabraClave(palabra);
        }
    }

    // Método para acceder directamente al modelo
    public koolfileindexer.modelo.Archivo getModelo() {
        return modelo;
    }

    // Añadir estos setters que faltan
    public void setId(Long id) {
        if (modelo != null) {
            modelo.setId(id);
        }
    }

    public void setNombre(String nombre) {
        if (this.modelo != null) {
            // Creamos un nuevo modelo copiando los valores existentes pero con el nuevo
            // nombre
            koolfileindexer.modelo.Archivo nuevoModelo = new koolfileindexer.modelo.Archivo(
                    nombre,
                    this.modelo.getRutaCompleta(),
                    this.modelo.getExtension(),
                    this.modelo.getTamanoBytes(),
                    this.modelo.getFechaCreacion(),
                    this.modelo.getFechaModificacion());

            // Copiamos otros datos importantes
            if (this.modelo.getCategoria() != null) {
                nuevoModelo.asignarCategoria(this.modelo.getCategoria());
            }

            // Copiamos palabras clave
            for (String palabraClave : this.modelo.getPalabrasClave()) {
                nuevoModelo.agregarPalabraClave(palabraClave);
            }

            // Reemplazamos el modelo
            this.modelo = nuevoModelo;
        }
    }

    public void setRutaCompleta(String rutaCompleta) {
        if (this.modelo != null) {
            // Creamos un nuevo modelo copiando los valores existentes pero con la nueva
            // ruta
            koolfileindexer.modelo.Archivo nuevoModelo = new koolfileindexer.modelo.Archivo(
                    this.modelo.getNombre(),
                    rutaCompleta,
                    this.modelo.getExtension(),
                    this.modelo.getTamanoBytes(),
                    this.modelo.getFechaCreacion(),
                    this.modelo.getFechaModificacion());

            // Copiamos otros datos importantes
            if (this.modelo.getCategoria() != null) {
                nuevoModelo.asignarCategoria(this.modelo.getCategoria());
            }

            // Copiamos palabras clave
            for (String palabraClave : this.modelo.getPalabrasClave()) {
                nuevoModelo.agregarPalabraClave(palabraClave);
            }

            // Reemplazamos el modelo
            this.modelo = nuevoModelo;
        }
    }

    public void setExtension(String extension) {
        if (this.modelo != null) {
            // Creamos un nuevo modelo copiando los valores existentes pero con la nueva
            // extensión
            koolfileindexer.modelo.Archivo nuevoModelo = new koolfileindexer.modelo.Archivo(
                    this.modelo.getNombre(),
                    this.modelo.getRutaCompleta(),
                    extension,
                    this.modelo.getTamanoBytes(),
                    this.modelo.getFechaCreacion(),
                    this.modelo.getFechaModificacion());

            // Copiamos otros datos importantes
            if (this.modelo.getCategoria() != null) {
                nuevoModelo.asignarCategoria(this.modelo.getCategoria());
            }

            // Copiamos palabras clave
            for (String palabraClave : this.modelo.getPalabrasClave()) {
                nuevoModelo.agregarPalabraClave(palabraClave);
            }

            // Reemplazamos el modelo
            this.modelo = nuevoModelo;
        }
    }

    public void setTamanoBytes(long tamanoBytes) {
        if (this.modelo != null) {
            // Creamos un nuevo modelo copiando los valores existentes pero con el nuevo
            // tamaño
            koolfileindexer.modelo.Archivo nuevoModelo = new koolfileindexer.modelo.Archivo(
                    this.modelo.getNombre(),
                    this.modelo.getRutaCompleta(),
                    this.modelo.getExtension(),
                    tamanoBytes,
                    this.modelo.getFechaCreacion(),
                    this.modelo.getFechaModificacion());

            // Copiamos otros datos importantes
            if (this.modelo.getCategoria() != null) {
                nuevoModelo.asignarCategoria(this.modelo.getCategoria());
            }

            // Copiamos palabras clave
            for (String palabraClave : this.modelo.getPalabrasClave()) {
                nuevoModelo.agregarPalabraClave(palabraClave);
            }

            // Reemplazamos el modelo
            this.modelo = nuevoModelo;
        }
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        if (this.modelo != null) {
            // Creamos un nuevo modelo copiando los valores existentes pero con la nueva
            // fecha
            koolfileindexer.modelo.Archivo nuevoModelo = new koolfileindexer.modelo.Archivo(
                    this.modelo.getNombre(),
                    this.modelo.getRutaCompleta(),
                    this.modelo.getExtension(),
                    this.modelo.getTamanoBytes(),
                    fechaCreacion,
                    this.modelo.getFechaModificacion());

            // Copiamos otros datos importantes
            if (this.modelo.getCategoria() != null) {
                nuevoModelo.asignarCategoria(this.modelo.getCategoria());
            }

            // Copiamos palabras clave
            for (String palabraClave : this.modelo.getPalabrasClave()) {
                nuevoModelo.agregarPalabraClave(palabraClave);
            }

            // Reemplazamos el modelo
            this.modelo = nuevoModelo;
        }
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        if (modelo != null) {
            modelo.actualizarFechaModificacion(fechaModificacion);
        }
    }

    public void setCategoria(String nombreCategoria) {
        if (modelo != null) {
            try {
                koolfileindexer.modelo.Categoria cat = koolfileindexer.modelo.Categoria
                        .valueOf(nombreCategoria.toUpperCase());
                modelo.asignarCategoria(cat);
            } catch (IllegalArgumentException e) {
                modelo.asignarCategoria(koolfileindexer.modelo.Categoria.OTRO);
            }
        }
    }
}