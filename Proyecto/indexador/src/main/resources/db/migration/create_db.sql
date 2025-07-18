SELECT 'CREATE DATABASE "KoolFileIndexer" OWNER kool_user ENCODING ''UTF8'''
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'KoolFileIndexer'
)\gexec

-- Otorgar permisos (opcional si el owner ya es kool_user)
GRANT ALL PRIVILEGES ON DATABASE "KoolFileIndexer" TO kool_user;