#!/bin/bash

# === Configuración ===
DB_NAME="KoolFileIndexer"
ADMIN_USER="postgres"
DB_HOST="localhost"
DB_PORT="5432"
DB_USER="kool_user"
DB_PASS="koolpass"
SQL_SETUP="app/src/main/resources/db/migration/setup_dev.sql"
SQL_CREATE="app/src/main/resources/db/migration/create_db.sql"
SQL_INIT="app/src/main/resources/db/migration/init_schema.sql"
SQL_FUNCTIONS="app/src/main/resources/db/functions/function_initialization.sql"

echo "================================"
echo "🛠 Inicializando entorno de desarrollo (Linux/macOS)"
echo "================================"
echo

# === Verificar Java ===
if ! command -v java &> /dev/null; then
    echo "Java no está instalado."
    echo "Instálalo desde: https://adoptium.net/"
    exit 1
fi

# === Verificar PostgreSQL ===
if ! command -v psql &> /dev/null; then
    echo "PostgreSQL no está instalado o no está en el PATH."
    echo "Instálalo desde: https://www.postgresql.org/download/"
    exit 1
fi

# === Verificar gradlew o gradle ===
if [[ -f "./gradlew" ]]; then
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    echo "Gradle no está instalado ni hay wrapper (gradlew)."
    exit 1
fi

# === Pedir contraseña del superusuario postgres ===
read -s -p "Ingresa la contraseña del superusuario ($ADMIN_USER): " PGPASSWORD
echo
export PGPASSWORD

# === Ejecutar script de creación de usuario ===
if [[ -f "$SQL_SETUP" ]]; then
    echo "Ejecutando script para crear usuario si no existe..."
    psql -U "$ADMIN_USER" -h "$DB_HOST" -p "$DB_PORT" -d postgres -f "$SQL_SETUP"
    if [[ $? -ne 0 ]]; then
        echo "Error al ejecutar $SQL_SETUP"
        exit 1
    fi
else
    echo "Archivo no encontrado: $SQL_SETUP"
    exit 1
fi

# === Ejecutar script de creación de base de datos ===
if [[ -f "$SQL_CREATE" ]]; then
    echo "Ejecutando script para crear base de datos si no existe..."
    psql -U "$ADMIN_USER" -h "$DB_HOST" -p "$DB_PORT" -d postgres -f "$SQL_CREATE"
    if [[ $? -ne 0 ]]; then
        echo "Error al ejecutar $SQL_CREATE"
        exit 1
    fi
else
    echo "Archivo no encontrado: $SQL_CREATE"
    exit 1
fi

# === Cambiar a contraseña del nuevo usuario ===
export PGPASSWORD="$DB_PASS"

# === Ejecutar script de inicialización del esquema ===
if [[ -f "$SQL_INIT" ]]; then
    echo "Ejecutando script de inicialización de base de datos..."
    psql -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -f "$SQL_INIT"
    if [[ $? -ne 0 ]]; then
        echo "Error al ejecutar el script de inicialización."
        exit 1
    fi
else
    echo "El archivo $SQL_INIT no existe. No se ejecutó nada."
fi

# === Ejecutar script de creación de funciones de base de datos ===
if [[ -f "$SQL_FUNCTIONS" ]]; then
    echo "Ejecutando script de creación de funciones de base de datos..."
    psql -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -f "$SQL_FUNCTIONS"
    if [[ $? -ne 0 ]]; then
        echo "Error al ejecutar el script de creación de funciones de base de datos."
        exit 1
    fi
else
    echo "El archivo $SQL_FUNCTIONS no existe. No se ejecutó nada."
fi

# === Limpiar variable de contraseña ===
unset PGPASSWORD

# === Compilar el proyecto ===
echo "Instalando dependencias con $GRADLE_CMD..."
$GRADLE_CMD build --refresh-dependencies
if [[ $? -ne 0 ]]; then
    echo "Error al compilar el proyecto."
    exit 1
fi

# === Ejecutar la aplicación ===
echo "Ejecutando la aplicación..."
$GRADLE_CMD run
if [[ $? -ne 0 ]]; then
    echo "Error al ejecutar la aplicación."
    exit 1
fi

echo
echo "Entorno de desarrollo listo."