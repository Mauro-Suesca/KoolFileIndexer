TRUNCATE
    Archivo_Palabra_clave,
    Etiqueta_Archivo,
    Palabra_clave,
    Archivo,
    Etiqueta,
    Categoria,
    Extension;

INSERT INTO Extension (ext_extension) VALUES
  ('pdf'),
  ('jpg'),
  ('png'),
  ('mp3'),
  ('mp4');

INSERT INTO Categoria (cat_nombre) VALUES
  ('Texto'),
  ('Imagen'),
  ('Audio'),
  ('Video');

INSERT INTO Archivo (arc_nombre, arc_tamano, arc_fecha_modificacion, arc_path, arc_ext_id, arc_cat_id) VALUES
  ('documento',    204800, '2024-06-01', '/home/usuario/docs', 1, 1),  -- .pdf, Texto
  ('foto_playa',   512000, '2024-06-05', '/home/usuario/fotos', 2, 2), -- .jpg, Imagen
  ('logotipo',     256000, '2024-06-10', '/home/usuario/fotos', 3, 2), -- .png, Imagen
  ('cancion',     3072000, '2024-06-15', '/home/usuario/musica', 4, 3),-- .mp3, Audio
  ('video_familia', 10485760, '2024-06-20', '/home/usuario/videos', 5, 4);-- .mp4, Video

INSERT INTO Etiqueta (eti_nombre) VALUES
  ('importante'),
  ('personal'),
  ('proyecto'),
  ('vacaciones');

INSERT INTO Etiqueta_Archivo (etia_eti_id, etia_arc_id) VALUES
  (1, 1),  -- importante - documento
  (2, 1),  -- personal - documento
  (3, 1),  -- proyecto - documento
  (4, 2),  -- vacaciones - foto_playa
  (2, 2),  -- personal - foto_playa
  (2, 3),  -- personal - logotipo
  (3, 5);  -- proyecto - video_familia

INSERT INTO Palabra_clave (pal_palabra) VALUES
  ('finanzas'),
  ('verano'),
  ('diseño'),
  ('familia'),
  ('trabajo');

INSERT INTO Archivo_Palabra_clave (arcp_arc_id, arcp_pal_id) VALUES
  (1, 1),  -- documento - finanzas
  (1, 5),  -- documento - trabajo
  (2, 2),  -- foto_playa - verano
  (2, 4),  -- foto_playa - familia
  (3, 3),  -- logotipo - diseño
  (3, 5),  -- logotipo - trabajo
  (4, 5),  -- cancion - trabajo
  (5, 4),  -- video_familia - familia
  (5, 2);  -- video_familia - verano