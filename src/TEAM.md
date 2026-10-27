# Equipo de Desarrollo — EcoPoint Edu (`TEAM.md`)

## Información Académica
* **Universidad:** Universidad Popular del Cesar (UPC) — Sede Valledupar
* **Facultad:** Ingenierías y Tecnologías — Programa de Ingeniería de Sistemas
* **Asignatura:** Programación de Computadores III
* **Docente:** Ing. Esp. Alfredo David Bautista Romero
* **Proyecto:** **EcoPoint Edu** — *Sistema para el Registro, Caracterización y Análisis de Residuos Sólidos en Puntos Ecológicos de Instituciones Educativas*

---

## Integrantes del Equipo y Distribución de Paquetes

| # | Nombre Completo | Usuario GitHub | Correo Institucional | Rol en el Equipo | Paquetes y Ramas Asignadas |
| :-: | :--- | :--- | :--- | :--- | :--- |
| **1** | **Merlín Yelenis Landero Cuello** | `mlanderoc` | `mlandero@unicesar.edu.co` | **Líder Técnica & Arquitectura / Excepciones** | • Configuración base Maven (`pom.xml`)<br>• Paquete `com.ecopoint.exceptions` (`feature/excepciones`)<br>• Documentación técnica y diagramas (`docs/documentacion`) |
| **2** | **Estefanía Márquez Martínez** | `EstefaniaMarquez` | `estefaniamarquez@unicesar.edu.co` | **Desarrolladora Backend — Modelo & Servicios** | • Paquete `com.ecopoint.model` (`feature/modelo`)<br>• Servicios de dominio en `com.ecopoint.service` (`feature/servicio`) |
| **3** | **Carlos Raúl Gómez Galvis** | `craulgomez` | `craulgomez@unicesar.edu.co` | **Desarrollador Backend — Validaciones & Servicios** | • Paquete `com.ecopoint.validation` (`feature/validacion`)<br>• Servicios de alertas y cuarteo en `com.ecopoint.service` (`feature/servicio`) |

---

## Convención de Control de Versiones (Git Flow)

1. **Ramas Principales:**
   * `main`: Rama de versiones estables y entregas oficiales.
   * `develop`: Rama de integración donde convergen los Pull Requests de cada paquete.
2. **Ramas de Trabajo (1 rama por paquete):**
   * `feature/excepciones`: Jerarquía de excepciones personalizadas (`com.ecopoint.exceptions`) y configuración Maven.
   * `feature/modelo`: Entidades del dominio (`com.ecopoint.model`) con encapsulamiento y métodos Experto en Información.
   * `feature/validacion`: Validadores de datos de entrada y reglas de negocio (`com.ecopoint.validation`).
   * `feature/servicios`: Lógica de negocio, cálculo de cuarteo y generación automática de alertas (`com.ecopoint.service`).
   * `docs/documentacion`: Documentación general del repositorio (`README.md`, `TEAM.md`) y diagramas UML/arquitectura en `docs/`.
3. **Reglas de Estilo e Idioma:**
   * **Código fuente, nombres de clases/métodos, JavaDoc y diagramas técnicos:** en **Inglés**.
   * **Mensajes de error para el usuario final y control de versiones (ramas, commits, PRs):** en **Español**.
