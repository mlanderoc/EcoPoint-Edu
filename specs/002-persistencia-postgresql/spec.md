# Spec 002 — Persistencia en PostgreSQL

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Claude / Antigravity |
| Requerimientos | Todos (almacenamiento de RF-01 a RF-14) |
| Depende de | Spec 001 aprobada |
| Fecha | 2026-11-11 |

## 1. Objetivo

Guardar de forma permanente y segura todos los datos del dominio de la spec 001 en **PostgreSQL 16+**, reemplazando los archivos planos (`data/*.txt`). La base de datos debe poder reconstruirse desde cero con dos scripts y quedar lista para la demostración.

Esta spec también sirve a la asignatura **Base de Datos I**: el esquema, las restricciones y los datos semilla son parte de su entrega.

## 2. Requerimientos que cubre

Todos los RF del plan de reestructuración, en lo que toca a almacenamiento. En particular:

- RF-04: geometría de las fuentes.
- RF-10: historial de verificaciones y correcciones.
- RF-14: datos que el plano necesita.

## 3. Actores

- **Equipo de desarrollo**: crea, reconstruye y migra la base de datos.
- **Servicios del sistema**: leen y escriben a través de la capa de persistencia. Ningún otro componente toca la base de datos.

## 4. Historias de usuario

- **HU-010.** Como integrante del equipo, quiero reconstruir la base de datos desde cero con dos scripts, para tener el mismo entorno en todos los computadores y en la sustentación.
- **HU-011.** Como coordinador, quiero que la base de datos rechace datos inválidos aunque alguien se salte la aplicación, para que la línea base sea confiable.
- **HU-012.** Como coordinador, quiero que un cuarteo se guarde completo o no se guarde, para no quedar con resultados a medias.
- **HU-013.** Como profe evaluador, quiero ver datos de ejemplo realistas, para probar el sistema sin capturar 8 días de muestreo.

## 5. Requisitos

### 5.1 Scripts

- **R-001** (Debe). `sql/001_schema.sql` crea todo el esquema desde cero: tipos, tablas, claves, restricciones, índices y vistas. Se puede ejecutar varias veces: primero borra lo que exista.
- **R-002** (Debe). `sql/002_seed.sql` carga los datos semilla (sección 5.5).
- **R-003** (Debe). Ejecutar `001` y luego `002` sobre una base vacía termina sin errores.
- **R-004** (Debe). El script actual `data/ecopoint_script_entrega2.sql` queda como histórico de la Entrega 2 y no se modifica.

### 5.2 Integridad en la base de datos

Las reglas de la spec 001 que se pueden expresar en la base de datos **se repiten ahí**, como segunda línea de defensa:

- **R-010** (Debe). Claves primarias en todas las tablas y claves foráneas en todas las relaciones.
- **R-011** (Debe). Restricciones `UNIQUE` para: código de fuente por institución (RN-002), correo de usuario (RN-084), nombre de subcategoría (RN-041), día por estudio, turno por día (RN-020), rótulo de bolsa por estudio (RN-032), un cuarteo por turno (RN-050), una subcategoría por cuarteo (RN-055).
- **R-012** (Debe). Restricciones `CHECK` para: formato del código de fuente, días del 1 al 8, población ≥ 0 y total > 0, peso bruto > tara ≥ 0, volumen > 0, pesos de subcategoría ≥ 0, latitud y longitud en rango.
- **R-013** (Debe). Listas cerradas (tipo de fuente, grupo, turno, estados, roles, tipos de alerta) como tipos `ENUM` o tablas de catálogo.
- **R-014** (Debe). El peso neto y la densidad de la bolsa **no** se guardan digitados: se calculan (columna generada o vista), para que nunca contradigan bruto, tara y volumen.
- **R-015** (Debe). Políticas de borrado: `RESTRICT` por defecto (no se borra lo que tiene registros dependientes); `CASCADE` solo de cuarteo a sus resultados. Cada política lleva un comentario con su porqué, como ya hace el script de la Entrega 2.

### 5.3 Transacciones

- **R-020** (Debe). Estas operaciones se guardan **todo o nada**:
  - un cuarteo con todos sus resultados;
  - una corrección con su registro en el historial;
  - el cambio de estado de un estudio con sus validaciones.
- **R-021** (Debe). Si falla el guardado, no queda nada a medias. El error llega a los servicios como `PersistenceException`, con un mensaje que dice qué operación falló, sin mostrar detalles internos de SQL al usuario.

### 5.4 Trazabilidad

- **R-030** (Debe). Cada registro verificable (RN-060) guarda: quién lo digitó, cuándo, su estado de verificación, quién lo verificó y cuándo.
- **R-031** (Debe). Las correcciones (RN-063) se guardan en una tabla de historial que nunca se actualiza ni se borra: entidad, registro, campo, valor anterior, valor nuevo, motivo, usuario, fecha y hora.
- **R-032** (Debe). Las geometrías de las fuentes se guardan en formato GeoJSON (`jsonb`), en coordenadas WGS84 (latitud/longitud).
- **R-033** (Podría). Usar PostGIS para las geometrías, si el equipo lo instala sin problemas.

### 5.5 Datos semilla

- **R-040** (Debe). Catálogo completo de grupos y subcategorías de la Res. 2184 de 2019 (al menos: papel, cartón, plástico PET, otros plásticos, vidrio, metales, Tetra Pak, restos de comida, hojas y podas, papel higiénico y servilletas, icopor, barrido, pilas, luminarias, RAEE).
- **R-041** (Debe). La institución piloto, I.E. Prudencia Daza, con sus fuentes de generación. Los polígonos son provisionales hasta trazar el plano real (spec 007).
- **R-042** (Debe). Un usuario por rol, con contraseñas de demostración documentadas en el README.
- **R-043** (Debe). Un estudio de demostración CERRADO, con los 8 días completos y datos realistas, para que el tablero y los reportes muestren resultados desde el primer arranque.
- **R-044** (Debería). Un segundo estudio EN_EJECUCION, a medio llenar, para demostrar el registro en campo.

### 5.6 Acceso desde la aplicación

- **R-050** (Debe). Los servicios dependen de **interfaces** de repositorio, nunca de una implementación concreta (Variaciones Protegidas, como ya está diseñado hoy).
- **R-051** (Debe). Los datos de conexión (servidor, usuario, contraseña) se leen de configuración o de variables de entorno. Nunca se escriben en el código ni se suben al repositorio.
- **R-052** (Debe). Se eliminan los repositorios de archivos planos (`File*Repository`, `FileStorageHelper`) y la carpeta `data/*.txt`.

### 5.7 Documentación

- **R-060** (Debe). Diccionario de datos actualizado: tabla por tabla, columna por columna (tipo, nulabilidad, significado), como pide la guía de Base de Datos I.
- **R-061** (Debe). Diagrama entidad-relación actualizado en `docs/`.

## 6. Criterios de aceptación

- **CA-100.** Dada una base de datos vacía, cuando se ejecutan `001_schema.sql` y `002_seed.sql`, entonces terminan sin errores y existen la institución piloto, sus fuentes, los 4 grupos con sus subcategorías, un usuario por rol y el estudio de demostración.
- **CA-101.** Dada una base de datos ya creada, cuando se ejecutan de nuevo los dos scripts, entonces terminan sin errores y el contenido queda igual que la primera vez.
- **CA-102.** Cuando se inserta directamente en SQL una bolsa con tara mayor que el peso bruto, entonces la base de datos la rechaza.
- **CA-103.** Cuando se inserta directamente en SQL un segundo cuarteo para el mismo turno, entonces la base de datos lo rechaza.
- **CA-104.** Cuando se intenta borrar una fuente con bolsas registradas, entonces la base de datos lo rechaza.
- **CA-105.** Dado un cuarteo con 5 resultados donde el quinto es inválido, cuando se guarda, entonces no queda guardado ni el cuarteo ni ningún resultado.
- **CA-106.** Dada una bolsa con bruto 12,40 y tara 0,40, cuando se consulta, entonces su peso neto es 12,00 sin que nadie lo haya digitado.
- **CA-107.** Dada una corrección hecha desde la aplicación, cuando se consulta el historial, entonces aparece con valor anterior, valor nuevo, motivo, usuario y fecha; y no existe forma, desde la aplicación, de modificarla o borrarla.
- **CA-108.** Cuando se cambian los datos de conexión en la configuración, entonces la aplicación se conecta a otra base sin recompilar.
- **CA-109.** Cuando se busca en el repositorio, entonces no aparece ninguna contraseña real de base de datos.
- **CA-110.** Las pruebas de integración de cada repositorio (guardar, buscar por id, listar, actualizar, borrar o desactivar) pasan contra una base PostgreSQL real.

## 7. Fuera de alcance

- Reglas que no se pueden expresar en la base de datos (ej. "quien verifica es distinto de quien digitó"): las hacen cumplir los validadores y servicios de la spec 001.
- Respaldo automático y réplicas.
- Migración de los datos de los archivos planos actuales: eran de demostración y se reemplazan por la semilla nueva.

## 8. Preguntas abiertas

| # | Pregunta | Propuesta |
| --- | --- | --- |
| P1 | ¿Nombres de tablas en español (como la Entrega 2) o en inglés (como las clases Java)? | En español, en mayúsculas, como la Entrega 2 (`ESTUDIO`, `TURNO`, `BOLSA`…). Así se mantiene la continuidad para Base de Datos I. |
| P2 | ¿Acceso a datos con Spring Data JPA o con JDBC (`JdbcTemplate`)? | JDBC con `JdbcTemplate`: el SQL queda visible y se defiende mejor en Base de Datos I. Se decide en `plan.md`. |
| P3 | ¿Listas cerradas como `ENUM` de PostgreSQL o como tablas de catálogo? | `ENUM`, como la Entrega 2. Las subcategorías sí son tabla, porque pueden crecer. |
| P4 | ¿Las pruebas de integración usan un PostgreSQL local o uno en contenedor (Testcontainers)? | Testcontainers si el equipo tiene Docker; si no, una base local de pruebas. |
| P5 | ¿Instalar PostGIS? | No por ahora (R-033 queda como "Podría"). |
