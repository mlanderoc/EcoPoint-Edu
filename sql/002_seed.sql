-- =============================================================================
-- EcoPoint Edu - Datos semilla de demostración (spec 002, R-040 a R-044)
-- Ejecutar después de 001_schema.sql. Se puede ejecutar varias veces: vacía las
-- tablas y vuelve a cargar exactamente los mismos datos (CA-101).
--
--   psql -U postgres -d ecopoint -v ON_ERROR_STOP=1 -f sql/002_seed.sql
--
-- Los datos son FICTICIOS PERO REALISTAS (decisión D12): fuentes, pesos y
-- poblaciones de ejemplo para la I.E. Prudencia Daza, hasta tener los reales.
-- Los valores se calculan con fórmulas fijas (sin azar), así que siempre dan lo mismo.
--
-- Usuarios de demostración (contraseña de todos: EcoPoint2026):
--   docente.demo@unicesar.edu.co      DOCENTE
--   estudiante.demo@unicesar.edu.co   ESTUDIANTE_UNIVERSITARIO
--   + 3 estudiantes más de la cuadrilla
-- =============================================================================

SET client_min_messages = WARNING;

TRUNCATE EVIDENCIA, ALERTA, CORRECCION, MEDICION_DENSIDAD, RESULTADO_CUARTEO, CUARTEO,
         BOLSA, POBLACION_TURNO, TURNO_SIN_GENERACION, TURNO, FUENTE_ESTUDIO,
         REAPERTURA_ESTUDIO, DIA_MUESTREO, ESTUDIO, CUADRILLA_INTEGRANTE, CUADRILLA,
         USUARIO, SUBCATEGORIA_RESIDUO, FUENTE_GENERACION, INSTITUCION
         RESTART IDENTITY CASCADE;

-- -----------------------------------------------------------------------------
-- 1. Catálogo de subcategorías de la Res. 2184 de 2019 (R-040)
-- -----------------------------------------------------------------------------
INSERT INTO SUBCATEGORIA_RESIDUO (nombre, grupo, descripcion) VALUES
    ('Papel',                         'BLANCO',    'Hojas, cuadernos y papel de oficina limpios y secos'),
    ('Cartón',                        'BLANCO',    'Cajas y empaques de cartón'),
    ('Plástico PET',                  'BLANCO',    'Botellas de bebidas'),
    ('Otros plásticos',               'BLANCO',    'Envases, bolsas y empaques plásticos limpios'),
    ('Vidrio',                        'BLANCO',    'Botellas y frascos'),
    ('Metales',                       'BLANCO',    'Latas y piezas metálicas'),
    ('Tetra Pak',                     'BLANCO',    'Cajas de jugo y leche'),
    ('Restos de comida',              'VERDE',     'Sobras de alimentos del PAE y la tienda'),
    ('Hojas y podas',                 'VERDE',     'Residuos de jardinería y zonas verdes'),
    ('Papel higiénico y servilletas', 'NEGRO',     'Papel sanitario y servilletas usadas'),
    ('Icopor',                        'NEGRO',     'Poliestireno expandido'),
    ('Barrido',                       'NEGRO',     'Polvo y residuos del barrido'),
    ('Pilas',                         'SIN_COLOR', 'Pilas y baterías (posconsumo)'),
    ('Luminarias',                    'SIN_COLOR', 'Bombillos y tubos fluorescentes (posconsumo)'),
    ('RAEE',                          'SIN_COLOR', 'Residuos de aparatos eléctricos y electrónicos');

-- -----------------------------------------------------------------------------
-- 2. Institución piloto, fuentes ficticias y usuarios (R-041, R-042)
-- -----------------------------------------------------------------------------
INSERT INTO INSTITUCION (nombre, direccion, centro_latitud, centro_longitud, zoom_mapa)
VALUES ('I.E. Prudencia Daza', 'Valledupar, Cesar', 10.4700000, -73.2500000, 18);

-- Polígonos PROVISIONALES: rectángulos alrededor del centro, hasta trazar el plano real (spec 007).
INSERT INTO FUENTE_GENERACION (id_institucion, codigo, nombre, tipo, geometria)
SELECT 1, f.codigo, f.nombre, f.tipo::tipo_fuente,
       jsonb_build_object('type', 'Polygon', 'coordinates', jsonb_build_array(jsonb_build_array(
           jsonb_build_array(-73.2500 + f.dx,          10.4700 + f.dy),
           jsonb_build_array(-73.2500 + f.dx + 0.0003, 10.4700 + f.dy),
           jsonb_build_array(-73.2500 + f.dx + 0.0003, 10.4700 + f.dy + 0.0002),
           jsonb_build_array(-73.2500 + f.dx,          10.4700 + f.dy + 0.0002),
           jsonb_build_array(-73.2500 + f.dx,          10.4700 + f.dy))))
FROM (VALUES
    ('AU1', 'Bloque de aulas 1',          'AULAS',          -0.0006,  0.0003),
    ('AU2', 'Bloque de aulas 2',          'AULAS',          -0.0002,  0.0003),
    ('OF',  'Oficinas administrativas',   'OFICINAS',        0.0002,  0.0003),
    ('TI',  'Tienda escolar',             'TIENDA_ESCOLAR', -0.0006,  0.0000),
    ('PAE', 'Comedor del PAE',            'PAE',            -0.0002,  0.0000),
    ('BA',  'Baños',                      'BANOS',           0.0002,  0.0000),
    ('ZV',  'Zonas verdes y patios',      'ZONAS_VERDES',   -0.0004, -0.0003),
    ('LAB', 'Laboratorio de ciencias',    'LABORATORIO',     0.0001, -0.0003)
) AS f (codigo, nombre, tipo, dx, dy);

-- Contraseña de demostración de todos: EcoPoint2026 (hash BCrypt fijo).
INSERT INTO USUARIO (nombre, apellido, correo, rol, id_institucion, clave_hash) VALUES
    ('Docente',    'Demo',     'docente.demo@unicesar.edu.co',    'DOCENTE',                  1, '$2a$10$MhdpayjBM9C3p0KGJ8zzwuMd.EZcmQKyTk9hnnaqxv04Vdhwal.Um'),
    ('Estudiante', 'Demo',     'estudiante.demo@unicesar.edu.co', 'ESTUDIANTE_UNIVERSITARIO', 1, '$2a$10$MhdpayjBM9C3p0KGJ8zzwuMd.EZcmQKyTk9hnnaqxv04Vdhwal.Um'),
    ('Laura',      'Ríos',     'laura.rios@unicesar.edu.co',      'ESTUDIANTE_UNIVERSITARIO', 1, '$2a$10$MhdpayjBM9C3p0KGJ8zzwuMd.EZcmQKyTk9hnnaqxv04Vdhwal.Um'),
    ('Andrés',     'Mendoza',  'andres.mendoza@unicesar.edu.co',  'ESTUDIANTE_UNIVERSITARIO', 1, '$2a$10$MhdpayjBM9C3p0KGJ8zzwuMd.EZcmQKyTk9hnnaqxv04Vdhwal.Um'),
    ('Valentina',  'Arias',    'valentina.arias@unicesar.edu.co', 'ESTUDIANTE_UNIVERSITARIO', 1, '$2a$10$MhdpayjBM9C3p0KGJ8zzwuMd.EZcmQKyTk9hnnaqxv04Vdhwal.Um');

INSERT INTO CUADRILLA (nombre, id_responsable) VALUES ('Cuadrilla 1', 1);
INSERT INTO CUADRILLA_INTEGRANTE (id_cuadrilla, id_usuario) VALUES (1, 2), (1, 3), (1, 4), (1, 5);

-- -----------------------------------------------------------------------------
-- 3. Estudios: 1 = CERRADO completo (R-043); 2 = EN_EJECUCION a medio llenar (R-044)
-- -----------------------------------------------------------------------------
INSERT INTO ESTUDIO (id_institucion, estado) VALUES (1, 'CERRADO'), (1, 'EN_EJECUCION');

-- Estudio 1: 8 días hábiles de marzo de 2026 (el día 1 se descarta en los cálculos).
INSERT INTO DIA_MUESTREO (id_estudio, numero_dia, fecha) VALUES
    (1, 1, '2026-03-02'), (1, 2, '2026-03-03'), (1, 3, '2026-03-04'), (1, 4, '2026-03-05'),
    (1, 5, '2026-03-06'), (1, 6, '2026-03-09'), (1, 7, '2026-03-10'), (1, 8, '2026-03-11');
-- Estudio 2: 8 días hábiles de abril de 2026.
INSERT INTO DIA_MUESTREO (id_estudio, numero_dia, fecha) VALUES
    (2, 1, '2026-04-06'), (2, 2, '2026-04-07'), (2, 3, '2026-04-08'), (2, 4, '2026-04-09'),
    (2, 5, '2026-04-10'), (2, 6, '2026-04-13'), (2, 7, '2026-04-14'), (2, 8, '2026-04-15');

INSERT INTO FUENTE_ESTUDIO (id_estudio, id_fuente)
SELECT e.id, f.id FROM ESTUDIO e CROSS JOIN FUENTE_GENERACION f;

-- -----------------------------------------------------------------------------
-- 4. Datos de campo generados con fórmulas fijas
--    Generación base por turno (kg): AU1 9, AU2 8, OF 3, TI 7, PAE 16, BA 6, ZV 10, LAB 1.5
--    La tarde genera 85 % de la mañana; el día 1 genera 60 % más (residuos acumulados).
--    La variación de ±5 % sale de (día*31 + fuente*17 + turno*7) mod 11.
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    v_estudio      BIGINT;
    v_ultimo_dia   INT;
    v_dia          INT;
    v_tipo         tipo_turno;
    v_t            INT;
    v_turno        BIGINT;
    v_cerrado      BOOLEAN;
    v_fecha        DATE;
    v_hora_cierre  TIMESTAMP;
    v_neto_turno   NUMERIC;
    v_submuestra   NUMERIC;
    v_cuarteo      BIGINT;
    v_estado_cal   estado_calidad;
    v_factor_cal   NUMERIC;
    v_motivo       TEXT;
    v_verif        estado_verificacion;
    v_fuente       RECORD;
    v_sub          RECORD;
    v_neto         NUMERIC;
    v_tara         NUMERIC;
    v_vol          NUMERIC;
    v_var          NUMERIC;
    v_bolsa        BIGINT;
BEGIN
    FOR v_estudio IN 1..2 LOOP
        v_ultimo_dia := CASE v_estudio WHEN 1 THEN 8 ELSE 2 END;
        FOR v_dia IN 1..v_ultimo_dia LOOP
            SELECT fecha INTO v_fecha FROM DIA_MUESTREO WHERE id_estudio = v_estudio AND numero_dia = v_dia;
            FOR v_t IN 0..1 LOOP
                v_tipo := CASE v_t WHEN 0 THEN 'MANANA'::tipo_turno ELSE 'TARDE'::tipo_turno END;
                -- En el estudio 2, el turno de la tarde del día 2 sigue abierto y sin verificar.
                v_cerrado := NOT (v_estudio = 2 AND v_dia = 2 AND v_t = 1);
                v_verif := CASE WHEN v_estudio = 1 THEN 'VERIFICADO'::estado_verificacion
                                WHEN v_dia = 1 THEN 'VERIFICADO'::estado_verificacion
                                ELSE 'PENDIENTE'::estado_verificacion END;
                v_hora_cierre := v_fecha + CASE v_t WHEN 0 THEN TIME '12:30' ELSE TIME '18:30' END;

                INSERT INTO TURNO (id_estudio, numero_dia, tipo, estado, id_cuadrilla, abierto_en, cerrado_en)
                VALUES (v_estudio, v_dia, v_tipo,
                        CASE WHEN v_cerrado THEN 'CERRADO'::estado_turno ELSE 'ABIERTO'::estado_turno END,
                        1, v_fecha + CASE v_t WHEN 0 THEN TIME '06:30' ELSE TIME '12:45' END,
                        CASE WHEN v_cerrado THEN v_hora_cierre END)
                RETURNING id INTO v_turno;

                -- Población: la jornada de la tarde tiene menos estudiantes.
                INSERT INTO POBLACION_TURNO (id_turno, estudiantes, docentes, personal, estado_verificacion,
                                             digitado_por, digitado_en, verificado_por, verificado_en)
                VALUES (v_turno,
                        CASE v_t WHEN 0 THEN 480 ELSE 430 END + ((v_dia * 13) % 9) - 4,
                        CASE v_t WHEN 0 THEN 28 ELSE 25 END,
                        CASE v_t WHEN 0 THEN 15 ELSE 12 END,
                        v_verif, 2, v_fecha + CASE v_t WHEN 0 THEN TIME '06:40' ELSE TIME '12:50' END,
                        CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE 1 END,
                        CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE v_hora_cierre + INTERVAL '2 hours' END);

                -- Bolsas: una por fuente. El laboratorio no genera en la tarde.
                FOR v_fuente IN
                    SELECT f.id, f.codigo,
                           CASE f.codigo WHEN 'AU1' THEN 9 WHEN 'AU2' THEN 8 WHEN 'OF' THEN 3 WHEN 'TI' THEN 7
                                         WHEN 'PAE' THEN 16 WHEN 'BA' THEN 6 WHEN 'ZV' THEN 10 ELSE 1.5 END AS base,
                           CASE f.codigo WHEN 'PAE' THEN 420 WHEN 'TI' THEN 160 WHEN 'ZV' THEN 210 WHEN 'BA' THEN 110
                                         WHEN 'OF' THEN 90 ELSE 130 END AS densidad
                    FROM FUENTE_GENERACION f ORDER BY f.id
                LOOP
                    IF v_fuente.codigo = 'LAB' AND v_t = 1 THEN
                        IF v_cerrado THEN
                            INSERT INTO TURNO_SIN_GENERACION (id_turno, id_fuente) VALUES (v_turno, v_fuente.id);
                        END IF;
                        CONTINUE;
                    END IF;
                    -- En el turno abierto del estudio 2 solo se han pesado 4 fuentes.
                    IF NOT v_cerrado AND v_fuente.codigo IN ('ZV', 'LAB', 'BA', 'OF') THEN
                        CONTINUE;
                    END IF;
                    v_var  := 1 + (((v_dia * 31 + v_fuente.id * 17 + v_t * 7) % 11) - 5) / 100.0;
                    v_neto := round(v_fuente.base * v_var * CASE v_t WHEN 0 THEN 1 ELSE 0.85 END
                                    * CASE v_dia WHEN 1 THEN 1.6 ELSE 1 END, 2);
                    v_tara := round(0.10 + ((v_fuente.id * 7 + v_dia) % 4) * 0.10, 2);
                    v_vol  := round(v_neto / v_fuente.densidad, 3);
                    INSERT INTO BOLSA (id_turno, id_fuente, rotulo, peso_bruto, tara, volumen, atipica,
                                       estado_verificacion, digitado_por, digitado_en, verificado_por, verificado_en)
                    VALUES (v_turno, v_fuente.id,
                            v_fuente.codigo || '-D' || v_dia || '-' || CASE v_t WHEN 0 THEN 'M' ELSE 'T' END,
                            v_neto + v_tara, v_tara, v_vol, FALSE,
                            v_verif, 2 + (v_fuente.id % 4), v_hora_cierre - INTERVAL '20 minutes',
                            CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE 1 END,
                            CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE v_hora_cierre + INTERVAL '2 hours' END)
                    RETURNING id INTO v_bolsa;
                END LOOP;

                IF NOT v_cerrado THEN
                    CONTINUE;  -- el turno abierto todavía no tiene cuarteo ni densidad
                END IF;

                -- Cuarteo: submuestra de 50 kg (o todo el turno si generó menos).
                SELECT COALESCE(SUM(peso_neto), 0) INTO v_neto_turno FROM BOLSA WHERE id_turno = v_turno;
                v_submuestra := LEAST(50.00, v_neto_turno);
                -- Estudio 1, día 5 tarde: muestra mojada por lluvia, anulada con motivo.
                IF v_estudio = 1 AND v_dia = 5 AND v_t = 1 THEN
                    v_estado_cal := 'ANULADO';
                    v_factor_cal := 0.95;
                    v_motivo := 'Muestra mojada por lluvia; no representa el peso real.';
                ELSE
                    v_estado_cal := 'CONFORME';
                    v_factor_cal := 0.994;
                    v_motivo := NULL;
                END IF;
                INSERT INTO CUARTEO (id_turno, peso_neto_turno, peso_submuestra, estado_calidad, motivo_anulacion,
                                     anulado_por, anulado_en, estado_verificacion, digitado_por, digitado_en,
                                     verificado_por, verificado_en)
                VALUES (v_turno, v_neto_turno, v_submuestra, v_estado_cal, v_motivo,
                        CASE WHEN v_motivo IS NULL THEN NULL ELSE 1 END,
                        CASE WHEN v_motivo IS NULL THEN NULL ELSE v_hora_cierre + INTERVAL '1 hour' END,
                        v_verif, 3, v_hora_cierre + INTERVAL '30 minutes',
                        CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE 1 END,
                        CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE v_hora_cierre + INTERVAL '2 hours' END)
                RETURNING id INTO v_cuarteo;

                -- Composición base: Blanco 32 %, Verde 42 %, Negro 25 %, Sin color 1 %.
                FOR v_sub IN
                    SELECT s.id,
                           CASE s.nombre
                               WHEN 'Papel' THEN 0.08 WHEN 'Cartón' THEN 0.06 WHEN 'Plástico PET' THEN 0.07
                               WHEN 'Otros plásticos' THEN 0.05 WHEN 'Vidrio' THEN 0.02 WHEN 'Metales' THEN 0.02
                               WHEN 'Tetra Pak' THEN 0.02 WHEN 'Restos de comida' THEN 0.30
                               WHEN 'Hojas y podas' THEN 0.12 WHEN 'Papel higiénico y servilletas' THEN 0.14
                               WHEN 'Icopor' THEN 0.05 WHEN 'Barrido' THEN 0.06 WHEN 'Pilas' THEN 0.004
                               WHEN 'Luminarias' THEN 0.002 ELSE 0.004 END AS fraccion
                    FROM SUBCATEGORIA_RESIDUO s ORDER BY s.id
                LOOP
                    INSERT INTO RESULTADO_CUARTEO (id_cuarteo, id_subcategoria, peso)
                    VALUES (v_cuarteo, v_sub.id, round(v_submuestra * v_sub.fraccion * v_factor_cal, 2));
                END LOOP;

                -- Densidad del turno en una caneca de 55 galones (0,208 m³).
                INSERT INTO MEDICION_DENSIDAD (id_turno, volumen_recipiente, peso_neto, estado_verificacion,
                                               digitado_por, digitado_en, verificado_por, verificado_en)
                VALUES (v_turno, 0.208, round(0.208 * (150 + ((v_dia * 19 + v_t * 11) % 21) - 10), 2),
                        v_verif, 4, v_hora_cierre + INTERVAL '40 minutes',
                        CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE 1 END,
                        CASE WHEN v_verif = 'PENDIENTE' THEN NULL ELSE v_hora_cierre + INTERVAL '2 hours' END);
            END LOOP;
        END LOOP;
    END LOOP;
END;
$$;

-- -----------------------------------------------------------------------------
-- 5. Ejemplos de calidad del dato en el estudio 1
-- -----------------------------------------------------------------------------
-- Una bolsa corregida al cotejar con la planilla en papel (RN-063).
UPDATE BOLSA
SET peso_bruto = peso_bruto - 0.36, estado_verificacion = 'CORREGIDO'
WHERE rotulo = 'PAE-D3-M' AND id_turno = (SELECT id FROM TURNO WHERE id_estudio = 1 AND numero_dia = 3 AND tipo = 'MANANA');

INSERT INTO CORRECCION (tipo_registro, id_registro, campo, valor_anterior, valor_nuevo, motivo, id_usuario, corregido_en)
SELECT 'Bolsa', b.id, 'pesoBruto', to_char(b.peso_bruto + 0.36, 'FM9990.00'), to_char(b.peso_bruto, 'FM9990.00'),
       'Error de digitación: en la planilla dice ' || to_char(b.peso_bruto, 'FM9990.00') || ' kg.', 1, TIMESTAMP '2026-03-04 15:10'
FROM BOLSA b JOIN TURNO t ON t.id = b.id_turno
WHERE b.rotulo = 'PAE-D3-M' AND t.id_estudio = 1;

-- Bolsa atípica del día 6 (muy pesada), marcada y con su alerta atendida (RN-037, RN-071).
UPDATE BOLSA SET atipica = TRUE
WHERE rotulo = 'PAE-D6-M' AND id_turno = (SELECT id FROM TURNO WHERE id_estudio = 1 AND numero_dia = 6 AND tipo = 'MANANA');

INSERT INTO ALERTA (tipo, tipo_registro, id_registro, descripcion, creada_en, atendida, atendida_por, atendida_en, observacion_atencion)
SELECT 'VALOR_ATIPICO', 'Bolsa', b.id,
       'Valor atípico en la bolsa PAE-D6-M: revisar antes de verificar.', TIMESTAMP '2026-03-09 12:10',
       TRUE, 1, TIMESTAMP '2026-03-09 14:00', 'Se repesó: el PAE sirvió almuerzo especial ese día.'
FROM BOLSA b JOIN TURNO t ON t.id = b.id_turno WHERE b.rotulo = 'PAE-D6-M' AND t.id_estudio = 1;

-- Cuarteo del día 5 tarde: no conforme, alerta atendida y luego anulado (RN-052, RN-070).
INSERT INTO ALERTA (tipo, tipo_registro, id_registro, descripcion, creada_en, atendida, atendida_por, atendida_en, observacion_atencion)
SELECT 'CUARTEO_NO_CONFORME', 'Cuarteo', c.id,
       'Cuarteo no conforme en el turno D5-T: la diferencia supera el 2 %. Vuelva a pesar.', TIMESTAMP '2026-03-06 19:00',
       TRUE, 1, TIMESTAMP '2026-03-06 19:30', 'La muestra se mojó con la lluvia; se anuló el cuarteo.'
FROM CUARTEO c JOIN TURNO t ON t.id = c.id_turno WHERE t.id_estudio = 1 AND t.numero_dia = 5 AND t.tipo = 'TARDE';
