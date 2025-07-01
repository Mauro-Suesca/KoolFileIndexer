-- =========================================
-- 1. Función: Archivos según extensión
-- =========================================
DROP FUNCTION IF EXISTS sp_archivo_segun_extension(VARCHAR);

CREATE OR REPLACE FUNCTION sp_archivo_segun_extension(extension_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension 
    FROM Extension
    JOIN Archivo ON ext_id = arc_ext_id
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    WHERE ext_extension = extension_deseada;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 2. Función: Archivos según ubicación
-- =========================================
DROP FUNCTION IF EXISTS sp_archivo_segun_ubicacion(VARCHAR);

CREATE OR REPLACE FUNCTION sp_archivo_segun_ubicacion(ubicacion_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension 
    FROM Ubicacion
    JOIN Archivo ON ubi_id = arc_ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE ubi_path = ubicacion_deseada;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 3. Función: Archivos según categoría
-- =========================================
DROP FUNCTION IF EXISTS sp_archivo_segun_categoria(VARCHAR);

CREATE OR REPLACE FUNCTION sp_archivo_segun_categoria(categoria_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension 
    FROM Categoria
    JOIN Categoria_tiene_Archivo ON cat_id = cata_cat_id
    JOIN Archivo ON cata_arc_id = arc_id
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE cat_nombre = categoria_deseada;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 4. Función: Archivos según etiqueta
-- =========================================
DROP FUNCTION IF EXISTS sp_archivo_segun_etiqueta(VARCHAR);

CREATE OR REPLACE FUNCTION sp_archivo_segun_etiqueta(etiqueta_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension 
    FROM Etiqueta
    JOIN Archivo ON eti_id = arc_eti_id
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE eti_nombre = etiqueta_deseada;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 5. Función: Archivos según palabras clave (VARIADIC)
-- =========================================
DROP FUNCTION IF EXISTS sp_archivo_segun_palabra_clave(VARIADIC VARCHAR[]);

CREATE OR REPLACE FUNCTION sp_archivo_segun_palabra_clave(VARIADIC palabras_deseadas VARCHAR[])
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension 
    FROM Palabra_clave
    JOIN Archivo_tiene_Palabra_clave ON pal_id = arcp_pal_id
    JOIN Archivo ON arcp_arc_id = arc_id
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE pal_palabra = ANY(palabras_deseadas);
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 6. Función: Archivos según tamaño
-- =========================================
DROP FUNCTION IF EXISTS sp_archivo_segun_tamano(INT, INT);

CREATE OR REPLACE FUNCTION sp_archivo_segun_tamano(tamano_minimo INT, tamano_maximo INT)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension 
    FROM Archivo
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE arc_tamano > tamano_minimo AND arc_tamano < tamano_maximo;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 7. Función: Archivos cuyo nombre contiene patrón
-- =========================================
DROP FUNCTION IF EXISTS sp_archivo_segun_nombre(VARCHAR);

CREATE OR REPLACE FUNCTION sp_archivo_segun_nombre(patron VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension 
    FROM Archivo
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE LOWER(arc_nombre) LIKE '%' || LOWER(patron) || '%';
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 8. Función: Eliminar archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_eliminar_archivo(VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_eliminar_archivo(carpeta VARCHAR, nombre VARCHAR, extension VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Archivo
  WHERE arc_id IN (
    SELECT arc_id
    FROM Archivo
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE ubi_path = carpeta AND arc_nombre = nombre AND ext_extension = extension
  );
END;
$$ LANGUAGE plpgsql;