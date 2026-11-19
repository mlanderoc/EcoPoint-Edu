# Tareas — Ajustes D14: segunda ronda de observaciones de uso

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (18 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `fix/observaciones-pruebas` |

Reglas: un commit por responsable según `src/TEAM.md`, y en verde `npm test`, `npm run typecheck`, `npm run lint` y `npm run build`. Pruebas nombradas con su criterio. Antigravity revisa al final.

- [x] **B01. Specs.** R-311, R-321, R-325, R-330, R-337 y CA-318 a CA-320 en la spec 004; D14 en `specs/README.md`; este plan y sus tareas. *(Merlín)*

- [x] **B02. Menú del docente.** Configuración del estudio antes que Registro en campo, en el menú lateral y en la barra del celular. *(Carlos)*
  *Verificación:* CA-318.

- [x] **B03. Filtro sin ojo.** Se quita el botón de ojo de `ActiveFilter`. *(Carlos)*
  *Verificación:* CA-320, CA-312.

- [x] **B04. Institución del estudio.**
  - `InstitutionSelect` reutilizable.
  - Nuevo estudio: lista de instituciones activas; al cambiarla se recargan las fuentes.
  - Lista de estudios: filtro por institución en `?institucion=` y nombre de la institución en cada estudio.
  - Fuentes: institución elegible.
  - Nueva bolsa y Cuarteo: límites de la institución del estudio.

  *(Carlos)*
  *Verificación:* CA-319.

- [ ] **B05. Base de datos.** `sql/004_ajustes_d14.sql` (idempotente; llena la cuadrilla de los estudios existentes con la de sus turnos), `001_schema.sql` y `002_seed.sql`. *(Merlín)*
  *Verificación:* `SchemaIT`; la base de desarrollo se actualiza sin perder datos.

- [ ] **B06. Dominio y validación.** `CharacterizationStudy` con `crewId` y `assignCrew` (solo PLANIFICADO); `AccessPolicy.requireCrewMember`. *(Estefanía y Carlos)*
  *Verificación:* CA-083, CA-084 (dominio).

- [ ] **B07. Repositorio.** `JdbcStudyRepository` guarda y lee la cuadrilla; `findByStatus`. *(Merlín)*
  *Verificación:* IT del repositorio contra `ecopoint_test`.

- [ ] **B08. Servicios.** `StudyService`: crear con cuadrilla, `changeCrew`, `fieldStudies`. `ShiftService.open` toma la cuadrilla del estudio. Permiso por cuadrilla en población, sin generación, bolsas, cuarteo y densidad. *(Estefanía; cuarteo y densidad, Carlos)*
  *Verificación:* CA-083, CA-084.

- [ ] **B09. API.** `crewId` en la creación, `PUT /studies/{id}/crew`, `GET /studies/field`; `OpenShiftRequest` sin cuadrilla. *(Carlos)*
  *Verificación:* CA-213.

- [ ] **B10. Pantallas.** Nuevo estudio y detalle con la cuadrilla; Registro en campo con la lista de estudios; Abrir turno con la cuadrilla solo visible. *(Carlos)*
  *Verificación:* CA-321, CA-322.

- [ ] **B11. Revisión de Antigravity** y PR a `develop`.
