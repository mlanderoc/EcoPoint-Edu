# Tareas — Ajustes D13: observaciones de las pruebas de uso

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (13 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `fix/observaciones-pruebas` |

Reglas: en orden, un commit por responsable según `src/TEAM.md`, y en verde `./mvnw verify`, `npm test`, `npm run build` y `npm run lint`. Pruebas nombradas con su criterio. Cada fase deja la aplicación funcionando. Antigravity revisa al final.

## Fase 1 — Base de datos y dominio

- [x] **A01. Script de actualización de la base.** `sql/003_ajustes_d13.sql` (idempotente) y `001_schema.sql` actualizado: `activa` en institución, estado CANCELADO con sus tres datos y su CHECK. *Ajuste:* `recrear-base` no ejecuta el 003, porque `001_schema.sql` ya trae los cambios; el 003 es solo para bases creadas antes. *(Merlín)*
  *Verificación:* `SchemaIT` (R-016); la base `ecopoint` de desarrollo se actualiza sin perder datos.

- [x] **A02. Modelo.**
  - `Institution` con `active`, `activate()` y `deactivate()`.
  - `StudyStatus.CANCELADO`, fuera de `canTransitionTo`.
  - `CharacterizationStudy.cancel(userId, reason, at)`: solo desde EN_EJECUCION o EN_DEPURACION, con motivo.
  - `CharacterizationStudy.requireDeletable()`: solo PLANIFICADO.

  *(Estefanía)*
  *Verificación:* CA-015 (dominio), CA-016, CA-017.

- [x] **A03. Validadores.**
  - `StudyValidator`: excluye los CANCELADOS del solapamiento (RN-013) y rechaza una institución inactiva (RN-006).
  - `UserValidator`: rechaza desactivar a un DOCENTE (RN-087).

  *(Carlos)*
  *Verificación:* CA-005, CA-015 (fechas libres), CA-082.

- [x] **A04. Repositorios.**
  - Institución: `active`, filtro por estado, activar y desactivar.
  - Estudio: cancelación y borrado.
  - Subcategorías e instituciones: `active` al crear.
  - Usuarios: filtro `q`.

  *Ajuste:* las subcategorías ya guardaban `activa`, y el filtro `q` de usuarios se hace en `UserService` (A06), igual que los filtros de rol y estado que ya existen. *(Merlín)*
  *Verificación:* IT de cada repositorio contra `ecopoint_test`.

- [x] **A05. Totales de registros.** `UserActivityRepository` con una consulta por usuario y una agrupada para la tabla (decisión T7). *(Merlín)*
  *Verificación:* IT con la semilla: los totales coinciden con un conteo manual.

## Fase 2 — Servicios y API

- [x] **A06. Servicios.**
  - `StudyService.cancel` y `delete`.
  - `InstitutionService`, `WasteCategoryService` y `GenerationSourceService`: activar, desactivar y crear con estado.
  - `UserService`: activar, filtro `q` y totales.
  - `AuthService.changePassword`.

  *Ajuste:* el perfil y el cambio de contraseña quedaron en un servicio propio, `ProfileService`, en vez de `AuthService`. *(Estefanía)*
  *Verificación:* pruebas unitarias con dobles de los repositorios; CA-004, CA-057, CA-082.

- [x] **A07. Rutas y DTO.** Las rutas de la sección 4 del plan, con `active` en los `Request` y `Response` y el DTO `UserActivityResponse`. *(Carlos)*
  *Verificación:* `@WebMvcTest` de las rutas y permisos (403 para el estudiante); IT de cancelar, eliminar, cambiar contraseña y `GET /users?q=`.
  *Ajuste:* el perfil se consulta con una sola ruta, `GET /auth/me/profile` (datos, cuadrillas y totales), en vez de `/auth/me/activity`. El docente ve los totales de cada usuario en `GET /users`, así que no hace falta `GET /users/{id}/activity`. Las pruebas de las rutas nuevas están en `AdjustmentsD13IT`.

- [x] **A08. Mensajes sin códigos.** Quitar "(RN-021)" y cualquier otro código de los mensajes de excepción visibles para el usuario (R-134). *(Estefanía)*
  *Verificación:* prueba que recorre los mensajes de `BusinessRuleException` en las pruebas del dominio y busca el patrón.

## Fase 3 — Frontend común

- [x] **A09. Cursor de mano.** Regla base en `index.css` (decisión T12). *(Carlos)*
  *Verificación:* revisión en el navegador; `npm run check:mobile`.

- [x] **A10. Atrás y Descartar.**
  - `BackButton`, `useUnsavedChanges` y `ConfirmDiscardModal`.
  - Atrás en Población, Pesar bolsa, Turno, Cuarteo, Densidad, Registro, Detalle y Nuevo estudio.
  - Guardar y Descartar en los formularios de campo y administración.

  *Ajuste:* las ventanas de administración pasan a Guardar y Descartar en A14 y A15, que rehacen esas pantallas. Los botones Atrás miden 48 px, como pide R-301 en las pantallas de campo. *(Carlos)*
  *Verificación:* CA-316.

- [x] **A11. Sin códigos en pantalla.** Quitar los códigos de títulos y mensajes. Agregar la prueba que recorre las pantallas (decisión T13). *(Carlos)*
  *Verificación:* CA-317.

## Fase 4 — Pantallas

- [x] **A12. Fechas del estudio.**
  - `holidays.ts` y `recalculateFrom`.
  - `StudyDatesEditor` en Nuevo estudio y en el Detalle del estudio PLANIFICADO (R-334).

  *(Carlos)*
  *Verificación:* CA-301, CA-311.

- [x] **A13. Eliminar y cancelar estudio.** Botones en el detalle, cada uno con su confirmación; el de cancelar pide motivo. La etiqueta "Cancelado" aparece en la lista. Registro en campo ignora los cancelados. *(Carlos)*
  *Verificación:* CA-315.

- [x] **A14. Activar y desactivar en administración.**
  - `ActiveFilter` y `StatusToggleButton`.
  - Instituciones, Fuentes y Subcategorías con filtro y botón.
  - La casilla "activa" se respeta al crear.

  *(Carlos)*
  *Verificación:* CA-312.

- [x] **A15. Usuarios en tabla.**
  - `UsersTable` con filtros por rol y estado, buscador y paginación.
  - Columna de registros.
  - Activar y desactivar solo en estudiantes.
  - Filas apiladas en el celular.

  *(Carlos)*
  *Verificación:* CA-313; sin desplazamiento horizontal a 360 px.

- [ ] **A16. Mi Perfil.** Datos del usuario, mis registros y cambiar contraseña. Reemplaza la página provisional. *(Carlos)*
  *Verificación:* CA-314; cambio de contraseña con la actual errónea muestra el error en el campo.

- [ ] **A17. Datos simulados.** Rutas y manejadores nuevos en MSW con el mismo contrato que la API. *(Carlos)*
  *Verificación:* `npm test` con `VITE_USE_MOCK_API` por defecto.

## Fase 5 — Cierre

- [ ] **A18. Documentación.** `README.md`, `frontend/README.md` y `docs/base-de-datos.md` con lo nuevo (script de actualización 003, rutas y pantallas). *(Merlín)*

- [ ] **A19. Revisión y PR.** Suite completa en verde y revisión de Antigravity. PR a `develop` con la descripción de las 9 observaciones. *(Merlín)*

- [ ] **A20. Indicadores sin cancelados.** En la rama `feature/006-indicadores-reportes`: `IndicatorService` rechaza un estudio CANCELADO y el selector de Indicadores no lo ofrece (decisión T15). *(Estefanía y Carlos)*
  *Verificación:* prueba unitaria de `IndicatorService` y Vitest del tablero.
