SELECT * FROM sp_buscar_archivos_segun_extension('pdf');

SELECT * FROM sp_buscar_archivos_segun_ubicacion('/home/usuario/fotos');

SELECT * FROM sp_buscar_archivos_segun_categoria('Imagen');

SELECT * FROM sp_buscar_archivos_segun_etiqueta('personal');

SELECT * FROM sp_buscar_archivos_con_minimo_una_palabra_clave_de_varias(ARRAY['trabajo', 'verano']);
SELECT * FROM sp_buscar_archivos_con_una_palabra_clave_dada('trabajo');

SELECT * FROM sp_buscar_archivos_segun_tamano(200000, 600000);

SELECT * FROM sp_buscar_archivos_segun_nombre('foto');

SELECT sp_eliminar_archivo('/home/usuario/fotos', 'logotipo', 'png');
SELECT * FROM sp_buscar_archivos_segun_extension('png'); -- para verificar

SELECT sp_desasociar_palabra_clave_archivo('/home/usuario/fotos', 'foto_playa', 'jpg', 'familia');
SELECT * FROM sp_buscar_archivos_con_una_palabra_clave_dada('familia'); -- para verificar

SELECT sp_desasociar_etiqueta_archivo('/home/usuario/docs', 'documento', 'pdf', 'proyecto');
SELECT * FROM sp_buscar_archivos_segun_etiqueta('proyecto'); -- para verificar

SELECT sp_eliminar_etiqueta('vacaciones');
SELECT * FROM Etiqueta; -- para verificar

SELECT sp_eliminar_archivos_en_ubicacion('/home/usuario/videos');
SELECT * FROM Archivo WHERE arc_path = '/home/usuario/videos'; -- para verificar

SELECT sp_crear_etiqueta('urgente');
SELECT * FROM Etiqueta; -- para verificar

SELECT sp_asociar_etiqueta_archivo('/home/usuario/docs', 'documento', 'pdf', 'urgente');
SELECT * FROM sp_buscar_archivos_segun_etiqueta('urgente'); -- para verificar

SELECT sp_crear_palabra_clave('confidencial');
SELECT * FROM Palabra_clave; -- para verificar

SELECT sp_asociar_palabra_clave_archivo('/home/usuario/docs', 'documento', 'pdf', 'confidencial');
SELECT * FROM sp_buscar_archivos_con_una_palabra_clave_dada('confidencial'); -- para verificar

SELECT sp_actualizar_archivos_con_nombre_nuevo_ubicacion('/home/usuario/musica', '/home/usuario/audio');
SELECT * FROM sp_buscar_archivos_segun_ubicacion('/home/usuario/audio'); -- para verificar

SELECT sp_actualizar_archivo_ubicacion('/home/usuario/fotos', 'foto_playa', 'jpg', '/home/usuario/imagenes');
SELECT * FROM sp_buscar_archivos_segun_ubicacion('/home/usuario/imagenes'); -- para verificar

SELECT sp_crear_archivo('plan', 102400, '2024-07-01', '/home/usuario/docs', 'pdf', 'Texto');
SELECT * FROM sp_buscar_archivos_segun_nombre('plan'); -- para verificar

SELECT sp_actualizar_nombre_archivo('/home/usuario/docs', 'plan', 'pdf', 'plan_actualizado');
SELECT * FROM sp_buscar_archivos_segun_nombre('plan_actualizado'); -- para verificar

SELECT sp_actualizar_tamano_fecha_modificacion_archivo('/home/usuario/docs', 'plan_actualizado', 'pdf', 204000, '2024-07-01');
SELECT * FROM Archivo WHERE arc_nombre = 'plan_actualizado'; -- para verificar

SELECT sp_actualizar_categoria_archivo('/home/usuario/docs', 'plan_actualizado', 'pdf', 'Imagen');
SELECT * FROM sp_buscar_archivos_segun_categoria('Imagen'); -- para verificar