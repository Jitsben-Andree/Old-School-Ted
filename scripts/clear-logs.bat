@echo off
echo ==========================================
echo      LIMPIANDO CARPETA DE LOGS
echo ==========================================

cd ..
if exist logs (
    echo Borrando archivos .log y .gz en /logs...
    del /q /s logs\*
    echo Logs eliminados. Carpeta limpia.
) else (
    echo No se encontro la carpeta logs.
)

pause