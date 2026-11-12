# Tareas — Spec 001: Dominio de caracterización

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (11 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/001-dominio-caracterizacion` (desde `develop`) |

Reglas:

- Las tareas se hacen **en orden**. Cada una termina con su verificación cumplida y **un commit** (mensaje en español, autor un integrante).
- Una tarea no se marca `[x]` si `./mvnw test` falla.
- Cada prueba se nombra con su criterio: `caNNN_descripcion()` o `rnNNN_descripcion()`.

## Fase 0 — Preparación

- [x] **T00. Tag del modelo anterior.** Crear el tag `v1-modelo-anterior` sobre el `develop` actual y subirlo.
  *Verificación:* el tag aparece en GitHub.

- [x] **T01. Base del proyecto.** Agregar el Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`), JUnit Jupiter 5.11 y `maven-surefire-plugin` 3.5 al `pom.xml`, y quitar `exec.mainClass`. Agregar `target/` al `.gitignore`.
  *Verificación:* `./mvnw test` corre y termina en BUILD SUCCESS.

- [x] **T02. Eliminar el código del modelo anterior** (plan, sección 2): `ui/`, `persistence/`, `service/`, `Main.java`, las 6 clases del modelo de puntos ecológicos y `Zone`, y los 2 validadores viejos. Ajustar lo mínimo para que compile lo que queda.
  *Verificación:* `./mvnw compile` sin errores; `git grep -n "EcologicalPoint\|Zone\b\|Swing"` en `src/` no encuentra nada.

## Fase 1 — Excepciones, enums y objetos de valor

- [x] **T03. Excepciones nuevas.** `QualityControlException` y `AccessDeniedException` (plan, sección 6).
  *Verificación:* `ExceptionsTest`: cada una devuelve su `errorCode` y su mensaje en español.

- [x] **T04. Enums del dominio.** `WasteGroup`, `SourceType`, `ShiftType`, `ShiftStatus`, `StudyStatus`, `VerificationStatus`, `QualityStatus`, `AlertType`, `EvidenceTarget`.
  *Verificación:* `StudyStatusTest` (CA-013: PLANIFICADO → CERRADO no permitido; cada transición válida sí); `WasteGroupTest` (`isRecoverable`, `isHazardous`).

- [x] **T05. Roles y permisos.** `Permission` y `UserRole` con la matriz RN-081.
  *Verificación:* `UserRolePermissionTest`: una prueba por cada celda de la matriz (4 roles × 11 acciones).

- [x] **T06. Objetos de valor.** `GeoPoint`, `GeoPolygon`, `BagLabel`, `CharacterizationSettings`.
  *Verificación:* `GeoPolygonTest` (CA-003), `BagLabelTest` (formato `AU-D3-M` y `AU-D3-M-2`), `CharacterizationSettingsTest` (valores por defecto).

## Fase 2 — Institución, fuentes, residuos y usuarios

- [x] **T07. Institución y fuentes.** Reescribir `Institution`; crear `GenerationSource` y `GenerationSourceValidator`.
  *Verificación:* `GenerationSourceValidatorTest`: CA-001, CA-002, CA-003; RN-005 (fuente inactiva).

- [x] **T08. Residuos.** Reescribir `WasteCategory` (subcategoría con `WasteGroup`); crear `WasteCategoryValidator`.
  *Verificación:* `WasteCategoryValidatorTest`: RN-041 (nombre único) y RN-042 (no cambia de grupo si tiene resultados).

- [x] **T09. Usuarios y cuadrillas.** Reescribir `User` (rol enum) y `UserValidator`; crear `Crew`, `CrewValidator` y `AccessPolicy`.
  *Verificación:* `UserValidatorTest` (CA-080, RN-084), `CrewValidatorTest` (CA-081), `AccessPolicyTest` (lanza `AccessDeniedException`).

## Fase 3 — Estudio y turnos

- [ ] **T10. Estudio de caracterización.** `CharacterizationStudy`, `SamplingDay` y `StudyValidator`.
  *Verificación:* `CharacterizationStudyTest` (CA-010, `overlaps`), `StudyValidatorTest` (CA-011, CA-012, CA-014, RN-015).

- [ ] **T11. Verificación (base común).** `VerificationInfo`, `VerifiableRecord` (abstracta), `Correction`.
  *Verificación:* `VerificationInfoTest`: un registro nuevo queda PENDIENTE; `markVerified` y `markCorrected` guardan usuario y fecha.

- [ ] **T12. Turnos y población.** `ShiftPopulation`, `SamplingShift` y `ShiftValidator`.
  *Verificación:* `SamplingShiftTest` y `ShiftValidatorTest`: CA-020, CA-021, CA-022, CA-023; RN-021 (no se registra en un turno cerrado).

## Fase 4 — Bolsas, cuarteo y densidad

- [ ] **T13. Bolsas.** `WasteBag` y `WasteBagValidator`; generación del rótulo en `SamplingShift.nextLabelFor(...)`.
  *Verificación:* `WasteBagTest` (CA-032), `WasteBagValidatorTest` (CA-031, CA-033, RN-030), `SamplingShiftTest` (CA-030, RN-032).

- [ ] **T14. Cuarteo.** Reescribir `Quartering` y `QuarteringResult`; reescribir `QuarteringValidator`.
  *Verificación:* `QuarteringTest` (CA-052, CA-053, CA-054, `annul`), `QuarteringValidatorTest` (CA-050, CA-051, CA-055, CA-056, RN-055).

- [ ] **T15. Densidad.** `DensityMeasurement` y `DensityValidator`.
  *Verificación:* `DensityMeasurementTest`: 31,20 kg en 0,208 m³ = 150,00 kg/m³; RN-057 (una por turno, valores > 0).

## Fase 5 — Calidad, alertas y evidencias

- [ ] **T16. Verificación y correcciones.** `VerificationValidator`.
  *Verificación:* `VerificationValidatorTest`: CA-060, CA-061, CA-062; motivo obligatorio en la corrección.

- [ ] **T17. Alertas.** Reescribir `Alert`; crear `AlertFactory`.
  *Verificación:* `AlertFactoryTest` (alerta de CA-033 y de CA-053), `AlertTest` (RN-073: nota obligatoria, no se atiende dos veces).

- [ ] **T18. Evidencias.** Reescribir `Evidence`; crear `EvidenceValidator`.
  *Verificación:* `EvidenceValidatorTest`: JPG y PNG ≤ 5 MB aceptados; otros formatos o > 5 MB rechazados.

## Fase 6 — Cierre

- [ ] **T19. Mapa de excepciones.** `ExceptionMappingTest`: prueba parametrizada que confirma que cada rechazo lanza el tipo correcto (CA-090) con mensaje en español.
  *Verificación:* la prueba pasa.

- [ ] **T20. Documentación.** Rehacer `docs/class-diagrama.md` con el modelo nuevo (Mermaid), actualizar `docs/exception-hierarchy-diagram.md` con las 2 excepciones nuevas, y poner una nota en el `README.md`: "En reestructuración; ver `specs/`".
  *Verificación:* los diagramas se ven en GitHub.

- [ ] **T21. Revisión final y PR.** Revisar la tabla de trazabilidad del plan (sección 8): cada CA tiene su prueba y todas pasan. Abrir el PR a `develop` con el resumen y la lista de CA cubiertos. Revisa: Antigravity.
  *Verificación:* `./mvnw test` en BUILD SUCCESS; PR abierto.
