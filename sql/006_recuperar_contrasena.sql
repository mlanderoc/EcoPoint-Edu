-- Ajustes D18: tabla de códigos para recuperar la contraseña por correo.
-- Para bases creadas antes de la D18, como la de Neon. Se puede ejecutar varias veces.
-- Solo se guarda el SHA-256 del código, nunca el código (RP-04).
BEGIN;

CREATE TABLE IF NOT EXISTS TOKEN_RECUPERACION (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Política: CASCADE. Un código no tiene valor sin su usuario.
    id_usuario   BIGINT    NOT NULL REFERENCES USUARIO (id) ON DELETE CASCADE,
    codigo_hash  CHAR(64)  NOT NULL UNIQUE CHECK (codigo_hash ~ '^[0-9a-f]{64}$'),
    creado_en    TIMESTAMP NOT NULL,
    vence_en     TIMESTAMP NOT NULL,
    -- Momento en que se usó o en que otro código lo reemplazó (RP-03).
    usado_en     TIMESTAMP,
    CHECK (vence_en > creado_en)
);
CREATE INDEX IF NOT EXISTS ix_token_recuperacion_usuario ON TOKEN_RECUPERACION (id_usuario, creado_en);

COMMIT;
