-- 1. Buscar archivos por extensión
SELECT * FROM sp_buscar_archivos_segun_extension('pdf');

-- 2. Buscar archivos por ubicación
SELECT * FROM sp_buscar_archivos_segun_ubicacion('/home/usuario/fotos');

-- 3. Buscar archivos por categoría
SELECT * FROM sp_buscar_archivos_segun_categoria('Imagen');

-- 4. Buscar archivos por etiqueta
SELECT * FROM sp_buscar_archivos_segun_etiqueta('personal');

-- 5. Buscar archivos por palabras clave
SELECT * FROM sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias(ARRAY['trabajo', 'verano']);
SELECT * FROM sp_buscar_archivos_con_una_palabra_clave_dada('trabajo');

-- 6. Buscar archivos por tamaño
SELECT * FROM sp_buscar_archivos_segun_tamano(200000, 600000);

-- 7. Buscar archivos por patrón en el nombre
SELECT * FROM sp_buscar_archivos_segun_nombre('foto');

-- 8. Eliminar un archivo
SELECT sp_eliminar_archivo('/home/usuario/fotos', 'logotipo', 'png');
SELECT * FROM sp_buscar_archivos_segun_extension('png'); -- para verificar

-- 9. Desasociar palabra clave
SELECT sp_desasociar_palabra_clave_archivo('/home/usuario/fotos', 'foto_playa', 'jpg', 'familia');
SELECT * FROM sp_buscar_archivos_con_una_palabra_clave_dada('familia'); -- para verificar

-- 10. Desasociar etiqueta
SELECT sp_desasociar_etiqueta_archivo('/home/usuario/docs', 'documento', 'pdf', 'proyecto');
SELECT * FROM sp_buscar_archivos_segun_etiqueta('proyecto'); -- para verificar

-- 11. Eliminar etiqueta (vacaciones solo estaba en foto_playa)
SELECT sp_eliminar_etiqueta('vacaciones');
SELECT * FROM Etiqueta; -- para verificar

-- 12. Eliminar todos los archivos en una ubicación
SELECT sp_eliminar_archivos_en_ubicacion('/home/usuario/videos');
SELECT * FROM Archivo WHERE arc_path = '/home/usuario/videos'; -- para verificar

-- 13. Crear una etiqueta nueva
SELECT sp_crear_etiqueta('urgente');
SELECT * FROM Etiqueta; -- para verificar

-- 14. Asociar etiqueta a archivo
SELECT sp_asociar_etiqueta_archivo('/home/usuario/docs', 'documento', 'pdf', 'urgente');
SELECT * FROM sp_buscar_archivos_segun_etiqueta('urgente'); -- para verificar

-- 15. Crear una palabra clave nueva
SELECT sp_crear_palabra_clave('confidencial');
SELECT * FROM Palabra_clave; -- para verificar

-- 16. Asociar palabra clave a archivo
SELECT sp_asociar_palabra_clave_archivo('/home/usuario/docs', 'documento', 'pdf', 'confidencial');
SELECT * FROM sp_buscar_archivos_con_una_palabra_clave_dada('confidencial'); -- para verificar

-- 17. Actualizar nombre de carpeta
SELECT sp_actualizar_archivos_con_nombre_nuevo_ubicacion('/home/usuario/musica', '/home/usuario/audio');
SELECT * FROM sp_buscar_archivos_segun_ubicacion('/home/usuario/audio'); -- para verificar

-- 18. Actualizar ubicación de archivo
SELECT sp_actualizar_archivo_ubicacion('/home/usuario/fotos', 'foto_playa', 'jpg', '/home/usuario/imagenes');
SELECT * FROM sp_buscar_archivos_segun_ubicacion('/home/usuario/imagenes'); -- para verificar

-- 19. Crear archivo nuevo
SELECT sp_crear_archivo('plan', 102400, '2024-07-01', '/home/usuario/docs', 'pdf', 'Texto');
SELECT * FROM sp_buscar_archivos_segun_nombre('plan'); -- para verificar

-- 20. Actualizar nombre de archivo
SELECT sp_actualizar_nombre_archivo('/home/usuario/docs', 'plan', 'pdf', 'plan_actualizado');
SELECT * FROM sp_buscar_archivos_segun_nombre('plan_actualizado'); -- para verificar

-- 21. Actualizar tamaño y fecha de modificación
SELECT sp_actualizar_tamano_fecha_modificacion_archivo('/home/usuario/docs', 'plan_actualizado', 'pdf', 204000, '2024-07-01');
SELECT * FROM Archivo WHERE arc_nombre = 'plan_actualizado'; -- para verificar

-- 22. Actualizar categoría de archivo
SELECT sp_actualizar_categoria_archivo('/home/usuario/docs', 'plan_actualizado', 'pdf', 'Imagen');
SELECT * FROM sp_buscar_archivos_segun_categoria('Imagen'); -- para verificar