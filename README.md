# EcoPoint Edu

Sistema de información para el registro, caracterización y análisis de residuos sólidos en puntos ecológicos de instituciones educativas (colegios, escuelas y universidades), alineado con la **Resolución 2184 de 2019** (código de colores **Blanco**, **Verde** y **Negro**) y el **Método del Cuarteo**.

---

## 1. Arquitectura del Sistema

El proyecto está implementado en **Java 17+** bajo una arquitectura desacoplada por capas que aplica principios **GRASP** y **SOLID**:

* **`com.ecopoint.exceptions`**: Jerarquía de excepciones personalizadas no verificadas (`RuntimeException`) encabezada por `EcoPointException` y sus subclases `InvalidDataException`, `BusinessRuleException`, `ResourceNotFoundException` y `PersistenceException`.
* **`com.ecopoint.model`**: Las 13 clases del modelo de dominio (`Institution`, `Zone`, `EcologicalPoint`, `Container`, `User`, `InspectionDay`, `InspectionRecord`, `ContainerDetail`, `Alert`, `Evidence`, `WasteCategory`, `Quartering`, `QuarteringResult`) con encapsulamiento estricto y métodos **Experto en Información**.
* **`com.ecopoint.validation`**: Validadores dedicados sin estado (`UserValidator`, `EcologicalPointValidator`, `InspectionValidator`, `QuarteringValidator`) que verifican formato de datos y reglas de negocio antes de procesar cualquier operación.
* **`com.ecopoint.persistence` (Variaciones Protegidas & DIP)**: Define interfaces de repositorio (`UserRepository`, `InstitutionRepository`, `ZoneRepository`, `EcologicalPointRepository`, `InspectionRepository`, `QuarteringRepository`, `AlertRepository`) e implementaciones actuales sobre **archivos planos UTF-8** (`data/*.txt`). Gracias al principio de **Variaciones Protegidas**, el sistema puede migrar a una base de datos relacional sin modificar los servicios ni la interfaz gráfica.
* **`com.ecopoint.service`**: Servicios de negocio (`UserService`, `EcologicalPointService`, `InspectionService`, `AlertService`, `QuarteringService`) con soporte de inicio de sesión, CRUD completo, disparo automático de alertas y cálculo de estadísticas generales.
* **`com.ecopoint.ui` y `com.ecopoint.Main`**: Interfaz gráfica de escritorio en **Java Swing** con la paleta de colores oficial del logo de **EcoPoint Edu**, inicio de sesión, administración CRUD de usuarios, zonas y puntos con mapa interactivo en porcentaje (`%`), registro de inspecciones, calculadora del Método del Cuarteo y tablero de estadísticas/alertas.

---

## 2. Documentación Técnica y Diagramas (en Inglés)

* [Diagrama de Clases UML y Trazabilidad GRASP/SOLID (`docs/class-diagrama.md`)](docs/class-diagrama.md)
* [Diagrama de Capas y Dependencias (`docs/layer-diagram.md`)](docs/layer-diagram.md)
* [Diagrama de Jerarquía de Excepciones (`docs/exception-hierarchy-diagram.md`)](docs/exception-hierarchy-diagram.md)
* [Equipo de Desarrollo y Roles (`src/TEAM.md`)](src/TEAM.md)

---

## 3. Compilación y Ejecución de la Demostración Gráfica

Para compilar y ejecutar la aplicación desde la raíz del repositorio:

```bash
mvn clean compile exec:java
```

O ejecutando directamente la clase principal [`com.ecopoint.Main`](src/main/java/com/ecopoint/Main.java) desde tu IDE (NetBeans, IntelliJ IDEA, Eclipse o VS Code). Al iniciar por primera vez, el sistema crea automáticamente la carpeta `data/` con archivos planos precargados para la demostración.
