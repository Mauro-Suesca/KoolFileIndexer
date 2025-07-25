DROP TABLE IF EXISTS 
    Archivo_Palabra_clave,
    Etiqueta_Archivo,
    Palabra_clave,
    Archivo,
    Etiqueta,
    Categoria,
    Extension;

CREATE TABLE Extension (
    ext_id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    ext_extension VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE Etiqueta (
    eti_id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    eti_nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE Categoria (
    cat_id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    cat_nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE Archivo (
    arc_id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    arc_nombre VARCHAR(200) NOT NULL,
    arc_tamano BIGINT NOT NULL, -- En Bytes
    arc_fecha_modificacion DATE NOT NULL,
    arc_path VARCHAR(200) NOT NULL,
    arc_ext_id INTEGER NOT NULL REFERENCES Extension (ext_id),
    arc_cat_id INTEGER NOT NULL REFERENCES Categoria (cat_id)
);

CREATE INDEX idx_arc_ext_id ON Archivo (arc_ext_id);
CREATE INDEX idx_arc_cat_id ON Archivo (arc_cat_id);
CREATE UNIQUE INDEX idx_arc_nombre_completo ON Archivo (arc_path, arc_nombre, arc_ext_id);

CREATE TABLE Palabra_clave (
    pal_id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    pal_palabra VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE Archivo_Palabra_clave (
    arcp_arc_id INTEGER NOT NULL REFERENCES Archivo (arc_id) ON DELETE CASCADE,
    arcp_pal_id INTEGER NOT NULL REFERENCES Palabra_clave (pal_id) ON DELETE CASCADE,
    PRIMARY KEY (arcp_pal_id, arcp_arc_id)
);

CREATE INDEX idx_arcp_pal_id ON Archivo_Palabra_clave (arcp_pal_id);
CREATE INDEX idx_arcp_arc_id ON Archivo_Palabra_clave (arcp_arc_id);

CREATE TABLE Etiqueta_Archivo (
    etia_eti_id INTEGER NOT NULL REFERENCES Etiqueta (eti_id) ON DELETE CASCADE,
    etia_arc_id INTEGER NOT NULL REFERENCES Archivo (arc_id) ON DELETE CASCADE,
    PRIMARY KEY (etia_arc_id, etia_eti_id)
);

CREATE INDEX idx_etia_eti_id ON Etiqueta_Archivo (etia_eti_id);
CREATE INDEX idx_etia_arc_id ON Etiqueta_Archivo (etia_arc_id);


CREATE OR REPLACE FUNCTION borrar_etiqueta_si_no_usada() RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM Etiqueta_Archivo WHERE etia_eti_id = OLD.etia_eti_id
    ) THEN
        DELETE FROM Etiqueta WHERE eti_id = OLD.etia_eti_id;
    END IF;
    RETURN NULL;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_borrar_etiqueta
AFTER DELETE OR UPDATE ON Etiqueta_Archivo
FOR EACH ROW
EXECUTE FUNCTION borrar_etiqueta_si_no_usada();

CREATE OR REPLACE FUNCTION borrar_palabra_si_no_usada() RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM Archivo_Palabra_clave WHERE arcp_pal_id = OLD.arcp_pal_id
    ) THEN
        DELETE FROM Palabra_clave WHERE pal_id = OLD.arcp_pal_id;
    END IF;
    RETURN NULL;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_borrar_palabra
AFTER DELETE OR UPDATE ON Archivo_Palabra_clave
FOR EACH ROW
EXECUTE FUNCTION borrar_palabra_si_no_usada();

GRANT USAGE ON SCHEMA public TO kool_user;


INSERT INTO Categoria (cat_nombre) VALUES
  ('Documento'),
  ('Imagen'),
  ('Música'),
  ('Video'),
  ('Sin categoría');