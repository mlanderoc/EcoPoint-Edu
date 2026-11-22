# Spec 004 — Configuración del estudio y registro en campo

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Claude / Antigravity |
| Requerimientos | RF-01, RF-02, RF-03, RF-04, RF-05, RF-11 |
| Depende de | Spec 003 aprobada; decisión D4 |
| Fecha | 2026-11-11 |
| Ajustes | **D13** (13 nov 2026): observaciones de las pruebas de uso. **Aprobados** (13 nov 2026). **D14** (18 nov 2026): segunda ronda de observaciones. **Aprobados** (18 nov 2026). |

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
- **R-306** (Debe). Ninguna pantalla ni mensaje muestra códigos de reglas (R-, RN-, CA-). Esos códigos quedan solo en el código y las pruebas (D13).
- **R-307** (Debe). Toda pantalla interna (que no es del menú) tiene un botón **Atrás** que vuelve a la pantalla anterior (D13).
- **R-308** (Debe). Todo formulario tiene **Guardar** y **Descartar**. Si hay cambios sin guardar, Descartar o Atrás preguntan "¿Descartar los cambios?" antes de salir (D13).
- **R-309** (Debe). Todo lo que se puede tocar o hacer clic (botones, enlaces, casillas, listas desplegables, filas seleccionables) muestra el cursor de mano; lo deshabilitado, el cursor de prohibido (D13).

### 5.2 Inicio de sesión y menú

- **R-310** (Debe). Pantalla de inicio de sesión con correo y contraseña.
- **R-311** (Debe). Menú lateral (o inferior en el celular) con solo las opciones del rol (RN-081). Muestra el nombre del usuario, su rol y un botón para cerrar sesión. Para el docente, el orden es: **Configuración del estudio, Registro en campo, Verificación, Indicadores, Alertas y Mi Perfil**, y luego Administración (D14).
- **R-312** (Debe). Al vencer la sesión, se vuelve al inicio de sesión con el aviso "Su sesión expiró".

### 5.3 Administración (solo docente)

- **R-320** (Debe). **Instituciones**: listar, crear, editar (nombre, dirección), **activar y desactivar** (RN-006).
- **R-321** (Debe). **Fuentes de generación**: listar, crear, editar, **activar y desactivar**, con código, nombre, tipo y estado. La casilla "activa" del formulario se respeta al crear. El polígono se dibuja en la spec 007. El docente **elige la institución** cuyas fuentes administra; por defecto, la suya (D14).
- **R-322** (Debe). **Usuarios**: listar, crear y editar, con nombre, apellido, correo y rol (DOCENTE o ESTUDIANTE_UNIVERSITARIO). Se muestran en una **tabla** (no en tarjetas) con nombre, correo, rol, estado y total de registros; con filtros por **rol** y por **estado**, buscador por nombre o correo, y **paginación**. Cada fila de estudiante tiene **Activar** o **Desactivar**; las filas de docentes no tienen esa opción (RN-087).
- **R-323** (Debe). **Cuadrillas**: crear, nombrar, asignar docente responsable e integrantes. Avisa si la cuadrilla no tiene un docente responsable (RN-085).
- **R-324** (Debe). **Subcategorías de residuos**: listar por grupo; crear, editar, **activar y desactivar** (RN-043). La casilla "activa" del formulario se respeta al crear.
- **R-325** (Debe). En Instituciones, Fuentes y Subcategorías, la lista muestra por defecto **solo los activos**, con un filtro **Activos / Inactivos / Todos**, sin ícono de ojo. Los inactivos se ven atenuados y con la etiqueta "Inactivo" (D13, D14).

### 5.4 Configuración del estudio (solo docente)

- **R-330** (Debe). Crear un estudio: institución (se **elige de una lista con las instituciones activas**; por defecto, la del docente; al cambiarla se cargan las fuentes de esa institución, D14), fecha del día 1 y fechas de los días 2 a 8. El sistema propone los siguientes 7 días hábiles y el docente puede cambiarlos. **Día hábil** = de lunes a viernes y que no sea festivo en Colombia (incluidos los festivos que se trasladan al lunes y los de Semana Santa). Al cambiar **cualquier** día, todos los días posteriores se recalculan como los días hábiles siguientes, aunque se hubieran cambiado a mano (D13).
- **R-331** (Debe). Elegir qué fuentes participan (por defecto, todas las activas) y **la cuadrilla del estudio**, obligatoria. En el detalle de un estudio PLANIFICADO se puede cambiar la cuadrilla, y en uno EN_EJECUCION sin cuadrilla se puede asignar. Siempre se muestra el nombre real de la cuadrilla, aunque sea de otra institución (RN-088, D14).
- **R-332** (Debe). Ver el estado del estudio y los botones para avanzarlo (RN-014), con una confirmación antes de cada cambio. Si un cambio no es posible, se explica por qué (ej. "Quedan 4 registros pendientes de verificación").
- **R-333** (Debe). El día 1 se muestra con la etiqueta "Día de descarte: no entra en los cálculos".
- **R-334** (Debe). En el detalle de un estudio PLANIFICADO se pueden **editar las fechas** con la misma regla de R-330 (D13).
- **R-335** (Debe). En el detalle de un estudio PLANIFICADO hay un botón **Eliminar estudio**, con confirmación (RN-019) (D13).
- **R-336** (Debe). En el detalle de un estudio EN_EJECUCION o EN_DEPURACION hay un botón **Cancelar estudio**, que pide el motivo y advierte que es definitivo (RN-018). La lista de estudios muestra los cancelados con su etiqueta; no aparecen en Registro en campo ni en Indicadores (D13).
- **R-337** (Debe). La lista de estudios tiene un **filtro por institución** con todas las instituciones registradas; por defecto, la del usuario. La elección queda en la dirección (`?institucion=`) para que sobreviva al volver atrás. Cada estudio muestra el nombre de su institución (D14).

### 5.5 Registro en campo

- **R-340** (Debe). **Pantalla de inicio de campo**: arriba, una lista para **elegir el estudio en ejecución** (el docente ve todos; el estudiante, solo los de su cuadrilla, RN-089). Si hay uno solo, se elige solo, y la elección queda en la dirección (`?estudio=`) (D14). Muestra el estudio elegido, el día y el turno actual (según la fecha y la hora), y un botón grande "Abrir turno" o "Continuar turno".
- **R-341** (Debe). **Abrir turno** (docente): confirma día y turno. La cuadrilla es la del estudio y solo se muestra (RN-088, D14).
- **R-342** (Debe). **Población**: tres campos (estudiantes, docentes, personal) y el total calculado. Hasta registrarla, la opción de bolsas aparece bloqueada con el mensaje "Registre primero la población del turno".
- **R-343** (Debe). **Registrar bolsa**: elegir la fuente (lista con código y nombre, botones grandes), digitar peso bruto, tara y volumen. Antes de guardar, se muestran el peso neto y la densidad calculados. Al guardar, se muestra **el rótulo en letra grande** (ej. `AU-D3-M`) para escribirlo en la bolsa.
- **R-344** (Debe). Si el valor es atípico (RN-037), se muestra un aviso amarillo "Valor fuera de lo esperado; revise antes de continuar", y se puede guardar igual.
- **R-345** (Debe). **Lista del turno**: cada fuente participante con sus bolsas, su peso neto total y su estado: con bolsas, "sin generación" o pendiente. Cada fuente pendiente tiene un botón "Marcar sin generación".
- **R-346** (Debe). **Cerrar turno** (docente): solo se habilita cuando ninguna fuente está pendiente (RN-024). Muestra un resumen (población, número de bolsas, kg netos) antes de confirmar.
- **R-347** (Debería). Adjuntar una foto a la bolsa desde la cámara del celular (RN-090).
- **R-348** (Debería). Corregir una bolsa del turno abierto mientras siga PENDIENTE de verificación. Después de verificada, se corrige solo desde la pantalla de verificación (spec 005).
- **R-349** (Podría). Lista de verificación de EPP al abrir el turno (RN-086).

### 5.6 Mi Perfil (todos los roles)

- **R-350** (Debe). Muestra nombre, apellido, correo, rol, institución y cuadrillas del usuario (D13).
- **R-351** (Debe). Muestra **sus propios** totales de registros: poblaciones, bolsas, cuarteos y densidades digitados, verificaciones hechas y correcciones que recibieron sus datos. Un estudiante no ve los totales de otros; el docente los ve en la tabla de Usuarios (R-322) (D13).
- **R-352** (Debe). Permite **cambiar la contraseña**, pidiendo la actual, la nueva y su confirmación (D13).
- **R-353** (Debe). La ruta `/` es una página pública de bienvenida con acceso a Iniciar sesión; quien ya tiene sesión pasa a Inicio (D15).
- **R-354** (Debe). El inicio de sesión muestra la foto de la UPC bajo un velo verde y a Eco saludando, sin frases ni cambios de expresión (D15).
- **R-355** (Debe). El menú, el encabezado de cada módulo y la barra del celular siguen el diseño aprobado. En el celular, "Más" reúne Alertas, Mi perfil y, para el docente, Administración (D15).
- **R-356** (Debe). Inicio muestra el estudio en ejecución como un recorrido de 16 turnos, las fuentes del turno elegido, la composición y lo que está por atender (D15).
- **R-357** (Debe). El turno muestra sus cuatro pasos. Al elegir una fuente en Registrar bolsa, la pantalla pasa sola al formulario de pesaje (D15).
- **R-358** (Debe). En la lista de estudios, un estudio cerrado no muestra el botón Abrir (D15).

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
- **CA-311.** Dado un estudio nuevo con día 1 el viernes 13 de noviembre de 2026, cuando se elige el día 1, entonces se proponen del 17 al 20 y del 23 al 25 de noviembre: se saltan el fin de semana y el lunes 16, festivo. Si luego se cambia el día 4 al lunes 23, los días 5 a 8 pasan a ser del 24 al 27 (R-330).
- **CA-312.** Dada una fuente creada con la casilla "activa" desmarcada, cuando se guarda, entonces no aparece en la lista por defecto, aparece con el filtro "Inactivos" y se puede activar desde ahí (R-321, R-325).
- **CA-313.** Dados 50 estudiantes y 2 docentes, cuando el docente abre Usuarios, entonces ve una tabla paginada; al filtrar por rol Estudiante y estado Inactivo, solo ve los estudiantes inactivos; las filas de docentes no tienen el botón Desactivar (R-322).
- **CA-314.** Dado un estudiante que registró 3 bolsas, cuando abre Mi Perfil, entonces ve sus datos y "3 bolsas", y no ve totales de otros usuarios (R-350, R-351).
- **CA-315.** Dado un estudio EN_EJECUCION, cuando el docente pulsa Cancelar estudio sin escribir motivo, entonces no se puede confirmar; con motivo, el estudio queda Cancelado y desaparece de Registro en campo (R-336).
- **CA-316.** Dado un formulario con datos escritos sin guardar, cuando se pulsa Atrás o Descartar, entonces se pregunta "¿Descartar los cambios?" y, si se responde que no, los datos siguen ahí (R-307, R-308).
- **CA-317.** Dada cualquier pantalla, cuando se revisa su texto, entonces no contiene códigos como "R-320" o "RN-085" (R-306).
- **CA-318.** Dado un DOCENTE, cuando inicia sesión, entonces el menú muestra primero Configuración del estudio y después Registro en campo; en el celular, Estudios va antes que Campo (R-311).
- **CA-319.** Dadas dos instituciones activas, cuando el docente crea un estudio y elige la segunda, entonces se listan las fuentes de la segunda, el estudio queda en ella y aparece en la lista al filtrar por esa institución, no al filtrar por la primera (R-330, R-337).
- **CA-321.** Dados dos estudios en ejecución, uno de la cuadrilla del estudiante y otro no, cuando el estudiante entra a Registro en campo, entonces solo ve el de su cuadrilla; el docente ve los dos y puede elegir cualquiera (R-340).
- **CA-322.** Dado un estudio nuevo, cuando se intenta crear sin cuadrilla, entonces no se guarda y se pide elegirla; al abrir un turno, se muestra la cuadrilla del estudio sin poder cambiarla (R-331, R-341).
- **CA-323.** Dado Nuevo estudio, cuando solo se cambió la institución o la cuadrilla y se pulsa Atrás, entonces se avisa que hay cambios sin guardar (R-308).
- **CA-324.** Dada la ruta `/` sin sesión, entonces se ve la bienvenida y su enlace lleva al inicio de sesión (R-353).
- **CA-325.** Dado un estudio en ejecución, cuando se entra a Inicio, entonces se ven sus 16 turnos y lo que está por atender (R-356).
- **CA-326.** Dada la lista de estudios, entonces un estudio cerrado no tiene botón Abrir y uno en ejecución sí (R-358).
- **CA-327.** Dado un estudiante en el menú Más, entonces ve Alertas y no ve Administración (R-355).
- **CA-320.** Dado el filtro de Instituciones, Fuentes o Subcategorías, cuando se mira, entonces solo tiene los botones Activos, Inactivos y Todos, sin ícono de ojo (R-325).

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
