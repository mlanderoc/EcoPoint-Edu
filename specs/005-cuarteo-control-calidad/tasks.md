# Tareas — Spec 005: Cuarteo, densidad y control de calidad

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (12 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/005-cuarteo-control-calidad` |

Reglas: en orden, un commit por responsable, `./mvnw verify`, `npm test`, `npm run build` y `npm run lint` en verde. Pruebas nombradas con su criterio. Antigravity revisa al cerrar cada fase.

## Fase 1 — Consultas nuevas en la API

- [x] **T400. Servicio.** En `VerificationService`: resumen de depuración por turno (R-424), registros de un turno y un registro por tipo e id, con sus valores (R-420, R-421). *(Estefanía)*
  *Verificación:* pruebas unitarias de cada consulta.

- [x] **T401. Rutas.** `GET /studies/{id}/verification-summary`, `GET /shifts/{id}/records`, `GET /records/{tipo}/{id}` y su DTO `VerifiableRecordResponse`; `VerificationSummaryIT` con la semilla. *(Carlos)*
  *Verificación:* CA-410 en el backend; `@WebMvcTest` de las rutas.

## Fase 2 — Cuarteo y densidad

- [x] **T402. Utilidad de control en vivo.** Suma, diferencia en kg y %, estado contra la tolerancia, rango de submuestra y "muestra completa". *(Carlos)*
  *Verificación:* CA-400, CA-401, CA-402, CA-403.

- [x] **T403. Pantalla de cuarteo.** Subcategorías por grupo, aviso de peligrosos, barra en vivo, guardado, composición, aviso de no conforme y anulación con motivo. Accesos desde el turno. *(Carlos)*
  *Verificación:* CA-403, CA-405, CA-406 en Vitest.

- [x] **T404. Pantalla de densidad.** Volumen y peso con la densidad en vivo y los recipientes frecuentes. *(Carlos)*
  *Verificación:* CA-407.

## Fase 3 — Verificación, historial y alertas

- [ ] **T405. Bandeja de verificación y resumen.** Selector de estudio y turno, resumen de depuración, lista de registros con filtros, "Coincide con el papel", selección múltiple. *(Carlos)*
  *Verificación:* CA-408 y CA-410 en Vitest.

- [ ] **T406. Registro, corrección e historial.** *(Carlos)*
  *Verificación:* CA-404 (re-pesaje como corrección), CA-409 y CA-412.

- [ ] **T407. Alertas.** Bandeja con filtros, enlace al registro, atención con observación y contador en el menú. *(Carlos)*
  *Verificación:* CA-411.

## Fase 4 — Cierre

- [ ] **T408. Revisión final y PR.** Trazabilidad CA-400 a CA-412, `check:mobile` de las pantallas nuevas, README del frontend, revisión de Antigravity. *(Merlín)*
  *Verificación:* veredicto "APROBADO PARA PR".
