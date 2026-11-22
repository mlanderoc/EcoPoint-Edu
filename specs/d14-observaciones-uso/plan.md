# Plan — Ajustes D14: segunda ronda de observaciones de uso

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (18 nov 2026) |
| Specs | Ajustes D14 de las specs [001](../001-dominio-caracterizacion/spec.md), [002](../002-persistencia-postgresql/spec.md), [003](../003-api-rest-seguridad/spec.md) y [004](../004-registro-en-campo/spec.md) (aprobados el 18 nov 2026) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo |
| Rama | `fix/observaciones-pruebas` (la misma de D13, puesta al día con `develop`) |
| Fecha | 2026-11-18 |

## 1. Resumen

La prueba de uso del 18 de noviembre dejó cinco observaciones. Las tres primeras solo cambian pantallas; las dos últimas tocan la base, el dominio y la API.

| # | Observación | Reglas |
| --- | --- | --- |
| 1 | En el menú del docente, Configuración del estudio va antes que Registro en campo | R-311, CA-318 |
| 2 | El filtro de activos no lleva el ícono de ojo | R-325, CA-320 |
| 3 | Al crear un estudio se elige la institución, y la lista de estudios se filtra por institución | R-330, R-337, CA-319 |
| 4 | Cada estudio tiene una cuadrilla; el turno la toma del estudio | RN-088, R-331, R-341, CA-083, CA-322 |
| 5 | En Registro en campo se elige el estudio en ejecución; el estudiante solo ve y registra en los de su cuadrilla | RN-089, R-340, CA-084, CA-213, CA-321 |

## 2. Decisiones

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | El filtro de estudios no tiene la opción "Todas": se ve una institución a la vez. | La API lista los estudios de una institución; así no se cambia el backend. |
| T2 | La lista de instituciones para crear un estudio solo trae las activas; la del filtro trae todas. | Una institución inactiva no recibe estudios nuevos (RN-006), pero sus estudios viejos se siguen consultando. |
| T3 | La pantalla de Fuentes deja elegir la institución (antes estaba fija en la 1). | Sin esto no se pueden crear fuentes para otra institución, y su estudio quedaría sin fuentes. |
| T4 | Nueva bolsa y Cuarteo toman los límites de la institución **del estudio**, no la del usuario. | Con estudios de varias instituciones, los límites de otra institución darían alertas equivocadas. |
| T5 | En la barra del celular solo cambia el orden; Alertas se queda. | El encabezado no tiene otra forma de llegar a las alertas. |
| T6 | `ESTUDIO.id_cuadrilla` admite nulo en la base, y el servicio lo exige al crear. El script 004 les pone a los estudios existentes la cuadrilla de sus turnos. | Los estudios viejos no se pierden ni rompen la actualización. |
| T7 | La cuadrilla del estudio se elige entre las cuadrillas del docente (`GET /crews`), sin exigir que sean de la institución del estudio. | Los estudiantes de la UPC caracterizan colegios: la cuadrilla no es de la institución estudiada. |
| T8 | El permiso por cuadrilla se valida en `AccessPolicy.requireCrewMember`, que llaman los servicios de turno, bolsas, cuarteo y densidad. | La API lo impide aunque alguien se salte la pantalla. |
| T9 | `GET /studies/field` filtra en el servicio los estudios EN_EJECUCION con `StudyRepository.findByStatus`. | Son pocos estudios; no hace falta una consulta especial. |

## 3. Archivos

- `frontend/src/components/layout/Sidebar.tsx` y `BottomNav.tsx`: orden del menú.
- `frontend/src/components/ui/ActiveFilter.tsx`: sin el botón de ojo.
- `frontend/src/features/admin/components/InstitutionSelect.tsx` (nuevo): lista de instituciones reutilizable; `frontend/src/features/study/services/studySettings.ts` (nuevo): límites de la institución del estudio.
- `frontend/src/features/study/pages/NewStudyPage.tsx` y `StudiesPage.tsx`: institución al crear y filtro.
- `frontend/src/features/admin/pages/SourcesPage.tsx`: institución elegible.
- `frontend/src/features/field/pages/NewBagPage.tsx` y `frontend/src/features/quality/pages/QuarteringPage.tsx`: límites de la institución del estudio.
- Backend: `sql/004_ajustes_d14.sql`, `001_schema.sql` y `002_seed.sql`; `CharacterizationStudy` (cuadrilla); `AccessPolicy.requireCrewMember`; `StudyRepository.findByStatus`; `StudyService` (crear con cuadrilla, cambiarla, estudios para el campo); `ShiftService`, `WasteBagService`, `QuarteringService` y `DensityService` (permiso por cuadrilla; el turno toma la cuadrilla del estudio); `StudyController` y sus DTO.
- Frontend: Nuevo estudio y detalle (cuadrilla), Registro en campo (lista de estudios), Abrir turno (cuadrilla solo visible).
- Mocks y pruebas de Vitest y JUnit.

## 4. Pruebas

- `layout.test.tsx`: CA-318 (orden del menú en escritorio y celular).
- `admin-active-filter.test.tsx`: CA-320 (sin ojo; el filtro sigue funcionando con los tres botones).
- `study-institution.test.tsx` (nuevo): CA-319.
- JUnit: CA-083 y CA-084 (dominio y servicios), CA-213 (API), IT del repositorio de estudios con la cuadrilla.
- `study-crew.test.tsx` (nuevo): CA-321 y CA-322.
