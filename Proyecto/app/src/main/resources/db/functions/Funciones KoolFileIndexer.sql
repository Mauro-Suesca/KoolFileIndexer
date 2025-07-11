-- =========================================
-- 1. Función: Buscar Archivos según extensión
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_extension(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_extension(extension_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Extension
    JOIN Archivo ON ext_id = arc_ext_id
    WHERE ext_extension = extension_deseada;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_segun_extension(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_segun_extension(VARCHAR) TO usuario_final;

-- =========================================
-- 2. Función: Buscar Archivos según ubicación
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_ubicacion(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_ubicacion(ubicacion_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Extension
    JOIN Archivo ON ext_id = arc_ext_id
    WHERE arc_path = ubicacion_deseada;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_segun_ubicacion(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_segun_ubicacion(VARCHAR) TO usuario_final;

-- =========================================
-- 3. Función: Buscar Archivos según categoría
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_categoria(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_categoria(categoria_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Categoria
    JOIN Archivo ON cat_id = arc_cat_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE cat_nombre = categoria_deseada;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_segun_categoria(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_segun_categoria(VARCHAR) TO usuario_final;

-- =========================================
-- 4. Función: Buscar Archivos según etiqueta
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_etiqueta(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_etiqueta(etiqueta_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Etiqueta
    JOIN Etiqueta_Archivo ON eti_id = etia_eti_id
    JOIN Archivo ON etia_arc_id = arc_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE eti_nombre = etiqueta_deseada;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_segun_etiqueta(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_segun_etiqueta(VARCHAR) TO usuario_final;

-- =========================================
-- 5. Función: Buscar Archivos según palabras clave
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias(VARCHAR[]);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias(palabras_deseadas VARCHAR[])
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Palabra_clave
    JOIN Archivo_Palabra_clave ON pal_id = arcp_pal_id
    JOIN Archivo ON arcp_arc_id = arc_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE pal_palabra = ANY(palabras_deseadas);
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias(VARCHAR[]) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias(VARCHAR[]) TO usuario_final;

-- =========================================
-- 6. Función: Buscar Archivos que contengan una palabra clave dada
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_con_una_palabra_clave_dada(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_con_una_palabra_clave_dada(palabra_deseada VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Palabra_clave
    JOIN Archivo_Palabra_clave ON pal_id = arcp_pal_id
    JOIN Archivo ON arcp_arc_id = arc_id
    JOIN Extension ON arc_ext_id = ext_id
    WHERE pal_palabra = palabra_deseada;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_con_una_palabra_clave_dada(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_con_una_palabra_clave_dada(VARCHAR) TO usuario_final;

-- =========================================
-- 7. Función: Buscar Archivos según tamaño
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_tamano(INT, INT);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_tamano(tamano_minimo INT, tamano_maximo INT)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Archivo
    JOIN Extension ON arc_ext_id = ext_id
    WHERE arc_tamano BETWEEN tamano_minimo AND tamano_maximo;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_segun_tamano(INT, INT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_segun_tamano(INT, INT) TO usuario_final;

-- =========================================
-- 8. Función: Archivos cuyo nombre contiene patrón
-- =========================================
DROP FUNCTION IF EXISTS sp_buscar_archivos_segun_nombre(VARCHAR);

CREATE OR REPLACE FUNCTION sp_buscar_archivos_segun_nombre(patron VARCHAR)
RETURNS TABLE(archivo TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT arc_path || '/' || arc_nombre || '.' || ext_extension
    FROM Archivo
    JOIN Extension ON arc_ext_id = ext_id
    WHERE LOWER(arc_nombre) LIKE '%' || LOWER(patron) || '%';
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_buscar_archivos_segun_nombre(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_buscar_archivos_segun_nombre(VARCHAR) TO usuario_final;

-- =========================================
-- 9. Función: Eliminar archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_eliminar_archivo(VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_eliminar_archivo(ubicacion VARCHAR, nombre VARCHAR, extension_archivo VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Archivo
  WHERE arc_id IN (
    SELECT arc_id
    FROM Archivo
    JOIN Extension ON arc_ext_id = ext_id
    WHERE arc_path = ubicacion AND arc_nombre = nombre AND ext_extension = extension_archivo
  );
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_eliminar_archivo(VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_eliminar_archivo(VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 10. Función: Desasociar Palabra Clave de Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_desasociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_desasociar_palabra_clave_archivo(ubicacion VARCHAR, nombre VARCHAR, extension_archivo VARCHAR, palabra VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Archivo_Palabra_clave
  WHERE arcp_arc_id IN (
    SELECT arc_id
    FROM Archivo
    JOIN Extension ON arc_ext_id = ext_id
    WHERE arc_path = ubicacion AND arc_nombre = nombre AND ext_extension = extension_archivo
  ) AND arcp_pal_id IN (
	SELECT pal_id
    FROM Palabra_clave
    WHERE pal_palabra = palabra
  );
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_desasociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_desasociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 11. Función: Desasociar Etiqueta de Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_desasociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_desasociar_etiqueta_archivo(ubicacion VARCHAR, nombre VARCHAR, extension_archivo VARCHAR, vieja_etiqueta VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Etiqueta_Archivo
  WHERE etia_arc_id IN (
    SELECT arc_id
    FROM Archivo
    JOIN Extension ON arc_ext_id = ext_id
    WHERE arc_path = ubicacion AND arc_nombre = nombre AND ext_extension = extension_archivo
  ) AND etia_eti_id IN (
	SELECT eti_id
    FROM Etiqueta
    WHERE eti_nombre = vieja_etiqueta
  );
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_desasociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_desasociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 12. Función: Eliminar Etiqueta
-- =========================================
DROP FUNCTION IF EXISTS sp_eliminar_etiqueta(VARCHAR);

CREATE OR REPLACE FUNCTION sp_eliminar_etiqueta(nombre VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Etiqueta
  WHERE eti_nombre = nombre;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_eliminar_etiqueta(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_eliminar_etiqueta(VARCHAR) TO usuario_final;

-- =========================================
-- 13. Función: Eliminar Archivos que estén en una ubicación específica
-- =========================================
DROP FUNCTION IF EXISTS sp_eliminar_archivos_en_ubicacion(VARCHAR);

CREATE OR REPLACE FUNCTION sp_eliminar_archivos_en_ubicacion(ubicacion VARCHAR)
RETURNS VOID AS $$
BEGIN
  DELETE FROM Archivo
  WHERE arc_path = ubicacion;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_eliminar_archivos_en_ubicacion(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_eliminar_archivos_en_ubicacion(VARCHAR) TO usuario_final;

-- =========================================
-- 14. Función: Crear Etiqueta
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
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_crear_etiqueta(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_crear_etiqueta(VARCHAR) TO usuario_final;

-- =========================================
-- 15. Función: Asociar Etiqueta a Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_asociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_asociar_etiqueta_archivo(ubicacion VARCHAR, nombre VARCHAR, extension_archivo VARCHAR, nueva_etiqueta VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_etiqueta_asociar INT;
  id_archivo_asociar INT;
BEGIN
  PERFORM sp_crear_etiqueta(nueva_etiqueta);

  SELECT arc_id INTO id_archivo_asociar
  FROM Archivo
  JOIN Extension ON arc_ext_id = ext_id
  WHERE arc_path = ubicacion AND arc_nombre = nombre AND ext_extension = extension_archivo;

  SELECT eti_id INTO id_etiqueta_asociar
  FROM Etiqueta
  WHERE eti_nombre = nueva_etiqueta;

  INSERT INTO Etiqueta_Archivo (etia_arc_id, etia_eti_id)
  SELECT id_archivo_asociar, id_etiqueta_asociar
  WHERE NOT EXISTS (
    SELECT 1 FROM Etiqueta_Archivo
    WHERE etia_arc_id = id_archivo_asociar AND etia_eti_id = id_etiqueta_asociar
  );
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_asociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_asociar_etiqueta_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 16. Función: Crear Palabra Clave
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
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_crear_palabra_clave(VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_crear_palabra_clave(VARCHAR) TO usuario_final;

-- =========================================
-- 17. Función: Asociar Palabra Clave a Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_asociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_asociar_palabra_clave_archivo(ubicacion VARCHAR, nombre VARCHAR, extension_archivo VARCHAR, palabra VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_palabra_asociar INT;
  id_archivo_asociar INT;
BEGIN
  PERFORM sp_crear_palabra_clave(palabra);

  SELECT arc_id INTO id_archivo_asociar
  FROM Archivo
  JOIN Extension ON arc_ext_id = ext_id
  WHERE arc_path = ubicacion AND arc_nombre = nombre AND ext_extension = extension_archivo;

  SELECT pal_id INTO id_palabra_asociar
  FROM Palabra_clave WHERE pal_palabra = palabra;  

  INSERT INTO Archivo_Palabra_clave (arcp_arc_id, arcp_pal_id)
  SELECT id_archivo_asociar, id_palabra_asociar
  WHERE NOT EXISTS (
    SELECT 1 FROM Archivo_Palabra_clave
    WHERE arcp_arc_id = id_archivo_asociar AND arcp_pal_id = id_palabra_asociar
  );
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_asociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_asociar_palabra_clave_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 18. Función: Actualizar archivos con nombre nuevo de ubicacion
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_archivos_con_nombre_nuevo_ubicacion(VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_archivos_con_nombre_nuevo_ubicacion(vieja_ubicacion VARCHAR, nueva_ubicacion VARCHAR)
RETURNS VOID AS $$
BEGIN
  UPDATE Archivo
  SET arc_path = nueva_ubicacion
  WHERE arc_path = vieja_ubicacion;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_actualizar_archivos_con_nombre_nuevo_ubicacion(VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_actualizar_archivos_con_nombre_nuevo_ubicacion(VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 19. Función: Actualizar ubicación de archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_archivo_ubicacion(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_archivo_ubicacion(vieja_ubicacion VARCHAR, nombre_archivo VARCHAR, extension_archivo VARCHAR, nueva_ubicacion VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_archivo_actualizar INT;
BEGIN  
  SELECT arc_id INTO id_archivo_actualizar
  FROM Archivo
  JOIN Extension ON arc_ext_id = ext_id
  WHERE arc_path = vieja_ubicacion AND arc_nombre = nombre_archivo AND ext_extension = extension_archivo;
  
  UPDATE Archivo
  SET arc_path = nueva_ubicacion
  WHERE arc_id = id_archivo_actualizar;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_actualizar_archivo_ubicacion(VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_actualizar_archivo_ubicacion(VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 20. Función: Crear Archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_crear_archivo(VARCHAR, INT, DATE, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_crear_archivo(nombre VARCHAR, tamano INT, fecha_modificacion DATE, ubicacion VARCHAR, extension_archivo VARCHAR, nueva_categoria VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_extension INT;
  id_categoria INT;
BEGIN 
  SELECT ext_id INTO id_extension
  FROM Extension WHERE ext_extension = extension_archivo; 
  
  SELECT cat_id INTO id_categoria
  FROM Categoria WHERE cat_nombre = nueva_categoria; 

  INSERT INTO Archivo (arc_nombre, arc_tamano, arc_fecha_modificacion, arc_path, arc_ext_id, arc_cat_id)
  SELECT nombre, tamano, fecha_modificacion, ubicacion, id_extension, id_categoria
  WHERE NOT EXISTS (
    SELECT 1 FROM Archivo
    WHERE arc_id IN(
		SELECT arc_id
        FROM Archivo
        JOIN Extension ON ext_id = arc_ext_id
        WHERE arc_path = ubicacion AND ext_id = id_extension AND arc_nombre = nombre
    )
  );
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_crear_archivo(VARCHAR, INT, DATE, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_crear_archivo(VARCHAR, INT, DATE, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 21. Función: Actualizar nombre de archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_nombre_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_nombre_archivo(ubicacion VARCHAR, viejo_nombre VARCHAR, extension_archivo VARCHAR, nuevo_nombre VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_archivo_actualizar INT;
BEGIN
  SELECT arc_id INTO id_archivo_actualizar
  FROM Archivo
  JOIN Extension ON arc_ext_id = ext_id
  WHERE arc_path = ubicacion AND arc_nombre = viejo_nombre AND ext_extension = extension_archivo;
  
  UPDATE Archivo
  SET arc_nombre = nuevo_nombre
  WHERE arc_id = id_archivo_actualizar;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_actualizar_nombre_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_actualizar_nombre_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;

-- =========================================
-- 22. Función: Actualizar tamaño y fecha de modificación de archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_tamano_fecha_modificacion_archivo(VARCHAR, VARCHAR, VARCHAR, INT, DATE);

CREATE OR REPLACE FUNCTION sp_actualizar_tamano_fecha_modificacion_archivo(ubicacion VARCHAR, nombre_archivo VARCHAR, extension_archivo VARCHAR, nuevo_tamano INT, nueva_fecha_modificacion DATE)
RETURNS VOID AS $$
DECLARE
  id_archivo_actualizar INT;
BEGIN
  SELECT arc_id INTO id_archivo_actualizar
  FROM Archivo
  JOIN Extension ON arc_ext_id = ext_id
  WHERE arc_path = ubicacion AND arc_nombre = nombre_archivo AND ext_extension = extension_archivo;

  UPDATE Archivo
  SET arc_tamano = nuevo_tamano, arc_fecha_modificacion = nueva_fecha_modificacion
  WHERE arc_id = id_archivo_actualizar;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_actualizar_tamano_fecha_modificacion_archivo(VARCHAR, VARCHAR, VARCHAR, INT, DATE) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_actualizar_tamano_fecha_modificacion_archivo(VARCHAR, VARCHAR, VARCHAR, INT, DATE) TO usuario_final;

-- =========================================
-- 23. Función: Actualizar categoría de archivo
-- =========================================
DROP FUNCTION IF EXISTS sp_actualizar_categoria_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_categoria_archivo(ubicacion VARCHAR, nombre VARCHAR, extension_archivo VARCHAR, nueva_categoria VARCHAR)
RETURNS VOID AS $$
DECLARE
  id_archivo_actualizar INT;
  id_categoria_nueva INT;
BEGIN
  SELECT arc_id INTO id_archivo_actualizar
  FROM Archivo
  JOIN Extension ON arc_ext_id = ext_id
  WHERE arc_path = ubicacion AND arc_nombre = nombre AND ext_extension = extension_archivo;

  SELECT cat_id INTO id_categoria_nueva
  FROM Categoria
  WHERE cat_nombre = nueva_categoria;

  UPDATE Archivo
  SET arc_cat_id = id_categoria_nueva
  WHERE arc_id = id_archivo_actualizar;
END;
$$ LANGUAGE plpgsql
  SECURITY DEFINER;

REVOKE ALL ON FUNCTION public.sp_actualizar_categoria_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sp_actualizar_categoria_archivo(VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO usuario_final;