# Tareas — Spec 002: Persistencia en PostgreSQL

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (11 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/002-persistencia-postgresql` (desde `develop`) |

Reglas:

- Las tareas se hacen **en orden**. Cada una termina con su verificación cumplida y **un commit por responsable** (autor y committer según la sección 6 del plan).
- `./mvnw test` (unitarias) debe pasar siempre. `./mvnw verify` (integración) debe pasar desde T04.
- Las pruebas se nombran con su criterio: `caNNN_descripcion()`.

## Fase 0 — Base del proyecto

- [x] **T00. Spring Boot y PostgreSQL.** `pom.xml` con padre Spring Boot 3.5, `spring-boot-starter-jdbc`, driver PostgreSQL, `spring-boot-starter-test` y `maven-failsafe-plugin`. Clase `EcoPointApplication`. `application.properties` con `ECOPOINT_DB_URL`, `ECOPOINT_DB_USER` y `ECOPOINT_DB_PASSWORD` (sin valor por defecto para la contraseña). `.env.example` documentado.
  *Verificación:* `./mvnw test` sigue con las 264 pruebas en verde; `git grep -i password` no muestra contraseñas reales.

## Fase 1 — Esquema y semilla

- [x] **T01. Esquema.** `sql/001_schema.sql`: tipos `ENUM`, las 20 tablas del plan (sección 3.2) con sus PK, FK, `UNIQUE`, `CHECK`, columnas generadas, *trigger* de solo inserción en `CORRECCION`, comentarios de política de borrado y las 2 vistas. Empieza borrando todo lo que exista.
  *Verificación:* se ejecuta dos veces seguidas con `psql` sin errores.

- [x] **T02. Semilla.** `sql/002_seed.sql`: las 15+ subcategorías (R-040), la I.E. Prudencia Daza con sus 8 fuentes ficticias y polígonos provisionales (R-041), un docente y un estudiante de demostración (R-042), un estudio CERRADO completo (R-043) y un estudio EN_EJECUCION a medio llenar (R-044).
  *Verificación:* 001 + 002 dos veces seguidas sin errores, con el mismo contenido (CA-100, CA-101).

- [ ] **T03. Scripts de recreación.** `sql/recrear-base.ps1` y `sql/recrear-base.sh` (crean la base si no existe y ejecutan 001 + 002).
  *Verificación:* un solo comando deja lista la base `ecopoint`.

- [ ] **T04. Pruebas del esquema.** Infraestructura de pruebas de integración (recrear `ecopoint_test` antes de la suite) y `SchemaIT`.
  *Verificación:* `./mvnw verify` pasa con CA-100 a CA-104 y CA-106.

## Fase 2 — Repositorios de configuración (Merlín)

- [ ] **T05. Traducción de errores.** Utilidad que convierte `DataAccessException` en `PersistenceException` con la operación en español (R-021).
  *Verificación:* prueba con una consulta inválida.

- [ ] **T06. Institución y fuentes.** `InstitutionRepository`, `GenerationSourceRepository` y sus implementaciones JDBC (incluye la geometría `jsonb`).
  *Verificación:* `InstitutionRepositoryIT`, `GenerationSourceRepositoryIT` (CA-110, CA-104 desde la aplicación).

- [ ] **T07. Subcategorías, usuarios y cuadrillas.** `WasteCategoryRepository`, `UserRepository`, `CrewRepository` y sus implementaciones.
  *Verificación:* sus `*RepositoryIT` (CA-110).

- [ ] **T08. Estudio.** `StudyRepository` (estudio con sus 8 días, reaperturas, fuentes participantes y conteos de pendientes).
  *Verificación:* `StudyRepositoryIT` (CA-110; los conteos alimentan RN-016).

## Fase 3 — Repositorios de campo (Estefanía)

- [ ] **T09. Reconstrucción del dominio.** Métodos `restore(...)` en `CharacterizationStudy`, `SamplingShift` y los registros verificables, para rearmarlos desde la base sin repetir validaciones (riesgo R1 del plan).
  *Verificación:* pruebas unitarias de cada `restore`.

- [ ] **T10. Turnos y bolsas.** `ShiftRepository` (agregado completo) y `WasteBagRepository`.
  *Verificación:* `ShiftRepositoryIT`, `WasteBagRepositoryIT` (CA-110; peso neto y densidad leídos de las columnas generadas: CA-106).

- [ ] **T11. Cuarteo y densidad.** `QuarteringRepository` (transaccional) y `DensityRepository`.
  *Verificación:* `QuarteringRepositoryIT` (CA-105, CA-103 desde la aplicación), `DensityRepositoryIT`.

- [ ] **T12. Verificación y correcciones.** `VerificationRepository` y `CorrectionRepository`.
  *Verificación:* `CorrectionRepositoryIT` (CA-107).

- [ ] **T13. Alertas y evidencias.** `AlertRepository` y `EvidenceRepository`.
  *Verificación:* sus `*RepositoryIT` (CA-110).

## Fase 4 — Cierre

- [ ] **T14. Configuración.** `ConfigurationIT`: la aplicación se conecta a otra base solo cambiando `ECOPOINT_DB_URL` (CA-108).
  *Verificación:* la prueba pasa.

- [ ] **T15. Documentación.** `docs/base-de-datos.md` con el diagrama Mermaid de tablas y relaciones, el propósito de cada tabla y las decisiones de diseño (R-060). README: instalar PostgreSQL, definir las variables y recrear la base (R-053). Actualizar `docs/layer-diagram.md`.
  *Verificación:* los diagramas se ven en GitHub.

- [ ] **T16. Revisión final y PR.** Revisar la trazabilidad (CA-100 a CA-110), `git grep` de contraseñas (CA-109), `./mvnw verify` en verde. Revisión de Antigravity y PR a `develop`.
  *Verificación:* `./mvnw verify` en BUILD SUCCESS; PR abierto.
