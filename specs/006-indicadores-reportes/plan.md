# Plan — Spec 006: Indicadores de línea base y reportes

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (12 nov 2026) |
| Spec | [spec.md](spec.md) (aprobada) |
| Redacta | Merlín Yelenis Landero Cuello (`mlanderoc`) |
| Revisa | Claude, Estefanía Márquez, Carlos Gómez |
| Rama | `feature/006-indicadores-reportes` (incluye la 005: los indicadores usan cuarteo, densidad y verificación) |
| Fecha | 2026-11-12 |

---

## 1. Resumen Ejecutivo

La **Spec 006** representa la culminación analítica y el producto final del sistema **EcoPoint Edu**: la transformación de todos los registros de pesaje de bolsas, censos de población, cuarteos y mediciones volumétricas recolectados durante la campaña de 8 días en **indicadores de línea base estadísticamente rigurosos**, presentados a través de un **tablero interactivo** y exportables en **reportes de calidad profesional (PDF y Excel)**.

El objetivo central es entregar a las directivas de la institución educativa (I.E. Prudencia Daza en la fase piloto) y al Comité Ambiental Escolar (PRAE) la evidencia técnica necesaria para diseñar programas institucionales de separación en la fuente, proyectar rutas de aprovechamiento y justificar inversiones en infraestructura ambiental.

Siguiendo el principio de **consistencia cruzada absoluta (R-533)**, el sistema garantiza que los valores matemáticos desplegados en las tarjetas y gráficos del frontend web, los datos entregados por la API REST en formato JSON, las tablas de cálculo del libro Excel y las secciones del informe técnico en PDF provengan de un único motor de cálculo inmutable y coincidan con exactitud hasta el último decimal.

El plan contempla una implementación estructurada que prioriza la entrega de valor para la demostración académica (motor matemático, API REST, tablero web interactivo y exportación en Excel) complementada con la generación del informe formal de línea base en PDF con marcas de agua dinámicas según el estado de maduración de los datos (*Preliminar* vs *Oficial*).

---

## 2. Decisiones Arquitectónicas y Tecnológicas

### 2.1 Resumen de Decisiones Técnicas (T1 a T10)

| Código | Tema | Decisión adoptada | Justificación |
| --- | --- | --- | --- |
| **T1** | **Motor de cálculo de indicadores** | Servicio dedicado sin estado `IndicatorService` en `com.ecopoint.service`. | Separa la lógica matemática de agregación de los controladores y repositorios. Opera sobre consultas de lectura optimizadas y no produce efectos secundarios ni mutaciones de datos. |
| **T2** | **Precisión aritmética y redondeo** | Uso estricto de `BigDecimal` con `MathContext.DECIMAL128`. Desviación estándar muestral ($n-1 = 6$) calculada con `BigDecimal.sqrt(mc)` (Java 9+). | Regla general de `AGENTS.md`. Prohibido el uso de `double` o `float` para magnitudes físicas. Los cálculos intermedios no redondean; el redondeo a 2 decimales (pesos, porcentajes, densidad) y 3 decimales (PPC) con `RoundingMode.HALF_UP` se aplica exclusivamente al serializar en DTOs o escribir en reportes. |
| **T3** | **Biblioteca de gráficos (Frontend)** | **Recharts v2.15.1** (`recharts`). | Responde a la pregunta abierta **P4**. Componentes declarativos para React basados en SVG nativo, soporte fluido para gráficos responsivos mediante `<ResponsiveContainer>`, animación fluida, integración limpia con Tailwind CSS v4 y excelente legibilidad en dispositivos móviles táctiles desde 360 px de ancho. |
| **T4** | **Generación de hojas de cálculo (Excel)** | **Apache POI v5.4.0** (`org.apache.poi:poi-ooxml`). | Estándar de la industria en Java para generación de libros Office Open XML (`.xlsx`). Permite estructurar múltiples pestañas temáticas, formatear celdas con la paleta de EcoPoint Edu, aplicar bordes, encabezados congelados y autofiltros sin dependencias nativas del sistema operativo. |
| **T5** | **Generación de informes formales (PDF)** | **OpenPDF v2.0.3** (`com.github.librepdf:openpdf`). | Fork de código abierto mantenido activamente bajo licencias libres LGPL y MPL (evitando los problemas y costos de licenciamiento AGPL de iText 7/8). Soporta construcción fluida de tablas (`PdfPTable`), inserción de logos en encabezados y eventos de página (`PdfPageEventHelper`) para foliado y marcas de agua. |
| **T6** | **Marca de agua diagonal condicional** | Evento `PdfPageEventHelper` con `PdfContentByte` y opacidad transparente. | Cumple R-532 y CA-507. Si el estudio no está en estado `CERRADO`, estampa en 45° el texto *"PRELIMINAR – datos sin depurar"* en gris claro semitransparente sobre todas las páginas sin bloquear la legibilidad del texto subyacente. |
| **T7** | **Seguridad y permisos de consulta** | Permiso `VIEW_INDICATORS` asignado a los roles `DOCENTE` y `ESTUDIANTE_UNIVERSITARIO`. | Cumple RN-081 y decisión D9. Tanto docentes como estudiantes universitarios pueden consultar el tablero y descargar los archivos, facilitando el análisis participativo en el aula. |
| **T8** | **Métrica de calidad y completitud** | Cálculo dinámico de completitud de cuarteos ($X$ de 14 turnos) y porcentaje de registros verificados. | Cumple R-502, R-512, R-526, CA-504 y CA-505. Explicita en pantalla y en reportes la madurez de los datos para evitar la toma de decisiones basada en información incompleta. |
| **T9** | **Gestión de memoria en exportaciones** | Generación en memoria sobre `ByteArrayOutputStream` devuelto como stream HTTP. | Un estudio de 8 días en la I.E. Prudencia Daza genera alrededor de 350 bolsas y 14 cuarteos. La carga en heap es inferior a 10 MB y el tiempo de respuesta es inferior a 150 ms, haciendo innecesario el almacenamiento en disco o colas asíncronas pesadas. |
| **T10** | **Estrategia de entrega (Demo vs Post-Demo)** | Fase 1-3 (Backend, Tablero React y Excel) prioritarias; Fase 4 (PDF formal con logos) como cierre institucional. | Maximiza el valor entregado en las fechas límite de entrega académica y asegura que el núcleo de cálculo esté 100 % probado antes de maquetar el informe estético. |

---

## 3. Arquitectura del Sistema y Flujo de Datos

### 3.1 Diagrama de Dependencias y Componentes

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Frontend Web (React)                          │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │               IndicatorsDashboard (/estudios/:id)              │   │
│   │                                                                │   │
│   │  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │   │
│   │  │   KpiCards   │  │ DailyGenChar │  │  CompositionDonut     │ │   │
│   │  │  (6 tarjetas)│  │ (Recharts)   │  │  (4 grupos Res. 2184) │ │   │
│   │  └──────────────┘  └──────────────┘  └───────────────────────┘ │   │
│   │  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │   │
│   │  │ SourceGenBar │  │ ShiftComp    │  │  DataQualityPanel     │ │   │
│   │  │ (horizontal) │  │ (Mañana/Tard)│  │  (% verificación)     │ │   │
│   │  └──────────────┘  └──────────────┘  └───────────────────────┘ │   │
│   │                                                                │   │
│   │     [ Descargar Excel (.xlsx) ]      [ Descargar PDF (.pdf) ]  │   │
│   └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP REST /api/v1/studies/{id}/...
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  Controlador REST (IndicatorController)                │
│                                                                        │
│   • GET /api/v1/studies/{id}/indicators       -> StudyIndicatorsDTO    │
│   • GET /api/v1/studies/{id}/reports/excel    -> Resource (.xlsx)      │
│   • GET /api/v1/studies/{id}/reports/pdf      -> Resource (.pdf)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼─────────────────────────────┐
       ▼                            ▼                             ▼
┌──────────────┐             ┌──────────────┐              ┌──────────────┐
│  Indicator   │             │ ExcelReport  │              │  PdfReport   │
│   Service    │             │   Service    │              │   Service    │
│ (Matemática) │             │ (Apache POI) │              │  (OpenPDF)   │
└──────┬───────┘             └──────┬───────┘              └──────┬───────┘
       │                            │                             │
       │                            └──────────────┬──────────────┘
       ▼                                           ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       Repositorios de Persistencia                     │
│                                                                        │
│   • StudyRepository             • ShiftRepository                      │
│   • WasteBagRepository          • QuarteringRepository                 │
│   • DensityMeasurementRepo      • VerificationRecordRepository         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ JDBC
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL 16+ (ecopoint)                       │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Paquetes y Asignación de Responsabilidades (`TEAM.md`)

Siguiendo estrictamente la convención de `src/TEAM.md`:

1. **`EstefaniaMarquez` (`com.ecopoint.service`, `model`)**:
   - `IndicatorService` y su implementación de agregación matemática.
   - Definición de métodos de cálculo del dominio y validación de reglas R-500 a R-514.
   - Pruebas unitarias de las fórmulas matemáticas (`IndicatorServiceTest`) basadas en los casos de prueba CA-500 a CA-503 y CA-509.
2. **`craulgomez` (`com.ecopoint.controller`, `com.ecopoint.dto`, `frontend`)**:
   - Controlador REST `IndicatorController` y DTOs de salida (`StudyIndicatorsDTO`, `DailyGenerationDTO`, etc.).
   - Módulo frontend React en `frontend/src/features/indicators/`: Tablero interactivo, tarjetas KPI, gráficos con Recharts y tabla de subcategorías.
   - Pruebas de integración del frontend y validación de estados Preliminar/Oficial (CA-504, CA-505).
3. **`mlanderoc` (`com.ecopoint.service.report`, arquitectura, Maven)**:
   - Configuración de dependencias en `pom.xml` (`poi-ooxml`, `openpdf`).
   - Implementación de `ExcelReportService` (generación multi-hoja con Apache POI, CA-508).
   - Implementación de `PdfReportService` (informe formal de línea base con OpenPDF, logos de la I.E. Prudencia Daza, UPC y EcoPoint Edu, y marca de agua de CA-506 y CA-507).
   - Pruebas de integración de exportación y verificación de consistencia cruzada (R-533).

---

## 4. Fórmulas Matemáticas y Reglas de Negocio

### 4.1 Definición Rigurosa de Indicadores

Notación:
- $d \in \{2, 3, 4, 5, 6, 7, 8\}$: días oficiales de muestreo ($n = 7$). El día $d=1$ es el día de descarte y queda **estrictamente excluido** de todos los promedios e indicadores consolidados (R-500, RN-012, CA-500).
- $t \in \{\text{M}, \text{T}\}$: turno de muestreo (Mañana o Tarde).
- $f \in F$: fuente de generación participante en el estudio.

#### 1. Generación de Residuos
- **Generación del turno por fuente:**
  $$G(d, t, f) = \sum_{b \in \text{Bolsas}(d, t, f)} \text{pesoNeto}(b)$$
- **Generación total del turno:**
  $$G(d, t) = \sum_{f \in F} G(d, t, f) = \sum_{b \in \text{Bolsas}(d, t)} \text{pesoNeto}(b)$$
- **Generación diaria total:**
  $$G(d) = G(d, \text{M}) + G(d, \text{T})$$
- **Generación diaria promedio ($\bar{G}$):**
  $$\bar{G} = \frac{1}{7} \sum_{d=2}^{8} G(d)$$
- **Generación diaria promedio por fuente ($\bar{G}_f$):**
  $$\bar{G}_f = \frac{1}{7} \sum_{d=2}^{8} \left[ G(d, \text{M}, f) + G(d, \text{T}, f) \right]$$

#### 2. Población y Generación Per Cápita (PPC)
- **Población del día:**
  $$P(d) = P(d, \text{M}) + P(d, \text{T})$$
  *(Nota técnica P1: Se suman ambas poblaciones dado que en los colegios públicos colombianos las jornadas de mañana y tarde atienden a estudiantes y docentes distintos).*
- **PPC diaria:**
  $$PPC(d) = \frac{G(d)}{P(d)} \quad [\text{kg/persona/día}]$$
- **PPC promedio ($\overline{PPC}$):**
  $$\overline{PPC} = \frac{1}{7} \sum_{d=2}^{8} PPC(d)$$
- **Desviación estándar muestral de la PPC ($s_{\text{PPC}}$):**
  Calculada sobre los 7 días oficiales con $n-1 = 6$ grados de libertad (R-500, P3, CA-501):
  $$s_{\text{PPC}} = \sqrt{\frac{\sum_{d=2}^{8} \left( PPC(d) - \overline{PPC} \right)^2}{6}}$$
- **Coeficiente de variación de la PPC ($CV_{\text{PPC}}$):**
  $$CV_{\text{PPC}} = \left( \frac{s_{\text{PPC}}}{\overline{PPC}} \right) \times 100 \quad [\%]$$

#### 3. Densidad Volumétrica Promedio
- **Densidad por turno:** Medida directamente en el recipiente estandarizado según NMX-AA-019 (RN-057):
  $$\rho(d, t) = \frac{\text{pesoNetoTurno}}{\text{volumenRecipiente}} \quad [\text{kg/m}^3]$$
- **Densidad promedio institucional:**
  Promedio aritmético de las mediciones válidas registradas en los turnos de los días 2 a 8:
  $$\bar{\rho} = \frac{1}{k} \sum_{i=1}^{k} \rho_i \quad (k \le 14)$$

#### 4. Composición Física Ponderada por Masa
El cuarteo (método NMX-AA-015) se realiza por turno homogenizando los residuos de todas las fuentes (R-513, P2). Los cuarteos anulados por no conformidad o error de calidad quedan **estrictamente excluidos** (R-501, CA-503).
- **Peso total acumulado clasificado:**
  $$W_{\text{total}} = \sum_{q \in \text{CuarteosVálidos}} W(q)$$
- **Porcentaje de la subcategoría $s$:**
  Calculado ponderando directamente por masa (suma de pesos de la subcategoría sobre masa total clasificada, R-510, CA-502):
  $$\%_s = \left( \frac{\sum_{q \in \text{CuarteosVálidos}} w(q, s)}{W_{\text{total}}} \right) \times 100$$
- **Porcentaje por grupo de la Resolución 2184 de 2019:**
  Suma de los porcentajes de sus subcategorías constituyentes:
  $$\%_{\text{Grupo}} = \sum_{s \in \text{Grupo}} \%_s$$
  - `BLANCO`: Residuos aprovechables limpios y secos (plásticos, metales, papel, cartón, vidrio).
  - `VERDE`: Residuos orgánicos aprovechables (restos de alimentos, podas).
  - `NEGRO`: Residuos no aprovechables (papel higiénico, servilletas usadas, empaques metalizados).
  - `SIN_COLOR`: Residuos peligrosos o especiales (RAEE, pilas, biosanitarios).
- **Fracción aprovechable total:**
  $$\%_{\text{Aprovechable}} = \%_{\text{BLANCO}} + \%_{\text{VERDE}}$$
- **Generación diaria aprovechable estimada:**
  $$\bar{G}_{\text{Aprovechable}} = \bar{G} \times \frac{\%_{\text{Aprovechable}}}{100} \quad [\text{kg/día}]$$

### 4.2 Casos de Prueba Verificados con la Spec

| Criterio | Datos de Entrada | Resultado Esperado | Verificación Matemática |
| --- | --- | --- | --- |
| **CA-500** | $P(d)=400$; $G(1)=150$ (descarte); $G(2..8)=[60, 62, 58, 64, 56, 60, 60]$ kg | Generación promedio = **60,00 kg/día** | $\frac{60+62+58+64+56+60+60}{7} = \frac{420}{7} = 60,00$. El día 1 (150 kg) no interviene. |
| **CA-501** | Mismos datos de CA-500. $PPC(2..8)=[0.150, 0.155, 0.145, 0.160, 0.140, 0.150, 0.150]$ | $\overline{PPC}$ = **0,150 kg/persona/día**<br>$s_{\text{PPC}}$ = **0,006 kg/persona/día** (0,006455)<br>$CV$ = **4,30 %** | Desviaciones al cuadrado: $[0, 2.5\times 10^{-5}, 2.5\times 10^{-5}, 10^{-4}, 10^{-4}, 0, 0]$. Suma = $0,000250$. Varianza muestral $s^2 = 0,000250 / 6 = 4,16667\times 10^{-5}$. $s = \sqrt{s^2} = 0,006455$. Redondeado a 3 decimales da **0,006**. $CV = (0,006455 / 0,150) \times 100 = 4,3033\% \rightarrow$ **4,30 %**. |
| **CA-502** | Cuarteo 1 (50 kg): Blanco 10, Verde 25, Negro 15.<br>Cuarteo 2 (25 kg): Blanco 15, Verde 5, Negro 5. | Blanco = **33,33 %**<br>Verde = **40,00 %**<br>Negro = **26,67 %**<br>Aprovechable = **73,33 %** | Masa total = $50 + 25 = 75$ kg.<br>Blanco = $(10+15)/75 = 25/75 = 33,333\% \rightarrow$ **33,33 %**.<br>Verde = $(25+5)/75 = 30/75 = 40,000\% \rightarrow$ **40,00 %**.<br>Negro = $(15+5)/75 = 20/75 = 26,667\% \rightarrow$ **26,67 %**.<br>Fracción aprovechable = $33,33 + 40,00 =$ **73,33 %**. |
| **CA-503** | Cuarteo adicional de 40 kg con estado `ANULADO`. | No altera porcentajes de CA-502. | Exclusión estricta de registros no conformes en la consulta SQL / filtro de servicio. |

---

## 5. Diseño del Contrato API REST

### 5.1 Endpoints de la Spec 006

Base URL: `/api/v1`

| Método | Endpoint | Permiso | Descripción |
| --- | --- | --- | --- |
| `GET` | `/studies/{id}/indicators` | `VIEW_INDICATORS` | Obtiene el conjunto completo de indicadores de línea base, estado (Preliminar/Oficial), completitud y métricas de calidad en formato JSON. |
| `GET` | `/studies/{id}/reports/excel` | `VIEW_INDICATORS` | Descarga el libro estructurado `.xlsx` con todas las hojas temáticas del estudio (CA-508). |
| `GET` | `/studies/{id}/reports/pdf` | `VIEW_INDICATORS` | Descarga el informe formal de línea base en `.pdf` con portada institucional, tablas y marca de agua condicional (CA-506, CA-507). |

### 5.2 Estructura del DTO Principal (`StudyIndicatorsDTO`)

```json
{
  "studyId": 1,
  "studyName": "Caracterización I.E. Prudencia Daza 2026",
  "institutionName": "I.E. Prudencia Daza",
  "startDate": "2026-03-02",
  "endDate": "2026-03-11",
  "status": "PRELIMINAR",
  "isClosed": false,
  "verificationPercentage": 85.50,
  "summary": {
    "dailyAverageGenerationKg": 60.00,
    "averagePpcKgPerPersonDay": 0.150,
    "ppcStandardDeviation": 0.006,
    "ppcCoefficientOfVariation": 4.30,
    "averageDensityKgM3": 178.45,
    "recoverableFractionPercentage": 73.33,
    "estimatedRecoverableDailyKg": 44.00,
    "quarteringCompletenessNote": "Composición calculada con 12 de 14 turnos"
  },
  "dailyGeneration": [
    {
      "dayNumber": 1,
      "date": "2026-03-02",
      "isDiscardDay": true,
      "morningGenerationKg": 80.00,
      "afternoonGenerationKg": 70.00,
      "totalGenerationKg": 150.00,
      "morningPopulation": 210,
      "afternoonPopulation": 190,
      "totalPopulation": 400,
      "dailyPpc": 0.375
    },
    {
      "dayNumber": 2,
      "date": "2026-03-03",
      "isDiscardDay": false,
      "morningGenerationKg": 32.00,
      "afternoonGenerationKg": 28.00,
      "totalGenerationKg": 60.00,
      "morningPopulation": 205,
      "afternoonPopulation": 195,
      "totalPopulation": 400,
      "dailyPpc": 0.150
    }
  ],
  "sourcesGeneration": [
    {
      "sourceId": 4,
      "code": "CAF",
      "name": "Cafetería y Restaurante Escolar",
      "type": "ALIMENTACION",
      "dailyAverageKg": 22.40,
      "percentageOfTotal": 37.33,
      "totalBags": 28
    },
    {
      "sourceId": 1,
      "code": "AU",
      "name": "Aulas Bloque Central",
      "type": "AULAS",
      "dailyAverageKg": 14.10,
      "percentageOfTotal": 23.50,
      "totalBags": 32
    }
  ],
  "shiftComparison": {
    "morning": {
      "averageGenerationKg": 33.20,
      "averagePopulation": 215,
      "averagePpc": 0.154,
      "averageDensityKgM3": 182.10
    },
    "afternoon": {
      "averageGenerationKg": 26.80,
      "averagePopulation": 185,
      "averagePpc": 0.145,
      "averageDensityKgM3": 174.80
    }
  },
  "composition": {
    "totalClassifiedWeightKg": 75.00,
    "validQuarteringCount": 2,
    "totalExpectedShifts": 14,
    "completenessNote": "Composición calculada con 12 de 14 turnos",
    "groups": [
      {
        "groupCode": "VERDE",
        "name": "Orgánicos Aprovechables",
        "colorHex": "#27AE60",
        "weightKg": 30.00,
        "percentage": 40.00
      },
      {
        "groupCode": "BLANCO",
        "name": "Aprovechables Limpios",
        "colorHex": "#FFFFFF",
        "weightKg": 25.00,
        "percentage": 33.33
      },
      {
        "groupCode": "NEGRO",
        "name": "No Aprovechables",
        "colorHex": "#1F2937",
        "weightKg": 20.00,
        "percentage": 26.67
      },
      {
        "groupCode": "SIN_COLOR",
        "name": "Peligrosos y Especiales",
        "colorHex": "#E74C3C",
        "weightKg": 0.00,
        "percentage": 0.00
      }
    ],
    "subcategories": [
      {
        "subcategoryId": 1,
        "name": "Restos de comida y frutas",
        "groupCode": "VERDE",
        "weightKg": 30.00,
        "percentage": 40.00
      },
      {
        "subcategoryId": 2,
        "name": "Plásticos (PET y PEAD)",
        "groupCode": "BLANCO",
        "weightKg": 15.00,
        "percentage": 20.00
      },
      {
        "subcategoryId": 3,
        "name": "Papel y cartón limpios",
        "groupCode": "BLANCO",
        "weightKg": 10.00,
        "percentage": 13.33
      },
      {
        "subcategoryId": 4,
        "name": "Servilletas y papel higiénico",
        "groupCode": "NEGRO",
        "weightKg": 20.00,
        "percentage": 26.67
      }
    ]
  },
  "dataQuality": {
    "totalBags": 160,
    "verifiedBags": 145,
    "totalQuarterings": 13,
    "validQuarterings": 12,
    "annulledQuarterings": 1,
    "verificationPercentage": 90.62,
    "correctionRecordsCount": 4
  }
}
```

---

## 6. Arquitectura del Tablero Web (Frontend)

El módulo se ubica en `frontend/src/features/indicators/` y se integra en la ruta `/estudios/:id/indicadores`.

### 6.1 Componentes Principales

```
frontend/src/features/indicators/
├── components/
│   ├── IndicatorsHeader.tsx         # Selector de estudio, Badge Preliminar/Oficial, botones de descarga
│   ├── KpiCardsGrid.tsx             # Grilla responsiva con las 6 tarjetas KPI principales (R-521)
│   ├── KpiCard.tsx                  # Tarjeta individual con valor gigante, unidad, icono y comparativa
│   ├── DailyGenerationChart.tsx     # Gráfico de barras Recharts (días 1 a 8, día 1 atenuado, R-522)
│   ├── CompositionDonutChart.tsx    # Gráfico Donut Recharts con los 4 grupos de la Res. 2184 (R-523)
│   ├── CompositionTable.tsx         # Tabla colapsable con detalle de subcategorías y nota de completitud (CA-505)
│   ├── SourceGenerationChart.tsx    # Gráfico de barras horizontales de generación por fuente (R-524)
│   ├── ShiftComparisonCard.tsx      # Tarjeta comparativa mañana vs tarde (R-525)
│   └── DataQualityPanel.tsx         # Panel con % de verificación, correcciones y cuarteos anulados (R-526)
├── hooks/
│   ├── useIndicators.ts             # Hook de TanStack Query para GET /api/v1/studies/:id/indicators
│   └── useReportDownloads.ts        # Manejo de descargas binarias de Excel y PDF con indicador de progreso
├── services/
│   └── indicatorService.ts          # Métodos apiClient para indicators, exportExcel y exportPdf
└── types/
    └── indicatorTypes.ts            # Interfaces TypeScript tipadas estrictamente según el DTO del backend
```

### 6.2 Ergonomía y Visualización Móvil (R-527)

- **Diseño fluido sin scroll horizontal:** En pantallas móviles (< 768 px y desde 360 px de ancho), los gráficos adaptan su altura automáticamente (`<ResponsiveContainer width="100%" height={260}>`).
- **Paleta accesible y contrastada:** Los colores de los grupos de residuos respetan la Resolución 2184 de 2019 acompañados siempre de texto legible (`BLANCO: Aprovechables`, `VERDE: Orgánicos`, `NEGRO: No aprovechables`, `ROJO/SIN COLOR: Peligrosos`), cumpliendo los estándares de accesibilidad para personas con daltonismo.
- **Diferenciación explícita del día de descarte:** En `DailyGenerationChart`, la barra del Día 1 se renderiza con un color grisáceo y patrón rayado, con la etiqueta destacada *"Día 1: Descarte"*, impidiendo interpretaciones erróneas.

---

## 7. Estructura y Especificación de Reportes

### 7.1 Reporte en Hoja de Cálculo Excel (`.xlsx`) — Apache POI

El archivo generado por `ExcelReportService` incluye **8 hojas temáticas** estructuradas para análisis estadístico por investigadores y docentes (R-531, CA-508):

| # Hoja | Nombre de Hoja | Contenido y Estructura | Verificación CA-508 |
| --- | --- | --- | --- |
| 1 | `Resumen Indicadores` | Ficha técnica del estudio, estado (Preliminar/Oficial), población promedio, $\bar{G}$, $\overline{PPC}$, $s_{\text{PPC}}$, $CV$, densidad promedio y fracción aprovechable. | Resumen consolidado que encabeza el libro. |
| 2 | `Generación Diaria` | Tabla día por día (1 a 8): fecha, turno M, turno T, total diario, población M, población T, población total y PPC diaria. Días 2 a 8 marcados con estilo activo; día 1 marcado como *Descarte*. | Fórmula Excel que suma días 2 a 8 y divide entre 7 para contrastar con la hoja 1. |
| 3 | `Generación por Fuente` | Matriz bidimensional: filas = fuentes de generación; columnas = días 2 a 8; última columna = Promedio kg/día y % de aporte. | Ordenada descendentemente por generación media. |
| 4 | `Población Censada` | Desglose por día y turno discriminando estudiantes, docentes y personal de apoyo. | Totales concordantes con los divisores de la PPC. |
| 5 | `Registro de Bolsas` | Listado plano dato por dato: ID bolsa, código de rotulación (`AU-D3-M`), fecha, turno, fuente, peso bruto, tara, peso neto, volumen, densidad y estado de verificación. | **La suma de los pesos netos de las bolsas de los días 2 a 8 dividida entre 7 es exactamente idéntica a $\bar{G}$**. |
| 6 | `Cuarteos y Composición` | Tabla de submuestras: ID cuarteo, fecha, turno, estado (Válido/Anulado), peso total, desglose por subcategoría y pesos por grupo. | Excluye filas anuladas en el cálculo ponderado. |
| 7 | `Densidades de Turno` | Mediciones volumétricas por turno: tara recipiente, peso con residuos, volumen ($m^3$) y densidad calculada ($kg/m^3$). | Base de la densidad promedio. |
| 8 | `Historial de Correcciones` | Log de auditoría: fecha/hora, usuario, entidad modificada, valor anterior, valor nuevo y justificación. | Trazabilidad del doble registro y depuración. |

### 7.2 Informe Formal de Línea Base en PDF — OpenPDF

El informe generado por `PdfReportService` se estructura en formato ejecutivo listo para radicar ante la Secretaría de Educación y las directivas del plantel (R-530, CA-506, CA-507):

1. **Portada Institucional:**
   - Título formal: *Informe de Caracterización y Línea Base de Residuos Sólidos Escolares*.
   - Subtítulo: *Institución Educativa Prudencia Daza — Valledupar, Cesar*.
   - Logos institucionales: Universidad Popular del Cesar (UPC), I.E. Prudencia Daza y EcoPoint Edu (P5).
   - Metadatos: Período del muestreo, equipo responsable (cuadrilla universitaria y docente asesor) y estado del documento (*Oficial* o *Preliminar*).
2. **Metodología Resumida del Estudio:**
   - Explicación del protocolo de 8 días con descarte del primer día (RN-012).
   - Métodos técnicos aplicados: Cuarteo por turno según NMX-AA-015 y determinación de densidad según NMX-AA-019.
   - Esquema de control de calidad: doble registro independiente y tolerancia del 2 % en cuarteo.
3. **Indicadores de Línea Base Consolidados:**
   - Tabla formal con los 6 indicadores principales: $\bar{G}$ (kg/día), $\overline{PPC}$ (kg/hab/día), $s$ y $CV$ (%), $\bar{\rho}$ ($kg/m^3$) y fracción aprovechable (%).
4. **Composición Física y Potencial de Aprovechamiento:**
   - Tabla y resumen gráfico de los 4 grupos de la Res. 2184/2019.
   - Desglose porcentual y en masa por subcategoría de residuo.
   - Nota explícita de completitud de cuarteos (ej. *"Composición calculada con 12 de 14 turnos"*).
5. **Generación por Fuente:**
   - Tabla de fuentes jerarquizada de mayor a menor generación para focalización de campañas del PRAE.
6. **Comparación Mañana vs Tarde:**
   - Matriz de turnos contrastando generación, asistencia poblacional y densidad de residuos.
7. **Control de Calidad y Depuración del Dato:**
   - Resumen del proceso de verificación: porcentaje de registros verificados, volumen de correcciones y tasa de cuarteos válidos.
8. **Marca de Agua Condicional (R-532, CA-507):**
   - En estudios en estado `EN_DEPURACION` o `EN_MUESTREO`: marca de agua diagonal semi-transparente en todas las páginas con el texto *"PRELIMINAR – datos sin depurar"*.
   - En estudios en estado `CERRADO`: la marca de agua se omite automáticamente.

---

## 8. Estrategia de Implementación por Fases

Para compatibilizar los tiempos de desarrollo y maximizar la efectividad en la sustentación académica, el trabajo se distribuye en fases claramente delimitadas:

```
┌────────────────────────────────────────────────────────────────────────┐
│ FASE 0: Scaffolding de Dependencias y Modelos DTO                      │
│ • Dependencias Maven (POI 5.4.0, OpenPDF 2.0.3) y Recharts en React    │
│ • Contratos de interfaces DTO en backend y frontend                    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ FASE 1: Motor Matemático en Backend (Estefanía Márquez)                │
│ • Implementación de IndicatorService con precisión BigDecimal plena    │
│ • Pruebas unitarias matemáticas con casos CA-500, CA-501, CA-502, CA-503│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ FASE 2: API REST y Controladores (Carlos Gómez)                        │
│ • Endpoint GET /api/v1/studies/{id}/indicators                         │
│ • Validaciones de permisos VIEW_INDICATORS y pruebas de integración    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ FASE 3: Tablero Web Interactivo en Frontend (Carlos Gómez)             │
│ • Pantalla /estudios/:id/indicadores con Recharts y tarjetas KPI       │
│ • Visualización de estado Preliminar vs Oficial y completitud          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ FASE 4: Generación de Reporte Excel Multi-Hoja (Merlín Landero)        │
│ • ExcelReportService con Apache POI (8 hojas temáticas, CA-508)        │
│ • Endpoint GET /api/v1/studies/{id}/reports/excel                      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ FASE 5: Informe Formal PDF con Marca de Agua (Merlín Landero)          │
│ • PdfReportService con OpenPDF, portada y logos (CA-506)               │
│ • Marca de agua diagonal condicional PRELIMINAR (CA-507)               │
│ • Endpoint GET /api/v1/studies/{id}/reports/pdf                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ FASE 6: Verificación Integral, Cierre y Documentación                  │
│ • Pruebas cruzadas de consistencia tablero vs Excel vs PDF (R-533)     │
│ • Verificación de la suite completa con ./mvnw test y npm test         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Mapeo de Criterios de Aceptación (CA-500 a CA-509)

| Criterio | Descripción | Tarea Asociada | Tipo de Prueba |
| --- | --- | --- | --- |
| **CA-500** | Generación diaria promedio = 60,00 kg/día. Día 1 (150 kg) no entra en el cálculo. | T502, T503 | `IndicatorServiceTest.ca500_dailyAverageGenerationExcludesDayOne()` |
| **CA-501** | PPC promedio = 0,150 kg/persona/día, desviación estándar muestral = 0,006 y CV = 4,30 %. | T502, T503 | `IndicatorServiceTest.ca501_ppcAverageStdDevAndCvCalculated()` |
| **CA-502** | Composición total ponderada por masa (Blanco 33,33 %, Verde 40,00 %, Negro 26,67 %, Fracción aprovechable 73,33 %). | T502, T503 | `IndicatorServiceTest.ca502_massWeightedCompositionAndRecoverableFraction()` |
| **CA-503** | Cuarteo anulado no entra en el cálculo de composición. | T502, T503 | `IndicatorServiceTest.ca503_annulledQuarteringExcludedFromComposition()` |
| **CA-504** | Tablero muestra etiqueta "Preliminar" y % verificado si no está CERRADO; "Oficial" al cerrarse. | T505, T507 | `IndicatorsDashboard.test.tsx` (CA-504) |
| **CA-505** | Nota explícita de completitud si faltan turnos ("Composición calculada con 12 de 14 turnos"). | T502, T507 | `CompositionTable.test.tsx` (CA-505) |
| **CA-506** | PDF de estudio cerrado incluye todas las secciones de R-530 y datos idénticos al tablero. | T511, T512 | `PdfReportServiceIT.ca506_closedStudyPdfContainsAllSectionsAndMatchingData()` |
| **CA-507** | PDF de estudio no cerrado lleva marca de agua diagonal "PRELIMINAR – datos sin depurar" en todas las páginas. | T511, T512 | `PdfReportServiceIT.ca507_preliminaryPdfIncludesWatermarkOnAllPages()` |
| **CA-508** | Archivo Excel tiene las 8 hojas de R-531; suma de pesos netos de bolsas dividida entre 7 coincide exactamente con el resumen. | T509, T510 | `ExcelReportServiceIT.ca508_excelSheetsAndBagsSumMatchesAverage()` |
| **CA-509** | Pruebas unitarias de las fórmulas de la sección 5.2 con los datos exactos de CA-500 a CA-503. | T503 | `IndicatorServiceTest` suite completa |

## Ajustes de la aprobación (12 nov 2026)

1. La rama incorpora la spec 005, porque los indicadores dependen del cuarteo, la densidad y la verificación.
2. Logos: el de EcoPoint Edu ya está en `src/main/resources/logo_ecopoint.jpeg`. Los de la UPC y la I.E. Prudencia Daza los entrega el equipo; si no llegan a tiempo, el PDF se genera sin ellos y se agregan después sin cambiar el código.
3. Recharts: se instala una versión exacta compatible con React 19, comprobada con el build y las pruebas.
