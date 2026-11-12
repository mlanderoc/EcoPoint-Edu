# AGENTS.md — Reglas del proyecto EcoPoint Edu

Este archivo es la "constitución" del proyecto. Lo leen y lo cumplen **todos**: los integrantes del equipo y los agentes de IA (Claude, Antigravity u otros). Si una instrucción de una conversación contradice este archivo, se pregunta antes de actuar.

## 1. Qué es el proyecto

**EcoPoint Edu** es una aplicación web para la **caracterización de residuos sólidos en instituciones educativas**. Sistematiza el protocolo de muestreo del póster *Caracterización participativa de los residuos sólidos en la I.E. Prudencia Daza*: estudio de 8 días, turnos de mañana y tarde, fuentes de generación, pesaje de bolsas, cuarteo, densidad, control de calidad del 2 %, doble registro e indicadores de línea base.

- Proyecto de aula de **Programación de Computadores III**, Universidad Popular del Cesar.
- Equipo e integrantes: [`src/TEAM.md`](src/TEAM.md).
- Este repositorio **no** es el de la asignatura Base de Datos I, que tiene su propio repositorio. No se mezclan formatos ni entregas de esa materia.

## 2. Fuente de verdad: las specs

- Las especificaciones aprobadas están en [`specs/`](specs/README.md). Empieza siempre por `specs/README.md`.
- **Nada se implementa sin una spec aprobada.** Si algo no está en una spec, se pregunta al equipo antes de construirlo.
- Flujo por spec:
  1. `spec.md` aprobada: qué y por qué.
  2. `plan.md`: cómo se construye. El equipo lo revisa y lo aprueba.
  3. `tasks.md`: pasos pequeños y verificables. El equipo lo revisa y lo aprueba.
  4. Implementación tarea por tarea, en la rama de la spec.
  5. PR a `develop`, revisado por quien no implementó.
- Si durante la implementación aparece algo que contradice la spec, **se detiene esa tarea y se pregunta**. La spec se corrige primero y el código después.
- Al terminar cada tarea se marca en `tasks.md` (`- [x]`).

### Reparto de trabajo entre agentes

| Spec | Implementa | Revisa |
| --- | --- | --- |
| 001 Dominio, 002 Persistencia, 007 Plano | Claude | Antigravity |
| 003 API, 004 Registro en campo, 005 Cuarteo y calidad, 006 Indicadores | Antigravity | Claude |

## 3. Stack tecnológico

| Capa | Tecnología |
| --- | --- |
| Backend | Java 17, Maven, Spring Boot 3 (a partir de la spec 002/003) |
| Base de datos | **PostgreSQL 16+, instalado localmente** por cada integrante. **Nada de archivos planos.** |
| Frontend | React + Vite, MapLibre GL JS para el plano |
| Pruebas | JUnit 5; pruebas de integración contra PostgreSQL local (`ecopoint_test`) |

## 4. Arquitectura y dependencias

```
Frontend (React)  →  controller (API REST /api/v1)  →  service  →  validation  →  model
                                                        service  →  persistence  →  model
exceptions: transversal, la puede usar cualquier paquete
```

Reglas obligatorias:

- `model` no importa nada de `validation`, `service`, `persistence` ni `controller`.
- `validation` solo importa `model` y `exceptions`.
- `service` depende de **interfaces** de repositorio, nunca de implementaciones (Variaciones Protegidas).
- `controller` no contiene reglas de negocio: recibe, delega en `service` y responde.
- La API nunca expone clases del modelo directamente: usa DTO.

Dónde va cada regla:

| Tipo de regla | Dónde | Ejemplo |
| --- | --- | --- |
| Cálculo sobre los datos propios de un objeto | Método del modelo (Experto en Información) | `WasteBag.getNetWeight()` |
| Invariante de estado de un objeto | Método del modelo, que lanza excepción | Cerrar un turno ya cerrado |
| Formato, rango, nulos | Validador | Tara ≥ 0 |
| Regla que necesita datos de otros objetos | Validador, que recibe esos datos como parámetros | Código de fuente único en la institución |
| Permisos por rol | `AccessPolicy` | Solo un docente registra residuos peligrosos |
| Orquestación, consultas a la base de datos, transacciones | Servicio | Registrar una bolsa y generar su alerta |

## 5. Convenciones de código

- **Idioma:** clases, métodos, variables, comentarios, JavaDoc y diagramas técnicos en **inglés**. Mensajes al usuario final (excepciones, pantallas) en **español**.
- **JavaDoc** en todas las clases y métodos públicos. El `@author <usuario_github>` es el integrante responsable de ese paquete según el reparto de [`src/TEAM.md`](src/TEAM.md) (ej. `exceptions` → `mlanderoc`, `model` → `EstefaniaMarquez`, `validation` → `craulgomez`). Si un paquete nuevo no está en `TEAM.md`, se agrega allí antes de crear sus clases.
- **Pesos, volúmenes y porcentajes:** siempre `BigDecimal`, nunca `double` ni `float`. Redondeo `HALF_UP`. Solo se redondea al mostrar o al guardar; los cálculos internos conservan la precisión.
- **Fechas:** `LocalDate` para días y `LocalDateTime` para instantes. Los métodos del dominio reciben la fecha y hora como parámetro; no llaman a `now()` por dentro, para poder probarlos.
- **Identificadores:** `Long`. Vale `null` mientras el objeto no se ha guardado.
- **Validadores:** clases `final`, sin estado, con constructor privado y métodos `static`, como los existentes.
- **Excepciones:** solo las de la jerarquía `EcoPointException`, nunca `IllegalArgumentException` ni excepciones genéricas.

  | Excepción | Cuándo | HTTP |
  | --- | --- | --- |
  | `InvalidDataException` | Formato, nulo o rango inválido | 400 |
  | `AccessDeniedException` | El rol no permite la acción | 403 |
  | `ResourceNotFoundException` | El registro no existe | 404 |
  | `BusinessRuleException` | Viola una regla de negocio | 422 |
  | `QualityControlException` | Cuarteo fuera de la tolerancia del 2 % | 422 |
  | `PersistenceException` | Falla de base de datos | 500 |

- **Código limpio:** nombres significativos, métodos cortos con una sola responsabilidad, sin código comentado ni comentarios obvios, y principios GRASP y SOLID.

## 6. Pruebas

- Toda regla de negocio (RN) y todo criterio de aceptación (CA) de una spec tiene al menos una prueba automática.
- El nombre del método de prueba empieza con el código del criterio: `ca032_netWeightAndDensityAreCalculated()`.
- `./mvnw test` debe pasar completo antes de abrir un PR. Un PR con pruebas fallando no se fusiona.

## 7. Git y GitHub

- Ramas: `main` ← `develop` ← `feature/NNN-nombre` (una rama por spec) o `fix/nombre`.
- **Prohibido** hacer commit directo a `main` o `develop`, usar `git push --force` y aprobar el PR propio.
- Mensajes de commit en **español**, con prefijo: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`. Ejemplo: `feat: agregar cálculo de densidad en WasteBag`.
- **Autoría:** el **autor** de cada commit es el integrante responsable de lo que cambia, según el reparto de [`src/TEAM.md`](src/TEAM.md). El mismo integrante va como **autor y como committer**, para que ningún otro aparezca en el commit. Se indica con `GIT_COMMITTER_NAME="Nombre" GIT_COMMITTER_EMAIL="correo" git commit --author="Nombre <correo>"`:

  | Lo que cambia el commit | Autor |
  | --- | --- |
  | `exceptions`, `pom.xml`, Maven Wrapper, `docs/`, `specs/`, `AGENTS.md` | `Merlín Yelenis Landero Cuello <mlandero@unicesar.edu.co>` |
  | `model` y sus pruebas | `Estefanía Márquez Martínez <estefaniamarquez@unicesar.edu.co>` |
  | `validation` y sus pruebas, interfaz | `Carlos Gomez <craulgomez@unicesar.edu.co>` |

  Si una tarea toca varios paquetes, se divide en **un commit por responsable**. Los agentes de IA **no** se ponen como autores ni coautores: nada de líneas `Co-Authored-By` ni firmas de IA en commits o PR.
- Commits pequeños: idealmente uno por tarea de `tasks.md` y por responsable.
- Títulos y descripciones de PR en español, con la spec y las tareas que cubre.

## 8. Lo que no se toca

- `data/`, `target/` y los PDF sueltos en la carpeta local: no se suben ni se usan como referencia.
- El script de base de datos del modelo anterior: no aplica a esta reestructuración. El esquema nuevo se crea desde cero (spec 002).
- Secretos: contraseñas de base de datos o claves nunca van en el código ni en el repositorio.

## 9. Ante la duda

Pregunta al equipo antes de suponer. Es mejor una pregunta corta que una semana de trabajo en la dirección equivocada.
