-- =============================================================================
-- EcoPoint Edu - Script de actualización de los ajustes D14 (spec 002, R-017)
--
-- Agrega la cuadrilla del estudio (RN-088) a una base creada antes del
-- 18 nov 2026, sin borrar ni mover datos. Se puede ejecutar varias veces: lo
-- que ya existe no cambia. Una base nueva no lo necesita, porque
-- 001_schema.sql ya trae este cambio.
--
--   psql -U postgres -d ecopoint -v ON_ERROR_STOP=1 -f sql/004_ajustes_d14.sql
-- =============================================================================

SET client_min_messages = WARNING;

-- RN-088: cada estudio tiene una cuadrilla. Admite nulo para los estudios
-- viejos sin turnos; el servicio la exige al crear un estudio.
ALTER TABLE ESTUDIO
    ADD COLUMN IF NOT EXISTS id_cuadrilla BIGINT REFERENCES CUADRILLA (id) ON DELETE RESTRICT;

-- Los estudios existentes toman la cuadrilla que más turnos trabajó en ellos.
UPDATE ESTUDIO e
SET id_cuadrilla = (
    SELECT t.id_cuadrilla
    FROM TURNO t
    WHERE t.id_estudio = e.id AND t.id_cuadrilla IS NOT NULL
    GROUP BY t.id_cuadrilla
    ORDER BY COUNT(*) DESC, t.id_cuadrilla
    LIMIT 1)
WHERE e.id_cuadrilla IS NULL;
