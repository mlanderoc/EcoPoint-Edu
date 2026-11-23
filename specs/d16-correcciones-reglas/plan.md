# Plan — D16: correcciones de reglas

| Campo | Valor |
| --- | --- |
| Estado | **PENDIENTE DE APROBACIÓN** del equipo |
| Specs | Ajustes propuestos a las specs [001](../001-dominio-caracterizacion/spec.md) y [003](../003-api-rest-seguridad/spec.md) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo (pendiente) |
| Rama | `fix/observaciones-pruebas` |
| Fecha | 2026-11-22 |

## 1. Resumen

Corrige errores de reglas encontrados al revisar la aplicación. No incluye el diseño visual, que va en `feature/personalizacion-diseno`.

| # | Corrección | Reglas |
| --- | --- | --- |
| 1 | Solo se agregan estudiantes libres a una cuadrilla | RN-091, CA-085 |
| 2 | Una bolsa pendiente solo la ajusta quien la digitó | RN-092, CA-086 |
| 3 | El cuarteo se reevalúa si cambia el peso del turno | RN-093, CA-087 |
| 4 | Se puede agregar una subcategoría olvidada con historial | RN-094, CA-088 |
| 5 | Fechas y fuentes solo cambian en un estudio planificado; un turno cierra con población y fuentes | RN-095, CA-089 |
| 6 | Roles de docente protegidos y usuarios limitados a su institución | RN-096, CA-091, CA-096, CA-097 |
| 7 | El estudiante solo lee y trabaja en los estudios de su cuadrilla actual | RN-097, CA-093, CA-098 |
| 8 | Una cuadrilla tiene un solo estudio abierto y sus integrantes no están en otro | RN-098, CA-095 |
| 9 | Un cuarteo tiene una sola alerta abierta, que se cierra al resolverse | RN-099, CA-094 |
| 10 | Los estudios terminados no se alteran | RN-100, CA-099 |

## 2. Base de datos

`sql/005_cuadrilla_libre.sql` agrega una cuadrilla libre con dos estudiantes para poder crear estudios en la demostración. Se puede ejecutar varias veces.
