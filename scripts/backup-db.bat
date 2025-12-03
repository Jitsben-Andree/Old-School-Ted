@echo off
setlocal

:: CONFIGURACIÓN
set DB_NAME=OldSchoolTeedDB
set DB_USER=postgres
set PGPASSWORD=0102
set PG_BIN="C:\Program Files\PostgreSQL\16\bin"
set BACKUP_DIR=..\backups
set TIMESTAMP=%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set FILENAME=%BACKUP_DIR%\backup_%DB_NAME%_%TIMESTAMP%.sql

echo ==========================================
echo INICIANDO BACKUP: %DB_NAME%
echo ==========================================

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

%PG_BIN%\pg_dump -U %DB_USER% -d %DB_NAME% -F c -b -v -f "%FILENAME%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ EXITO: Backup guardado en:
    echo %FILENAME%
    exit /b 0
) else (
    echo.
    echo ❌ ERROR: Fallo el backup.
    exit /b 1
)