# Spec 005 — Cuarteo, densidad y control de calidad

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Antigravity / Claude |
| Requerimientos | RF-06, RF-07, RF-08, RF-09, RF-10 |
| Depende de | Spec 004 aprobada; decisiones D8, D9 |
| Fecha | 2026-11-11 |

## 1. Objetivo

Registrar en cada turno el **cuarteo** (submuestra de ≈ 50 kg clasificada por subcategoría) y la **medición de densidad**, aplicar el **control de calidad del 2 %**, y ofrecer el flujo de **doble registro y depuración**: cotejar cada dato con la planilla en papel antes de calcular la línea base.

Esta spec es la que garantiza la **calidad del dato**. El póster la señala como lo que "sostiene" la caracterización.

## 2. Requerimientos que cubre

| RF | Cómo se cubre |
| --- | --- |
| RF-06 | Clasificación por las subcategorías de los 4 grupos |
| RF-07 | Pantalla de cuarteo por turno |
| RF-08 | Pantalla de medición de densidad |
| RF-09 | Indicador en vivo del 2 %, estado CONFORME / NO_CONFORME, alerta |
| RF-10 | Pantallas de verificación, corrección e historial |

Reglas aplicadas: RN-040 a RN-064 y RN-070 a RN-073 (spec 001).

## 3. Actores

- **Cuadrilla** (docente y estudiantes): registra el cuarteo y la densidad.
- **Docente y coordinador**: registran los residuos peligrosos (Sin color), verifican, anulan cuarteos y atienden alertas.
- **Estudiante universitario**: verifica registros que no digitó.

## 4. Historias de usuario

- **HU-040.** Como integrante de la cuadrilla, quiero digitar el peso de cada subcategoría y ver en vivo cuánto falta para cuadrar con la submuestra, para saber si debo repesar antes de botar los residuos.
- **HU-041.** Como docente, quiero que el sistema me avise si el cuarteo no cumple el 2 %, para corregirlo el mismo día.
- **HU-042.** Como verificador, quiero ver lado a lado lo digitado y un espacio para confirmar contra la planilla, para cotejar rápido.
- **HU-043.** Como coordinador, quiero ver qué queda pendiente de verificar por día y turno, para saber cuándo puedo cerrar el estudio.
- **HU-044.** Como coordinador, quiero ver el historial de correcciones de cualquier dato, para defender la calidad de la línea base.

## 5. Pantallas y requisitos

### 5.1 Cuarteo

- **R-400** (Debe). Se entra desde el turno abierto (spec 004). Muestra el peso neto total del turno (suma de sus bolsas).
- **R-401** (Debe). Se digita el peso de la submuestra. El sistema valida el rango (RN-051). Si el total del turno es menor que el mínimo, propone "muestra completa" con el peso total.
- **R-402** (Debe). Lista de subcategorías agrupadas por los 4 grupos, cada grupo con su color y su nombre. Se digita el peso de cada una; las vacías cuentan como 0.
- **R-403** (Debe). Las subcategorías del grupo SIN_COLOR solo se pueden editar si el usuario es docente o coordinador (RN-056). Para los demás aparecen bloqueadas, con el aviso "Solo un docente registra residuos peligrosos".
- **R-404** (Debe). **Barra de control en vivo**, siempre visible al digitar: suma clasificada, peso de la submuestra, diferencia en kg y en %, y estado en color: verde "Conforme (≤ 2 %)" o rojo "No conforme (> 2 %)".
- **R-405** (Debe). Al guardar, se muestra la composición por subcategoría y por grupo (RN-053, RN-054), en una tabla y en un gráfico de barras o de torta.
- **R-406** (Debe). Si se guarda NO_CONFORME (RN-052), se crea la alerta (RN-070) y el turno muestra un aviso rojo "Cuarteo no conforme: repesar". Al corregir los pesos se recalcula el estado.
- **R-407** (Debe). Un docente o coordinador puede **anular** un cuarteo NO_CONFORME escribiendo el motivo. Queda en el historial.

### 5.2 Densidad

- **R-410** (Debe). Formulario por turno: volumen del recipiente de medición y peso neto de los residuos en él. Muestra la densidad calculada (RN-057).
- **R-411** (Debería). Lista de recipientes de medición frecuentes con su volumen ya guardado (ej. "Caneca 55 galones = 0,208 m³"), para no digitarlo cada vez.

### 5.3 Verificación (doble registro)

- **R-420** (Debe). Bandeja de verificación filtrable por estudio, día, turno, tipo de registro (población, bolsa, cuarteo, densidad) y estado. Por defecto muestra los PENDIENTES.
- **R-421** (Debe). Cada registro muestra sus datos digitados, quién lo digitó y cuándo. Tiene dos acciones: **"Coincide con el papel"** (pasa a VERIFICADO) y **"Corregir"**.
- **R-422** (Debe). "Corregir" abre el registro con sus valores actuales, pide los valores correctos y un **motivo obligatorio**, y lo deja CORREGIDO (RN-063).
- **R-423** (Debe). Las acciones de verificar y corregir no aparecen en los registros que el propio usuario digitó (RN-062), ni para estudiantes de colegio (RN-064).
- **R-424** (Debe). Resumen de avance de la depuración: por día y turno, cuántos registros hay PENDIENTES, VERIFICADOS y CORREGIDOS, y si hay cuarteos NO_CONFORMES. Indica claramente si el estudio **ya puede cerrarse**.
- **R-425** (Debe). Historial de cada registro: todas sus correcciones con valor anterior, nuevo, motivo, usuario y fecha.
- **R-426** (Debería). Verificar varios registros a la vez (selección múltiple + "Coinciden con el papel").

### 5.4 Alertas

- **R-430** (Debe). Bandeja de alertas con filtros por tipo, estado (pendiente/atendida) y estudio. Cada alerta lleva directo al registro que la causó.
- **R-431** (Debe). Docente o coordinador marcan la alerta como atendida, con una observación obligatoria (RN-073).
- **R-432** (Debería). El menú muestra un contador de alertas pendientes.

## 6. Criterios de aceptación

- **CA-400.** Dado un turno con 120 kg netos, cuando se digita una submuestra de 60 kg, entonces se muestra "Debe estar entre 45 y 55 kg" y no se puede guardar.
- **CA-401.** Dado un turno con 32 kg netos, cuando se abre el cuarteo, entonces el sistema propone "muestra completa" con 32 kg.
- **CA-402.** Dada una submuestra de 50 kg, cuando se han digitado subcategorías que suman 49,2 kg, entonces la barra muestra diferencia 0,80 kg (1,60 %) en verde "Conforme".
- **CA-403.** Dada la misma submuestra, cuando las subcategorías suman 48,5 kg, entonces la barra muestra 1,50 kg (3,00 %) en rojo; al guardar se crea una alerta CUARTEO_NO_CONFORME y el turno muestra "Cuarteo no conforme: repesar".
- **CA-404.** Dado el cuarteo no conforme de CA-403, cuando se corrige un peso y la suma queda en 49,5 kg, entonces pasa a CONFORME y la corrección queda en el historial.
- **CA-405.** Dado un ESTUDIANTE_UNIVERSITARIO en la pantalla de cuarteo, cuando mira las subcategorías de Sin color, entonces están bloqueadas con el aviso de R-403.
- **CA-406.** Dados papel 10 kg, restos de comida 25 kg e icopor 15 kg, cuando se guarda el cuarteo, entonces la composición muestra 20 %, 50 % y 30 %, y por grupo Blanco 20 %, Verde 50 %, Negro 30 %, Sin color 0 %.
- **CA-407.** Dado un recipiente de 0,208 m³ con 31,20 kg, cuando se registra la densidad, entonces se muestra 150,00 kg/m³.
- **CA-408.** Dada una bolsa digitada por A, cuando A abre la bandeja de verificación, entonces esa bolsa aparece sin botones de verificar ni corregir; cuando la abre B, sí los tiene.
- **CA-409.** Dado que B corrige el peso bruto de una bolsa sin escribir motivo, cuando intenta guardar, entonces no se permite.
- **CA-410.** Dado un estudio en depuración con 3 bolsas pendientes y 1 cuarteo no conforme, cuando el coordinador abre el resumen, entonces ve esos 4 problemas y el mensaje "El estudio aún no puede cerrarse"; al resolverlos, ve "Listo para cerrar".
- **CA-411.** Dada una alerta pendiente, cuando un docente la marca como atendida sin observación, entonces no se permite; con observación, queda atendida con su nombre y la fecha.
- **CA-412.** Dada cualquier corrección, cuando se abre el historial del registro, entonces aparece con valor anterior, valor nuevo, motivo, usuario y fecha.

## 7. Fuera de alcance

- Cálculo de indicadores de la línea base y reportes: spec 006.
- Escaneo automático (OCR) de las planillas en papel.
- Conexión directa con balanzas digitales.

## 8. Preguntas abiertas

| # | Pregunta | Propuesta |
| --- | --- | --- |
| P1 | ¿La densidad se mide una vez por turno o por cada fuente? | Una vez por turno, sobre los residuos del turno (RN-057). La densidad por bolsa (RN-036) queda como dato complementario. |
| P2 | ¿Qué recipientes de medición usa el equipo de Ingeniería Ambiental y cuál es su volumen? | Pedir la lista para cargarla en R-411. |
| P3 | ¿El cuarteo de la mañana y el de la tarde se hacen por separado, o uno solo al final del día? | Por separado, uno por turno, como indica el póster. Confirmar con Ingeniería Ambiental. |
| P4 | ¿Se diseña la planilla en papel oficial para que coincida campo por campo con las pantallas? | Sí: se genera desde el sistema como PDF imprimible (puede entrar en la spec 006). |
