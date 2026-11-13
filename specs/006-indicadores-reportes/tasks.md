# Tareas — Spec 006: Indicadores de línea base y reportes

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (12 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/006-indicadores-reportes` (desde `develop` o `feature/004-registro-en-campo`) |
| Responsables | Merlín Landero (`mlanderoc`), Estefanía Márquez (`EstefaniaMarquez`), Carlos Gómez (`craulgomez`) |

---

## Reglas de Implementación

1. **Orden estricto:** Las fases se ejecutan de manera progresiva. No se avanza a la siguiente fase sin verificar los criterios de la anterior con pruebas automatizadas en verde.
2. **Commits atómicos y semánticos:** Un commit por tarea con prefijo en español (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
3. **Autoría obligatoria según el paquete (`TEAM.md`):**
   - Paquetes de modelos y servicios de cálculo (`com.ecopoint.service`, `model`): `Estefania Marquez <estefaniamarquez@unicesar.edu.co>`.
   - Controladores, DTOs y Frontend (`com.ecopoint.controller`, `com.ecopoint.dto`, `frontend/`): `Carlos Gomez <craulgomez@unicesar.edu.co>`.
   - Configuración Maven, arquitectura, reportes y documentación (`com.ecopoint.service.report`, `pom.xml`, `specs/`): `Merlín Yelenis Landero Cuello <mlandero@unicesar.edu.co>`.
   - **Prohibido** incluir firmas o coautorías de agentes de IA en commits o PRs.
4. **Cero regresiones:** Antes de marcar cualquier tarea como completada (`- [x]`), debe ejecutarse `./mvnw test` o `npm test` garantizando que no se introducen roturas en specs anteriores.

---

## Fase 0 — Scaffolding, Dependencias y Modelos DTO

- [x] **T500. Configuración de dependencias en backend y frontend.** *(Merlín)*
  En el backend (`pom.xml`), agregar las dependencias de Apache POI para exportación Excel (`org.apache.poi:poi-ooxml:5.4.0`) y OpenPDF para generación de reportes PDF libres de licencias restrictivas (`com.github.librepdf:openpdf:2.0.3`). En el frontend (`frontend/package.json`), instalar la biblioteca de visualización de datos declarativa `recharts` (`2.15.1` exacta, compatible con React 19).
  *Verificación:* `./mvnw compile` compila exitosamente las nuevas dependencias y `cd frontend && npm install && npm run build` pasa sin advertencias ni conflictos de paquetes.

- [x] **T501. Definición de contratos DTO de indicadores en backend y frontend.** *(Carlos)*
  En el backend (`com.ecopoint.dto`), crear las clases de transferencia inmutables: `StudyIndicatorsDTO`, `SummaryKPIsDTO`, `DailyGenerationDTO`, `SourceGenerationDTO`, `ShiftComparisonDTO`, `CompositionDTO`, `WasteGroupCompositionDTO`, `WasteSubcategoryCompositionDTO` y `DataQualityDTO`. En el frontend (`frontend/src/features/indicators/types/indicatorTypes.ts`), crear las interfaces TypeScript espejo con tipado estricto.
  *Verificación:* `cd frontend && npm run typecheck` valida los tipos sin errores y las clases de DTO compilan en el backend.

---

## Fase 1 — Motor Matemático en Backend (Estefanía)

- [x] **T502. Implementación de `IndicatorService` con precisión `BigDecimal`.** *(Estefanía)*
  En `com.ecopoint.service`, crear el servicio `IndicatorService` (clase concreta, como el resto de servicios del proyecto) y la clase sin estado `IndicatorCalculator` con las fórmulas. Implementar las fórmulas rigurosas de la sección 5.2 de la spec:
  - Exclusión estricta del Día 1 de todos los promedios e indicadores consolidados ($n=7$, días 2 a 8, R-500).
  - Exclusión de cuarteos en estado `ANULADO` (R-501).
  - Cálculo de generación del turno $G(d,t)$, diaria $G(d)$ y promedio $\bar{G}$.
  - Población diaria $P(d)$ y cálculo de $PPC(d)$.
  - Cálculo de la $PPC$ promedio ($\overline{PPC}$).
  - Cálculo de la desviación estándar muestral de la PPC con $n-1 = 6$ grados de libertad mediante `BigDecimal.sqrt(MathContext.DECIMAL128)` (R-500, P3).
  - Coeficiente de variación ($CV$) en porcentaje.
  - Densidad promedio general y por turno.
  - Composición física ponderada directamente por masa clasificada (R-510) agrupada por las subcategorías y los 4 grupos de la Resolución 2184 de 2019.
  - Cálculo de la fracción aprovechable ($\%_{\text{BLANCO}} + \%_{\text{VERDE}}$) y generación diaria aprovechable estimada.
  - Conteo de completitud de cuarteos y generación del mensaje descriptivo (ej. *"Composición calculada con 12 de 14 turnos"*, R-512, CA-505).
  - Redondeo `RoundingMode.HALF_UP` aplicado exclusivamente al ensamblar los valores del DTO final (pesos a 2 decimales, PPC a 3, porcentajes a 2, densidad a 2, R-511).
  *Verificación:* Compilación limpia de `IndicatorServiceImpl` respetando la regla de arquitectura que prohíbe importar controladores en el servicio.

- [x] **T503. Pruebas unitarias de las fórmulas matemáticas (CA-500 a CA-503, CA-509).** *(Estefanía)*
  Crear la suite `src/test/java/com/ecopoint/service/IndicatorServiceTest.java` verificando con exactitud los casos de prueba de la spec:
  - `ca500_dailyAverageGenerationExcludesDayOne()`: verifica que con $G(1)=150$ kg y $G(2..8)=[60, 62, 58, 64, 56, 60, 60]$ kg, la generación diaria promedio sea exactamente **60,00 kg/día** (CA-500).
  - `ca501_ppcAverageStdDevAndCvCalculated()`: con población de 400 personas y los datos de CA-500, comprueba que la $\overline{PPC}$ sea **0,150 kg/persona/día**, la desviación estándar muestral sea **0,006 kg/persona/día** (0,006455) y el $CV$ sea **4,30 %** (CA-501).
  - `ca502_massWeightedCompositionAndRecoverableFraction()`: dados dos cuarteos de 50 kg y 25 kg con las distribuciones de la spec, comprueba que Blanco = **33,33 %**, Verde = **40,00 %**, Negro = **26,67 %** y Fracción aprovechable = **73,33 %** (CA-502).
  - `ca503_annulledQuarteringExcludedFromComposition()`: comprueba que un cuarteo con estado `ANULADO` no afecta el cálculo de la composición (CA-503).
  *Verificación:* `./mvnw test -Dtest=IndicatorServiceTest` ejecuta y pasa al 100 % en verde.

---

## Fase 2 — API REST y Seguridad (Carlos)

- [x] **T504. Endpoint `GET /api/v1/studies/{id}/indicators` y seguridad por roles.** *(Carlos)*
  En `com.ecopoint.controller.IndicatorController`, implementar el método `getStudyIndicators(@PathVariable Long id)`. Restringir el acceso con `AccessPolicy.require(actor, VIEW_INDICATORS)` en el servicio, como el resto de la API, permitiendo la consulta a los roles `DOCENTE` y `ESTUDIANTE_UNIVERSITARIO`. Incorporar en el DTO la etiqueta *"Preliminar"* (si el estudio no está en estado `CERRADO`) o *"Oficial"* (si está `CERRADO`), junto con el porcentaje de registros ya verificados (R-502, CA-504).
  *Verificación:* Pruebas unitarias de controlador en Spring MockMvc validando serialización del JSON y respuesta HTTP 200.

- [x] **T505. Pruebas de integración del endpoint de indicadores (CA-504, CA-505).** *(Carlos)*
  En `src/test/java/com/ecopoint/api/IndicatorControllerIT.java`, crear las pruebas de integración contra la base de datos de pruebas:
  - `ca504_preliminaryStatusWithVerificationPercentageWhenStudyNotClosed()`: comprueba que un estudio en depuración devuelva el estado "Preliminar" y el porcentaje de verificación.
  - `ca504_officialStatusWhenStudyClosed()`: comprueba que un estudio cerrado devuelva el estado "Oficial".
  - `ca505_quarteringCompletenessNoteReported()`: comprueba que cuando faltan turnos con cuarteo, la respuesta incluya la nota de completitud correspondiente.
  *Verificación:* `./mvnw test -Dtest=IndicatorControllerIT` pasa en verde.

---

## Fase 3 — Tablero Web de Línea Base en Frontend (Carlos)

- [ ] **T506. Estructura base del tablero, cliente HTTP y tarjetas KPI (R-520, R-521, CA-504).** *(Carlos)*
  En `frontend/src/features/indicators/`:
  - Crear `services/indicatorService.ts` consumiendo `GET /api/v1/studies/:id/indicators` con `apiClient`.
  - Crear hook `useIndicators(studyId)` mediante TanStack Query con revalidación y estado de carga.
  - Implementar `IndicatorsHeader.tsx` con selector de estudio y badge visual destacado *"Preliminar"* (con tooltip del porcentaje verificado) u *"Oficial"* (R-520, CA-504).
  - Implementar `KpiCardsGrid.tsx` y `KpiCard.tsx` con los 6 indicadores principales del póster: Generación diaria promedio (kg/día), PPC promedio (kg/persona/día), Densidad promedio (kg/m³), Fracción aprovechable (%), Composición física y CV de la PPC (%) (R-521).
  *Verificación:* `test('kpiCardsRenderAccurateValues')` en Vitest valida el renderizado de las tarjetas con los datos esperados.

- [ ] **T507. Gráficos interactivos Recharts y tabla de subcategorías (R-522, R-523, R-524, R-525, R-526, R-527, CA-505).** *(Carlos)*
  Desarrollar los componentes gráficos basados en Recharts:
  - `DailyGenerationChart.tsx`: Gráfico de barras de los días 1 a 8, con la barra del Día 1 diferenciada visualmente en gris y la leyenda destacada *"Día de descarte"* (R-522).
  - `CompositionDonutChart.tsx`: Gráfico Donut de los 4 grupos de la Res. 2184/2019 con sus colores oficiales y etiquetas legibles (R-523).
  - `CompositionTable.tsx`: Tabla colapsable con el detalle de subcategorías y la nota de completitud *"Composición calculada con X de 14 turnos"* (R-523, CA-505).
  - `SourceGenerationChart.tsx`: Gráfico de barras horizontales ordenado de mayor a menor generación promedio por fuente (R-524).
  - `ShiftComparisonCard.tsx`: Comparativa de generación, población y PPC entre los turnos mañana y tarde (R-525).
  - `DataQualityPanel.tsx`: Resumen de calidad del dato con % verificado, total de bolsas y cuarteos conformes (R-526).
  - Ajuste de responsividad para visualización limpia en viewports de 360 px sin scroll horizontal (R-527).
  *Verificación:* Suite de pruebas en `frontend/src/features/indicators/__tests__/IndicatorsDashboard.test.tsx` confirmando renderizado y ausencia de desbordamiento horizontal en 360 px.

---

## Fase 4 — Reporte en Hoja de Cálculo Excel (Merlín)

- [ ] **T508. Servicio de exportación Excel con Apache POI (R-531, CA-508).** *(Merlín)*
  En `com.ecopoint.service.report`, crear `ExcelReportService` y su implementación `ExcelReportServiceImpl`. Diseñar el libro `.xlsx` con 8 hojas temáticas estructuradas:
  1. `Resumen Indicadores`: Metadatos institucionales y los 6 KPIs principales.
  2. `Generación Diaria`: Tabla día por día con días 2 a 8 activos y día 1 marcado como descarte.
  3. `Generación por Fuente`: Matriz de fuentes x días con promedios y porcentajes.
  4. `Población Censada`: Conteo por turno y categoría de personas.
  5. `Registro de Bolsas`: Listado dato por dato con pesos netos, tara, volumen y verificación.
  6. `Cuarteos y Composición`: Pesos clasificados por subcategoría de cuarteos válidos.
  7. `Densidades de Turno`: Mediciones de densidad volumétrica por turno.
  8. `Historial de Correcciones`: Registro de auditoría y depuración.
  Garantizar el formato visual con estilos de encabezados en verde institucional, fuentes negritas, bordes delgados y formatos numéricos consistentes (`0.00` y `0.000`).
  *Verificación:* Prueba unitaria que genera el archivo en un `ByteArrayOutputStream` y valida la presencia de las 8 hojas y el contenido de celdas clave.

- [ ] **T509. Endpoint `GET /api/v1/studies/{id}/reports/excel` y prueba de integración (CA-508).** *(Merlín)*
  En `IndicatorController`, exponer la ruta `/studies/{id}/reports/excel` configurando las cabeceras `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` y `Content-Disposition: attachment; filename="ecopoint-reporte-estudio-{id}.xlsx"`.
  En `src/test/java/com/ecopoint/service/report/ExcelReportServiceIT.java`, implementar la prueba de integración `ca508_excelSheetsAndBagsSumMatchesAverage()`: descargar el archivo generado de la base de prueba, inspeccionar con Apache POI la hoja `Registro de Bolsas`, sumar los pesos netos de los días 2 a 8, dividir entre 7 y verificar que coincida con exactitud matemática con la celda de generación diaria promedio de la hoja `Resumen Indicadores`.
  *Verificación:* `./mvnw test -Dtest=ExcelReportServiceIT` pasa en verde.

---

## Fase 5 — Informe Formal PDF con Marca de Agua (Merlín)

- [ ] **T510. Maquetación del informe formal en PDF con OpenPDF (R-530, R-532, P5, CA-506, CA-507).** *(Merlín)*
  En `com.ecopoint.service.report`, crear `PdfReportService` y su implementación `PdfReportServiceImpl` utilizando OpenPDF 2.0.3:
  - Diseñar la portada ejecutiva: Logos institucionales (UPC, I.E. Prudencia Daza, EcoPoint Edu), título formal, período del estudio, cuadrilla participante y estado del informe.
  - Redactar y maquetar la sección de metodología resumida (8 días, descarte del día 1, NMX-AA-015, NMX-AA-019, control de calidad del 2 %).
  - Tablas formales de indicadores de línea base, composición física por grupo y subcategoría, generación por fuente y comparación de turnos.
  - Implementar el evento `WatermarkPageEvent` (`PdfPageEventHelper`): si el estudio no está en estado `CERRADO`, estampar de forma diagonal a 45° en todas las páginas el texto *"PRELIMINAR – datos sin depurar"* en gris claro con transparencia (R-532, CA-507). Si está `CERRADO`, suprimir la marca de agua.
  - Incorporar numeración de página *"Página X de Y"* y marca temporal de generación en el pie de página.
  *Verificación:* Prueba unitaria que genera el documento PDF en memoria y verifica que el flujo binario contenga las secciones estipuladas.

- [ ] **T511. Endpoint `GET /api/v1/studies/{id}/reports/pdf` y prueba de integración (CA-506, CA-507).** *(Merlín)*
  En `IndicatorController`, exponer la ruta `/studies/{id}/reports/pdf` con cabeceras `Content-Type: application/pdf` y `Content-Disposition: attachment; filename="ecopoint-informe-linea-base-{id}.pdf"`.
  En `src/test/java/com/ecopoint/service/report/PdfReportServiceIT.java`, implementar:
  - `ca506_closedStudyPdfContainsAllSectionsAndMatchingData()`: comprueba que un estudio cerrado genere un PDF con todas las secciones de R-530 y sin marca de agua preliminar.
  - `ca507_preliminaryPdfIncludesWatermarkOnAllPages()`: comprueba que un estudio en estado no cerrado incorpore la marca de agua preliminar en todas sus páginas.
  *Verificación:* `./mvnw test -Dtest=PdfReportServiceIT` pasa en verde.

---

## Fase 6 — Verificación de Usabilidad, Consistencia Cruzada y Cierre

- [ ] **T512. Botones de descarga y experiencia interactiva en Frontend.** *(Carlos)*
  En `IndicatorsHeader.tsx`, agregar los botones "Exportar Excel" y "Descargar PDF" con iconos de Lucide React (`FileSpreadsheet`, `FileText`), gestionando estados de descarga (`isDownloading`), feedback visual al usuario y disparo de la descarga binaria en el navegador mediante `window.URL.createObjectURL`.
  *Verificación:* Pruebas de interacción con `@testing-library/react` verificando la activación de la descarga tras el clic.

- [ ] **T513. Prueba de consistencia cruzada integral (R-533).** *(Merlín)*
  Crear la prueba de integración `src/test/java/com/ecopoint/integration/IndicatorCrossConsistencyIT.java` que consulte el estudio semilla cerrado:
  - Obtener los indicadores desde `IndicatorService`.
  - Parsear los valores de celda del Excel generado por `ExcelReportService`.
  - Extraer los datos textuales del PDF generado por `PdfReportService`.
  - Validar que $\bar{G}$, $\overline{PPC}$, densidad promedio y los porcentajes de composición coincidan de forma idéntica en los tres artefactos hasta el último decimal (R-533).
  *Verificación:* `./mvnw test -Dtest=IndicatorCrossConsistencyIT` ejecuta y pasa al 100 % en verde.

- [ ] **T514. Documentación y revisión final del PR.** *(Merlín)*
  Actualizar `README.md` del proyecto y `specs/006-indicadores-reportes/plan.md` si surgieron ajustes menores durante la revisión. Registrar el informe de trazabilidad en la bitácora de uso de IA (`docs/ai-usage/`). Verificar que la suite completa de pruebas unitarias y de integración de todo el repositorio compile y pase en verde.
  *Verificación:* `./mvnw verify` y `npm test` exitosos en verde; Pull Request listo para revisión.
