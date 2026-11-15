# Specs de EcoPoint Edu (Spec Driven Development)

Esta carpeta contiene las especificaciones del sistema **EcoPoint Edu: caracterización de residuos sólidos en instituciones educativas**. Nacen del documento *EcoPoint Edu: Plan de reestructuración para SDD* (11 nov 2026) y del póster *Caracterización participativa de los residuos sólidos en la I.E. Prudencia Daza*.

> **Estado de las specs: APROBADAS (11 nov 2026).**
> Las especificaciones han sido revisadas y aprobadas por el equipo para ejecución y desarrollo.

## Índice

| Spec | Nombre | Requerimientos | Depende de | Redacta | Implementa | Revisa | Estado |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [001](001-dominio-caracterizacion/spec.md) | Dominio de caracterización | RF-01 a RF-12 (reglas) | — | Claude | Claude | Antigravity | **Aprobada** |
| [002](002-persistencia-postgresql/spec.md) | Persistencia PostgreSQL | Todos (almacenamiento) | 001 | Claude | Claude | Antigravity | **Aprobada** |
| [003](003-api-rest-seguridad/spec.md) | API REST y seguridad | RF-11 (permisos) | 002 | Claude | Claude | Antigravity | **Aprobada** |
| [004](004-registro-en-campo/spec.md) | Registro en campo | RF-01 a RF-05 | 003 | Claude | Claude | Antigravity | **Aprobada** |
| [005](005-cuarteo-control-calidad/spec.md) | Cuarteo y control de calidad | RF-06 a RF-10 | 004 | Claude | Claude | Antigravity | **Aprobada** |
| [006](006-indicadores-reportes/spec.md) | Indicadores y reportes | RF-12, RF-13 | 005 | Claude | Claude | Antigravity | **Aprobada** |
| [007](007-plano-interactivo-fuentes/spec.md) | Plano interactivo de fuentes | RF-14 | 003 | Claude | Claude | Antigravity | **Aprobada** |
| [D13](d13-ajustes-pruebas/plan.md) | Ajustes por las pruebas de uso (plan y tareas; los cambios están en las specs 001 a 004) | RF-01 a RF-05, RF-11 | 001–004 | Claude | Claude | Antigravity | **En implementación** |

El reparto sigue la sección 8 del plan de reestructuración. La 007 puede avanzar en paralelo con la 004–006.

## Cómo leer una spec

Cada `spec.md` dice **qué** debe hacer el sistema y **por qué**, no **cómo** se programa. Tiene siempre estas partes:

1. **Objetivo**: el resultado que entrega la spec.
2. **Requerimientos que cubre**: los RF-xx del plan de reestructuración.
3. **Actores**: quién usa esa parte del sistema.
4. **Historias de usuario** (HU): "Como <rol>, quiero <acción>, para <beneficio>".
5. **Reglas de negocio** (RN): reglas que el sistema debe hacer cumplir siempre.
6. **Criterios de aceptación** (CA): pruebas verificables en formato *Dado / Cuando / Entonces*. Una spec está terminada cuando todos sus CA pasan.
7. **Fuera de alcance**: lo que esta spec **no** hace.
8. **Preguntas abiertas**: lo que el equipo debe responder antes de aprobarla. Cada pregunta trae una **propuesta**, que es lo que se asume si nadie dice lo contrario.

Prioridad de cada requisito (MoSCoW):

- **Debe**: obligatorio para aprobar la spec.
- **Debería**: importante; se hace salvo que falte tiempo.
- **Podría**: deseable; solo si sobra tiempo.

Las reglas de negocio tienen un identificador global (RN-001, RN-002…) y viven en la spec donde se definen. Las demás specs las citan por su número, sin repetirlas.

## Cómo aprobar una spec

1. Cada integrante lee la spec y responde las **preguntas abiertas** (puede ser con comentarios en el PR o en el documento compartido).
2. Se ajusta la spec con las respuestas.
3. Cuando el equipo está de acuerdo, se cambia el estado a **APROBADA** con la fecha.
4. Solo entonces se redactan `plan.md` (cómo se construye) y `tasks.md` (pasos concretos) de esa spec, y se revisan también.
5. Se implementa en la rama `feature/NNN-nombre`, tarea por tarea, y se abre PR a `develop`. Revisa el agente que no implementó.

## Decisiones del equipo que aplican a todas las specs

Decisiones D1–D9 del plan de reestructuración, confirmadas el 11 nov 2026:

| # | Decisión |
| --- | --- |
| D1 | Se **elimina** la auditoría de puntos ecológicos (recipientes, nivel de llenado, señalización). El sistema se centra en la caracterización. |
| D2 | Institución piloto: **I.E. Prudencia Daza** (Valledupar). El sistema es multi-institución. |
| D3 | Backend en **Java 17 + Spring Boot 3**. |
| D4 | Frontend en **React + Vite**. |
| D5 | Plano **2D** primero; **3D** por extrusión si sobra tiempo. |
| D6 | Rótulo de bolsa **FUENTE-DÍA-TURNO** (ej. `AU-D3-M`), generado por el sistema. |
| D7 | Reportes exportables en **PDF y Excel**. |
| D8 | Submuestra de cuarteo en un **rango configurable**, por defecto **45 a 55 kg**. |
| D9 | Solo hay **dos roles**: **DOCENTE**, con todos los permisos, y **ESTUDIANTE_UNIVERSITARIO**, con todo excepto administrar, crear estudios o cambiar su estado, y abrir o cerrar turnos. **No hay estudiantes de colegio ni menores de edad** en el sistema (ajustado el 11 nov 2026). |
| — | Plano con **latitud/longitud (WGS84) y MapLibre GL JS**, no coordenadas en %. |
| D10 | Toda la persistencia en **PostgreSQL 16+, instalado localmente** por cada integrante. Nada de archivos planos. El esquema se diseña desde cero; el script del modelo anterior no se usa. |
| D11 | El código del modelo anterior (Swing, repositorios de archivos, puntos ecológicos) se **elimina al empezar** la spec 001. Antes se marca con un tag de git para poder consultarlo. |
| D12 | Mientras no lleguen los datos reales de la I.E. Prudencia Daza, los datos semilla son **ficticios pero realistas**. |
| D13 | **Ajustes por las pruebas de uso** (13 nov 2026): fechas del estudio recalculadas con días hábiles y festivos de Colombia; activar y desactivar fuentes, subcategorías e instituciones, con filtro de inactivos; sin códigos de reglas en pantalla; botones Atrás, Guardar y Descartar, y cursor de mano; pantalla Mi Perfil; eliminar un estudio planificado y cancelar uno iniciado; solo los estudiantes se desactivan; usuarios en tabla con filtros. Afecta las specs 001 a 004. |

### Diferencias con el Requerimiento Técnico oficial (registro de riesgo)

El documento *Requerimiento Técnico Integrador – EcoPoint Edu* (UPC, oct 2026) pide cosas que el equipo decidió cambiar. Se registran aquí para que nadie se sorprenda en la sustentación:

| Tema | Requerimiento oficial | Decisión del equipo |
| --- | --- | --- |
| Alcance | Puntos ecológicos con 3 recipientes, inspecciones, alertas por DESBORDADO y mala separación; la demo final de 6 pasos se basa en eso | Se eliminan los puntos ecológicos (D1); la demo se rehace sobre la caracterización |
| Backend | Servlets REST (`com.ecopoint.controller`), DAO con JDBC puro (`DatabaseConnection`) | Spring Boot 3 |
| Frontend | HTML5/CSS3/JS ES6 con Fetch API (`webapp/`) | React + Vite |
| Plano | Coordenadas en % (`coord_x`, `coord_y` entre 0 y 100) sobre una imagen | Latitud/longitud con MapLibre |
| Nombres de clases | En español (`PuntoEcologico`, `Jornada`…) | En inglés, según `TEAM.md` y el código actual |

**Se conserva del requerimiento oficial**: la jerarquía de excepciones (`EcoPointException` y sus 4 subclases), los validadores dedicados sin estado, la dirección estricta de dependencias entre capas, el prefijo `/api/v1/*`, el formato JSON de error con `errorCode` y los códigos HTTP 400/404/422/500, los scripts `sql/001_schema.sql` y `sql/002_seed.sql`, y las bitácoras de uso de IA en `docs/ai-usage/`.

**Acción pendiente:** confirmar por escrito con el profe estos cambios (correo o acta), porque la rúbrica oficial se basa en el requerimiento.

## Glosario

| Término | Significado |
| --- | --- |
| Estudio de caracterización | Campaña de muestreo de 8 días en una institución, que produce su línea base. |
| Día de muestreo | Cada uno de los 8 días del estudio, numerados del 1 al 8. El día 1 se descarta de los cálculos. |
| Turno | Mañana (M) o tarde (T) de un día de muestreo. Equivale a "jornada" en el póster. |
| Fuente de generación | Lugar de la institución donde se generan residuos: aulas, oficinas, tienda escolar, PAE, zonas verdes, baños, laboratorio. |
| Bolsa | Unidad recolectada al cierre de un turno en una fuente, rotulada con su código. |
| Peso bruto / tara / peso neto | Peso de la bolsa con su recipiente / peso del recipiente o bolsa vacía / bruto − tara. |
| Cuarteo | Método NMX-AA-015 para obtener una submuestra representativa (≈ 50 kg). |
| Submuestra | Porción obtenida por cuarteo que se clasifica por subcategoría. |
| Grupo | Uno de los 4 grupos de la Res. 2184 de 2019: Blanco, Verde, Negro, Sin color. |
| Subcategoría | Tipo específico de residuo dentro de un grupo (papel, cartón, PET, restos de comida…). |
| Densidad (peso volumétrico) | Peso neto / volumen ocupado, en kg/m³ (NMX-AA-019). |
| PPC | Producción per cápita: kg por persona por día. |
| CV | Coeficiente de variación = desviación estándar / promedio. |
| Doble registro | Cada dato se anota en papel y en el aplicativo; luego se cotejan. |
| Depuración | Revisión y corrección de los datos antes de calcular la línea base. |
| Línea base | Conjunto de indicadores que describe los residuos de la institución al cerrar el estudio. |
| Cuadrilla | Grupo de estudiantes universitarios con un docente responsable que hace el muestreo. |
| PAE | Programa de Alimentación Escolar. |
| PRAE | Proyecto Ambiental Escolar. |
| EPP | Elementos de protección personal. |
