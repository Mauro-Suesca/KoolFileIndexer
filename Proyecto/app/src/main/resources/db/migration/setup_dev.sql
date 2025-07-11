-- Crear usuario si no existe (no es 100% portable, requiere control externo)
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'kool_user') THEN
      CREATE USER kool_user WITH PASSWORD 'koolpass';
   END IF;
END $$;
