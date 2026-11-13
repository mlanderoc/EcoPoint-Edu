# Plan — Ajustes D13: observaciones de las pruebas de uso

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (13 nov 2026) |
| Specs | Ajustes D13 de las specs [001](../001-dominio-caracterizacion/spec.md), [002](../002-persistencia-postgresql/spec.md), [003](../003-api-rest-seguridad/spec.md) y [004](../004-registro-en-campo/spec.md) (aprobados el 13 nov 2026) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo |
| Rama | `fix/observaciones-pruebas` (sale de `develop`) |
| Fecha | 2026-11-13 |

## 1. Resumen

Las pruebas de uso del 12 y 13 de noviembre dejaron nueve observaciones. Ya se aprobaron como reglas y requisitos en las specs 001 a 004. Este plan dice cómo se construyen.

| # | Observación | Reglas |
| --- | --- | --- |
| 1 | Fechas del estudio recalculadas con días hábiles y festivos | R-330, R-334 |
| 2 | Activar y desactivar fuentes, subcategorías e instituciones, con filtro de inactivos | RN-005, RN-006, RN-043, R-320, R-321, R-324, R-325 |
| 3 | Sin códigos de reglas en pantalla ni en mensajes | R-306, R-134 |
| 4 | Atrás, Guardar y Descartar, cursor de mano | R-307, R-308, R-309 |
| 5 | Mi Perfil | R-132, R-133, R-350 a R-352 |
| 6 | Eliminar un estudio planificado | RN-019, R-335 |
| 7 | Cancelar un estudio iniciado | RN-013, RN-014, RN-018, R-336 |
| 8 | Solo los estudiantes se desactivan | RN-087 |
| 9 | Usuarios en tabla con filtros | R-322 |

## 2. Decisiones

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | Los **festivos de Colombia** se calculan en el frontend, en `features/study/utils/holidays.ts`. Hay tres clases: fijos (1 ene, 1 may, 20 jul, 7 ago, 8 dic, 25 dic); trasladables al lunes siguiente por la Ley 51 de 1983 (Reyes, San José, San Pedro, Asunción, Raza, Todos los Santos, Independencia de Cartagena); y los que dependen de la Pascua (Jueves y Viernes Santo; Ascensión, Corpus Christi y Sagrado Corazón, trasladados al lunes) | Solo la pantalla propone fechas; el backend sigue validando RN-011 (orden creciente) sin exigir días hábiles. Así no hace falta una tabla de festivos |
| T2 | El recálculo (R-330) es una función pura `recalculateFrom(dates, index)`: deja los días hasta `index` y llena los posteriores con los días hábiles siguientes | Se prueba sin pantalla (CA-311) y la usan la creación y la edición |
| T3 | **Cancelar** es una ruta propia, `POST /studies/{id}/cancel` con `{ reason }`, y no un valor más de `POST /studies/{id}/status` | Exige motivo y guarda quién y cuándo, como la reapertura. CANCELADO no entra en `canTransitionTo`, para que el orden normal (RN-014) no cambie |
| T4 | **Eliminar** es `DELETE /studies/{id}`. El servicio exige PLANIFICADO; la base borra en cascada los días y las fuentes participantes y rechaza el borrado si hay turnos | Doble defensa: regla en el dominio y restricción en la base (R-016) |
| T5 | Activar y desactivar usan rutas `POST .../{id}/activate` y `POST .../{id}/deactivate`, como las que ya tienen fuentes y usuarios. Los listados reciben `?active=true\|false` (sin el parámetro, todos) | Mismo patrón en los cuatro recursos |
| T6 | La casilla "activa" del formulario viaja en el `POST` de creación (`active`, opcional, por defecto `true`) de fuentes, subcategorías e instituciones | Era la causa de que se crearan siempre activas |
| T7 | Los **totales de registros** salen de una sola consulta SQL (`UserActivityRepository`): cuenta por `digitado_por` en población, bolsa, cuarteo y densidad, por `verificado_por` en los cuatro, y las correcciones hechas sobre registros digitados por el usuario | Una consulta por usuario; para la tabla de usuarios, una sola consulta agrupada por usuario |
| T8 | Rutas del perfil: `GET /auth/me/activity` (mis totales) y `POST /auth/me/password` (`currentPassword`, `newPassword`). El docente consulta los de otro con `GET /users/{id}/activity`, y la tabla recibe los totales en `GET /users` | El estudiante nunca llama `/users` (403) |
| T9 | El filtro de inactivos (R-325) es un componente común `ActiveFilter` (Activos / Inactivos / Todos, con ícono de ojo) que guarda la elección en la URL (`?estado=`) | Una sola pieza para tres pantallas; la URL conserva el filtro al volver con Atrás |
| T10 | **Atrás** (R-307) es un componente `BackButton` con destino explícito (ej. el turno, la lista), no `history.back()` | Si la pantalla se abrió desde un enlace directo, `history.back()` sacaría de la app |
| T11 | **Descartar** (R-308) usa un hook `useUnsavedChanges(isDirty)` que pregunta "¿Descartar los cambios?" en un `Modal` al pulsar Atrás o Descartar, y con `beforeunload` al cerrar la pestaña | Un mismo comportamiento en todos los formularios |
| T12 | El cursor de mano (R-309) va en una regla base de `index.css` para `button`, `a`, `[role=button]`, `select`, `summary`, `label[for]` e `input` de tipo casilla, opción, fecha y archivo; con `cursor: not-allowed` cuando están deshabilitados | Tailwind 4 deja los botones con el cursor por defecto; una regla global cubre también los botones que se agreguen después |
| T13 | Los códigos (R-306) se quitan de los textos visibles. Una prueba de Vitest recorre las pantallas renderizadas y falla si encuentra `/\b(R|RN|CA)-\d{3}\b/` (CA-317). Los comentarios y nombres de pruebas los conservan | Evita que vuelvan a aparecer |
| T14 | Usuarios (R-322) en una tabla HTML con paginación del servidor (`page`, `size` = 20) y filtros `role`, `active` y `q` (texto en nombre, apellido o correo). En el celular, cada fila se apila como tarjeta compacta | Con 50 o más estudiantes la lista no se alarga; el filtro trabaja sobre toda la base, no solo la página visible |
| T15 | **Indicadores**: la exclusión de estudios cancelados se hace en la rama `feature/006-indicadores-reportes`, porque `IndicatorService` aún no está en `develop`. Mientras tanto, la lista de estudios del frontend ya oculta los cancelados en Registro en campo | Evita mezclar esta rama con la 006 |

## 3. Base de datos

Script de actualización `sql/003_ajustes_d13.sql`: agrega los cambios a las bases ya creadas, sin borrar ni mover datos, y se puede ejecutar varias veces sin efecto adicional; `001_schema.sql` se actualiza con lo mismo para las bases nuevas.

```sql
ALTER TABLE INSTITUCION ADD COLUMN IF NOT EXISTS activa BOOLEAN NOT NULL DEFAULT TRUE;   -- RN-006
ALTER TYPE estado_estudio ADD VALUE IF NOT EXISTS 'CANCELADO';                           -- RN-018
ALTER TABLE ESTUDIO ADD COLUMN IF NOT EXISTS cancelado_por    BIGINT REFERENCES USUARIO (id),
                    ADD COLUMN IF NOT EXISTS cancelado_en     TIMESTAMP,
                    ADD COLUMN IF NOT EXISTS motivo_cancelacion VARCHAR(500);
-- CHECK: los tres datos de cancelación van juntos y solo en estado CANCELADO.
```

Los días (`DIA_MUESTREO`) y las fuentes participantes (`FUENTE_ESTUDIO`) ya se borran en cascada con el estudio; los turnos lo impiden con `RESTRICT` (RN-019). `recrear-base.ps1` y `recrear-base.sh` no lo necesitan, porque crean la base con `001_schema.sql`, que ya trae los cambios.

## 4. API

| Ruta | Permiso | Cambio |
| --- | --- | --- |
| `GET /institutions?active=` | Sesión | Filtro por estado; la respuesta lleva `active` |
| `POST /institutions` | `MANAGE_ADMINISTRATION` | Acepta `active` |
| `POST /institutions/{id}/activate` · `/deactivate` | `MANAGE_ADMINISTRATION` | Nuevas |
| `POST /institutions/{id}/sources` | `MANAGE_ADMINISTRATION` | Acepta `active` |
| `GET /institutions/{id}/sources?active=` | Sesión | Hoy `active=false` devuelve todas; pasa a filtrar de verdad, y sin parámetro devuelve todas |
| `GET /waste-categories?active=` | Sesión | Filtro por estado |
| `POST /waste-categories` | `MANAGE_ADMINISTRATION` | Acepta `active` |
| `POST /waste-categories/{id}/activate` · `/deactivate` | `MANAGE_ADMINISTRATION` | Nuevas |
| `GET /users?role=&active=&q=&page=&size=` | `MANAGE_ADMINISTRATION` | Nuevo filtro `q`; cada usuario lleva `activity` (totales) |
| `POST /users/{id}/activate` | `MANAGE_ADMINISTRATION` | Nueva; `/deactivate` rechaza a un DOCENTE (RN-087) |
| `GET /users/{id}/activity` | `MANAGE_ADMINISTRATION` | Nueva |
| `GET /auth/me` | Sesión | Agrega institución por nombre y cuadrillas |
| `GET /auth/me/activity` | Sesión | Nueva (R-133) |
| `POST /auth/me/password` | Sesión | Nueva (R-132); 400 si la actual no coincide |
| `POST /studies/{id}/cancel` | `MANAGE_STUDIES` | Nueva (RN-018) |
| `DELETE /studies/{id}` | `MANAGE_STUDIES` | Nueva (RN-019); 204 |
| `POST /studies` | `MANAGE_STUDIES` | Rechaza una institución inactiva (RN-006) |

El permiso del estudio es el que ya usa `StudyService` para crear y cambiar estado. La validación de solapamiento (RN-013) excluye los estudios CANCELADOS.

## 5. Frontend

| Pieza | Dónde |
| --- | --- |
| `holidays.ts`, `samplingDays.ts` (recálculo) | `features/study/utils/` |
| `StudyDatesEditor` (8 fechas con recálculo), usado al crear y en el detalle | `features/study/components/` |
| Detalle del estudio: editar fechas, Eliminar estudio, Cancelar estudio con motivo | `features/study/pages/StudyDetailPage.tsx` |
| `ActiveFilter`, `StatusToggleButton` | `components/ui/` |
| Instituciones, Fuentes, Subcategorías con filtro y activar/desactivar | `features/admin/pages/` |
| `UsersTable` con filtros, buscador, paginación y totales | `features/admin/components/` |
| Mi Perfil: datos, mis registros, cambiar contraseña | `features/profile/` (nuevo) |
| `BackButton`, `useUnsavedChanges`, `ConfirmDiscardModal` | `components/ui/`, `hooks/` |
| Regla de cursor | `index.css` |
| Rutas y manejadores nuevos en MSW | `mocks/` |

## 6. Pruebas

| Criterio | Prueba |
| --- | --- |
| CA-004, CA-005, CA-015, CA-016, CA-017, CA-057, CA-082 | Unitarias del dominio y validadores |
| R-016 | `SchemaIT`: cancelación, borrado en cascada y rechazo con turnos |
| Rutas nuevas | `@WebMvcTest` e IT contra `ecopoint_test` |
| CA-311 | Vitest de `holidays.ts` y `samplingDays.ts` (2026: 16 nov festivo; Semana Santa) |
| CA-312, CA-313, CA-314, CA-315, CA-316, CA-317 | Vitest con MSW |
| R-309 | Revisión con `npm run check:mobile` y a mano en el navegador |

## 7. Riesgos

| Riesgo | Mitigación |
| --- | --- |
| La 006 aún no está en `develop` y también toca `App.tsx`, `Sidebar.tsx` y `handlers.ts` | Cambios pequeños en esos archivos; se resuelven al fusionar. T15 lleva lo de Indicadores a la 006 |
| `ALTER TYPE ... ADD VALUE` no corre dentro de una transacción en algunas versiones | El script lo ejecuta fuera de `BEGIN`/`COMMIT` |
| Muchos cambios a la vez para la presentación | Fases independientes: cada una deja la app funcionando |
