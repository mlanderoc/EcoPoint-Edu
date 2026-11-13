-- =============================================================================
-- EcoPoint Edu - Esquema de la base de datos (spec 002)
-- PostgreSQL 16 o superior. Se puede ejecutar varias veces: primero borra todo.
--
--   psql -U postgres -d ecopoint -v ON_ERROR_STOP=1 -f sql/001_schema.sql
--
-- Convenciones:
--   * Tablas en español y mayúsculas; columnas en snake_case.
--   * Los valores de los ENUM son los mismos de los enums de Java.
--   * Las reglas de la spec 001 que se pueden expresar aquí se repiten como
--     restricciones (segunda línea de defensa, spec 002 sección 5.2).
--   * Política de borrado por defecto: RESTRICT (no se borra lo que tiene
--     registros que dependen de ello). Cada excepción lleva su porqué.
-- =============================================================================

SET client_min_messages = WARNING;

-- -----------------------------------------------------------------------------
-- 0. Limpieza: borra vistas, tablas, funciones y tipos si ya existen
-- -----------------------------------------------------------------------------
DROP VIEW IF EXISTS V_REGISTROS_PENDIENTES;
DROP VIEW IF EXISTS V_TURNO_RESUMEN;

DROP TABLE IF EXISTS EVIDENCIA CASCADE;
DROP TABLE IF EXISTS ALERTA CASCADE;
DROP TABLE IF EXISTS CORRECCION CASCADE;
DROP TABLE IF EXISTS MEDICION_DENSIDAD CASCADE;
DROP TABLE IF EXISTS RESULTADO_CUARTEO CASCADE;
DROP TABLE IF EXISTS CUARTEO CASCADE;
DROP TABLE IF EXISTS BOLSA CASCADE;
DROP TABLE IF EXISTS POBLACION_TURNO CASCADE;
DROP TABLE IF EXISTS TURNO_SIN_GENERACION CASCADE;
DROP TABLE IF EXISTS TURNO CASCADE;
DROP TABLE IF EXISTS FUENTE_ESTUDIO CASCADE;
DROP TABLE IF EXISTS REAPERTURA_ESTUDIO CASCADE;
DROP TABLE IF EXISTS DIA_MUESTREO CASCADE;
DROP TABLE IF EXISTS ESTUDIO CASCADE;
DROP TABLE IF EXISTS CUADRILLA_INTEGRANTE CASCADE;
DROP TABLE IF EXISTS CUADRILLA CASCADE;
DROP TABLE IF EXISTS USUARIO CASCADE;
DROP TABLE IF EXISTS SUBCATEGORIA_RESIDUO CASCADE;
DROP TABLE IF EXISTS FUENTE_GENERACION CASCADE;
DROP TABLE IF EXISTS INSTITUCION CASCADE;

DROP FUNCTION IF EXISTS fn_correccion_solo_insercion() CASCADE;

DROP TYPE IF EXISTS tipo_evidencia;
DROP TYPE IF EXISTS tipo_alerta;
DROP TYPE IF EXISTS rol_usuario;
DROP TYPE IF EXISTS estado_calidad;
DROP TYPE IF EXISTS estado_verificacion;
DROP TYPE IF EXISTS estado_estudio;
DROP TYPE IF EXISTS estado_turno;
DROP TYPE IF EXISTS tipo_turno;
DROP TYPE IF EXISTS grupo_residuo;
DROP TYPE IF EXISTS tipo_fuente;

-- -----------------------------------------------------------------------------
-- 1. Tipos ENUM (listas cerradas del dominio, R-013)
-- -----------------------------------------------------------------------------
CREATE TYPE tipo_fuente AS ENUM
    ('AULAS', 'OFICINAS', 'TIENDA_ESCOLAR', 'PAE', 'ZONAS_VERDES', 'BANOS', 'LABORATORIO', 'OTRA');
CREATE TYPE grupo_residuo AS ENUM ('BLANCO', 'VERDE', 'NEGRO', 'SIN_COLOR');
CREATE TYPE tipo_turno AS ENUM ('MANANA', 'TARDE');
CREATE TYPE estado_turno AS ENUM ('ABIERTO', 'CERRADO');
CREATE TYPE estado_estudio AS ENUM ('PLANIFICADO', 'EN_EJECUCION', 'EN_DEPURACION', 'CERRADO', 'CANCELADO');
CREATE TYPE estado_verificacion AS ENUM ('PENDIENTE', 'VERIFICADO', 'CORREGIDO');
CREATE TYPE estado_calidad AS ENUM ('CONFORME', 'NO_CONFORME', 'ANULADO');
CREATE TYPE rol_usuario AS ENUM ('DOCENTE', 'ESTUDIANTE_UNIVERSITARIO');
CREATE TYPE tipo_alerta AS ENUM ('CUARTEO_NO_CONFORME', 'VALOR_ATIPICO', 'VERIFICACION_PENDIENTE');
CREATE TYPE tipo_evidencia AS ENUM ('SHIFT', 'BAG', 'QUARTERING');

-- -----------------------------------------------------------------------------
-- 2. Institución, fuentes de generación y catálogo de residuos
-- -----------------------------------------------------------------------------
CREATE TABLE INSTITUCION (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                 VARCHAR(150)  NOT NULL CHECK (btrim(nombre) <> ''),
    direccion              VARCHAR(200)  NOT NULL CHECK (btrim(direccion) <> ''),
    activa                 BOOLEAN       NOT NULL DEFAULT TRUE,                  -- RN-006
    centro_latitud         NUMERIC(10,7) CHECK (centro_latitud BETWEEN -90 AND 90),
    centro_longitud        NUMERIC(10,7) CHECK (centro_longitud BETWEEN -180 AND 180),
    zoom_mapa              SMALLINT      NOT NULL DEFAULT 17 CHECK (zoom_mapa BETWEEN 0 AND 22),
    -- Parámetros de caracterización (CharacterizationSettings: D8, RN-037, RN-052)
    peso_min_submuestra    NUMERIC(6,2)  NOT NULL DEFAULT 45   CHECK (peso_min_submuestra > 0),
    peso_max_submuestra    NUMERIC(6,2)  NOT NULL DEFAULT 55   CHECK (peso_max_submuestra > 0),
    tolerancia_calidad     NUMERIC(5,2)  NOT NULL DEFAULT 2    CHECK (tolerancia_calidad > 0),
    peso_max_bolsa         NUMERIC(6,2)  NOT NULL DEFAULT 50   CHECK (peso_max_bolsa > 0),
    densidad_min_bolsa     NUMERIC(8,2)  NOT NULL DEFAULT 20   CHECK (densidad_min_bolsa > 0),
    densidad_max_bolsa     NUMERIC(8,2)  NOT NULL DEFAULT 1000 CHECK (densidad_max_bolsa > 0),
    CONSTRAINT uk_institucion_nombre UNIQUE (nombre),
    CONSTRAINT ck_institucion_centro CHECK ((centro_latitud IS NULL) = (centro_longitud IS NULL)),
    CONSTRAINT ck_institucion_submuestra CHECK (peso_min_submuestra < peso_max_submuestra),
    CONSTRAINT ck_institucion_densidad CHECK (densidad_min_bolsa < densidad_max_bolsa)
);

CREATE TABLE FUENTE_GENERACION (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Política: RESTRICT. Una institución con fuentes no se borra (RN-001).
    id_institucion  BIGINT       NOT NULL REFERENCES INSTITUCION (id) ON DELETE RESTRICT,
    codigo          VARCHAR(6)   NOT NULL CHECK (codigo ~ '^[A-Z0-9]{2,6}$'),   -- RN-002
    nombre          VARCHAR(80)  NOT NULL CHECK (btrim(nombre) <> ''),
    tipo            tipo_fuente  NOT NULL,                                       -- RN-003
    geometria       JSONB        CHECK (geometria IS NULL OR geometria ->> 'type' = 'Polygon'),  -- R-032
    activa          BOOLEAN      NOT NULL DEFAULT TRUE,                          -- RN-005
    CONSTRAINT uk_fuente_codigo UNIQUE (id_institucion, codigo)                  -- RN-002
);

CREATE TABLE SUBCATEGORIA_RESIDUO (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(80)   NOT NULL CHECK (btrim(nombre) <> ''),
    grupo        grupo_residuo NOT NULL,                                         -- RN-040, RN-041
    descripcion  VARCHAR(500),
    activa       BOOLEAN       NOT NULL DEFAULT TRUE
);
-- RN-041: nombre único sin distinguir mayúsculas ni espacios al borde.
CREATE UNIQUE INDEX uk_subcategoria_nombre ON SUBCATEGORIA_RESIDUO (lower(btrim(nombre)));

-- -----------------------------------------------------------------------------
-- 3. Usuarios y cuadrillas
-- -----------------------------------------------------------------------------
CREATE TABLE USUARIO (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(80)  NOT NULL CHECK (btrim(nombre) <> ''),
    apellido        VARCHAR(80)  NOT NULL CHECK (btrim(apellido) <> ''),
    correo          VARCHAR(254) NOT NULL CHECK (correo ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]{2,}$'),
    rol             rol_usuario  NOT NULL,                                       -- RN-080
    -- Política: RESTRICT. No se borra una institución con usuarios.
    id_institucion  BIGINT       NOT NULL REFERENCES INSTITUCION (id) ON DELETE RESTRICT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    clave_hash      VARCHAR(100)                                                 -- BCrypt, lo usa la spec 003
);
-- RN-084: correo único sin distinguir mayúsculas.
CREATE UNIQUE INDEX uk_usuario_correo ON USUARIO (lower(btrim(correo)));

CREATE TABLE CUADRILLA (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(80) NOT NULL CHECK (btrim(nombre) <> ''),
    -- RN-085: el responsable debe ser DOCENTE (lo valida CrewValidator).
    -- Política: RESTRICT. No se borra un usuario que lidera una cuadrilla.
    id_responsable  BIGINT      NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT
);

CREATE TABLE CUADRILLA_INTEGRANTE (
    -- Política: CASCADE. Si se borra la cuadrilla, sobra la lista de sus integrantes.
    id_cuadrilla  BIGINT NOT NULL REFERENCES CUADRILLA (id) ON DELETE CASCADE,
    -- Política: RESTRICT. No se borra un usuario que pertenece a una cuadrilla.
    id_usuario    BIGINT NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    PRIMARY KEY (id_cuadrilla, id_usuario)
);

-- -----------------------------------------------------------------------------
-- 4. Estudio de caracterización
-- -----------------------------------------------------------------------------
CREATE TABLE ESTUDIO (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Política: RESTRICT. Una institución con estudios no se borra.
    id_institucion  BIGINT         NOT NULL REFERENCES INSTITUCION (id) ON DELETE RESTRICT,
    estado          estado_estudio NOT NULL DEFAULT 'PLANIFICADO',               -- RN-014
    -- Cancelación (RN-018): quién, cuándo y por qué. Política: RESTRICT, es historia del estudio.
    cancelado_por       BIGINT     REFERENCES USUARIO (id) ON DELETE RESTRICT,
    cancelado_en        TIMESTAMP,
    motivo_cancelacion  TEXT       CHECK (btrim(motivo_cancelacion) <> ''),
    CONSTRAINT ck_estudio_cancelacion CHECK (
        (estado = 'CANCELADO') = (cancelado_por IS NOT NULL)
        AND (cancelado_por IS NULL) = (cancelado_en IS NULL)
        AND (cancelado_por IS NULL) = (motivo_cancelacion IS NULL))
    -- RN-013 (sin estudios solapados) lo valida StudyValidator: las fechas viven en DIA_MUESTREO.
);

CREATE TABLE DIA_MUESTREO (
    -- Política: CASCADE. Los 8 días son parte del estudio (composición).
    id_estudio  BIGINT   NOT NULL REFERENCES ESTUDIO (id) ON DELETE CASCADE,
    numero_dia  SMALLINT NOT NULL CHECK (numero_dia BETWEEN 1 AND 8),           -- RN-010
    fecha       DATE,                                                            -- RN-011
    PRIMARY KEY (id_estudio, numero_dia),
    CONSTRAINT uk_dia_fecha UNIQUE (id_estudio, fecha)
);

CREATE TABLE REAPERTURA_ESTUDIO (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Política: RESTRICT. Las reaperturas son historia del estudio (RN-017).
    id_estudio    BIGINT    NOT NULL REFERENCES ESTUDIO (id) ON DELETE RESTRICT,
    id_usuario    BIGINT    NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    motivo        TEXT      NOT NULL CHECK (btrim(motivo) <> ''),
    reabierto_en  TIMESTAMP NOT NULL
);

CREATE TABLE FUENTE_ESTUDIO (
    -- Política: CASCADE. La lista de fuentes participantes es parte del estudio.
    id_estudio  BIGINT NOT NULL REFERENCES ESTUDIO (id) ON DELETE CASCADE,
    -- Política: RESTRICT. Una fuente que participa en un estudio no se borra (RN-005).
    id_fuente   BIGINT NOT NULL REFERENCES FUENTE_GENERACION (id) ON DELETE RESTRICT,
    PRIMARY KEY (id_estudio, id_fuente)
);

-- -----------------------------------------------------------------------------
-- 5. Turnos y registros de campo
--    Columnas de verificación (R-030) en POBLACION_TURNO, BOLSA, CUARTEO y
--    MEDICION_DENSIDAD: estado_verificacion, digitado_por, digitado_en,
--    verificado_por, verificado_en.
-- -----------------------------------------------------------------------------
CREATE TABLE TURNO (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_estudio    BIGINT       NOT NULL,
    numero_dia    SMALLINT     NOT NULL,
    tipo          tipo_turno   NOT NULL,
    estado        estado_turno NOT NULL DEFAULT 'ABIERTO',                       -- RN-021
    -- Política: SET NULL. Si se borra la cuadrilla, el turno queda sin cuadrilla asignada.
    id_cuadrilla  BIGINT       REFERENCES CUADRILLA (id) ON DELETE SET NULL,
    abierto_en    TIMESTAMP    NOT NULL DEFAULT now(),
    cerrado_en    TIMESTAMP,                                                     -- RN-072
    -- Política: RESTRICT. Un día con turnos registrados no se borra.
    CONSTRAINT fk_turno_dia FOREIGN KEY (id_estudio, numero_dia)
        REFERENCES DIA_MUESTREO (id_estudio, numero_dia) ON DELETE RESTRICT,
    CONSTRAINT uk_turno UNIQUE (id_estudio, numero_dia, tipo),                   -- RN-020
    CONSTRAINT ck_turno_cierre CHECK ((estado = 'CERRADO') = (cerrado_en IS NOT NULL))
);

CREATE TABLE TURNO_SIN_GENERACION (
    -- Política: CASCADE. Las marcas "sin generación" son parte del turno (RN-024).
    id_turno   BIGINT NOT NULL REFERENCES TURNO (id) ON DELETE CASCADE,
    id_fuente  BIGINT NOT NULL REFERENCES FUENTE_GENERACION (id) ON DELETE RESTRICT,
    PRIMARY KEY (id_turno, id_fuente)
);

CREATE TABLE POBLACION_TURNO (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Política: RESTRICT. La población es un dato de campo verificable; no se pierde.
    id_turno             BIGINT    NOT NULL REFERENCES TURNO (id) ON DELETE RESTRICT,
    estudiantes          INTEGER   NOT NULL CHECK (estudiantes >= 0),           -- RN-022
    docentes             INTEGER   NOT NULL CHECK (docentes >= 0),
    personal             INTEGER   NOT NULL CHECK (personal >= 0),
    estado_verificacion  estado_verificacion NOT NULL DEFAULT 'PENDIENTE',
    digitado_por         BIGINT    NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    digitado_en          TIMESTAMP NOT NULL,
    verificado_por       BIGINT    REFERENCES USUARIO (id) ON DELETE RESTRICT,
    verificado_en        TIMESTAMP,
    CONSTRAINT uk_poblacion_turno UNIQUE (id_turno),
    CONSTRAINT ck_poblacion_total CHECK (estudiantes + docentes + personal > 0),
    CONSTRAINT ck_poblacion_verificacion CHECK ((estado_verificacion = 'PENDIENTE') = (verificado_por IS NULL))
);

CREATE TABLE BOLSA (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Política: RESTRICT. Una bolsa es un pesaje verificable; no se pierde.
    id_turno             BIGINT       NOT NULL REFERENCES TURNO (id) ON DELETE RESTRICT,
    -- Política: RESTRICT. Una fuente con bolsas no se borra, solo se desactiva (RN-005, CA-104).
    id_fuente            BIGINT       NOT NULL REFERENCES FUENTE_GENERACION (id) ON DELETE RESTRICT,
    rotulo               VARCHAR(20)  NOT NULL CHECK (rotulo ~ '^[A-Z0-9]{2,6}-D[1-8]-[MT](-[0-9]+)?$'),  -- RN-031
    peso_bruto           NUMERIC(7,2) NOT NULL CHECK (peso_bruto > 0),           -- RN-033
    tara                 NUMERIC(7,2) NOT NULL CHECK (tara >= 0),
    volumen              NUMERIC(8,3) NOT NULL CHECK (volumen > 0),              -- RN-035
    -- R-014: peso neto y densidad se calculan, nunca se digitan (RN-034, RN-036).
    peso_neto            NUMERIC(7,2)  GENERATED ALWAYS AS (peso_bruto - tara) STORED,
    densidad             NUMERIC(10,2) GENERATED ALWAYS AS (round((peso_bruto - tara) / volumen, 2)) STORED,
    atipica              BOOLEAN      NOT NULL DEFAULT FALSE,                    -- RN-037
    estado_verificacion  estado_verificacion NOT NULL DEFAULT 'PENDIENTE',
    digitado_por         BIGINT       NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    digitado_en          TIMESTAMP    NOT NULL,
    verificado_por       BIGINT       REFERENCES USUARIO (id) ON DELETE RESTRICT,
    verificado_en        TIMESTAMP,
    CONSTRAINT ck_bolsa_tara CHECK (peso_bruto > tara),                         -- RN-033, CA-102
    CONSTRAINT uk_bolsa_rotulo UNIQUE (id_turno, rotulo),                       -- RN-032
    CONSTRAINT ck_bolsa_verificacion CHECK ((estado_verificacion = 'PENDIENTE') = (verificado_por IS NULL))
);

CREATE TABLE CUARTEO (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Política: RESTRICT. El cuarteo es la base de la composición; no se pierde.
    id_turno             BIGINT       NOT NULL REFERENCES TURNO (id) ON DELETE RESTRICT,
    peso_neto_turno      NUMERIC(8,2) NOT NULL CHECK (peso_neto_turno > 0),
    peso_submuestra      NUMERIC(7,2) NOT NULL CHECK (peso_submuestra > 0),
    estado_calidad       estado_calidad NOT NULL,                                -- RN-052
    motivo_anulacion     TEXT,
    anulado_por          BIGINT       REFERENCES USUARIO (id) ON DELETE RESTRICT,
    anulado_en           TIMESTAMP,
    estado_verificacion  estado_verificacion NOT NULL DEFAULT 'PENDIENTE',
    digitado_por         BIGINT       NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    digitado_en          TIMESTAMP    NOT NULL,
    verificado_por       BIGINT       REFERENCES USUARIO (id) ON DELETE RESTRICT,
    verificado_en        TIMESTAMP,
    CONSTRAINT uk_cuarteo_turno UNIQUE (id_turno),                              -- RN-050, CA-103
    CONSTRAINT ck_cuarteo_submuestra CHECK (peso_submuestra <= peso_neto_turno), -- RN-051
    CONSTRAINT ck_cuarteo_anulacion CHECK (
        (estado_calidad = 'ANULADO') = (motivo_anulacion IS NOT NULL AND btrim(motivo_anulacion) <> '')),
    CONSTRAINT ck_cuarteo_verificacion CHECK ((estado_verificacion = 'PENDIENTE') = (verificado_por IS NULL))
);

CREATE TABLE RESULTADO_CUARTEO (
    -- Política: CASCADE. Los resultados no existen sin su cuarteo (R-015).
    id_cuarteo       BIGINT       NOT NULL REFERENCES CUARTEO (id) ON DELETE CASCADE,
    -- Política: RESTRICT. Una subcategoría con resultados no se borra (RN-042).
    id_subcategoria  BIGINT       NOT NULL REFERENCES SUBCATEGORIA_RESIDUO (id) ON DELETE RESTRICT,
    peso             NUMERIC(7,2) NOT NULL CHECK (peso >= 0),                    -- RN-055
    PRIMARY KEY (id_cuarteo, id_subcategoria)                                    -- RN-055
);

CREATE TABLE MEDICION_DENSIDAD (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_turno             BIGINT       NOT NULL REFERENCES TURNO (id) ON DELETE RESTRICT,
    volumen_recipiente   NUMERIC(8,3) NOT NULL CHECK (volumen_recipiente > 0),   -- RN-057
    peso_neto            NUMERIC(7,2) NOT NULL CHECK (peso_neto > 0),
    densidad             NUMERIC(10,2) GENERATED ALWAYS AS (round(peso_neto / volumen_recipiente, 2)) STORED,
    estado_verificacion  estado_verificacion NOT NULL DEFAULT 'PENDIENTE',
    digitado_por         BIGINT       NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    digitado_en          TIMESTAMP    NOT NULL,
    verificado_por       BIGINT       REFERENCES USUARIO (id) ON DELETE RESTRICT,
    verificado_en        TIMESTAMP,
    CONSTRAINT uk_densidad_turno UNIQUE (id_turno),                             -- RN-057
    CONSTRAINT ck_densidad_verificacion CHECK ((estado_verificacion = 'PENDIENTE') = (verificado_por IS NULL))
);

-- -----------------------------------------------------------------------------
-- 6. Historial de correcciones, alertas y evidencias
-- -----------------------------------------------------------------------------
CREATE TABLE CORRECCION (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_registro   VARCHAR(40) NOT NULL CHECK (btrim(tipo_registro) <> ''),
    id_registro     BIGINT      NOT NULL,
    campo           VARCHAR(60) NOT NULL CHECK (btrim(campo) <> ''),
    valor_anterior  TEXT,
    valor_nuevo     TEXT,
    motivo          TEXT        NOT NULL CHECK (btrim(motivo) <> ''),            -- RN-063
    id_usuario      BIGINT      NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    corregido_en    TIMESTAMP   NOT NULL,
    CONSTRAINT ck_correccion_cambio CHECK (valor_anterior IS DISTINCT FROM valor_nuevo)
);
CREATE INDEX ix_correccion_registro ON CORRECCION (tipo_registro, id_registro);

-- R-031, CA-107: el historial de correcciones solo admite inserciones.
CREATE FUNCTION fn_correccion_solo_insercion() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'El historial de correcciones no se puede modificar ni borrar.';
END;
$$;

CREATE TRIGGER tg_correccion_solo_insercion
    BEFORE UPDATE OR DELETE ON CORRECCION
    FOR EACH ROW EXECUTE FUNCTION fn_correccion_solo_insercion();

CREATE TABLE ALERTA (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo                  tipo_alerta NOT NULL,                                  -- RN-070 a RN-072
    tipo_registro         VARCHAR(40) NOT NULL,
    id_registro           BIGINT,
    descripcion           TEXT        NOT NULL CHECK (btrim(descripcion) <> ''),
    creada_en             TIMESTAMP   NOT NULL,
    atendida              BOOLEAN     NOT NULL DEFAULT FALSE,                    -- RN-073
    atendida_por          BIGINT      REFERENCES USUARIO (id) ON DELETE RESTRICT,
    atendida_en           TIMESTAMP,
    observacion_atencion  TEXT,
    CONSTRAINT ck_alerta_atencion CHECK (
        atendida = (atendida_por IS NOT NULL AND atendida_en IS NOT NULL
                    AND observacion_atencion IS NOT NULL AND btrim(observacion_atencion) <> ''))
);
CREATE INDEX ix_alerta_pendiente ON ALERTA (atendida, creada_en);

CREATE TABLE EVIDENCIA (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo          tipo_evidencia NOT NULL,
    id_registro   BIGINT         NOT NULL,
    ruta_archivo  VARCHAR(300)   NOT NULL CHECK (btrim(ruta_archivo) <> ''),
    tipo_medio    VARCHAR(20)    NOT NULL CHECK (tipo_medio IN ('image/jpeg', 'image/png')),   -- RN-090
    tamano_bytes  BIGINT         NOT NULL CHECK (tamano_bytes BETWEEN 1 AND 5242880),           -- 5 MB
    descripcion   VARCHAR(500),
    subida_por    BIGINT         NOT NULL REFERENCES USUARIO (id) ON DELETE RESTRICT,
    subida_en     TIMESTAMP      NOT NULL
);
CREATE INDEX ix_evidencia_registro ON EVIDENCIA (tipo, id_registro);

-- -----------------------------------------------------------------------------
-- 7. Vistas de apoyo
-- -----------------------------------------------------------------------------
-- Resumen por turno: población, número de bolsas y kg netos (lo usa la spec 006).
CREATE VIEW V_TURNO_RESUMEN AS
SELECT t.id                                                 AS id_turno,
       t.id_estudio,
       t.numero_dia,
       t.tipo,
       t.estado,
       COALESCE(p.estudiantes + p.docentes + p.personal, 0) AS poblacion_total,
       COUNT(b.id)                                          AS numero_bolsas,
       COALESCE(SUM(b.peso_neto), 0)                        AS kg_netos
FROM TURNO t
LEFT JOIN POBLACION_TURNO p ON p.id_turno = t.id
LEFT JOIN BOLSA b           ON b.id_turno = t.id
GROUP BY t.id, t.id_estudio, t.numero_dia, t.tipo, t.estado, p.estudiantes, p.docentes, p.personal;

-- Registros todavía pendientes de verificación (RN-016, RN-072).
CREATE VIEW V_REGISTROS_PENDIENTES AS
SELECT t.id_estudio, t.id AS id_turno, 'Población del turno' AS tipo_registro, p.id AS id_registro
FROM POBLACION_TURNO p JOIN TURNO t ON t.id = p.id_turno
WHERE p.estado_verificacion = 'PENDIENTE'
UNION ALL
SELECT t.id_estudio, t.id, 'Bolsa', b.id
FROM BOLSA b JOIN TURNO t ON t.id = b.id_turno
WHERE b.estado_verificacion = 'PENDIENTE'
UNION ALL
SELECT t.id_estudio, t.id, 'Cuarteo', c.id
FROM CUARTEO c JOIN TURNO t ON t.id = c.id_turno
WHERE c.estado_verificacion = 'PENDIENTE'
UNION ALL
SELECT t.id_estudio, t.id, 'Medición de densidad', d.id
FROM MEDICION_DENSIDAD d JOIN TURNO t ON t.id = d.id_turno
WHERE d.estado_verificacion = 'PENDIENTE';
