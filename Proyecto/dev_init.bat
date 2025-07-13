@echo off
setlocal enabledelayedexpansion

REM === Configuración ===
set "DB_NAME=KoolFileIndexer"
set "ADMIN_USER=postgres"
set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_USER=kool_user"
set "DB_PASS=koolpass"
set "SQL_SETUP=app\src\main\resources\db\migration\setup_dev.sql"
set "SQL_CREATE=app\src\main\resources\db\migration\create_db.sql"
set "SQL_INIT=app\src\main\resources\db\migration\init_schema.sql"

echo ================================
echo 🛠 Inicializando entorno de desarrollo (Windows)
echo ================================
echo.

REM === Verificar Java ===
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java no está instalado.
    echo Descárgalo desde: https://adoptium.net/
    pause
    exit /b 1
)

REM === Verificar PostgreSQL ===
where psql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo PostgreSQL no está instalado o no está en PATH.
    echo Descárgalo desde: https://www.postgresql.org/download/windows/
    pause
    exit /b 1
)

REM === Verificar gradlew o gradle ===
if exist gradlew (
    set "GRADLE_CMD=gradlew"
) else (
    where gradle >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo Gradle no está instalado ni hay wrapper.
        pause
        exit /b 1
    )
    set "GRADLE_CMD=gradle"
)

REM === Pedir contraseña del superusuario postgres ===
set /p PGPASSWORD= Ingresa la contraseña del superusuario (%ADMIN_USER%): 

REM === Verificar o crear usuario de base de datos ===
if exist "%SQL_SETUP%" (
    echo Ejecutando script para crear usuario si no existen...
    psql -U %ADMIN_USER% -h %DB_HOST% -p %DB_PORT% -d postgres -f "%SQL_SETUP%"
    if errorlevel 1 (
        echo Error al ejecutar "%SQL_SETUP%"
        exit /b 1 
    )
) else (
    echo Archivo no encontrado: "%SQL_SETUP%"
    exit /b 1
)

REM === Verificar o crear base de datos ===
if exist "%SQL_CREATE%" (
    echo Ejecutando script para crear base de datos si no existen...
    psql -U %ADMIN_USER% -h %DB_HOST% -p %DB_PORT% -d postgres -f "%SQL_CREATE%"
    if errorlevel 1 (
        echo Error al ejecutar "%SQL_CREATE%"
        exit /b 1
    )
) else (
    echo Archivo no encontrado: "%SQL_CREATE%"
    exit /b 1
)

set "PGPASSWORD=%DB_PASS%"

REM === Ejecutar script de inicialización ===
if exist "%SQL_INIT%" (
    echo Ejecutando script de inicialización de base de datos...
    psql -U %DB_USER% -h %DB_HOST% -p %DB_PORT% -d %DB_NAME% -f "%SQL_INIT%"
    if errorlevel 1 (
        echo Error al ejecutar el script de inicialización.
        exit /b 1
    )
) else (
    echo El archivo "%SQL_INIT%" no existe. No se ejecutó nada.
)

set "PGPASSWORD="

REM === Compilar el proyecto ===
echo Instalando dependencias...
call %GRADLE_CMD% build --refresh-dependencies
if errorlevel 1 (
    echo Error al compilar el proyecto.
    exit /b 1
)

REM === Ejecutar la app ===
echo Ejecutando la aplicación...
call %GRADLE_CMD% run
if errorlevel 1 (
    echo Error al ejecutar la aplicación.
    exit /b 1
)

echo.
echo Entorno de desarrollo listo.
pause
endlocal