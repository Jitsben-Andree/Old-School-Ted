@echo off
setlocal

:: --- CONFIGURACIÓN (EDITA ESTO SEGÚN TU PC) ---
set DB_NAME=OldSchoolTeedDB
set DB_USER=postgres
:: La contraseña suele pedirse por teclado, pero si quieres automatizar, descomenta abajo:
 set PGPASSWORD=0102

:: Ruta a la carpeta BIN de PostgreSQL (Verifica si es versión 14, 15, 16...)
set PG_BIN="C:\Program Files\PostgreSQL\16\bin"

:: Configuración de carpetas
set BACKUP_DIR=..\backups
set TIMESTAMP=%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set FILENAME=%BACKUP_DIR%\backup_%DB_NAME%_%TIMESTAMP%.sql

echo ==========================================
echo INICIANDO BACKUP DE: %DB_NAME%
echo ==========================================

:: Crear carpeta de backups si no existe
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

:: Ejecutar pg_dump
:: -F c: Formato Custom (comprimido)
:: -b: Incluye blobs
:: -v: Verbose (progreso)
%PG_BIN%\pg_dump -U %DB_USER% -d %DB_NAME% -F c -b -v -f "%FILENAME%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ EXITO: Backup guardado en:
    echo %FILENAME%
) else (
    echo.
    echo ❌ ERROR: Fallo el backup. Revisa usuario, nombre de DB o ruta de PostgreSQL.
)

echo.
pause