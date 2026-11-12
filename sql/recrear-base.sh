#!/usr/bin/env bash
# =============================================================================
# EcoPoint Edu - Recrea la base de datos desde cero (spec 002, HU-010)
#
# Uso (Git Bash, Linux o macOS, desde la raíz del repositorio):
#   ./sql/recrear-base.sh                    # base "ecopoint" con datos de demostración
#   ./sql/recrear-base.sh ecopoint_test --sin-semilla
#
# Requisitos: PostgreSQL 16+ y la variable ECOPOINT_DB_PASSWORD definida.
# =============================================================================
set -euo pipefail

BASE="${1:-ecopoint}"
SEMILLA="si"
[ "${2:-}" = "--sin-semilla" ] && SEMILLA="no"
USUARIO="${ECOPOINT_DB_USER:-postgres}"
SERVIDOR="${ECOPOINT_DB_HOST:-localhost}"
CARPETA="$(cd "$(dirname "$0")" && pwd)"

PSQL="$(command -v psql || true)"
if [ -z "$PSQL" ]; then
    PSQL="$(ls -d /c/Program\ Files/PostgreSQL/*/bin/psql.exe 2>/dev/null | sort -r | head -1 || true)"
fi
[ -n "$PSQL" ] || { echo "No se encontró psql. Instale PostgreSQL 16 o superior." >&2; exit 1; }

if [ -z "${ECOPOINT_DB_PASSWORD:-}" ] && command -v powershell >/dev/null 2>&1; then
    ECOPOINT_DB_PASSWORD="$(powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('ECOPOINT_DB_PASSWORD','User')" | tr -d '\r\n')"
fi
[ -n "${ECOPOINT_DB_PASSWORD:-}" ] || { echo "Defina la variable ECOPOINT_DB_PASSWORD con la contraseña de PostgreSQL." >&2; exit 1; }

export PGPASSWORD="$ECOPOINT_DB_PASSWORD" PGCLIENTENCODING=UTF8

if [ "$("$PSQL" -U "$USUARIO" -h "$SERVIDOR" -w -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$BASE'")" != "1" ]; then
    echo "Creando la base $BASE..."
    "$PSQL" -U "$USUARIO" -h "$SERVIDOR" -w -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $BASE ENCODING 'UTF8' TEMPLATE template0"
fi

echo "Creando el esquema en $BASE..."
"$PSQL" -U "$USUARIO" -h "$SERVIDOR" -w -d "$BASE" -v ON_ERROR_STOP=1 -q -f "$CARPETA/001_schema.sql"

if [ "$SEMILLA" = "si" ]; then
    echo "Cargando los datos de demostración..."
    "$PSQL" -U "$USUARIO" -h "$SERVIDOR" -w -d "$BASE" -v ON_ERROR_STOP=1 -q -f "$CARPETA/002_seed.sql"
fi

echo "Listo: la base $BASE quedó recreada."
