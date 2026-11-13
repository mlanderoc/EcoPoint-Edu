-- =============================================================================
-- EcoPoint Edu - Script de actualización de los ajustes D13 (spec 002, R-016)
--
-- Agrega los cambios de D13 a una base creada antes del 13 nov 2026, sin borrar
-- ni mover datos. Se puede ejecutar varias veces: lo que ya existe no cambia.
-- Una base nueva no lo necesita, porque 001_schema.sql ya trae estos cambios.
--
--   psql -U postgres -d ecopoint -v ON_ERROR_STOP=1 -f sql/003_ajustes_d13.sql
--
-- Cada sentencia se confirma sola (sin BEGIN/COMMIT), porque el valor nuevo de
-- un ENUM no se puede usar en la misma transacción que lo agrega.
-- =============================================================================

SET client_min_messages = WARNING;

-- RN-006: una institución se desactiva en vez de borrarse.
ALTER TABLE INSTITUCION ADD COLUMN IF NOT EXISTS activa BOOLEAN NOT NULL DEFAULT TRUE;

-- RN-018: estado CANCELADO, con quién, cuándo y por qué.
ALTER TYPE estado_estudio ADD VALUE IF NOT EXISTS 'CANCELADO';

ALTER TABLE ESTUDIO
    ADD COLUMN IF NOT EXISTS cancelado_por      BIGINT REFERENCES USUARIO (id) ON DELETE RESTRICT,
    ADD COLUMN IF NOT EXISTS cancelado_en       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS motivo_cancelacion TEXT CHECK (btrim(motivo_cancelacion) <> '');

ALTER TABLE ESTUDIO DROP CONSTRAINT IF EXISTS ck_estudio_cancelacion;
ALTER TABLE ESTUDIO ADD CONSTRAINT ck_estudio_cancelacion CHECK (
    (estado = 'CANCELADO') = (cancelado_por IS NOT NULL)
    AND (cancelado_por IS NULL) = (cancelado_en IS NULL)
    AND (cancelado_por IS NULL) = (motivo_cancelacion IS NULL));

-- RN-019: los días y las fuentes participantes ya se borran en cascada con el
-- estudio, y los turnos lo impiden (RESTRICT): no hace falta cambiar nada.
