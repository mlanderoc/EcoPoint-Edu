# Equipo de Desarrollo — EcoPoint Edu (`TEAM.md`)

## Información Académica
* **Universidad:** Universidad Popular del Cesar (UPC) — Sede Valledupar
* **Facultad:** Ingenierías y Tecnologías — Programa de Ingeniería de Sistemas
* **Asignatura:** Programación de Computadores III
* **Docente:** Ing. Esp. Alfredo David Bautista Romero
* **Proyecto:** **EcoPoint Edu** — *Aplicación web para la caracterización de residuos sólidos en instituciones educativas*

---

## Integrantes del Equipo y Distribución de Paquetes

El responsable de un paquete es el **autor de sus commits** y aparece en el `@author` de su JavaDoc (ver [`AGENTS.md`](../AGENTS.md)).

| # | Nombre Completo | Usuario GitHub | Correo Institucional | Rol en el Equipo | Paquetes y partes asignadas |
| :-: | :--- | :--- | :--- | :--- | :--- |
| **1** | **Merlín Yelenis Landero Cuello** | `mlanderoc` | `mlandero@unicesar.edu.co` | **Líder Técnica — Arquitectura, Persistencia y Seguridad** | • Configuración Maven (`pom.xml`, Maven Wrapper)<br>• `com.ecopoint.exceptions`<br>• `com.ecopoint.config` y `com.ecopoint.security`<br>• Scripts SQL (`sql/`) y repositorios de institución, fuentes, subcategorías, usuarios, cuadrillas y estudios en `com.ecopoint.persistence`<br>• Documentación, diagramas y specs (`docs/`, `specs/`, `AGENTS.md`, `README.md`) |
| **2** | **Estefanía Márquez Martínez** | `EstefaniaMarquez` | `estefaniamarquez@unicesar.edu.co` | **Desarrolladora Backend — Modelo, Persistencia y Servicios** | • `com.ecopoint.model`<br>• Repositorios de turnos, bolsas, cuarteo, densidad, verificación, correcciones, alertas y evidencias en `com.ecopoint.persistence`<br>• Servicios de `com.ecopoint.service`, salvo los de Carlos |
| **3** | **Carlos Raúl Gómez Galvis** | `craulgomez` | `craulgomez@unicesar.edu.co` | **Desarrollador Backend y Frontend — Validaciones, API e Interfaz** | • `com.ecopoint.validation`<br>• Servicios de alertas, cuarteo y densidad en `com.ecopoint.service`<br>• API REST: `com.ecopoint.controller` y `com.ecopoint.dto`<br>• Frontend web en `frontend/` (React) |

Cada paquete incluye sus pruebas.

---

## Convención de Control de Versiones (Git Flow)

1. **Ramas Principales:**
   * `main`: versiones estables y entregas oficiales.
   * `develop`: rama de integración donde convergen los Pull Requests.
2. **Ramas de Trabajo (una rama por spec):**
   * `feature/NNN-nombre`: implementación de la spec `NNN` de [`specs/`](../specs/README.md) (por ejemplo, `feature/003-api-rest-seguridad`).
   * `fix/nombre`: corrección puntual.
3. **Reglas de Estilo e Idioma:**
   * **Código fuente, nombres de clases/métodos, JavaDoc (texto simple con `@author <usuario_github>`) y diagramas técnicos:** en **Inglés**.
   * **Mensajes de error/interfaz para el usuario final y control de versiones (ramas, commits, PRs):** en **Español**.
