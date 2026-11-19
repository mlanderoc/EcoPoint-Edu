# Plan — Ajustes D14: segunda ronda de observaciones de uso

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (18 nov 2026) |
| Specs | Ajustes D14 de la spec [004](../004-registro-en-campo/spec.md) (aprobados el 18 nov 2026) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo |
| Rama | `fix/observaciones-pruebas` (la misma de D13, puesta al día con `develop`) |
| Fecha | 2026-11-18 |

## 1. Resumen

La prueba de uso del 18 de noviembre dejó tres observaciones. Solo cambian pantallas: la API ya recibe la institución al crear y al listar estudios.

| # | Observación | Reglas |
| --- | --- | --- |
| 1 | En el menú del docente, Configuración del estudio va antes que Registro en campo | R-311, CA-318 |
| 2 | El filtro de activos no lleva el ícono de ojo | R-325, CA-320 |
| 3 | Al crear un estudio se elige la institución, y la lista de estudios se filtra por institución | R-330, R-337, CA-319 |

## 2. Decisiones

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | El filtro de estudios no tiene la opción "Todas": se ve una institución a la vez. | La API lista los estudios de una institución; así no se cambia el backend. |
| T2 | La lista de instituciones para crear un estudio solo trae las activas; la del filtro trae todas. | Una institución inactiva no recibe estudios nuevos (RN-006), pero sus estudios viejos se siguen consultando. |
| T3 | La pantalla de Fuentes deja elegir la institución (antes estaba fija en la 1). | Sin esto no se pueden crear fuentes para otra institución, y su estudio quedaría sin fuentes. |
| T4 | Nueva bolsa y Cuarteo toman los límites de la institución **del estudio**, no la del usuario. | Con estudios de varias instituciones, los límites de otra institución darían alertas equivocadas. |
| T5 | En la barra del celular solo cambia el orden; Alertas se queda. | El encabezado no tiene otra forma de llegar a las alertas. |

## 3. Archivos

- `frontend/src/components/layout/Sidebar.tsx` y `BottomNav.tsx`: orden del menú.
- `frontend/src/components/ui/ActiveFilter.tsx`: sin el botón de ojo.
- `frontend/src/features/admin/components/InstitutionSelect.tsx` (nuevo): lista de instituciones reutilizable; `frontend/src/features/study/services/studySettings.ts` (nuevo): límites de la institución del estudio.
- `frontend/src/features/study/pages/NewStudyPage.tsx` y `StudiesPage.tsx`: institución al crear y filtro.
- `frontend/src/features/admin/pages/SourcesPage.tsx`: institución elegible.
- `frontend/src/features/field/pages/NewBagPage.tsx` y `frontend/src/features/quality/pages/QuarteringPage.tsx`: límites de la institución del estudio.
- Mocks y pruebas de Vitest.

## 4. Pruebas

- `layout.test.tsx`: CA-318 (orden del menú en escritorio y celular).
- `admin-active-filter.test.tsx`: CA-320 (sin ojo; el filtro sigue funcionando con los tres botones).
- `study-institution.test.tsx` (nuevo): CA-319.
