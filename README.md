# EcoPoint Edu

Sistema de información para el registro, caracterización y análisis de residuos sólidos en puntos ecológicos de instituciones educativas (colegios, escuelas y universidades), alineado con la **Resolución 2184 de 2019** (código de colores **Blanco**, **Verde** y **Negro**) y el **Método del Cuarteo**.

---

## 1. Alcance de la Entrega Actual (Arquitectura Base Backend)

Esta fase implementa el núcleo de dominio y lógica de negocio del sistema en **Java 17**, estructurado en cuatro paquetes desacoplados bajo principios **GRASP** y **SOLID** (sin capa de interfaz gráfica ni controladores de API REST por el momento):

* **`com.ecopoint.exceptions`**: Jerarquía de excepciones personalizadas no verificadas (`RuntimeException`) encabezada por `EcoPointException` y sus subclases `InvalidDataException`, `BusinessRuleException`, `ResourceNotFoundException` y `PersistenceException`.
* **`com.ecopoint.model`**: Las 13 clases del modelo de dominio (`Institution`, `Zone`, `EcologicalPoint`, `Container`, `User`, `InspectionDay`, `InspectionRecord`, `ContainerDetail`, `Alert`, `Evidence`, `WasteCategory`, `Quartering`, `QuarteringResult`) con encapsulamiento estricto y métodos **Experto en Información**.
* **`com.ecopoint.validation`**: Validadores dedicados sin estado (`UserValidator`, `EcologicalPointValidator`, `InspectionValidator`, `QuarteringValidator`) que verifican formato de datos y reglas de negocio antes de procesar cualquier operación.
* **`com.ecopoint.service`**: Servicios de negocio (`UserService`, `EcologicalPointService`, `InspectionService`, `AlertService`, `QuarteringService`) encargados de coordinar el registro de entidades, disparar alertas automáticas por desbordamiento o mala separación, y calcular los porcentajes de composición del método del cuarteo.

---

## 2. Reglas de Validación Implementadas

1. **Validación de Usuarios (`UserValidator`):**
   * El nombre (`firstName`), apellido (`lastName`) y rol institucional (`role`) son obligatorios y **no pueden contener números ni caracteres especiales** (solo letras y espacios).
   * El correo electrónico (`email`) debe contener obligatoriamente el símbolo `@`, usuario antes del `@` y un dominio válido sin espacios.
   * **Unicidad de correo:** No se permite registrar dos usuarios con el mismo correo electrónico (`BusinessRuleException`).
   * El atributo `role` representa únicamente el rol descriptivo de la persona dentro de la institución educativa (ej. *Estudiante*, *Docente*, *Inspector Ambiental*, *Coordinador*).
2. **Validación de Puntos Ecológicos y Zonas (`EcologicalPointValidator`):**
   * Prohibición de nombres de instituciones, zonas o ubicaciones vacíos o compuestos únicamente por números.
   * Unicidad del nombre de la zona dentro de una misma institución.
   * Georreferenciación relativa (`coordX`, `coordY`) validada dentro del rango porcentual `[0.00, 100.00]`.
   * Cumplimiento estricto de la **Resolución 2184 de 2019**: todo punto ecológico debe tener exactamente **3 recipientes** con los colores `BLANCO`, `VERDE` y `NEGRO`, sin colores repetidos y con capacidad mayor a cero.
3. **Validación de Inspecciones y Alertas (`InspectionValidator` & `AlertService`):**
   * Las fechas de jornada y captura no pueden ser nulas ni fechas futuras.
   * Toda inspección evalúa los 3 recipientes del punto ecológico con niveles de llenado válidos (`VACIO`, `BAJO`, `MEDIO`, `ALTO`, `DESBORDADO`) y estados físicos válidos (`BUENO`, `REGULAR`, `MALO`, `AUSENTE`).
   * Las evidencias fotográficas deben tener extensión de imagen válida (`.jpg`, `.jpeg`, `.png`, `.webp`).
   * Generación automática de alertas cuando un recipiente se reporta `DESBORDADO` o con `improperSeparation = true`.
4. **Validación del Método del Cuarteo (`QuarteringValidator` & `QuarteringService`):**
   * Pesos iniciales y de muestra estrictamente mayores a `0.00 kg`.
   * Coherencia física: el peso de la muestra no puede superar el peso inicial recolectado (`sampleWeight <= initialWeight`), y la suma de los pesos clasificados no puede exceder el peso de la muestra ($\sum \text{weight} \le \text{sampleWeight}$).
   * Unicidad de cuarteo por registro de inspección (`1 a 0..1`).

---

## 3. Documentación Técnica y Diagramas (en Inglés)

* [Diagrama de Clases UML y Trazabilidad GRASP/SOLID (`docs/class-diagram.md`)](docs/class-diagram.md)
* [Diagrama de Capas y Dependencias (`docs/layer-diagram.md`)](docs/layer-diagram.md)
* [Diagrama de Jerarquía de Excepciones (`docs/exception-hierarchy-diagram.md`)](docs/exception-hierarchy-diagram.md)
* [Equipo de Desarrollo y Roles (`TEAM.md`)](TEAM.md)

---

## 4. Requisitos y Compilación

* **Java JDK:** 17 o superior
* **Apache Maven:** 3.8+

Para compilar el proyecto desde la raíz del repositorio:

```bash
mvn clean compile
```
