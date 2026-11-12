# =============================================================================
# EcoPoint Edu - Recrea la base de datos desde cero (spec 002, HU-010)
#
# Uso (PowerShell, desde la raíz del repositorio):
#   .\sql\recrear-base.ps1                 # base "ecopoint" con datos de demostración
#   .\sql\recrear-base.ps1 -Base ecopoint_test -SinSemilla
#
# Requisitos: PostgreSQL 16+ instalado y la variable ECOPOINT_DB_PASSWORD definida
#   setx ECOPOINT_DB_PASSWORD "tu_contraseña"   (y abrir una terminal nueva)
# =============================================================================
param(
    [string]$Base = "ecopoint",
    [string]$Usuario = $(if ($env:ECOPOINT_DB_USER) { $env:ECOPOINT_DB_USER } else { "postgres" }),
    [string]$Servidor = "localhost",
    [switch]$SinSemilla
)

$ErrorActionPreference = "Stop"

# Busca psql en el PATH o en la carpeta de instalación de PostgreSQL.
$psql = (Get-Command psql -ErrorAction SilentlyContinue).Source
if (-not $psql) {
    $psql = Get-ChildItem "C:\Program Files\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $psql) { throw "No se encontró psql. Instale PostgreSQL 16 o superior." }

$clave = $env:ECOPOINT_DB_PASSWORD
if (-not $clave) { $clave = [Environment]::GetEnvironmentVariable("ECOPOINT_DB_PASSWORD", "User") }
if (-not $clave) { throw "Defina la contraseña con: setx ECOPOINT_DB_PASSWORD `"su_contraseña`" y abra una terminal nueva." }

$env:PGPASSWORD = $clave
$env:PGCLIENTENCODING = "UTF8"
$carpeta = $PSScriptRoot

$existe = & $psql -U $Usuario -h $Servidor -w -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$Base'"
if ($existe -ne "1") {
    Write-Host "Creando la base $Base..."
    & $psql -U $Usuario -h $Servidor -w -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $Base ENCODING 'UTF8' TEMPLATE template0"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear la base $Base." }
}

Write-Host "Creando el esquema en $Base..."
& $psql -U $Usuario -h $Servidor -w -d $Base -v ON_ERROR_STOP=1 -q -f (Join-Path $carpeta "001_schema.sql")
if ($LASTEXITCODE -ne 0) { throw "Falló 001_schema.sql." }

if (-not $SinSemilla) {
    Write-Host "Cargando los datos de demostración..."
    & $psql -U $Usuario -h $Servidor -w -d $Base -v ON_ERROR_STOP=1 -q -f (Join-Path $carpeta "002_seed.sql")
    if ($LASTEXITCODE -ne 0) { throw "Falló 002_seed.sql." }
}

Remove-Item Env:PGPASSWORD
Write-Host "Listo: la base $Base quedó recreada."
