-- Ajustes D16: cuadrilla libre para crear estudios en la demostración (RN-098).
-- Se puede ejecutar varias veces. No duplica usuarios ni cuadrillas, y solo agrega
-- estudiantes que no estén ya en una cuadrilla (RN-091).
BEGIN;

INSERT INTO USUARIO (nombre, apellido, correo, rol, id_institucion, clave_hash)
SELECT f.nombre, f.apellido, f.correo, 'ESTUDIANTE_UNIVERSITARIO', d.id_institucion, d.clave_hash
FROM (VALUES ('Sofía', 'Torres', 'sofia.torres@unicesar.edu.co'),
             ('Daniel', 'Rojas', 'daniel.rojas@unicesar.edu.co')) AS f (nombre, apellido, correo)
CROSS JOIN (SELECT id_institucion, clave_hash FROM USUARIO
            WHERE lower(btrim(correo)) = 'estudiante.demo@unicesar.edu.co') AS d
WHERE NOT EXISTS (SELECT 1 FROM USUARIO u WHERE lower(btrim(u.correo)) = f.correo);

INSERT INTO CUADRILLA (nombre, id_responsable)
SELECT 'Cuadrilla de práctica', r.id
FROM USUARIO r
WHERE lower(btrim(r.correo)) = 'docente.demo@unicesar.edu.co'
  AND NOT EXISTS (SELECT 1 FROM CUADRILLA WHERE nombre = 'Cuadrilla de práctica')
  AND EXISTS (SELECT 1 FROM USUARIO u
              WHERE lower(btrim(u.correo)) IN ('sofia.torres@unicesar.edu.co', 'daniel.rojas@unicesar.edu.co')
                AND NOT EXISTS (SELECT 1 FROM CUADRILLA_INTEGRANTE i WHERE i.id_usuario = u.id));

INSERT INTO CUADRILLA_INTEGRANTE (id_cuadrilla, id_usuario)
SELECT c.id, u.id
FROM (SELECT MIN(id) AS id FROM CUADRILLA WHERE nombre = 'Cuadrilla de práctica') AS c
JOIN USUARIO u ON lower(btrim(u.correo)) IN ('sofia.torres@unicesar.edu.co', 'daniel.rojas@unicesar.edu.co')
WHERE c.id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM CUADRILLA_INTEGRANTE i WHERE i.id_usuario = u.id);

COMMIT;
