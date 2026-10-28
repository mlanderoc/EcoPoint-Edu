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
| **1** | **Merlín Yelenis Landero Cuello** | `mlanderoc` | `mlandero@unicesar.edu.co` | **Líder Técnica & Arquitectura / Persistencia** | • Configuración base Maven (`pom.xml`)<br>• Paquete `com.ecopoint.exceptions` (`feature/excepciones`)<br>• Contratos y repositorios en archivos planos en `com.ecopoint.persistence` (`feature/persistencia`)<br>• Documentación técnica y diagramas (`docs`) |
| **2** | **Estefanía Márquez Martínez** | `EstefaniaMarquez` | `estefaniamarquez@unicesar.edu.co` | **Desarrolladora Backend — Modelo, Persistencia & Servicios** | • Paquete `com.ecopoint.model` (`feature/modelo`)<br>• Repositorios de inspección, cuarteo y alertas en `com.ecopoint.persistence` (`feature/persistencia`)<br>• Servicios de dominio en `com.ecopoint.service` (`feature/servicios`) |
| **3** | **Carlos Raúl Gómez Galvis** | `craulgomez` | `craulgomez@unicesar.edu.co` | **Desarrollador Backend & UI — Validaciones, Servicios & Interfaz Swing** | • Paquete `com.ecopoint.validation` (`feature/validacion`)<br>• Servicios de alertas y estadísticas de cuarteo en `com.ecopoint.service` (`feature/servicios`)<br>• Interfaz gráfica de usuario en `com.ecopoint.ui` y `Main` (`feature/ui`) |

---

## Convención de Control de Versiones (Git Flow)

1. **Ramas Principales:**
   * `main`: Rama de versiones estables y entregas oficiales.
   * `develop`: Rama de integración donde convergen los Pull Requests de cada paquete.
2. **Ramas de Trabajo (1 rama por paquete):**
   * `feature/excepciones`: Jerarquía de excepciones personalizadas (`com.ecopoint.exceptions`).
   * `feature/modelo`: Entidades del dominio (`com.ecopoint.model`) con encapsulamiento y métodos Experto en Información.
   * `feature/validacion`: Validadores de datos de entrada y reglas de negocio (`com.ecopoint.validation`).
   * `feature/persistencia`: Módulo de persistencia en archivos planos aplicando Variaciones Protegidas (`com.ecopoint.persistence`).
   * `feature/servicios`: Lógica de negocio, CRUD completo, cálculo de cuarteo, alertas automáticas y estadísticas (`com.ecopoint.service`).
   * `feature/ui`: Interfaz gráfica en Java Swing (`com.ecopoint.ui` y `com.ecopoint.Main`).
   * `docs`: Documentación general del repositorio (`README.md`, `src/TEAM.md`) y diagramas UML/arquitectura en `docs/`.
3. **Reglas de Estilo e Idioma:**
   * **Código fuente, nombres de clases/métodos, JavaDoc (texto simple con `@author <usuario_github>`) y diagramas técnicos:** en **Inglés**.
   * **Mensajes de error/interfaz para el usuario final y control de versiones (ramas, commits, PRs):** en **Español**.
