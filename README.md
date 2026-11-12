# EcoPoint Edu

Aplicación web para la **caracterización de residuos sólidos en instituciones educativas**. Sistematiza el protocolo de muestreo del póster *Caracterización participativa de los residuos sólidos en la I.E. Prudencia Daza, Valledupar*: estudio de 8 días, turnos de mañana y tarde, fuentes de generación, pesaje de bolsas, cuarteo, densidad, control de calidad del 2 %, doble registro e indicadores de línea base. Aplica la **Resolución 2184 de 2019** (grupos Blanco, Verde, Negro y Sin color).

Proyecto de aula de **Programación de Computadores III**, Universidad Popular del Cesar. Equipo: [`src/TEAM.md`](src/TEAM.md).

> **Estado: en reestructuración.** El proyecto se está reconstruyendo con *Spec Driven Development*. La versión anterior (escritorio en Swing, archivos planos y auditoría de puntos ecológicos) está guardada en el tag [`v1-modelo-anterior`](../../tree/v1-modelo-anterior).

---

## 1. Avance

| Spec | Contenido | Estado |
| --- | --- | --- |
| [001](specs/001-dominio-caracterizacion/spec.md) | Dominio de caracterización: modelo, validadores y excepciones (Java puro) | Terminada, en revisión |
| [002](specs/002-persistencia-postgresql/spec.md) | Persistencia en PostgreSQL | Pendiente |
| [003](specs/003-api-rest-seguridad/spec.md) | API REST y seguridad (Spring Boot) | Pendiente |
| [004](specs/004-registro-en-campo/spec.md) a [007](specs/007-plano-interactivo-fuentes/spec.md) | Pantallas web (React), indicadores, reportes y plano interactivo | Pendiente |

Mientras no se completen las specs 002 y 003 no hay aplicación para ejecutar: por ahora el proyecto es el dominio con sus pruebas.

## 2. Cómo empezar

1. Lee [`AGENTS.md`](AGENTS.md): reglas del proyecto (arquitectura, convenciones, Git y autoría).
2. Lee [`specs/README.md`](specs/README.md): índice de specs, decisiones del equipo y glosario.

## 3. Compilar y probar

Requiere **Java 17 o superior**. No hace falta instalar Maven: el proyecto trae el Maven Wrapper.

```bash
# Git Bash, Linux o macOS
./mvnw test

# PowerShell o CMD en Windows
mvnw.cmd test
```

Debe terminar en `BUILD SUCCESS` con todas las pruebas en verde.

## 4. Arquitectura

```
Frontend (React)  →  controller (API REST /api/v1)  →  service  →  validation  →  model
                                                        service  →  persistence  →  PostgreSQL
exceptions: transversal
```

| Paquete | Contenido | Spec |
| --- | --- | --- |
| `com.ecopoint.exceptions` | Jerarquía `EcoPointException` y sus 6 subclases | 001 |
| `com.ecopoint.model` | Estudio, días, turnos, fuentes, bolsas, cuarteo, densidad, verificación, usuarios, alertas | 001 |
| `com.ecopoint.validation` | Validadores sin estado y `AccessPolicy` (permisos por rol) | 001 |
| `com.ecopoint.persistence` | Repositorios sobre PostgreSQL | 002 |
| `com.ecopoint.service`, `com.ecopoint.controller` | Servicios y API REST | 003 |

## 5. Documentación técnica (en inglés)

* [Diagrama de clases y trazabilidad GRASP/SOLID](docs/class-diagrama.md)
* [Diagrama de capas y dependencias](docs/layer-diagram.md)
* [Jerarquía de excepciones](docs/exception-hierarchy-diagram.md)
