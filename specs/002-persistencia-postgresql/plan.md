# Plan — Spec 002: Persistencia en PostgreSQL

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (11 nov 2026) |
| Spec | [spec.md](spec.md) (aprobada) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo |
| Rama | `feature/002-persistencia-postgresql` |
| Fecha | 2026-11-11 |

## 1. Resumen

La spec 002 guarda en **PostgreSQL** todo el dominio de la spec 001. Tiene tres partes:

1. **Scripts SQL** (`sql/001_schema.sql` y `sql/002_seed.sql`). Crean la base desde cero, con las reglas de la spec 001 repetidas como restricciones de la base de datos, y cargan datos de demostración realistas.
2. **Repositorios**. En `com.ecopoint.persistence` hay una **interfaz** por agregado y, en `com.ecopoint.persistence.jdbc`, su implementación con **Spring JDBC (`JdbcTemplate`)**.
3. **Pruebas de integración** contra una base local `ecopoint_test`, más la documentación de la base de datos.

Spring Boot entra al proyecto en esta spec, pero **solo para el acceso a datos**: sin servidor web ni API, que llegan en la spec 003. El dominio y los validadores de la spec 001 no cambian.

## 2. Decisiones técnicas

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | **Spring Boot 3.5** como padre del `pom.xml`, con `spring-boot-starter-jdbc` y el driver `org.postgresql` | Gestiona versiones y transacciones; la spec 003 solo agrega el starter web |
| T2 | **`JdbcTemplate` / `NamedParameterJdbcTemplate`**, no JPA (spec, P2) | El SQL queda visible y revisable; el dominio no se llena de anotaciones de JPA |
| T3 | Tablas en **español y mayúsculas** (`ESTUDIO`, `TURNO`, `BOLSA`…); columnas en `snake_case` (spec, P1) | Decisión de la spec |
| T4 | Listas cerradas como **`ENUM` de PostgreSQL**, con los mismos valores que los enums de Java; las subcategorías son tabla (spec, P3) | Integridad en la base y mapeo directo con `Enum.valueOf` |
| T5 | Peso neto y densidad de la bolsa como **columnas generadas** (`GENERATED ALWAYS AS … STORED`) (R-014) | Nunca contradicen bruto, tara y volumen |
| T6 | Geometrías en **`jsonb`** con GeoJSON (R-032); PostGIS no (P5) | No exige instalar extensiones |
| T7 | Configuración por **variables de entorno** con valores por defecto sin contraseña: `ECOPOINT_DB_URL`, `ECOPOINT_DB_USER`, `ECOPOINT_DB_PASSWORD` (R-051) | Ninguna contraseña real en el repositorio (CA-109) |
| T8 | Dos bases locales: **`ecopoint`** (desarrollo, con semilla) y **`ecopoint_test`** (pruebas, se recrea en cada corrida) (P4) | Las pruebas no borran los datos de desarrollo |
| T9 | Pruebas de integración con sufijo **`*IT`**, ejecutadas por `maven-failsafe-plugin` en `./mvnw verify`; `./mvnw test` sigue sin base de datos | Quien no tenga PostgreSQL puede seguir corriendo las pruebas unitarias |
| T10 | Toda excepción de Spring (`DataAccessException`) se traduce a **`PersistenceException`** con el nombre de la operación en español (R-021) | Los servicios solo conocen la jerarquía `EcoPointException` |
| T11 | Los métodos que escriben en varias tablas son **`@Transactional`** (cuarteo con resultados, turno con población y marcas) (R-020) | Todo o nada |
| T12 | Las transacciones que combinan **varios repositorios** (corregir un registro y guardar su `Correction`; cambiar el estado de un estudio tras validarlo) se cierran en los **servicios** de la spec 003. Aquí se dejan los métodos y las pruebas de que cada uno es atómico | Los servicios llegan en la spec 003; R-020 se completa allá |
| T13 | Clase de arranque **`com.ecopoint.EcoPointApplication`** (`@SpringBootApplication`), por ahora sin web | La necesitan las pruebas de integración; la spec 003 le agrega la API |
| T14 | Script **`sql/recrear-base.ps1`** (PowerShell) y **`sql/recrear-base.sh`** (Git Bash) que ejecutan 001 y 002 con `psql` | Un solo comando para dejar la base lista (HU-010) |

## 3. Esquema de la base de datos

### 3.1 Tipos `ENUM`

`tipo_fuente`, `grupo_residuo`, `tipo_turno`, `estado_turno`, `estado_estudio`, `estado_verificacion`, `estado_calidad`, `rol_usuario`, `tipo_alerta`, `tipo_evidencia`. Sus valores son los mismos de los enums de Java (`SourceType`, `WasteGroup`, `ShiftType`…).

### 3.2 Tablas

| Tabla | Clase Java | Claves y restricciones principales |
| --- | --- | --- |
| `INSTITUCION` | `Institution` | Nombre `UNIQUE`; centro del mapa (lat/lon con `CHECK` de rango); zoom 0–22; parámetros de `CharacterizationSettings` con `CHECK` mínimo < máximo |
| `FUENTE_GENERACION` | `GenerationSource` | FK institución; `UNIQUE (id_institucion, codigo)`; `CHECK codigo ~ '^[A-Z0-9]{2,6}$'`; geometría `jsonb`; activa |
| `SUBCATEGORIA_RESIDUO` | `WasteCategory` | Nombre `UNIQUE` (sin distinguir mayúsculas: índice sobre `lower(nombre)`); `grupo_residuo` |
| `USUARIO` | `User` | Correo `UNIQUE` sin distinguir mayúsculas; `rol_usuario`; FK institución; `clave_hash` (la usa la spec 003) |
| `CUADRILLA`, `CUADRILLA_INTEGRANTE` | `Crew` | FK docente responsable; integrante `UNIQUE (id_cuadrilla, id_usuario)` |
| `ESTUDIO` | `CharacterizationStudy` | FK institución; `estado_estudio` |
| `DIA_MUESTREO` | `SamplingDay` | PK `(id_estudio, numero_dia)`; `CHECK numero_dia BETWEEN 1 AND 8` |
| `REAPERTURA_ESTUDIO` | `StudyReopening` | FK estudio y usuario; motivo no vacío |
| `FUENTE_ESTUDIO` | (fuentes participantes) | PK `(id_estudio, id_fuente)` |
| `TURNO` | `SamplingShift` | `UNIQUE (id_estudio, numero_dia, tipo_turno)` (RN-020); `estado_turno`; FK cuadrilla opcional |
| `TURNO_SIN_GENERACION` | marcas "sin generación" | PK `(id_turno, id_fuente)` |
| `POBLACION_TURNO` | `ShiftPopulation` | `UNIQUE (id_turno)`; `CHECK` ≥ 0 y total > 0; columnas de verificación |
| `BOLSA` | `WasteBag` | FK turno y fuente; `CHECK peso_bruto > tara AND tara >= 0 AND volumen > 0`; `peso_neto` y `densidad` generadas; `UNIQUE (id_turno, rotulo)`; atípica; columnas de verificación |
| `CUARTEO` | `Quartering` | `UNIQUE (id_turno)` (RN-050); `estado_calidad`; datos de anulación; columnas de verificación |
| `RESULTADO_CUARTEO` | `QuarteringResult` | PK `(id_cuarteo, id_subcategoria)` (RN-055); `CHECK peso >= 0`; **`ON DELETE CASCADE`** desde cuarteo |
| `MEDICION_DENSIDAD` | `DensityMeasurement` | `UNIQUE (id_turno)` (RN-057); `CHECK` > 0; columnas de verificación |
| `CORRECCION` | `Correction` | Solo inserción: un *trigger* impide `UPDATE` y `DELETE` (R-031, CA-107) |
| `ALERTA` | `Alert` | `tipo_alerta`; tipo y id del registro que la causó; datos de atención |
| `EVIDENCIA` | `Evidence` | `tipo_evidencia`; `CHECK tamano_bytes BETWEEN 1 AND 5242880` |

**Columnas de verificación** (R-030), iguales en las 4 tablas verificables: `estado_verificacion`, `digitado_por`, `digitado_en`, `verificado_por`, `verificado_en`.

**Rótulo único por estudio** (RN-032): ya está garantizado, porque el rótulo incluye día y turno y el turno es único por estudio. En la base queda como `UNIQUE (id_turno, rotulo)`.

**Políticas de borrado** (R-015): `ON DELETE RESTRICT` en todas las FK, salvo `RESULTADO_CUARTEO → CUARTEO` (`CASCADE`) y las tablas puente (`CUADRILLA_INTEGRANTE`, `FUENTE_ESTUDIO`, `TURNO_SIN_GENERACION`). Cada una lleva un comentario `-- Política:` con su porqué.

### 3.3 Vistas de apoyo

- `V_TURNO_RESUMEN`: kg netos, número de bolsas y población por turno. La usará la spec 006.
- `V_REGISTROS_PENDIENTES`: registros pendientes de verificación por estudio. Sirve para RN-016 y RN-072.

## 4. Repositorios

Interfaces en `com.ecopoint.persistence`; implementaciones en `com.ecopoint.persistence.jdbc`.

| Interfaz | Métodos principales |
| --- | --- |
| `InstitutionRepository` | `save`, `findById`, `findAll`, `existsByName` |
| `GenerationSourceRepository` | `save`, `findById`, `findByInstitution`, `findCodesByInstitution`, `hasBags`, `deactivate` |
| `WasteCategoryRepository` | `save`, `findById`, `findAll`, `findAllAsMap`, `findNames`, `hasResults` |
| `UserRepository` | `save`, `findById`, `findByEmail`, `findAll`, `findEmails`, `deactivate` |
| `CrewRepository` | `save` (con integrantes), `findById`, `findByInstitution` |
| `StudyRepository` | `save` (con sus 8 días y reaperturas), `findById`, `findByInstitution`, `updateStatus`, `saveParticipatingSources`, `findParticipatingSources`, `countPendingRecords`, `countUnresolvedQuarterings` |
| `ShiftRepository` | `save` (con población y marcas), `findById` (agregado completo: población, bolsas, cuarteo y densidad), `findByStudy`, `updateStatus` |
| `WasteBagRepository` | `save`, `findById`, `findByShift`, `update` (pesos y estado de verificación) |
| `QuarteringRepository` | `save` (cuarteo y resultados en una transacción), `findByShift`, `update` |
| `DensityRepository` | `save`, `findByShift`, `update` |
| `VerificationRepository` | `updateVerification(tabla, id, info)`, `findPendingByStudy` |
| `CorrectionRepository` | `save`, `findByRecord` (no hay `update` ni `delete`) |
| `AlertRepository` | `save`, `findById`, `findPending`, `findByStudy`, `update` (atención) |
| `EvidenceRepository` | `save`, `findByTarget` |

Cada implementación tiene un `RowMapper` que reconstruye el objeto del dominio con sus constructores públicos. Si el dominio no permite restaurar algún estado al leerlo (por ejemplo, un turno cerrado o un estudio en depuración), se agrega un método de fábrica de reconstrucción en el modelo (ver sección 7, R1).

## 5. Pruebas

| Tipo | Qué | Cómo corre |
| --- | --- | --- |
| Unitarias existentes (264) | Dominio y validadores | `./mvnw test`, sin base de datos |
| `SchemaIT` | CA-100, CA-101 (scripts dos veces), CA-102, CA-103, CA-104 (restricciones con SQL directo) y CA-106 (columna generada) | `./mvnw verify` contra `ecopoint_test` |
| `*RepositoryIT` | CA-110: guardar, buscar, listar, actualizar, borrar o desactivar, por repositorio | `./mvnw verify` |
| `QuarteringRepositoryIT` | CA-105: cuarteo con un resultado inválido → no queda nada | `./mvnw verify` |
| `CorrectionRepositoryIT` | CA-107: el historial no se puede modificar ni borrar | `./mvnw verify` |
| `ConfigurationIT` | CA-108: URL de otra base por variable de entorno | `./mvnw verify` |
| Revisión manual | CA-109: `git grep` de contraseñas | En la tarea final |

Antes de la suite, `ecopoint_test` se recrea con `001_schema.sql`. Cada prueba limpia sus datos.

## 6. Responsables por parte (autor de los commits, según `src/TEAM.md`)

| Parte | Responsable |
| --- | --- |
| `pom.xml`, configuración, `EcoPointApplication`, scripts SQL, `docs/base-de-datos.md`, README | Merlín (`mlanderoc`) |
| Repositorios de institución, fuentes, subcategorías, usuarios, cuadrillas y estudio | Merlín (`mlanderoc`) |
| Repositorios de turnos, bolsas, cuarteo, densidad, verificación, correcciones, alertas y evidencias | Estefanía (`EstefaniaMarquez`) |
| Métodos de reconstrucción que haga falta agregar al modelo | Estefanía (`EstefaniaMarquez`) |

## 7. Riesgos y puntos a confirmar

| # | Tema | Propuesta |
| --- | --- | --- |
| R1 | **Reconstruir objetos desde la base.** Algunos objetos del dominio solo cambian de estado con métodos que validan transiciones (`changeStatus`, `close`, `addBag`…), así que al leerlos de la base hay que restaurar su estado sin volver a validar. | Agregar al modelo métodos estáticos `restore(...)` documentados como "solo para la capa de persistencia". Se revisan en el PR. |
| R2 | **Contraseña de PostgreSQL de cada integrante.** No se sube al repositorio. | Cada uno define `ECOPOINT_DB_PASSWORD` en su computador. El README explica cómo. |
| R3 | Versión instalada: **PostgreSQL 18** en el computador de Carlos; la spec pide 16+. | Compatible. Los scripts no usan nada exclusivo de la versión 18. |
| R4 | Datos semilla grandes (estudio cerrado de 8 días × 2 turnos × 8 fuentes). | Generarlos con `generate_series` y valores deterministas, para que el script sea corto y siempre dé lo mismo (CA-101). |
