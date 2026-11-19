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

- [ ] **B05. Revisión de Antigravity** y PR a `develop`.
