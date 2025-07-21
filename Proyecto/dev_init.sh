#!/bin/bash
set -e

# === Configuración ===
DB_NAME="KoolFileIndexer"
ADMIN_USER="postgres"
DB_HOST="localhost"
DB_PORT="5432"
DB_USER="kool_user"
DB_PASS="koolpass"
SQL_SETUP="db/src/main/resources/db/migration/setup_dev.sql"
SQL_CREATE="db/src/main/resources/db/migration/create_db.sql"
SQL_INIT="db/src/main/resources/db/migration/init_schema.sql"
SQL_FUNCTIONS="db/src/main/resources/db/functions/function_initialization.sql"

echo "=============================="
echo "🛠 Inicializando entorno de desarrollo (Linux/macOS)"
echo "=============================="
echo

# === Verificar Java ===
if ! command -v java &> /dev/null; then
    echo "Java no está instalado. Descárgalo desde https://adoptium.net/"
    exit 1
fi

# === Verificar PostgreSQL (psql) ===
if ! command -v psql &> /dev/null; then
    echo "PostgreSQL no está instalado o 'psql' no está en PATH."
    echo "Descárgalo desde: https://www.postgresql.org/download/"
    exit 1
fi

# === Verificar gradlew o gradle ===
if [[ -f "./gradlew" ]]; then
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    echo "Gradle no está instalado ni hay wrapper."
    exit 1
fi

# === Pedir contraseña del superusuario postgres ===
read -s -p "Ingresa la contraseña del superusuario ($ADMIN_USER): " PGPASSWORD
export PGPASSWORD
echo

# === Crear usuario de base de datos ===
if [[ -f "$SQL_SETUP" ]]; then
    echo "Ejecutando script para crear usuario si no existen..."
    psql -U "$ADMIN_USER" -h "$DB_HOST" -p "$DB_PORT" -d postgres -f "$SQL_SETUP"
else
    echo "Archivo no encontrado: $SQL_SETUP"
    exit 1
fi

# === Crear base de datos ===
if [[ -f "$SQL_CREATE" ]]; then
    echo "Ejecutando script para crear base de datos si no existe..."
    psql -U "$ADMIN_USER" -h "$DB_HOST" -p "$DB_PORT" -d postgres -f "$SQL_CREATE"
else
    echo "Archivo no encontrado: $SQL_CREATE"
    exit 1
fi

# === Usar contraseña del usuario de aplicación ===
export PGPASSWORD="$DB_PASS"

# === Ejecutar script de inicialización de la base de datos ===
if [[ -f "$SQL_INIT" ]]; then
    echo "Ejecutando script de inicialización de base de datos..."
    psql -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -f "$SQL_INIT"
else
    echo "El archivo $SQL_INIT no existe. No se ejecutó nada."
fi

# === Ejecutar script de funciones ===
if [[ -f "$SQL_FUNCTIONS" ]]; then
    echo "Ejecutando script de funciones..."
    psql -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -f "$SQL_FUNCTIONS"
else
    echo "El archivo $SQL_FUNCTIONS no existe. No se ejecutó nada."
fi

# === Limpiar variable de contraseña ===
unset PGPASSWORD

# === Compilar el proyecto ===
echo "Instalando dependencias y compilando..."
$GRADLE_CMD build --refresh-dependencies

# === Ejecutar la aplicación ===
echo "Ejecutando la aplicación..."
$GRADLE_CMD run

echo
echo "Entorno de desarrollo listo."