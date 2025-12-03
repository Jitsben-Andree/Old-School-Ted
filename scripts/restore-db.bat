@echo off
setlocal

::  CONFIGURACIÓN
set DB_NAME=OldSchoolTeedDB
set DB_USER=postgres
set PG_BIN="C:\Program Files\PostgreSQL\16\bin"

echo ==========================================
echo      PELIGRO: RESTAURAR BASE DE DATOS
echo ==========================================
echo ESTO BORRARA LA BASE DE DATOS ACTUAL: %DB_NAME%
echo Y LA REEMPLAZARA CON UN BACKUP.
echo.
echo Arrastra el archivo .sql de backup aqui y presiona ENTER:
set /p BACKUP_FILE=

if "%BACKUP_FILE%"=="" goto fin

echo.
echo Vas a restaurar desde: %BACKUP_FILE%
pause

::  Desconectar usuarios (opcional, a veces necesario si la DB está en uso)
%PG_BIN%\psql -U %DB_USER% -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%DB_NAME%';"

::  Eliminar DB actual y crearla limpia
echo Borrando base de datos actual...
%PG_BIN%\dropdb -U %DB_USER% --if-exists %DB_NAME%

echo Creando base de datos limpia...
%PG_BIN%\createdb -U %DB_USER% %DB_NAME%

::  Restaurar
echo Restaurando datos...
%PG_BIN%\pg_restore -U %DB_USER% -d %DB_NAME% -v "%BACKUP_FILE%"

echo.
echo  PROCESO TERMINADO.

:fin
pause