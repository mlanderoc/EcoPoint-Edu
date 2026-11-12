# Plan — Spec 007: Plano interactivo de fuentes

| Campo | Valor |
| --- | --- |
| Estado | **BORRADOR** — pendiente de aprobación del equipo |
| Spec | [spec.md](spec.md) (aprobada) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo |
| Rama | `feature/007-plano-interactivo-fuentes` |
| Depende de | Spec 003 (API) fusionada; spec 004 (base del frontend) fusionada; cálculo de generación por fuente de la spec 006 |
| Fecha | 2026-11-12 |

## 1. Resumen

La spec 007 agrega una pantalla **Plano** (`/plano`) al frontend de la spec 004. Muestra la institución sobre la imagen satelital con **MapLibre GL JS**, dibuja cada fuente de generación como un polígono y lo colorea según los kg que genera. Además, permite al docente importar, dibujar y exportar los polígonos.

Tiene tres partes:

1. **Backend** (sobre la API de la spec 003): un endpoint que devuelve el plano completo en una sola llamada (GeoJSON con indicadores) y un endpoint de importación masiva que acepta los polígonos válidos y reporta los inválidos uno por uno.
2. **Frontend**: la pantalla del plano, el panel de detalle, la leyenda, el selector de estudio y las herramientas de edición del docente.
3. **Datos reales**: los polígonos de la I.E. Prudencia Daza, trazados por el equipo en geojson.io, reemplazan a los provisionales de la semilla.

El dominio no cambia: el polígono ya existe (`GeoPolygon`, `GeoPoint`), RN-004 ya se valida en `GenerationSourceValidator` y la base ya guarda la geometría en `jsonb` (spec 002).

## 2. Decisiones técnicas

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | **`maplibre-gl` directo**, envuelto en un componente propio `MapView` y un hook `useMapLibre`. No se usa `react-map-gl` | Una dependencia menos; el plano es una sola pantalla y se controla mejor el ciclo de vida del mapa |
| T2 | **Carga diferida**: la pantalla del plano se importa con `React.lazy`, así MapLibre (~800 KB) solo se descarga al abrir `/plano` | Las pantallas de registro en campo siguen livianas en el celular |
| T3 | **Imagen satelital: Esri World Imagery**, con su atribución visible ("Esri, Maxar, Earthstar Geographics…"). Antes de implementar se confirma que sus términos permiten este uso académico sin tarjeta de crédito (tarea T01). **Plan B** (spec, P1): una captura propia georreferenciada como capa `image` de MapLibre | R-601, R-631, CA-609 |
| T4 | **Mapa de calles: OpenFreeMap** (estilo vectorial gratuito, sin clave ni registro), con atribución de OpenStreetMap | R-601; no exige cuenta ni tarjeta |
| T5 | Las URL de las teselas y sus atribuciones van en **un solo archivo de configuración** (`features/map/config/basemaps.ts`) | Si un proveedor cambia sus condiciones, se cambia en un solo lugar |
| T6 | Coordenadas en **GeoJSON estándar (RFC 7946)**: cada vértice es `[longitud, latitud]`. El backend ya escribe así en `GeoJsonMapper`; el frontend no invierte nada | Evita el error clásico de latitud y longitud cruzadas |
| T7 | Solo se aceptan **`Polygon` simples** (un anillo, sin huecos). `MultiPolygon`, huecos, `Point` o `LineString` se reportan como inválidos con su motivo | Es lo que guarda el modelo (`GeoPolygon`) y la base (`CHECK type = 'Polygon'`) |
| T8 | **RN-004 se valida en los dos lados**: en el frontend, para la vista previa de la importación (función pura `validatePolygon`), y en el backend con `GenerationSourceValidator`, que es la autoridad | La vista previa avisa antes de enviar; el backend nunca confía en el cliente |
| T9 | **Escala de color**: 5 clases de **intervalo igual** desde 0 hasta la generación máxima, redondeadas a valores legibles, en una escala de un solo tono verde derivada de los tokens de la spec 004 (`--color-primary-light` → `--color-brand-dark`). Las fuentes sin datos van en gris con el texto "Sin datos" | R-605, CA-602: la fuente que más genera queda siempre en la clase más oscura. Con solo 8 fuentes, los cuantiles dejarían clases vacías o repetidas |
| T10 | El color de cada polígono lo calcula el **frontend** a partir de la generación; el backend solo envía números | La escala es presentación; el backend no conoce colores |
| T11 | **Dibujo en el sistema con `terra-draw`** y su adaptador para MapLibre (`terra-draw-maplibre-gl-adapter`) | R-622. Está mantenida y soporta MapLibre de forma oficial; `mapbox-gl-draw` está pensada para Mapbox |
| T12 | **Exportar** (R-624) se hace en el frontend a partir del mismo GeoJSON del plano, dejando solo `code`, `name` y `type` en las propiedades | CA-607: lo exportado se puede volver a importar tal cual, porque se asigna por `code` |
| T13 | **Pruebas en dos niveles**: Vitest para toda la lógica pura (validación, asignación por código, escala, leyenda, exportación) y **Playwright** en Chromium con emulación de celular para lo que necesita un mapa real (WebGL): CA-600, CA-605, CA-606 y CA-609. Las teselas se sustituyen por imágenes locales en las pruebas | jsdom no tiene WebGL, así que MapLibre no se puede probar ahí. AGENTS.md pide una prueba automática por CA |
| T14 | La vista 3D (R-609, "Podría") queda en una **fase opcional** que no bloquea el PR. Si se hace, usa `fill-extrusion` de MapLibre y requiere un dato nuevo, el número de pisos de la fuente (ver sección 8, R4) | Decisión D5 y pregunta P3 de la spec |

## 3. Backend (sobre la API de la spec 003)

Los nombres de paquetes, DTO y el formato de error siguen lo que defina el plan de la spec 003. Aquí solo va lo propio del plano.

### 3.1 Endpoints

| Método y ruta | Quién | Qué hace | Requisitos |
| --- | --- | --- | --- |
| `GET /api/v1/institutions/{id}/map?studyId={s}&shiftId={t}` | Sesión iniciada | Devuelve el plano en una sola llamada: `FeatureCollection` con una `Feature` por fuente **activa con polígono**, la lista de fuentes activas **sin polígono**, el centro y el zoom de la institución, y el estado del estudio. `studyId` y `shiftId` son opcionales | R-600, R-604 a R-608, R-630 |
| `PUT /api/v1/institutions/{id}/sources/geometries` | `MANAGE_ADMINISTRATION` | Importación masiva: recibe una lista `[{sourceId, geometry}]` ya asignada en la vista previa. Guarda las válidas y responde con el resultado de cada una (`SAVED` o `INVALID` con su motivo). Todas las válidas se guardan en una transacción | R-620, R-621, CA-603 |
| `PUT /api/v1/institutions/{id}/sources/{sourceId}/geometry` | `MANAGE_ADMINISTRATION` | Guarda o reemplaza el polígono de una fuente (dibujo en el sistema) | R-622 |
| `DELETE /api/v1/institutions/{id}/sources/{sourceId}/geometry` | `MANAGE_ADMINISTRATION` | Borra el polígono de una fuente (la fuente sigue existiendo) | R-622 |
| `PUT /api/v1/institutions/{id}` (ya existe en la 003) | `MANAGE_ADMINISTRATION` | Guarda el centro y el zoom con la vista actual | R-623 |

### 3.2 Respuesta del plano (`MapResponse`)

Ejemplo con números ilustrativos (los reales salen de la semilla):

```json
{
  "institutionId": 1,
  "center": { "latitude": 10.47, "longitude": -73.25 },
  "zoom": 18,
  "study": { "id": 2, "status": "EN_EJECUCION", "preliminary": true, "verifiedPercent": 62.5 },
  "features": {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "id": 4,
        "geometry": { "type": "Polygon", "coordinates": [[[-73.2506, 10.4697], "…"]] },
        "properties": {
          "sourceId": 4, "code": "TI", "name": "Tienda escolar", "type": "TIENDA_ESCOLAR",
          "avgDailyKg": 3.42, "totalKg": 23.94, "bagCount": 14, "percentOfTotal": 9.80,
          "shiftStatus": "WITH_BAGS"
        }
      }
    ]
  },
  "sourcesWithoutGeometry": [ { "sourceId": 9, "code": "CAF", "name": "Cafetería" } ]
}
```

- Sin `studyId`, las propiedades de indicadores llegan en `null` y el plano se ve sin colorear.
- `shiftStatus` solo llega si se pide `shiftId`: `WITH_BAGS`, `NO_GENERATION` o `PENDING` (R-608).
- Los números van redondeados a 2 decimales con `HALF_UP` al armar la respuesta (AGENTS.md); los cálculos internos usan `BigDecimal` sin redondear.

### 3.3 Servicio

`MapService` (en el paquete de servicios de la spec 003):

- Lee la institución, sus fuentes activas y, si se pidió estudio, la **generación por fuente**.
- La generación por fuente **no se calcula aquí**: se pide al servicio de indicadores de la spec 006 (`generationBySource(studyId)`), para que el panel y el tablero den siempre los mismos números (CA-601).
- El estado por fuente en un turno sale de las bolsas y las marcas "sin generación" del turno (`ShiftRepository.findById`, spec 002).
- La importación valida cada polígono con `GenerationSourceValidator` (RN-004) y comprueba que la fuente pertenezca a la institución (RN-001). Si una falla, se agrega su motivo al resultado y se sigue con las demás (R-621).
- Las escrituras usan `GenerationSourceRepository.save` dentro de un `@Transactional`.

## 4. Frontend

### 4.1 Estructura

```
frontend/src/features/map/
├── pages/MapPage.tsx              # /plano (carga diferida)
├── components/
│   ├── MapView.tsx                # Contenedor de MapLibre y capas
│   ├── BasemapToggle.tsx          # Satélite / calles (R-601)
│   ├── SourcePanel.tsx            # Detalle al tocar una fuente (R-604)
│   ├── GenerationLegend.tsx       # Rangos en kg/día (R-605)
│   ├── StudySelector.tsx          # Estudio + etiqueta "Preliminar" (R-606)
│   ├── UnmappedSources.tsx        # "Fuentes sin ubicar en el plano" (R-607)
│   ├── ModeToggle.tsx             # Generación / estado del turno (R-605, R-608)
│   └── editor/                    # Solo con MANAGE_ADMINISTRATION
│       ├── ImportDialog.tsx       # Subir archivo + vista previa (R-620)
│       ├── DrawToolbar.tsx        # Dibujar, editar vértices, borrar (R-622)
│       └── ExportButton.tsx       # Descargar GeoJSON (R-624)
├── config/basemaps.ts             # Proveedores y atribuciones (T5)
├── hooks/useMapLibre.ts, useInstitutionMap.ts
├── services/mapService.ts         # Llamadas a la API
└── utils/                         # Lógica pura, probada con Vitest
    ├── validatePolygon.ts         # RN-004 en el cliente
    ├── matchFeatures.ts           # Asignar polígonos por `code` (R-620, CA-604)
    ├── colorScale.ts              # Clases de intervalo igual (T9)
    └── exportGeoJson.ts           # Exportación (T12)
```

### 4.2 Pantalla

- **Ruta** `/plano`, en el menú lateral y en la barra inferior del celular para los dos roles.
- **Escritorio**: el mapa ocupa la pantalla; a la derecha va el panel de la fuente seleccionada y, abajo a la izquierda, la leyenda.
- **Celular (≥ 360 px)**: el mapa ocupa todo; el panel sube desde abajo como hoja deslizable y la leyenda se pliega en un botón. Sin desplazamiento horizontal (R-632, CA-606).
- **Etiquetas**: el código de cada fuente va centrado en su polígono (`symbol` de MapLibre), con halo para leerse sobre la imagen satelital.
- **Resaltado**: al pasar el mouse o tocar, el borde se engruesa (`feature-state`).
- **Preliminar**: si el estudio no está cerrado, va una etiqueta "Preliminar · 62 % verificado", igual que en la spec 006.
- **Importar** (docente): se sube el archivo, se ve la vista previa en una tabla (fuente asignada, válido o motivo del error) y en el mapa, y se confirma. Los polígonos sin `code` conocido piden elegir la fuente o descartarlo (CA-604).
- **Dibujar** (docente): se elige la fuente en una lista, se marcan los puntos sobre el mapa, se cierra el polígono y se guarda. También se pueden editar vértices o borrar el polígono.

### 4.3 Simulación con MSW

Mientras la spec 003 no esté fusionada, los endpoints del plano se simulan en MSW con los mismos datos de la semilla (spec 002): la institución piloto, sus 8 fuentes con polígonos y los dos estudios. Al conectar la API real se apaga MSW con `VITE_USE_MOCK_API=false`, igual que en la spec 004.

## 5. Datos reales de la institución piloto (R-640, R-641)

1. El equipo confirma con la I.E. Prudencia Daza la lista de fuentes y sus códigos (R-641, spec P2).
2. Se trazan los polígonos en geojson.io sobre la vista satelital, con la propiedad `code` en cada uno.
3. El archivo queda en `sql/datos/prudencia-daza-fuentes.geojson` y sus coordenadas reemplazan los polígonos provisionales de `sql/002_seed.sql`. `SchemaIT` sigue verificando que la semilla cargue igual dos veces (CA-101).
4. La importación del mismo archivo desde la pantalla sirve de prueba manual de CA-607.

Este paso no necesita código y **puede empezar ya**: solo depende de la visita o la lista de la institución.

## 6. Pruebas

| CA | Qué se prueba | Cómo |
| --- | --- | --- |
| CA-600 | Abre centrado en la institución con todas las fuentes dibujadas y etiquetadas | Playwright: centro y zoom del mapa; número de `features` renderizadas |
| CA-601 | Panel de `TI` con código, nombre, tipo e indicadores iguales a la spec 006 | Vitest del `SourcePanel` + prueba del backend que compara `MapService` con el servicio de indicadores sobre la semilla |
| CA-602 | La fuente que más genera queda en la clase más oscura; leyenda en kg/día | Vitest de `colorScale` y `GenerationLegend` |
| CA-603 | 6 polígonos con 1 sin cerrar → se guardan 5 y se reporta 1 | Vitest de `validatePolygon` + prueba de integración del endpoint de importación |
| CA-604 | `code` desconocido → la vista previa pide elegir o descartar | Vitest de `matchFeatures` y del `ImportDialog` |
| CA-605 | El estudiante no ve importar, dibujar ni exportar | Vitest del `MapPage` con cada rol + Playwright + prueba del backend (403 en los `PUT`) |
| CA-606 | En 360 px: zoom con dos dedos, tocar una fuente y leer el panel sin desplazamiento horizontal | Playwright con emulación de celular táctil |
| CA-607 | Exportar e importar deja los polígonos iguales | Vitest: `exportGeoJson` → `matchFeatures` → comparación de coordenadas |
| CA-608 | Fuente sin polígono en "Fuentes sin ubicar en el plano" | Vitest de `UnmappedSources` + prueba del backend de `sourcesWithoutGeometry` |
| CA-609 | Atribución del proveedor visible | Playwright: el control de atribución contiene el texto de `basemaps.ts` |

Además: `npm test`, `npm run build` y `./mvnw verify` en verde antes de cada commit.

## 7. Fases

| Fase | Contenido | Necesita |
| --- | --- | --- |
| 0 | Confirmar los términos de Esri y OpenFreeMap (T3, T4); instalar `maplibre-gl` y Playwright | Spec 004 fusionada |
| 1 | Lógica pura con sus pruebas: `validatePolygon`, `matchFeatures`, `colorScale`, `exportGeoJson` | Fase 0 |
| 2 | Plano de consulta con MSW: mapa, capas, etiquetas, panel, leyenda, selector de estudio, fuentes sin ubicar, modos | Fase 1 |
| 3 | Edición del docente con MSW: importar con vista previa, dibujar, exportar, fijar centro y zoom | Fase 2 |
| 4 | Backend: `MapService`, endpoints e integración con los indicadores de la 006; se apaga MSW para el plano | Spec 003 fusionada y generación por fuente de la 006 |
| 5 | Polígonos reales de la institución en la semilla (sección 5) | Lista de fuentes confirmada (R-641) |
| 6 (opcional) | Vista 3D (R-609) | Decisión del equipo (spec P3) |

## 8. Responsables (autor de los commits, según `src/TEAM.md`)

| Parte | Responsable |
| --- | --- |
| Frontend del plano (`frontend/src/features/map`) | Carlos (`craulgomez`), responsable de la interfaz |
| `MapService`, endpoints y DTO del plano | Quien el plan de la spec 003 asigne a servicios y controladores |
| Cambios en validadores, si hacen falta | Carlos (`craulgomez`) |
| Semilla con los polígonos reales, `docs` y este plan | Merlín (`mlanderoc`) |

`src/TEAM.md` todavía describe la versión anterior (Swing y archivos planos). Se actualiza con el plan de la spec 003, antes de crear los paquetes nuevos (AGENTS.md, sección 5).

## 9. Riesgos y puntos a confirmar

| # | Tema | Propuesta |
| --- | --- | --- |
| R1 | **Licencia de la imagen satelital.** Los términos de Esri pueden pedir una cuenta de ArcGIS para algunos usos | T01 confirma los términos. Si no sirve: plan B de la spec (captura propia georreferenciada como capa fija), que no depende de terceros |
| R2 | **Dependencia del orden.** El plano necesita la base del frontend (004), la API (003) y la generación por fuente (006) | Las fases 1 a 3 avanzan con MSW cuando se fusione la 004; la fase 4 espera a la 003 y a la 006. La fase 5 (trazado real) puede empezar ya |
| R3 | **Qué días cuentan** para "kg totales" y "número de bolsas" del panel | Propuesta: los días 2 a 8, igual que la generación diaria promedio de la spec 006 (el día 1 se descarta). Así el panel y el tablero cuadran (CA-601) |
| R4 | **Altura para la vista 3D.** La fuente no tiene número de pisos | Solo si se aprueba la fase 6: agregar `floors` (opcional, 1 a 10) al modelo, a la tabla `FUENTE_GENERACION` y al formulario de fuentes. Altura = pisos × 3,5 m |
| R5 | **Rendimiento en celulares de gama baja.** MapLibre usa WebGL | Carga diferida (T2), pocas capas y polígonos simples. Si el WebGL no está disponible, se muestra un aviso y la lista de fuentes con sus indicadores |
| R6 | **Playwright es una herramienta nueva** en el proyecto | Solo para 4 CA que necesitan un mapa real. Corre con `npm run test:e2e` y no reemplaza a Vitest |
