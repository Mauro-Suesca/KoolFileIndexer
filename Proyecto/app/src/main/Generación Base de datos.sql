-- =========================================
-- SCHEMA KoolFileIndexer - PostgreSQL
-- =========================================

-- Eliminar tablas si existen
DROP TABLE IF EXISTS Categoria_tiene_Archivo,
                    Categoria,
                    Archivo_tiene_Palabra_clave,
                    Palabra_clave,
                    Archivo,
                    Ubicacion,
                    Etiqueta,
                    Extension;

-- ========================
-- Tabla Extension
-- ========================
CREATE TABLE Extension (
    ext_id INTEGER NOT NULL DEFAULT 0 PRIMARY KEY,
    ext_extension VARCHAR(10) NOT NULL UNIQUE
);

-- ========================
-- Tabla Etiqueta
-- ========================
CREATE TABLE Etiqueta (
    eti_id INTEGER NOT NULL DEFAULT 0 PRIMARY KEY,
    eti_nombre VARCHAR(45) NOT NULL UNIQUE
);

-- ========================
-- Tabla Ubicacion
-- ========================
CREATE TABLE Ubicacion (
    ubi_id INTEGER NOT NULL DEFAULT 0 PRIMARY KEY,
    ubi_path VARCHAR(45) NOT NULL UNIQUE
);

-- ========================
-- Tabla Archivo
-- ========================
CREATE TABLE Archivo (
    arc_id INTEGER NOT NULL DEFAULT 0 PRIMARY KEY,
    arc_nombre VARCHAR(45) NOT NULL,
    arc_tamano INTEGER NOT NULL, -- Bytes
    arc_fecha_modificacion DATE NOT NULL,
    arc_ubi_id INTEGER NOT NULL,
    arc_ext_id INTEGER NOT NULL,
    arc_eti_id INTEGER,

    FOREIGN KEY (arc_ext_id) REFERENCES Extension (ext_id),
    FOREIGN KEY (arc_eti_id) REFERENCES Etiqueta (eti_id),
    FOREIGN KEY (arc_ubi_id) REFERENCES Ubicacion (ubi_id)
);

-- Índices auxiliares
CREATE INDEX idx_arc_ext_id ON Archivo (arc_ext_id);
CREATE INDEX idx_arc_eti_id ON Archivo (arc_eti_id);
CREATE INDEX idx_arc_ubi_id ON Archivo (arc_ubi_id);

-- ========================
-- Tabla Palabra_clave
-- ========================
CREATE TABLE Palabra_clave (
    pal_id INTEGER NOT NULL DEFAULT 0 PRIMARY KEY,
    pal_palabra VARCHAR(45) NOT NULL UNIQUE
);

-- ========================
-- Tabla Archivo_tiene_Palabra_clave
-- ========================
CREATE TABLE Archivo_tiene_Palabra_clave (
    arcp_arc_id INTEGER NOT NULL,
    arcp_pal_id INTEGER NOT NULL,
    PRIMARY KEY (arcp_pal_id, arcp_arc_id),
    FOREIGN KEY (arcp_arc_id) REFERENCES Archivo (arc_id),
    FOREIGN KEY (arcp_pal_id) REFERENCES Palabra_clave (pal_id)
);

CREATE INDEX idx_arcp_pal_id ON Archivo_tiene_Palabra_clave (arcp_pal_id);
CREATE INDEX idx_arcp_arc_id ON Archivo_tiene_Palabra_clave (arcp_arc_id);

-- ========================
-- Tabla Categoria
-- ========================
CREATE TABLE Categoria (
    cat_id INTEGER NOT NULL DEFAULT 0 PRIMARY KEY,
    cat_nombre VARCHAR(45) NOT NULL UNIQUE
);

-- ========================
-- Tabla Categoria_tiene_Archivo
-- ========================
CREATE TABLE Categoria_tiene_Archivo (
    cata_cat_id INTEGER NOT NULL,
    cata_arc_id INTEGER NOT NULL,
    PRIMARY KEY (cata_arc_id, cata_cat_id),
    FOREIGN KEY (cata_cat_id) REFERENCES Categoria (cat_id),
    FOREIGN KEY (cata_arc_id) REFERENCES Archivo (arc_id)
);

CREATE INDEX idx_cata_cat_id ON Categoria_tiene_Archivo (cata_cat_id);
CREATE INDEX idx_cata_arc_id ON Categoria_tiene_Archivo (cata_arc_id);

-- ========================
-- TRIGGER 1: Borrar Etiqueta si ningún Archivo la usa
-- ========================
CREATE OR REPLACE FUNCTION borrar_etiqueta_si_no_usada()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM Archivo WHERE arc_eti_id = OLD.arc_eti_id
    ) THEN
        DELETE FROM Etiqueta WHERE eti_id = OLD.arc_eti_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_borrar_etiqueta
AFTER DELETE ON Archivo
FOR EACH ROW
EXECUTE FUNCTION borrar_etiqueta_si_no_usada();

-- ========================
-- TRIGGER 2: Borrar Palabra_clave si ya no está asociada
-- ========================
CREATE OR REPLACE FUNCTION borrar_palabra_si_no_usada()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM Archivo_tiene_Palabra_clave WHERE arcp_pal_id = OLD.arcp_pal_id
    ) THEN
        DELETE FROM Palabra_clave WHERE pal_id = OLD.arcp_pal_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_borrar_palabra
AFTER DELETE ON Archivo_tiene_Palabra_clave
FOR EACH ROW
EXECUTE FUNCTION borrar_palabra_si_no_usada();