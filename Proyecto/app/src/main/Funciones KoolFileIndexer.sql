-- =========================================
-- 1. Función: Buscar Archivos según extensión
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_extension(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_extension(extension_deseada VARCHAR)
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
-- 2. Función: Buscar Archivos según ubicación
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_ubicacion(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_ubicacion(ubicacion_deseada VARCHAR)
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
-- 3. Función: Buscar Archivos según categoría
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_categoria(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_categoria(categoria_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension
    FROM Categoria
    JOIN Archivo ON cat_id = arc_cat_id
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE cat_nombre = categoria_deseada;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 4. Función: Buscar Archivos según etiqueta
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_etiqueta(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_etiqueta(etiqueta_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension
    FROM Etiqueta
    JOIN Etiqueta_tiene_Archivo ON eti_id = etia_eti_id
    JOIN Archivo ON etia_arc_id = arc_id
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE eti_nombre = etiqueta_deseada;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 5. Función: Buscar Archivos según palabras clave
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_palabra_clave(VARIADIC VARCHAR[]);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_palabra_clave(VARIADIC palabras_deseadas VARCHAR[])
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
-- 6. Función: Buscar Archivos según tamaño
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_tamano(INT, INT);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_tamano(tamano_minimo INT, tamano_maximo INT)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT ubi_path || '/' || arc_nombre || '.' || ext_extension
    FROM Archivo
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE arc_tamano BETWEEN tamano_minimo AND tamano_maximo;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 7. Función: Archivos cuyo nombre contiene patrón
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_nombre(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_nombre(patron VARCHAR)
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

-- =========================================
-- 9. Función: Desasociar Palabra Clave de Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_desasociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_desasociar_palabra_clave_archivo(carpeta VARCHAR, nombre VARCHAR, extension VARCHAR, palabra VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Archivo_tiene_Palabra_clave
  WHERE arcp_arc_id IN (
    SELECT arc_id
    FROM Archivo
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE ubi_path = carpeta AND arc_nombre = nombre AND ext_extension = extension
  ) AND arcp_pal_id IN (
	SELECT pal_id
    FROM Palabra_clave
    WHERE pal_palabra = palabra
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 10. Función: Desasociar Etiqueta de Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_desasociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_desasociar_etiqueta_archivo(carpeta VARCHAR, nombre VARCHAR, extension VARCHAR, etiqueta VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Etiqueta_tiene_Archivo
  WHERE etia_arc_id IN (
    SELECT arc_id
    FROM Archivo
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE ubi_path = carpeta AND arc_nombre = nombre AND ext_extension = extension
  ) AND etia_eti_id IN (
	SELECT eti_id
    FROM Etiqueta
    WHERE eti_nombre = etiqueta
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 11. Función: Eliminar Etiqueta
-- =========================================
DROP FUNCTION IF EXISTS sp_eliminar_etiqueta(VARCHAR);

CREATE OR REPLACE FUNCTION sp_eliminar_etiqueta(nombre VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Etiqueta
  WHERE eti_nombre = etiqueta;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 12. Función: Eliminar Ubicación
-- =========================================
DROP FUNCTION IF EXISTS sp_eliminar_ubicacion(VARCHAR);

CREATE OR REPLACE FUNCTION sp_eliminar_ubicacion(ubicacion VARCHAR)
RETURNS VOID AS $$
BEGIN
  PERFORM sp_eliminar_archivos_en_ubicacion(ubicacion);
  
  DELETE FROM Ubicacion
  WHERE ubi_path = ubicacion;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 13. Función: Eliminar Archivos que estén en una ubicación específica
-- =========================================

DROP FUNCTION IF EXISTS sp_eliminar_archivos_en_ubicacion(VARCHAR);

CREATE OR REPLACE FUNCTION sp_eliminar_archivos_en_ubicacion(carpeta VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Archivo
  WHERE arc_id IN (
    SELECT arc_id
    FROM Archivo
    JOIN Ubicacion ON arc_ubi_id = ubi_id
    WHERE ubi_path = carpeta
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 14. Función: Eliminar Ubicaciones que no tengan archivos asociados
-- =========================================
DROP FUNCTION IF EXISTS sp_eliminar_ubicaciones_sin_archivos();

CREATE OR REPLACE FUNCTION sp_eliminar_ubicaciones_sin_archivos()
RETURNS VOID AS $$
BEGIN 
  DELETE FROM Ubicacion
  WHERE ubi_id NOT IN(
	SELECT ubi_id
    FROM Ubicacion
    JOIN Archivo ON ubi_id = arc_ubi_id
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 15. Función: Crear Etiqueta
-- =========================================
DROP FUNCTION IF EXISTS sp_crear_etiqueta(VARCHAR);

CREATE OR REPLACE FUNCTION sp_crear_etiqueta(nombre VARCHAR)
RETURNS VOID AS $$
BEGIN
  INSERT INTO Etiqueta (eti_nombre)
  SELECT nombre
  WHERE NOT EXISTS (
    SELECT 1 FROM Etiqueta WHERE eti_nombre = nombre
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 16. Función: Asociar Etiqueta a Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_asociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_asociar_etiqueta_archivo(carpeta VARCHAR, nombre VARCHAR, extension VARCHAR, etiqueta VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_etiqueta_asociar INT;
  id_archivo_asociar INT;
BEGIN
  PERFORM sp_crear_etiqueta(etiqueta);

  SELECT arc_id INTO id_archivo_asociar
  FROM Archivo
  JOIN Ubicacion ON arc_ubi_id = ubi_id
  JOIN Extension ON arc_ext_id = ext_id
  WHERE ubi_path = carpeta AND arc_nombre = nombre AND ext_extension = extension;

  SELECT eti_id INTO id_etiqueta_asociar
  FROM Etiqueta
  WHERE eti_nombre = etiqueta;

  INSERT INTO Etiqueta_tiene_Archivo (etia_arc_id, etia_eti_id)
  SELECT id_archivo_asociar, id_etiqueta_asociar
  WHERE NOT EXISTS (
    SELECT 1 FROM Etiqueta_tiene_Archivo
    WHERE etia_arc_id = id_archivo_asociar AND etia_eti_id = id_etiqueta_asociar
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 17. Función: Crear Palabra Clave
-- =========================================
DROP FUNCTION IF EXISTS sp_crear_palabra_clave(VARCHAR);

CREATE OR REPLACE FUNCTION sp_crear_palabra_clave(palabra VARCHAR)
RETURNS VOID AS $$
BEGIN
  INSERT INTO Palabra_clave (pal_palabra)
  SELECT palabra
  WHERE NOT EXISTS (
    SELECT 1 FROM Palabra_clave WHERE pal_palabra = palabra
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 18. Función: Asociar Palabra Clave a Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_asociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_asociar_palabra_clave_archivo(carpeta VARCHAR, nombre VARCHAR, extension VARCHAR, palabra VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_palabra_asociar INT;
  id_archivo_asociar INT;
BEGIN
  PERFORM sp_crear_palabra_clave(palabra);

  SELECT arc_id INTO id_archivo_asociar
  FROM Archivo
  JOIN Ubicacion ON arc_ubi_id = ubi_id
  JOIN Extension ON arc_ext_id = ext_id
  WHERE ubi_path = carpeta AND arc_nombre = nombre AND ext_extension = extension;

  SELECT pal_id INTO id_palabra_asociar
  FROM Palabra_clave WHERE pal_palabra = palabra;  

  INSERT INTO Archivo_tiene_Palabra_clave (arcp_arc_id, arcp_pal_id)
  SELECT id_archivo_asociar, id_palabra_asociar
  WHERE NOT EXISTS (
    SELECT 1 FROM Archivo_tiene_Palabra_clave
    WHERE arcp_arc_id = id_archivo_asociar AND arcp_pal_id = id_palabra_asociar
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 19. Función: Actualizar ubicación con nombre nuevo de carpeta
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_ubicacion_con_nombre_nuevo_carpeta(VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_ubicacion_con_nombre_nuevo_carpeta(viejo_path VARCHAR, nuevo_path VARCHAR)
RETURNS VOID AS $$
BEGIN
  UPDATE Ubicacion
  SET ubi_path = nuevo_path
  WHERE ubi_path = viejo_path;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 20. Función: Crear Ubicación
-- =========================================
DROP FUNCTION IF EXISTS sp_crear_ubicacion(VARCHAR);

CREATE OR REPLACE FUNCTION sp_crear_ubicacion(nuevo_path VARCHAR)
RETURNS VOID AS $$
BEGIN
  INSERT INTO Ubicacion (ubi_path)
  SELECT nuevo_path
  WHERE NOT EXISTS (
    SELECT 1 FROM Ubicacion WHERE ubi_path = nuevo_path
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 21. Función: Actualizar ubicación de archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_archivo_ubicacion(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_archivo_ubicacion(viejo_path VARCHAR, nombre_archivo VARCHAR, extension VARCHAR, nuevo_path VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_nueva_ubicacion INT;
  id_archivo_actualizar INT;
BEGIN
  PERFORM sp_crear_ubicacion(nuevo_path);

  SELECT ubi_id INTO id_nueva_ubicacion
  FROM Ubicacion WHERE ubi_path = nuevo_path; 
  
  SELECT arc_id INTO id_archivo_actualizar
  FROM Archivo
  JOIN Ubicacion ON arc_ubi_id = ubi_id
  JOIN Extension ON arc_ext_id = ext_id
  WHERE ubi_path = viejo_path AND arc_nombre = nombre_archivo AND ext_extension = extension;
  
  UPDATE Archivo
  SET arc_ubi_id = id_nueva_ubicacion
  WHERE arc_id = id_archivo_actualizar;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 22. Función: Crear Archivo sin Categoría
-- =========================================
DROP FUNCTION IF EXISTS sp_crear_archivo_sin_categoria(VARCHAR);

CREATE OR REPLACE FUNCTION sp_crear_archivo_sin_categoria(nombre VARCHAR, tamano INT, fecha_modificacion DATE, carpeta VARCHAR, extension VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_ubicacion INT;
  id_extension INT;
BEGIN
  SELECT ubi_id INTO id_ubicacion
  FROM Ubicacion WHERE ubi_path = carpeta; 
  
  SELECT ext_id INTO id_extension
  FROM Extension WHERE ext_extension = extension; 

  INSERT INTO Archivo (arc_nombre, arc_tamano, arc_fecha_modificacion, arc_ubi_id, arc_ext_id)
  SELECT nombre, tamano, fecha_modificacion, id_ubicacion, id_extension
  WHERE NOT EXISTS (
    SELECT 1 FROM Archivo
    WHERE arc_id IN(
		SELECT arc_id
        FROM Ubicacion
        JOIN Archivo ON ubi_id = arc_ubi_id
        JOIN Extension ON ext_id = arc_ext_id
        WHERE ubi_id = id_ubicacion AND ext_id = id_extension AND arc_nombre = nombre
    )
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 23. Función: Crear Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_crear_archivo(VARCHAR);

CREATE OR REPLACE FUNCTION sp_crear_archivo(nombre VARCHAR, tamano INT, fecha_modificacion DATE, carpeta VARCHAR, extension VARCHAR, categoria VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_ubicacion INT;
  id_extension INT;
  id_categoria INT;
BEGIN
  SELECT ubi_id INTO id_ubicacion
  FROM Ubicacion WHERE ubi_path = carpeta; 
  
  SELECT ext_id INTO id_extension
  FROM Extension WHERE ext_extension = extension; 
  
  SELECT cat_id INTO id_categoria
  FROM Categoria WHERE cat_nombre = categoria; 

  INSERT INTO Archivo (arc_nombre, arc_tamano, arc_fecha_modificacion, arc_ubi_id, arc_ext_id, arc_cat_id)
  SELECT nombre, tamano, fecha_modificacion, id_ubicacion, id_extension, id_categoria
  WHERE NOT EXISTS (
    SELECT 1 FROM Archivo
    WHERE arc_id IN(
		SELECT arc_id
        FROM Ubicacion
        JOIN Archivo ON ubi_id = arc_ubi_id
        JOIN Extension ON ext_id = arc_ext_id
        WHERE ubi_id = id_ubicacion AND ext_id = id_extension AND arc_nombre = nombre
    )
  );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 24. Función: Actualizar nombre de archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_nombre_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_nombre_archivo(carpeta VARCHAR, viejo_nombre VARCHAR, extension VARCHAR, nuevo_nombre VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_archivo_actualizar INT;
BEGIN
  SELECT arc_id INTO id_archivo_actualizar
  FROM Archivo
  JOIN Ubicacion ON arc_ubi_id = ubi_id
  JOIN Extension ON arc_ext_id = ext_id
  WHERE ubi_path = carpeta AND arc_nombre = viejo_nombre AND ext_extension = extension;
  
  UPDATE Archivo
  SET arc_nombre = nuevo_nombre
  WHERE arc_id = id_archivo_actualizar;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 25. Función: Actualizar tamaño y fecha de modificación de archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_tamano_fecha_modificacion_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_tamano_fecha_modificacion_archivo(carpeta VARCHAR, nombre_archivo VARCHAR, extension VARCHAR, nuevo_tamano INT, nueva_fecha_modificacion DATE)
RETURNS VOID AS $$
DECLARE
  id_archivo_actualizar INT;
BEGIN
  SELECT arc_id INTO id_archivo_actualizar
  FROM Archivo
  JOIN Ubicacion ON arc_ubi_id = ubi_id
  JOIN Extension ON arc_ext_id = ext_id
  WHERE ubi_path = carpeta AND arc_nombre = nombre_archivo AND ext_extension = extension;
  
  UPDATE Archivo
  SET arc_tamano = nuevo_tamano, arc_fecha_modificacion = nueva_fecha_modificacion
  WHERE arc_id = id_archivo_actualizar;
END;
$$ LANGUAGE plpgsql;