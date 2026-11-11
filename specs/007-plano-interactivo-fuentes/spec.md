# Spec 007 — Plano interactivo de fuentes

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Claude / Antigravity |
| Requerimientos | RF-04 (geometría), RF-14 |
| Depende de | Spec 003 aprobada; decisiones D2, D5 |
| Fecha | 2026-11-11 |

## 1. Objetivo

Mostrar la institución como un **plano web interactivo sobre la imagen satelital**: cada fuente de generación es un polígono que se puede tocar para ver sus datos, y se colorea según los kg que genera. Es la "zonificación y plano de fuentes" que pide el póster, y la idea del profe de modelar la institución en 2D/3D.

Usa coordenadas reales de latitud y longitud (WGS84) y MapLibre GL JS.

## 2. Requerimientos que cubre

| RF | Cómo se cubre |
| --- | --- |
| RF-04 | Dibujo e importación de los polígonos de las fuentes |
| RF-14 | Plano con fuentes coloreadas por generación, con su detalle al tocarlas |

Reglas aplicadas: RN-001 a RN-005 (spec 001).

## 3. Actores

- **Docente**: dibuja o importa los polígonos de las fuentes.
- **Todos los usuarios**: consultan el plano.
- **Comunidad educativa**: ve el plano en las presentaciones de socialización.

## 4. Historias de usuario

- **HU-060.** Como docente, quiero dibujar sobre la imagen satelital el contorno de cada fuente (bloques de aulas, tienda, PAE, baños, zonas verdes), para tener el plano real de la institución.
- **HU-061.** Como docente, quiero importar un archivo GeoJSON trazado en geojson.io, para no dibujar todo dentro del sistema.
- **HU-062.** Como docente del colegio, quiero ver en el plano qué zonas generan más residuos, para decidir dónde reforzar las canecas y las campañas.
- **HU-063.** Como cuadrilla, quiero tocar una fuente en el plano y ver su código y nombre, para ubicarme en el muestreo.
- **HU-064.** Como docente, quiero mostrar el plano en 3D en la socialización, para que la comunidad lo entienda mejor.

## 5. Requisitos

### 5.1 Visualización

- **R-600** (Debe). El plano abre centrado en la institución, con el centro y el zoom guardados para ella.
- **R-601** (Debe). Fondo con imagen satelital. Botón para cambiar a un mapa de calles.
- **R-602** (Debe). Cada fuente activa con polígono se dibuja con su borde y su código como etiqueta.
- **R-603** (Debe). Se puede acercar, alejar y desplazar con el mouse y con gestos táctiles en el celular.
- **R-604** (Debe). Al pasar el mouse o tocar una fuente, se resalta. Al hacer clic o tocar, se abre un panel con: código, nombre, tipo, y los datos del estudio seleccionado (generación diaria promedio, kg totales, número de bolsas, % del total de la institución).
- **R-605** (Debe). **Modo "generación"**: las fuentes se colorean en una escala de un solo color, de claro (poco) a oscuro (mucho), según su generación diaria promedio en el estudio seleccionado. Tiene leyenda con los rangos en kg/día.
- **R-606** (Debe). Selector de estudio; si el estudio no está cerrado, el plano muestra la etiqueta "Preliminar" (como la spec 006).
- **R-607** (Debe). Las fuentes sin polígono se listan aparte: "Fuentes sin ubicar en el plano: …".
- **R-608** (Debería). **Modo "estado del turno"**: durante el muestreo, cada fuente se colorea según su estado en el turno actual: con bolsas, sin generación o pendiente. Así la cuadrilla ve qué zonas faltan.
- **R-609** (Podría). **Vista 3D** (D5): cada fuente se levanta con su altura (dato opcional; 1 piso ≈ 3,5 m), con botón para inclinar y girar la cámara.

### 5.2 Edición (solo docente)

- **R-620** (Debe). **Importar GeoJSON**: subir un archivo con polígonos (`FeatureCollection`). Cada polígono se asigna a una fuente existente, por su código (propiedad `code` del archivo) o eligiéndola en una lista. Antes de guardar se muestra una vista previa con lo que se va a asignar.
- **R-621** (Debe). Los polígonos se validan con RN-004 antes de guardarse. Los inválidos se reportan uno por uno, sin bloquear los válidos.
- **R-622** (Debería). **Dibujar en el sistema**: elegir una fuente, dibujar su polígono punto por punto sobre la imagen satelital, editar vértices o borrar el polígono.
- **R-623** (Debería). Fijar el centro y el zoom inicial de la institución con la vista actual del plano.
- **R-624** (Debe). Exportar los polígonos de la institución como GeoJSON, para respaldo y para editarlos fuera del sistema.

### 5.3 Datos y servicio

- **R-630** (Debe). El plano se carga con una sola llamada que devuelve un GeoJSON `FeatureCollection`: una `Feature` por fuente, con su polígono y sus propiedades (código, nombre, tipo e indicadores del estudio pedido).
- **R-631** (Debe). La imagen satelital viene de un proveedor de teselas de uso libre o gratuito para uso académico, con su atribución visible en el plano. No se usan servicios que exijan tarjeta de crédito.
- **R-632** (Debe). El plano se ve bien desde 360 px de ancho.

### 5.4 Plano real de la institución piloto

- **R-640** (Debe). El equipo traza en geojson.io los polígonos reales de las fuentes de la I.E. Prudencia Daza sobre la vista satelital, y los incluye en los datos semilla (spec 002, R-041) en lugar de los provisionales.
- **R-641** (Debe). La lista definitiva de fuentes y sus códigos se valida con la institución antes de trazar.

## 6. Criterios de aceptación

- **CA-600.** Dada la institución piloto, cuando se abre el plano, entonces aparece centrado en la I.E. Prudencia Daza sobre la imagen satelital, con todas sus fuentes dibujadas y etiquetadas.
- **CA-601.** Dado el estudio de demostración, cuando se toca la fuente `TI`, entonces el panel muestra su código, nombre, tipo, generación diaria promedio, kg totales, número de bolsas y % del total, y esos números coinciden con la spec 006.
- **CA-602.** En modo generación, la fuente que más genera es la de color más oscuro y la leyenda muestra los rangos en kg/día.
- **CA-603.** Dado un GeoJSON con 6 polígonos, uno de ellos sin cerrar, cuando se importa, entonces se guardan los 5 válidos y se reporta el inválido con el motivo.
- **CA-604.** Dado un polígono cuyo `code` no corresponde a ninguna fuente, cuando se importa, entonces la vista previa pide elegir la fuente o descartarlo.
- **CA-605.** Dado un ESTUDIANTE_UNIVERSITARIO, cuando abre el plano, entonces no ve las opciones de importar, dibujar ni exportar.
- **CA-606.** Dado un celular de 360 px, cuando se abre el plano, entonces se puede acercar con dos dedos, tocar una fuente y leer su panel sin desplazamiento horizontal.
- **CA-607.** Cuando se exporta y se vuelve a importar el GeoJSON de la institución, entonces los polígonos quedan iguales.
- **CA-608.** Dada una fuente sin polígono, cuando se abre el plano, entonces aparece en la lista "Fuentes sin ubicar en el plano".
- **CA-609.** El plano muestra la atribución del proveedor de la imagen satelital.

## 7. Fuera de alcance

- Navegación GPS o seguimiento en tiempo real de las cuadrillas.
- Modelado 3D detallado de los edificios (fachadas, interiores, techos).
- Ubicación de canecas o puntos ecológicos (D1).
- Planos por piso de un mismo edificio.

## 8. Preguntas abiertas

| # | Pregunta | Propuesta |
| --- | --- | --- |
| P1 | ¿Qué proveedor de imagen satelital? | Esri World Imagery para uso académico, con atribución; se confirma su licencia en `plan.md`. Plan B: una imagen propia (captura de Earth) georreferenciada como capa fija. |
| P2 | ¿Cuáles son las fuentes reales de la I.E. Prudencia Daza (bloques, tienda, PAE, baños, zonas verdes, laboratorio)? | Recorrido o lista de la institución antes de trazar (R-641). |
| P3 | ¿La vista 3D entra en esta entrega? | Queda como "Podría" (D5). Se decide según el tiempo disponible al terminar la 006. |
| P4 | ¿Varios bloques de aulas son una sola fuente o una por bloque? | Una por bloque (`AU1`, `AU2`…), para que el plano muestre diferencias entre bloques. Ver spec 001, P2. |
