-- Ajustes D16: cuadrilla libre para crear estudios en la demostración (RN-098).
-- Se puede ejecutar varias veces: no duplica usuarios, cuadrillas ni integrantes.
BEGIN;

INSERT INTO USUARIO (nombre, apellido, correo, rol, id_institucion, clave_hash)
SELECT f.nombre, f.apellido, f.correo, 'ESTUDIANTE_UNIVERSITARIO', d.id_institucion, d.clave_hash
FROM (VALUES ('Sofía', 'Torres', 'sofia.torres@unicesar.edu.co'),
             ('Daniel', 'Rojas', 'daniel.rojas@unicesar.edu.co')) AS f (nombre, apellido, correo)
CROSS JOIN (SELECT id_institucion, clave_hash FROM USUARIO WHERE correo = 'estudiante.demo@unicesar.edu.co') AS d
WHERE NOT EXISTS (SELECT 1 FROM USUARIO u WHERE u.correo = f.correo);

INSERT INTO CUADRILLA (nombre, id_responsable)
SELECT 'Cuadrilla de práctica', id FROM USUARIO WHERE correo = 'docente.demo@unicesar.edu.co'
AND NOT EXISTS (SELECT 1 FROM CUADRILLA WHERE nombre = 'Cuadrilla de práctica');

INSERT INTO CUADRILLA_INTEGRANTE (id_cuadrilla, id_usuario)
SELECT c.id, u.id
FROM CUADRILLA c
JOIN USUARIO u ON u.correo IN ('sofia.torres@unicesar.edu.co', 'daniel.rojas@unicesar.edu.co')
WHERE c.nombre = 'Cuadrilla de práctica'
ON CONFLICT DO NOTHING;

COMMIT;
