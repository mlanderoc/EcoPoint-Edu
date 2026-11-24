#!/usr/bin/env bash
# =============================================================================
# EcoPoint Edu - Prepara la base de la demostración web (D17, tarea T05)
#
# Uso (Git Bash, desde la raíz del repositorio):
#   ./sql/preparar-demo-web.sh
#
# Pide la cadena de conexión y la contraseña nueva sin mostrarlas ni guardarlas.
# Solo funciona sobre una base vacía.
# =============================================================================
set -euo pipefail

CARPETA="$(cd "$(dirname "$0")" && pwd)"
PSQL="$(command -v psql || true)"
if [ -z "$PSQL" ]; then
    PSQL="$(ls -d /c/Program\ Files/PostgreSQL/*/bin/psql.exe 2>/dev/null | sort -V | tail -1 || true)"
fi
[ -n "$PSQL" ] || { echo "No se encontró psql. Instale PostgreSQL 16 o superior." >&2; exit 1; }

read -rsp "Pegue la cadena de conexión de Neon: " CONEXION; echo
read -rsp "Escriba la contraseña nueva de las cuentas demo: " CLAVE; echo
read -rsp "Repita la contraseña nueva: " CLAVE_2; echo

[ "$CLAVE" = "$CLAVE_2" ] || { echo "Las contraseñas no coinciden." >&2; exit 1; }
[ "${#CLAVE}" -ge 10 ] || { echo "La contraseña debe tener al menos 10 caracteres." >&2; exit 1; }
[ "$CLAVE" != "EcoPoint2026" ] || { echo "No use la contraseña publicada en el repositorio." >&2; exit 1; }

PATRON='^postgres(ql)?://([^:/@]+):([^@]+)@([^/:?]+)(:([0-9]+))?/([^?]+)'
[[ "$CONEXION" =~ $PATRON ]] || { echo "La cadena de conexión no tiene el formato esperado." >&2; exit 1; }

export PGUSER="${BASH_REMATCH[2]}" PGPASSWORD="${BASH_REMATCH[3]}"
export PGHOST="${BASH_REMATCH[4]/-pooler/}" PGPORT="${BASH_REMATCH[6]:-5432}" PGDATABASE="${BASH_REMATCH[7]}"
export PGSSLMODE="${ECOPOINT_DEMO_SSLMODE:-require}" PGCLIENTENCODING=UTF8
export ECOPOINT_DEMO_CLAVE="$CLAVE"
unset CONEXION CLAVE CLAVE_2

TABLAS="$("$PSQL" -w -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")"
[ "$TABLAS" = "0" ] || { echo "La base no está vacía ($TABLAS tablas). No se cambió nada." >&2; exit 1; }

echo "Creando el esquema..."
"$PSQL" -w -v ON_ERROR_STOP=1 -q -f "$CARPETA/001_schema.sql"
echo "Cargando los datos ficticios..."
"$PSQL" -w -v ON_ERROR_STOP=1 -q -f "$CARPETA/002_seed.sql"

echo "Cambiando las contraseñas..."
"$PSQL" -w -v ON_ERROR_STOP=1 -q <<'SQL'
\getenv clave ECOPOINT_DEMO_CLAVE
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
UPDATE USUARIO SET clave_hash = crypt(:'clave', gen_salt('bf', 10));
DROP EXTENSION pgcrypto;
COMMIT;
SQL

echo "Listo: la base de la demostración quedó preparada."
