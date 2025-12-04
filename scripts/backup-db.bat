#!/bin/sh

# Usamos variables de entorno si existen (Docker), si no, valores por defecto
DB_NAME=${DB_NAME:-oldschoolteed_db}
DB_USER=${DB_USER:-postgres}
# PGPASSWORD es la variable que pg_dump busca automáticamente
export PGPASSWORD=${DB_PASSWORD:-0102} 
DB_HOST=${DB_HOST:-localhost}

BACKUP_DIR="../backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="${BACKUP_DIR}/backup_${DB_NAME}_${TIMESTAMP}.sql"

echo "=========================================="
echo "INICIANDO BACKUP: $DB_NAME en $DB_HOST"
echo "=========================================="

mkdir -p "$BACKUP_DIR"

# Ejecutamos pg_dump
# -h: Host, -U: Usuario, -d: Base de datos, -F c: Formato custom
pg_dump -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -F c -b -v -f "$FILENAME"

if [ $? -eq 0 ]; then
    echo "✅ EXITO: Backup guardado en $FILENAME"
    exit 0
else
    echo "❌ ERROR: Fallo el backup."
    exit 1
fi