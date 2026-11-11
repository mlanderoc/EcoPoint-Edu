# Spec 006 — Indicadores de línea base y reportes

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Antigravity / Claude |
| Requerimientos | RF-12, RF-13 |
| Depende de | Spec 005 aprobada; decisión D7 |
| Fecha | 2026-11-11 |

## 1. Objetivo

Calcular los **indicadores de la línea base** del póster, mostrarlos en un **tablero** y exportar el **informe de línea base** en PDF y Excel. Es el producto final del estudio: lo que la institución recibe para orientar la separación en la fuente, el aprovechamiento y el PRAE.

## 2. Requerimientos que cubre

| RF | Cómo se cubre |
| --- | --- |
| RF-12 | Cálculo de indicadores, por fuente, por turno y total |
| RF-13 | Tablero, informe PDF y archivo Excel |

## 3. Actores

- **Docentes y estudiantes universitarios**: consultan el tablero y descargan los reportes.
- **Institución educativa** (directivos, comité PRAE): recibe el informe.

## 4. Historias de usuario

- **HU-050.** Como docente, quiero ver la generación diaria, la PPC, la densidad y la composición calculadas automáticamente, para no hacer cálculos a mano.
- **HU-051.** Como docente, quiero saber si los indicadores son preliminares u oficiales, para no presentar datos sin depurar.
- **HU-052.** Como docente, quiero descargar el informe de línea base en PDF, para entregarlo a la institución.
- **HU-053.** Como investigador, quiero descargar todos los datos en Excel, para hacer análisis adicionales.
- **HU-054.** Como docente del colegio, quiero ver qué fuentes generan más residuos, para enfocar las campañas del PRAE.

## 5. Cálculo de los indicadores

### 5.1 Datos que entran en el cálculo

- **R-500** (Debe). Solo entran los días **2 a 8** (RN-012). El día 1 se muestra en las gráficas de forma diferenciada, pero no entra en ningún cálculo.
- **R-501** (Debe). Se usan los valores vigentes de cada registro (después de correcciones). Los cuarteos **anulados** no entran.
- **R-502** (Debe). Mientras el estudio no esté CERRADO, todos los indicadores se marcan como **"Preliminar"**, con el porcentaje de registros ya verificados. Al cerrarse, pasan a **"Oficial"**.

### 5.2 Fórmulas

Notación: *d* = día (2 a 8, n = 7); *t* = turno (M o T); *f* = fuente.

| Indicador | Unidad | Fórmula | Nivel de detalle |
| --- | --- | --- | --- |
| Generación del turno | kg | G(d,t) = Σ pesos netos de las bolsas del turno | Por turno; por fuente y turno |
| Generación diaria | kg/día | G(d) = G(d,M) + G(d,T) | Por día; por fuente y día |
| Generación diaria promedio | kg/día | Ḡ = Σ G(d) / 7 | Total y por fuente |
| Población del día | personas | P(d) = P(d,M) + P(d,T) | Por día |
| PPC del turno | kg/persona/turno | PPC(d,t) = G(d,t) / P(d,t) | Por turno |
| PPC diaria | kg/persona/día | PPC(d) = G(d) / P(d) | Por día |
| PPC promedio | kg/persona/día | promedio de PPC(d), d = 2..8 | Total |
| Desviación estándar de la PPC | kg/persona/día | Desviación estándar **muestral** (n − 1) de PPC(d) | Total |
| CV de la PPC | % | desviación estándar / PPC promedio × 100 | Total |
| Densidad promedio | kg/m³ | promedio de las densidades de turno (RN-057) | Total y por turno |
| Composición por subcategoría | % | Σ pesos de la subcategoría en todos los cuarteos válidos / Σ pesos clasificados en esos cuarteos × 100 | Total y por turno |
| Composición por grupo | % | Suma de la composición de sus subcategorías | Total y por turno |
| Fracción aprovechable | % | Composición BLANCO + composición VERDE | Total |
| Generación aprovechable estimada | kg/día | Ḡ × fracción aprovechable / 100 | Total |

- **R-510** (Debe). Las fórmulas se aplican exactamente como en la tabla. La composición total se **pondera por masa** (suma de pesos), no es un promedio de porcentajes.
- **R-511** (Debe). Redondeo solo al mostrar: pesos con 2 decimales, PPC con 3, porcentajes con 2, densidad con 2. Los cálculos internos no redondean.
- **R-512** (Debe). Si falta un dato para un indicador (ej. un turno sin cuarteo), el indicador se calcula con los datos disponibles y se indica cuántos de los 14 turnos entraron (ej. "Composición calculada con 12 de 14 turnos").
- **R-513** (Debe). La composición **no** se da por fuente, porque el cuarteo es por turno y mezcla todas las fuentes (ver pregunta P2). Por fuente se da la generación.
- **R-514** (Podría). Proyección mensual y anual: Ḡ × días de clase del periodo (dato configurable).

## 6. Tablero de línea base

- **R-520** (Debe). Selector de estudio. Etiqueta visible "Preliminar" u "Oficial" (R-502).
- **R-521** (Debe). Tarjetas con los 6 indicadores principales del póster: generación diaria promedio (kg/día), PPC promedio (kg/persona/día), densidad promedio (kg/m³), fracción aprovechable (%), composición física (enlace al detalle) y CV de la PPC (%).
- **R-522** (Debe). Gráfico de generación por día (días 1 a 8), con el día 1 atenuado y la leyenda "Día de descarte".
- **R-523** (Debe). Gráfico de composición por los 4 grupos, con sus colores y nombres, y tabla de detalle por subcategoría.
- **R-524** (Debe). Gráfico de generación por fuente (kg/día promedio), de mayor a menor.
- **R-525** (Debe). Comparación mañana frente a tarde: generación, población y PPC por turno.
- **R-526** (Debería). Indicador de calidad del dato: % verificado, número de correcciones y cuarteos no conformes.
- **R-527** (Debería). Las gráficas se leen bien en el celular.

## 7. Reportes

- **R-530** (Debe). **Informe de línea base en PDF**, con: portada (institución, fechas del estudio, estado Preliminar u Oficial, equipo responsable), metodología resumida (8 días, día 1 descartado, cuarteo NMX-AA-015, densidad NMX-AA-019), tabla de indicadores, composición por grupo y subcategoría, generación por fuente, comparación por turno, calidad del dato y fecha de generación del informe.
- **R-531** (Debe). **Archivo Excel** con una hoja por tema: resumen de indicadores, generación por día y turno, generación por fuente, población, bolsas (dato por dato), cuarteos con resultados, densidades, y historial de correcciones.
- **R-532** (Debe). Un informe Preliminar lleva una marca de agua "PRELIMINAR – datos sin depurar" en todas las páginas.
- **R-533** (Debe). Los números de los reportes son idénticos a los del tablero.
- **R-534** (Podría). Planilla de campo imprimible en PDF, con los mismos campos que las pantallas (relacionado con spec 005, P4).

## 8. Criterios de aceptación

Datos de prueba comunes (estudio cerrado): población diaria P(d) = 400 en los días 2 a 8; generación diaria G(d) de los días 2 a 8 = 60, 62, 58, 64, 56, 60, 60 kg; día 1 = 150 kg.

- **CA-500.** Con esos datos, la generación diaria promedio es **60,00 kg/día**. El día 1 (150 kg) no la afecta.
- **CA-501.** Con esos datos, la PPC promedio es **0,150 kg/persona/día**, la desviación estándar es **0,006 kg/persona/día** (0,006455) y el CV es **4,30 %**.
- **CA-502.** Dados dos cuarteos válidos, uno de 50 kg (Blanco 10, Verde 25, Negro 15) y otro de 25 kg (Blanco 15, Verde 5, Negro 5), cuando se calcula la composición total, entonces Blanco = 33,33 %, Verde = 40,00 %, Negro = 26,67 % (ponderado por masa), y la fracción aprovechable es 73,33 %.
- **CA-503.** Dado un cuarteo anulado, cuando se calculan los indicadores, entonces no entra en la composición.
- **CA-504.** Dado un estudio EN_DEPURACION, cuando se abre el tablero, entonces todos los indicadores muestran "Preliminar" y el porcentaje verificado; cuando se cierra el estudio, muestran "Oficial".
- **CA-505.** Dado un estudio con 12 de 14 turnos con cuarteo, cuando se muestra la composición, entonces aparece la nota "Composición calculada con 12 de 14 turnos".
- **CA-506.** Cuando se descarga el PDF de un estudio cerrado, entonces contiene todas las secciones de R-530 y sus valores coinciden con el tablero.
- **CA-507.** Cuando se descarga el PDF de un estudio no cerrado, entonces todas sus páginas tienen la marca de agua de R-532.
- **CA-508.** Cuando se descarga el Excel, entonces tiene las hojas de R-531, y la suma de los pesos netos de la hoja de bolsas de los días 2 a 8, dividida entre 7, da la generación diaria promedio del resumen.
- **CA-509.** Las fórmulas de la sección 5.2 tienen pruebas unitarias con los datos de CA-500 a CA-503.

## 9. Fuera de alcance

- Análisis normativo y de ODS (lo redacta el equipo de Ingeniería Ambiental en el informe final).
- Comparación entre varios estudios o instituciones (mejora futura).
- Gráficas sobre el plano: spec 007.

## 10. Preguntas abiertas

| # | Pregunta | Propuesta |
| --- | --- | --- |
| P1 | La PPC diaria suma la población de mañana y tarde. Si un docente trabaja en los dos turnos, cuenta doble. ¿Es aceptable? | Sí, porque en el colegio la mañana y la tarde suelen ser poblaciones distintas. Validar con Ingeniería Ambiental. |
| P2 | ¿Se necesita composición por fuente? Exigiría un cuarteo por fuente, que no es lo que describe el póster. | No. Composición por turno y total; generación por fuente. |
| P3 | ¿Desviación estándar muestral (n − 1) o poblacional (n)? | Muestral, porque los 7 días son una muestra del comportamiento de la institución. |
| P4 | ¿Biblioteca de gráficas? | Se decide en `plan.md` (Recharts o Chart.js). |
| P5 | ¿El informe lleva logos de UPC, Prudencia Daza y EcoPoint Edu? | Sí, los tres en la portada. |
