# Plan — Spec 005: Cuarteo, densidad y control de calidad

| Campo | Valor |
| --- | --- |
| Estado | **BORRADOR** — pendiente de aprobación del equipo |
| Spec | [spec.md](spec.md) (aprobada) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo |
| Rama | `feature/005-cuarteo-control-calidad` (sale de `feature/004-registro-en-campo`, porque el frontend aún no está en `develop`) |
| Fecha | 2026-11-12 |

## 1. Resumen

La API de la spec 003 ya registra el cuarteo con su control del 2 %, la densidad, la verificación con historial y las alertas. Esta spec agrega:

1. **Pantallas** (frontend de la spec 004): cuarteo con barra de control en vivo, densidad, bandeja de verificación, resumen de depuración, historial y bandeja de alertas.
2. **Tres consultas nuevas en la API**, de solo lectura, que las pantallas necesitan y la 003 no tiene (sección 3).

No cambian el modelo, la base de datos ni las reglas: todo el cálculo oficial (2 %, composición, densidad) sigue en el backend. El frontend repite el cálculo solo para mostrarlo en vivo mientras se digita.

## 2. Decisiones

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | La **barra de control en vivo** (R-404) calcula en el navegador la suma, la diferencia en kg y en % y el estado, con la tolerancia y el rango de submuestra de la institución (`settings`) | Es inmediata y no llama a la API en cada tecla. El estado oficial lo decide el backend al guardar |
| T2 | El **re-pesaje** de un cuarteo no conforme (R-406, CA-404) se hace con la corrección del doble registro (`POST /verifications/quartering/{id}/correct` con campos `result:<id>`) | Ya existe, deja la corrección en el historial y recalcula el estado. Como toda corrección, la hace una persona distinta de quien digitó (RN-062) |
| T3 | La composición (R-405) se muestra en una tabla y en **barras horizontales hechas con CSS**, sin librería de gráficos | Una barra por grupo y subcategoría es suficiente; evita una dependencia nueva. La 006 decidirá la librería de gráficos |
| T4 | Recipientes de medición frecuentes (R-411, "Debería") como **lista fija en el frontend** con "Caneca de 55 galones = 0,208 m³", ampliable cuando el equipo de Ingeniería Ambiental entregue su lista (P2) | No exige tabla nueva |
| T5 | El contador de alertas pendientes del menú (R-432) usa `GET /alerts?attended=false&size=1` y lee `totalItems` | Una sola consulta liviana |
| T6 | Verificar varios registros a la vez (R-426) llama la verificación de cada uno y reporta cuáles fallaron | No hace falta una ruta masiva |

## 3. Consultas nuevas en la API

| Ruta | Permiso | Respuesta |
| --- | --- | --- |
| `GET /api/v1/studies/{id}/verification-summary` | `VERIFY_RECORDS` | Por cada turno abierto o cerrado: día, turno, conteo de registros PENDIENTE, VERIFICADO y CORREGIDO, y si su cuarteo está NO_CONFORME. Totales del estudio y `readyToClose` = sin pendientes y sin cuarteos no conformes (RN-016). R-424, CA-410 |
| `GET /api/v1/shifts/{id}/records` | Sesión | Los registros verificables del turno (población, bolsas, cuarteo, densidad) con sus valores digitados, su estado de verificación, quién y cuándo los digitó. R-420, R-421 |
| `GET /api/v1/records/{recordType}/{id}` | Sesión | Un registro con sus valores, su turno y su verificación. Lleva de una alerta a su registro (R-430) y acompaña el historial (R-425) |

Las tres las arma `VerificationService` a partir de los repositorios existentes (`ShiftRepository.findById` ya trae el turno completo). DTO nuevo `VerifiableRecordResponse`: `recordType` (population, bag, quartering, density), `recordName`, `recordId`, `shiftId`, `shiftCode`, `label` (rótulo o nombre corto), `values` (campo → valor, con los mismos nombres que acepta la corrección) y `verification`.

## 4. Pantallas

| Ruta | Requisitos | Quién |
| --- | --- | --- |
| `/campo/turno/:id/cuarteo` | R-400 a R-407, CA-400 a CA-406 | Cuadrilla |
| `/campo/turno/:id/densidad` | R-410, R-411, CA-407 | Cuadrilla |
| `/calidad/verificacion` | R-420 a R-424, R-426, CA-408 a CA-410 | Docente y estudiante |
| `/calidad/registro/:tipo/:id` | R-421, R-422, R-425, CA-409, CA-412 | Docente y estudiante |
| `/alertas` | R-430 a R-432, CA-411 | Docente y estudiante |

- **Cuarteo:** peso neto del turno; submuestra con su rango y la propuesta de "muestra completa" si el turno pesa menos que el mínimo (CA-401); subcategorías agrupadas por los 4 grupos con su color y nombre, las de SIN_COLOR con el aviso "Residuos peligrosos: van a posconsumo" (R-403); barra de control fija abajo en el celular; al guardar, composición por subcategoría y por grupo; si queda no conforme, aviso rojo "Cuarteo no conforme: repesar" y, para quien tenga el permiso, "Anular" con motivo.
- **Turno (spec 004):** se agregan los accesos "Cuarteo" y "Densidad" y el aviso rojo cuando el cuarteo está no conforme.
- **Verificación:** selector de estudio y turno; resumen por día y turno con "El estudio aún no puede cerrarse" o "Listo para cerrar"; lista de registros del turno con filtros por tipo y estado; botones "Coincide con el papel" y "Corregir" ocultos en los registros que digitó el propio usuario (CA-408); selección múltiple.
- **Registro:** valores, quién y cuándo los digitó, formulario de corrección con motivo obligatorio (CA-409) e historial (CA-412).
- **Alertas:** filtros por tipo, estado y estudio; cada alerta enlaza a su registro; "Marcar atendida" con observación obligatoria (CA-411). Contador en el menú.

## 5. Pruebas

| CA | Cómo |
| --- | --- |
| CA-400, CA-401, CA-402, CA-403, CA-405, CA-406 | Vitest de la pantalla de cuarteo y de la utilidad de cálculo en vivo |
| CA-403, CA-404 | Backend: `QualityFlowIT` ya cubre el no conforme y la corrección; se agrega el caso exacto de CA-404 (de 48,5 a 49,5 kg) |
| CA-407 | Vitest de la pantalla de densidad |
| CA-408, CA-409, CA-412 | Vitest de verificación y del registro; backend: RN-062 ya probado |
| CA-410 | Backend `VerificationSummaryIT` con la semilla y Vitest del resumen |
| CA-411 | Vitest de alertas; backend ya probado (RN-073) |
| Consultas nuevas | `@WebMvcTest` y pruebas de servicio |

`npm test`, `npm run build`, `npm run lint` y `./mvnw verify` en verde antes de cada commit.

## 6. Responsables

| Parte | Responsable |
| --- | --- |
| Consultas nuevas en `VerificationService` | Estefanía (`EstefaniaMarquez`) |
| Controlador y DTO de las consultas | Carlos (`craulgomez`) |
| Pantallas y pruebas del frontend | Carlos (`craulgomez`) |
| Este plan y las tareas | Merlín (`mlanderoc`) |

## 7. Riesgos

| # | Tema | Propuesta |
| --- | --- | --- |
| R1 | La rama sale de la 004, que aún no está en `develop` | Al fusionar la 004, el PR de la 005 muestra solo sus cambios. Si la 004 cambia antes, se trae con un merge normal |
| R2 | Recipientes de medición reales desconocidos (P2) | Lista fija ampliable (T4) |
| R3 | Re-pesar exige otra persona (RN-062) | Es la regla del doble registro; la pantalla lo explica cuando quien digitó intenta corregir |
