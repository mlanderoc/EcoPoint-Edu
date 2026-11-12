# Spec 004 — Configuración del estudio y registro en campo

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Claude / Antigravity |
| Requerimientos | RF-01, RF-02, RF-03, RF-04, RF-05, RF-11 |
| Depende de | Spec 003 aprobada; decisión D4 |
| Fecha | 2026-11-11 |

## 1. Objetivo

Construir las primeras pantallas web: **inicio de sesión, menú, administración** (instituciones, fuentes, usuarios, cuadrillas), **configuración del estudio** y **registro en campo** (abrir turno, población, bolsas, cerrar turno).

El registro en campo se usa **de pie, en el celular, con guantes y al sol**. Por eso debe ser rápido, con botones grandes y pocos pasos.

## 2. Requerimientos que cubre

| RF | Cómo se cubre |
| --- | --- |
| RF-01 | Pantalla de configuración del estudio |
| RF-02 | Abrir y cerrar turnos de mañana y tarde |
| RF-03 | Registro de población al iniciar el turno |
| RF-04 | Administración de fuentes (sin el plano, que es la spec 007) |
| RF-05 | Registro de bolsas con rótulo automático |
| RF-11 | Administración de usuarios y cuadrillas; menú según el rol |

Reglas aplicadas: RN-001 a RN-037, RN-080 a RN-085 y RN-090 (spec 001).

## 3. Actores

- **Docente**: administra, configura el estudio, abre y cierra turnos y supervisa la cuadrilla.
- **Estudiantes universitarios**: registran población y bolsas.

## 4. Historias de usuario

- **HU-030.** Como docente, quiero crear un estudio eligiendo las 8 fechas, las fuentes participantes y las cuadrillas, para dejarlo listo antes del muestreo.
- **HU-031.** Como docente, quiero registrar a los estudiantes universitarios de la cuadrilla, para que puedan entrar al sistema.
- **HU-032.** Como docente, quiero abrir el turno y registrar cuántas personas hay, en menos de un minuto.
- **HU-033.** Como estudiante, quiero registrar una bolsa eligiendo la fuente y digitando bruto, tara y volumen, y ver de inmediato el rótulo que debo escribir en la bolsa.
- **HU-034.** Como docente, quiero ver qué fuentes faltan por registrar antes de cerrar el turno, para no dejar datos sin tomar.
- **HU-035.** Como cualquier usuario, quiero ver solo las opciones que mi rol permite, para no perderme ni equivocarme.

## 5. Pantallas y requisitos

### 5.1 Generales

- **R-300** (Debe). La interfaz funciona en celular (desde 360 px de ancho), tableta y computador.
- **R-301** (Debe). En el celular, los botones y campos de las pantallas de campo miden al menos 48 px de alto, y el texto tiene buen contraste para leerse al sol.
- **R-302** (Debe). Los campos numéricos abren el teclado numérico del celular y aceptan coma o punto decimal.
- **R-303** (Debe). Los errores de la API (formato de la spec 003) se muestran junto al campo afectado, o en un aviso si no son de un campo. Siempre en español.
- **R-304** (Debe). Se usa la paleta de colores del logo de EcoPoint Edu. Los grupos de residuos se muestran siempre con su color (Blanco, Verde, Negro, gris para Sin color) **y** con su nombre escrito, para que el color no sea la única pista.
- **R-305** (Debe). Mientras se espera una respuesta, el botón queda deshabilitado para evitar envíos dobles.

### 5.2 Inicio de sesión y menú

- **R-310** (Debe). Pantalla de inicio de sesión con correo y contraseña.
- **R-311** (Debe). Menú lateral (o inferior en el celular) con solo las opciones del rol (RN-081). Muestra el nombre del usuario, su rol y un botón para cerrar sesión.
- **R-312** (Debe). Al vencer la sesión, se vuelve al inicio de sesión con el aviso "Su sesión expiró".

### 5.3 Administración (solo docente)

- **R-320** (Debe). **Instituciones**: listar, crear y editar (nombre, dirección).
- **R-321** (Debe). **Fuentes de generación**: listar, crear, editar y desactivar, con código, nombre, tipo y estado. El polígono se dibuja en la spec 007.
- **R-322** (Debe). **Usuarios**: listar, crear, editar y desactivar, con nombre, apellido, correo y rol (DOCENTE o ESTUDIANTE_UNIVERSITARIO).
- **R-323** (Debe). **Cuadrillas**: crear, nombrar, asignar docente responsable e integrantes. Avisa si la cuadrilla no tiene un docente responsable (RN-085).
- **R-324** (Debe). **Subcategorías de residuos**: listar por grupo; crear y editar.

### 5.4 Configuración del estudio (solo docente)

- **R-330** (Debe). Crear un estudio: institución, fecha del día 1 y fechas de los días 2 a 8. El sistema propone los siguientes 7 días hábiles (lunes a viernes) y el docente puede cambiarlos.
- **R-331** (Debe). Elegir qué fuentes participan (por defecto, todas las activas) y asignar cuadrillas a cada turno.
- **R-332** (Debe). Ver el estado del estudio y los botones para avanzarlo (RN-014), con una confirmación antes de cada cambio. Si un cambio no es posible, se explica por qué (ej. "Quedan 4 registros pendientes de verificación").
- **R-333** (Debe). El día 1 se muestra con la etiqueta "Día de descarte: no entra en los cálculos".

### 5.5 Registro en campo

- **R-340** (Debe). **Pantalla de inicio de campo**: muestra el estudio en ejecución, el día y el turno actual (según la fecha y la hora), y un botón grande "Abrir turno" o "Continuar turno".
- **R-341** (Debe). **Abrir turno** (docente): confirma día, turno y cuadrilla.
- **R-342** (Debe). **Población**: tres campos (estudiantes, docentes, personal) y el total calculado. Hasta registrarla, la opción de bolsas aparece bloqueada con el mensaje "Registre primero la población del turno".
- **R-343** (Debe). **Registrar bolsa**: elegir la fuente (lista con código y nombre, botones grandes), digitar peso bruto, tara y volumen. Antes de guardar, se muestran el peso neto y la densidad calculados. Al guardar, se muestra **el rótulo en letra grande** (ej. `AU-D3-M`) para escribirlo en la bolsa.
- **R-344** (Debe). Si el valor es atípico (RN-037), se muestra un aviso amarillo "Valor fuera de lo esperado; revise antes de continuar", y se puede guardar igual.
- **R-345** (Debe). **Lista del turno**: cada fuente participante con sus bolsas, su peso neto total y su estado: con bolsas, "sin generación" o pendiente. Cada fuente pendiente tiene un botón "Marcar sin generación".
- **R-346** (Debe). **Cerrar turno** (docente): solo se habilita cuando ninguna fuente está pendiente (RN-024). Muestra un resumen (población, número de bolsas, kg netos) antes de confirmar.
- **R-347** (Debería). Adjuntar una foto a la bolsa desde la cámara del celular (RN-090).
- **R-348** (Debería). Corregir una bolsa del turno abierto mientras siga PENDIENTE de verificación. Después de verificada, se corrige solo desde la pantalla de verificación (spec 005).
- **R-349** (Podría). Lista de verificación de EPP al abrir el turno (RN-086).

## 6. Criterios de aceptación

- **CA-300.** Dado un ESTUDIANTE_UNIVERSITARIO, cuando inicia sesión, entonces el menú no muestra Administración ni Configuración del estudio, y en el registro en campo no ve los botones de abrir ni cerrar turno.
- **CA-301.** Dado un docente, cuando crea un estudio con día 1 el lunes 2 de marzo de 2026, entonces el sistema propone del 3 al 6 de marzo y del 9 al 11 de marzo para los días 2 a 8.
- **CA-302.** Dado un usuario con un correo que ya existe, cuando se intenta guardar, entonces el formulario no lo permite y lo señala en el campo correo.
- **CA-303.** Dado un turno abierto sin población, cuando un estudiante entra a registrar bolsas, entonces ve la opción bloqueada con el mensaje de R-342.
- **CA-304.** Dado un turno del día 3 en la mañana, cuando un estudiante registra una bolsa de la fuente `AU` con bruto 12,40, tara 0,40 y volumen 0,080, entonces ve neto 12,00 kg y densidad 150,00 kg/m³ antes de guardar, y el rótulo `AU-D3-M` en grande después de guardar.
- **CA-305.** Dado un campo de tara mayor que el bruto, cuando se intenta guardar, entonces aparece el mensaje en rojo debajo del campo tara y no se guarda.
- **CA-306.** Dado un turno con la fuente `BA` sin bolsas, cuando el docente intenta cerrar, entonces el botón está deshabilitado y `BA` aparece como pendiente; al marcarla "sin generación", el botón se habilita.
- **CA-307.** Dado un celular de 360 px de ancho, cuando se usa todo el flujo (abrir turno, población, 3 bolsas, cerrar), entonces no hay desplazamiento horizontal y todos los botones se pueden tocar sin hacer zoom.
- **CA-308.** Dado el flujo de CA-307, cuando lo hace un usuario entrenado, entonces registrar una bolsa toma menos de 30 segundos.
- **CA-309.** Dado un estudio con registros pendientes, cuando el docente intenta cerrarlo, entonces ve el motivo exacto y el estado no cambia.
- **CA-310.** Dado un doble clic rápido en "Guardar bolsa", cuando se procesa, entonces se crea una sola bolsa.

## 7. Fuera de alcance

- Cuarteo, densidad, verificación y depuración: spec 005.
- Tablero e indicadores: spec 006.
- Dibujo de los polígonos de las fuentes en el plano: spec 007.
- Funcionamiento sin conexión (spec 003, P3).

## 8. Preguntas abiertas

| # | Pregunta | Propuesta |
| --- | --- | --- |
| P1 | ¿El horario de cada turno es fijo para definir el "turno actual" (ej. mañana 6:00–12:30, tarde 12:30–18:30)? | Sí, configurable por institución con esos valores por defecto. |
| P2 | ¿Las cuadrillas se asignan por turno o fijas para todo el estudio? | Por turno, con la opción de copiar la asignación al resto de días. |
| P3 | ¿Hay que imprimir el rótulo (etiqueta) o basta mostrarlo en pantalla? | Mostrarlo; la bolsa se rotula a mano con marcador. |
| P4 | ¿Biblioteca de componentes visuales para React? | Se decide en `plan.md`; propuesta: una ligera y accesible (ej. Mantine o Chakra UI). |
