# Plan — D15: personalización del diseño

| Campo | Valor |
| --- | --- |
| Estado | **PENDIENTE DE APROBACIÓN** del equipo |
| Specs | Ajustes propuestos a las specs [004](../004-registro-en-campo/spec.md) y [006](../006-indicadores-reportes/spec.md) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo (pendiente) |
| Rama | `feature/personalizacion-diseno`, creada desde `fix/observaciones-pruebas` porque usa la cuadrilla y la institución de D14 |
| Fecha | 2026-11-21 |

## 1. Resumen

Lleva al frontend los tres diseños revisados por el equipo: bienvenida, inicio de sesión y panel con sus módulos. No cambia el backend, la API ni la base de datos.

| # | Cambio | Reglas |
| --- | --- | --- |
| 1 | Página pública de bienvenida en `/` | R-353, CA-324 |
| 2 | Inicio de sesión con la foto de la UPC y Eco saludando, sin frases ni cambios de expresión | R-354 |
| 3 | Menú verde, encabezado de módulo y barra del celular con "Más" | R-355, CA-327 |
| 4 | Pantalla Inicio con el recorrido de los 16 turnos, las fuentes, la composición y lo pendiente | R-356, CA-325 |
| 5 | Pasos del turno y paso automático al formulario al elegir la fuente | R-357 |
| 6 | Un estudio cerrado no muestra el botón Abrir | R-358, CA-326 |
| 7 | Indicadores: selector de estudio siempre visible y comparación de jornadas en lugar de Calidad del dato | R-520, R-525, R-526 |

## 2. Decisiones

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | La rama sale de la de D14 y se fusiona después de ella. | El diseño muestra la cuadrilla y la institución del estudio. |
| T2 | Inicio solo usa servicios existentes. | D15 no cambia la API. |
| T3 | En Inicio, bolsas y kilogramos son los del turno elegido. | La API entrega ese detalle por turno. |
| T4 | El cuarteo conserva su control en vivo del 2 %. | Es una regla de la spec 005; solo se omite el círculo y la barra gráfica del diseño. |
| T5 | Eco tampoco dice frases en el panel. | Se propone igual que en el login. **Falta que el equipo lo confirme.** |
| T6 | Reabrir un estudio cerrado (RN-017) sigue en su detalle, pero la lista ya no lleva a él. | Se quitó Abrir por la observación del equipo. **Falta decidir cómo se llega a reabrir.** |
