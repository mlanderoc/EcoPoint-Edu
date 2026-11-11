# Spec 001 — Dominio de caracterización

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Claude / Antigravity |
| Requerimientos | RF-01 a RF-12 (reglas del dominio) |
| Depende de | Decisiones D1, D6, D8, D9 |
| Fecha | 2026-11-11 |

## 1. Objetivo

Definir los conceptos del negocio (estudio, turnos, fuentes, bolsas, cuarteo, verificación, usuarios) y **todas las reglas que el sistema debe hacer cumplir** sobre ellos, sin depender de pantallas, base de datos ni API.

Esta spec es la base de las demás: las specs 002 a 007 citan sus reglas (RN-xxx) en lugar de repetirlas. Al terminarla, el dominio debe poder probarse completo con pruebas unitarias, sin base de datos.

## 2. Conceptos del dominio

| Concepto | Qué representa | Datos principales |
| --- | --- | --- |
| Institución | Colegio, escuela o universidad que se caracteriza | Nombre, dirección, centro y zoom del mapa |
| Fuente de generación | Lugar donde se generan residuos | Código, nombre, tipo, polígono en el plano, activa/inactiva |
| Estudio de caracterización | Campaña de 8 días en una institución | Institución, fecha de inicio, estado, días |
| Día de muestreo | Día 1 a 8 del estudio | Número, fecha, excluido sí/no |
| Turno | Mañana o tarde de un día | Día, tipo (M/T), estado, cuadrilla |
| Población del turno | Personas presentes al inicio del turno | Estudiantes, docentes, personal administrativo y de servicios |
| Bolsa | Residuos de una fuente al cierre de un turno | Rótulo, fuente, turno, peso bruto, tara, volumen |
| Cuarteo | Submuestra del turno y su clasificación | Peso total del turno, peso de la submuestra, resultados por subcategoría |
| Resultado de cuarteo | Peso de una subcategoría en la submuestra | Subcategoría, peso |
| Medición de densidad | Peso volumétrico de los residuos del turno | Volumen del recipiente, peso neto |
| Grupo de residuo | Clasificación de la Res. 2184 de 2019 | BLANCO, VERDE, NEGRO, SIN_COLOR |
| Subcategoría de residuo | Tipo específico dentro de un grupo | Nombre, grupo, descripción |
| Verificación | Cotejo de un registro contra la planilla en papel | Estado, digitador, verificador, fecha |
| Corrección | Cambio de un dato ya registrado | Valor anterior, valor nuevo, motivo, quién, cuándo |
| Usuario | Persona que usa el sistema | Nombre, correo, rol |
| Cuadrilla | Grupo mixto que trabaja un turno | Nombre, integrantes, docente responsable |
| Alerta | Aviso automático de un problema de calidad | Tipo, origen, descripción, atendida |
| Evidencia | Foto o archivo de soporte | Archivo, descripción, a qué registro pertenece |

## 3. Reglas de negocio

### 3.1 Institución y fuentes de generación

- **RN-001** (Debe). Toda fuente pertenece a una institución.
- **RN-002** (Debe). El código de la fuente tiene de 2 a 6 caracteres, solo letras mayúsculas y dígitos (ej. `AU`, `AU2`, `PAE`, `LAB`). Es único dentro de la institución.
- **RN-003** (Debe). El tipo de fuente es uno de: AULAS, OFICINAS, TIENDA_ESCOLAR, PAE, ZONAS_VERDES, BANOS, LABORATORIO, OTRA.
- **RN-004** (Debe). Si una fuente tiene polígono, debe ser cerrado (primer vértice = último), tener al menos 3 vértices distintos, y cada vértice debe tener latitud entre −90 y 90 y longitud entre −180 y 180.
- **RN-005** (Debe). Una fuente con registros (bolsas) no se borra; solo se desactiva. Una fuente inactiva no aparece para nuevos registros.

### 3.2 Estudio de caracterización

- **RN-010** (Debe). Un estudio pertenece a una institución y tiene exactamente **8 días de muestreo**, numerados del 1 al 8.
- **RN-011** (Debe). Cada día tiene una fecha. Las fechas van en orden estrictamente creciente (el día 2 es posterior al día 1, etc.). No se exige que sean días calendario seguidos, porque fines de semana y festivos no tienen clases (ver pregunta P1).
- **RN-012** (Debe). El **día 1 queda marcado como excluido**: se registra igual que los demás, pero no entra en los cálculos de la línea base. Los cálculos usan los días 2 a 8 (n = 7).
- **RN-013** (Debe). Una institución no puede tener dos estudios con fechas que se solapen.
- **RN-014** (Debe). El estudio tiene estos estados, y solo avanza en este orden:

  | Estado | Qué se puede hacer |
  | --- | --- |
  | PLANIFICADO | Editar fechas, fuentes participantes y cuadrillas. No se registran datos de campo. |
  | EN_EJECUCION | Registrar población, bolsas, cuarteos y densidades. |
  | EN_DEPURACION | No se registran datos nuevos; solo verificar y corregir. |
  | CERRADO | Solo lectura. La línea base es oficial. |

- **RN-015** (Debe). Para pasar a EN_EJECUCION, el estudio necesita al menos una fuente activa y las 8 fechas definidas.
- **RN-016** (Debe). Para pasar a CERRADO, no puede quedar ningún registro en estado PENDIENTE de verificación ni ningún cuarteo NO_CONFORME sin resolver (ver RN-052 y RN-062).
- **RN-017** (Debería). Un estudio CERRADO puede reabrirse a EN_DEPURACION solo por un DOCENTE, indicando el motivo. Queda registrado.

### 3.3 Turnos y población

- **RN-020** (Debe). Cada día de muestreo tiene exactamente dos turnos: MANANA (M) y TARDE (T). No puede haber dos turnos iguales en el mismo día.
- **RN-021** (Debe). Un turno está ABIERTO o CERRADO. Solo en un turno ABIERTO se registran población, bolsas, cuarteo y densidad.
- **RN-022** (Debe). La población se registra **al inicio** del turno: estudiantes, docentes y personal administrativo/de servicios, cada uno un entero ≥ 0. El total debe ser mayor que 0.
- **RN-023** (Debe). **No se pueden registrar bolsas en un turno sin población registrada.**
- **RN-024** (Debe). Para cerrar un turno, cada fuente participante debe tener al menos una bolsa registrada **o** estar marcada como "sin generación en este turno". Así se distingue un cero real de un dato olvidado.
- **RN-025** (Debería). Un turno cerrado puede reabrirse mientras el estudio esté EN_EJECUCION, por un DOCENTE.

### 3.4 Bolsas y pesajes

- **RN-030** (Debe). Toda bolsa pertenece a una fuente participante del estudio y a un turno ABIERTO.
- **RN-031** (Debe). El sistema genera el rótulo con el formato `{CÓDIGO_FUENTE}-D{día}-{M|T}`, por ejemplo `AU-D3-M`. Si la misma fuente tiene más de una bolsa en el mismo turno, se agrega un consecutivo: `AU-D3-M-1`, `AU-D3-M-2`. El usuario no escribe el rótulo.
- **RN-032** (Debe). El rótulo es único dentro del estudio.
- **RN-033** (Debe). Peso bruto > 0 y tara ≥ 0, en kg con 2 decimales. **Peso bruto > tara.**
- **RN-034** (Debe). Peso neto = peso bruto − tara. Lo calcula el sistema; nunca se digita.
- **RN-035** (Debe). Volumen ocupado > 0, en m³ con 3 decimales.
- **RN-036** (Debe). Densidad de la bolsa = peso neto / volumen, en kg/m³ con 2 decimales. La calcula el sistema.
- **RN-037** (Debería). Límites de plausibilidad configurables por institución (por defecto: peso neto ≤ 50 kg por bolsa, densidad entre 20 y 1 000 kg/m³). Un valor fuera de esos límites **no se bloquea**, pero se marca como atípico y genera una alerta (RN-071).

### 3.5 Grupos y subcategorías de residuos

- **RN-040** (Debe). Hay exactamente 4 grupos, según la Res. 2184 de 2019:

  | Grupo | Significado | Ejemplos de subcategorías |
  | --- | --- | --- |
  | BLANCO | Aprovechables | Papel, cartón, plásticos, vidrio, metales, Tetra Pak |
  | VERDE | Orgánicos aprovechables | Restos de comida, hojas y podas |
  | NEGRO | No aprovechables | Papel higiénico, servilletas, icopor, barrido |
  | SIN_COLOR | Peligrosos y especiales (van a posconsumo) | Pilas, luminarias, RAEE |

- **RN-041** (Debe). Cada subcategoría pertenece a un solo grupo. El nombre de la subcategoría es único en todo el sistema.
- **RN-042** (Debe). Una subcategoría que ya tiene resultados registrados no se borra ni cambia de grupo.

### 3.6 Cuarteo y densidad

- **RN-050** (Debe). Cada turno tiene **como máximo un cuarteo**.
- **RN-051** (Debe). El peso de la submuestra debe estar en el rango configurado (D8: por defecto 45 a 55 kg) **y** no puede superar el peso neto total del turno. Si el peso neto total del turno es menor que el mínimo del rango, se clasifica todo el residuo del turno y el cuarteo queda marcado como "muestra completa".
- **RN-052** (Debe). **Control de calidad del 2 %**: diferencia % = |Σ pesos de subcategorías − peso de la submuestra| / peso de la submuestra × 100. Si la diferencia es ≤ 2 %, el cuarteo es CONFORME. Si es > 2 %, el cuarteo es NO_CONFORME y se genera una alerta (RN-070). Un cuarteo NO_CONFORME se resuelve volviendo a pesar (corrigiendo los pesos) hasta que sea CONFORME, o lo anula un usuario con permiso para anular cuarteos (RN-081) con un motivo escrito.
- **RN-053** (Debe). Composición de una subcategoría (%) = peso de la subcategoría / Σ pesos de todas las subcategorías del cuarteo × 100, con 2 decimales. Se divide entre la suma clasificada, no entre la submuestra, para que la composición sume 100 % (ver pregunta P4).
- **RN-054** (Debe). Composición de un grupo (%) = suma de la composición de sus subcategorías.
- **RN-055** (Debe). Cada peso de subcategoría es ≥ 0. Una subcategoría no puede aparecer dos veces en el mismo cuarteo.
- **RN-056** (Debe). Registrar pesos de subcategorías del grupo SIN_COLOR exige el permiso de residuos peligrosos (RN-081). Hoy lo tienen los dos roles (D9).
- **RN-057** (Debe). Cada turno tiene como máximo una medición de densidad: volumen del recipiente de medición (m³, > 0) y peso neto de los residuos en él (kg, > 0). Densidad del turno = peso / volumen, en kg/m³.

### 3.7 Doble registro, verificación y correcciones

- **RN-060** (Debe). Se verifican la población del turno, cada bolsa, cada cuarteo con sus resultados, y la medición de densidad. Cada uno tiene un estado de verificación: PENDIENTE, VERIFICADO o CORREGIDO. Al crearlo queda PENDIENTE.
- **RN-061** (Debe). El sistema guarda quién digitó cada registro y cuándo.
- **RN-062** (Debe). **Quien verifica debe ser una persona distinta de quien digitó.** Al verificar se guarda quién y cuándo.
- **RN-063** (Debe). Si al cotejar con el papel el dato no coincide, se corrige: el registro pasa a CORREGIDO y se guarda el valor anterior, el nuevo, el motivo, quién y cuándo. Nunca se pierde el valor original.
- **RN-064** — Eliminada: no hay estudiantes de colegio en el sistema (D9).

### 3.8 Alertas

- **RN-070** (Debe). Se genera una alerta automática de tipo CUARTEO_NO_CONFORME cuando un cuarteo supera la tolerancia del 2 %.
- **RN-071** (Debería). Se genera una alerta de tipo VALOR_ATIPICO cuando una bolsa queda fuera de los límites de RN-037.
- **RN-072** (Debería). Se genera una alerta de tipo VERIFICACION_PENDIENTE cuando un turno cerrado conserva registros PENDIENTES 24 horas después de su cierre.
- **RN-073** (Debe). Una alerta se marca como atendida por un usuario con permiso para atender alertas (RN-081), con una observación. Una alerta atendida no se borra.

### 3.9 Usuarios, roles y cuadrillas

- **RN-080** (Debe). Los roles son:

  | Rol | Quién es |
  | --- | --- |
  | DOCENTE | Docente (de la universidad o de la institución) que dirige el estudio y supervisa las cuadrillas |
  | ESTUDIANTE_UNIVERSITARIO | Estudiante de la UPC en la cuadrilla |

- **RN-081** (Debe). Matriz de permisos:

  | Acción | DOCENTE | EST. UNIVERSITARIO |
  | --- | --- | --- |
  | Gestionar instituciones, fuentes y usuarios | Sí | No |
  | Crear estudio y cambiar su estado | Sí | No |
  | Abrir, cerrar y reabrir turnos | Sí | No |
  | Registrar población | Sí | Sí |
  | Registrar bolsas | Sí | Sí |
  | Registrar cuarteo y densidad (grupos Blanco, Verde, Negro) | Sí | Sí |
  | Registrar pesos del grupo SIN_COLOR | Sí | Sí |
  | Verificar y corregir registros | Sí | Sí |
  | Anular un cuarteo NO_CONFORME | Sí | Sí |
  | Atender alertas | Sí | Sí |
  | Ver indicadores y reportes | Sí | Sí |

- **RN-082** — Eliminada: no hay menores de edad en el sistema (D9). Texto anterior: un usuario marcado como menor de edad solo puede usar el sistema si tiene registrado el **consentimiento informado** (Ley 1098 de 2006, Res. 8430 de 1993): fecha y nombre del acudiente que lo firmó.
- **RN-083** — Eliminada: no hay menores de edad en el sistema (D9). Texto anterior: de los menores solo se guardan los datos mínimos: nombre, grado, institución y consentimiento. No se guardan documento de identidad, teléfono ni dirección.
- **RN-084** (Debe). El correo del usuario es único en el sistema.
- **RN-085** (Debe). Toda cuadrilla tiene un DOCENTE responsable.
- **RN-086** (Podría). Al abrir un turno se registra la verificación de EPP de la cuadrilla: guantes, tapabocas, bata u overol (sí/no cada uno).

### 3.10 Evidencias

- **RN-090** (Debería). Se pueden adjuntar evidencias (foto de la balanza, de la bolsa o de la planilla en papel) a una bolsa, un cuarteo o un turno. Formatos JPG o PNG, máximo 5 MB cada una.

## 4. Historias de usuario

- **HU-001.** Como docente, quiero que el sistema rechace datos imposibles (tara mayor que el bruto, población 0, submuestra fuera de rango), para no contaminar la línea base.
- **HU-002.** Como estudiante de la cuadrilla, quiero que el rótulo de la bolsa se genere solo, para no equivocarme al escribirlo.
- **HU-003.** Como docente, quiero saber de inmediato si el cuarteo cumple la tolerancia del 2 %, para repesar antes de que se mezclen los residuos.
- **HU-004.** Como docente, quiero que cada corrección guarde el valor original, para poder demostrar la trazabilidad del dato.
- **HU-005.** Como docente, quiero que cada rol solo haga lo que le corresponde, para mantener el control del estudio.

## 5. Criterios de aceptación

Todos se verifican con pruebas unitarias del dominio, sin base de datos.

**Fuentes**

- **CA-001.** Dado un código `au` (minúsculas), `A` (1 carácter) o `AULAS12` (7 caracteres), cuando se valida la fuente, entonces se rechaza por formato (RN-002); `AULAS1` (6 caracteres) sí es válido.
- **CA-002.** Dada una institución con una fuente `AU`, cuando se crea otra fuente `AU` en la misma institución, entonces se rechaza; en otra institución sí se acepta.
- **CA-003.** Dado un polígono de 3 puntos sin cerrar, o con una latitud de 95, cuando se valida, entonces se rechaza (RN-004).

**Estudio**

- **CA-010.** Dado un estudio nuevo, cuando se crea, entonces tiene 8 días, numerados del 1 al 8, y el día 1 está marcado como excluido.
- **CA-011.** Dadas fechas en las que el día 3 es anterior al día 2, cuando se valida el estudio, entonces se rechaza (RN-011).
- **CA-012.** Dado un estudio del 3 al 14 de marzo, cuando se crea otro estudio en la misma institución del 10 al 21 de marzo, entonces se rechaza por solapamiento (RN-013).
- **CA-013.** Dado un estudio PLANIFICADO, cuando se intenta pasar directo a CERRADO, entonces se rechaza (RN-014).
- **CA-014.** Dado un estudio con una bolsa PENDIENTE, cuando se intenta cerrar, entonces se rechaza indicando cuántos registros faltan (RN-016).

**Turnos y población**

- **CA-020.** Dado un día con turno MANANA, cuando se crea otro turno MANANA el mismo día, entonces se rechaza.
- **CA-021.** Dada población 0 estudiantes, 0 docentes, 0 personal, cuando se registra, entonces se rechaza (RN-022).
- **CA-022.** Dado un turno abierto sin población, cuando se registra una bolsa, entonces se rechaza con el mensaje "Registre primero la población del turno" (RN-023).
- **CA-023.** Dado un turno con una fuente participante sin bolsas ni marca de "sin generación", cuando se intenta cerrar, entonces se rechaza nombrando la fuente (RN-024).

**Bolsas**

- **CA-030.** Dada la fuente `AU`, día 3, turno mañana, cuando se registra la primera bolsa, entonces su rótulo es `AU-D3-M`; cuando se registra la segunda, los rótulos quedan `AU-D3-M-1` y `AU-D3-M-2`.
- **CA-031.** Dado peso bruto 3,20 kg y tara 3,50 kg, cuando se valida, entonces se rechaza (RN-033).
- **CA-032.** Dado peso bruto 12,40 kg, tara 0,40 kg y volumen 0,080 m³, cuando se registra, entonces peso neto = 12,00 kg y densidad = 150,00 kg/m³.
- **CA-033.** Dado peso neto 70 kg con el límite por defecto, cuando se registra, entonces se acepta, queda marcada como atípica y se genera una alerta VALOR_ATIPICO.

**Cuarteo**

- **CA-050.** Dado un turno con 120 kg netos, cuando se registra una submuestra de 40 kg, entonces se rechaza por estar fuera del rango 45–55 kg.
- **CA-051.** Dado un turno con 30 kg netos, cuando se registra una submuestra de 30 kg, entonces se acepta como "muestra completa".
- **CA-052.** Dada una submuestra de 50,00 kg cuyas subcategorías suman 49,20 kg (diferencia 1,6 %), cuando se evalúa, entonces es CONFORME.
- **CA-053.** Dada una submuestra de 50,00 kg cuyas subcategorías suman 48,50 kg (diferencia 3,0 %), cuando se evalúa, entonces es NO_CONFORME y se genera una alerta CUARTEO_NO_CONFORME.
- **CA-054.** Dadas subcategorías papel 10 kg, restos de comida 25 kg e icopor 15 kg, cuando se calcula la composición, entonces papel = 20,00 %, restos de comida = 50,00 %, icopor = 30,00 %, y los grupos quedan BLANCO 20 %, VERDE 50 %, NEGRO 30 %, SIN_COLOR 0 %.
- **CA-055.** Dado un turno que ya tiene un cuarteo, cuando se registra otro, entonces se rechaza (RN-050).
- **CA-056.** Dado un ESTUDIANTE_UNIVERSITARIO, cuando registra un peso de la subcategoría "Pilas" (SIN_COLOR), entonces se acepta, porque su rol tiene el permiso de residuos peligrosos (RN-056, RN-081).

**Verificación**

- **CA-060.** Dada una bolsa digitada por el usuario A, cuando A intenta verificarla, entonces se rechaza; cuando la verifica B, queda VERIFICADO con B y la fecha.
- **CA-061.** Dada una bolsa con bruto 12,40 kg, cuando B la corrige a 12,04 kg con el motivo "error de digitación", entonces queda CORREGIDO y la corrección guarda 12,40 → 12,04, el motivo, B y la fecha.
- **CA-062.** Dado un ESTUDIANTE_UNIVERSITARIO, cuando verifica un registro que digitó otra persona, entonces se acepta.

**Usuarios**

- **CA-080.** Dado un usuario con un correo ya registrado, cuando se crea, entonces se rechaza (RN-084).
- **CA-081.** Dada una cuadrilla cuyo responsable es un ESTUDIANTE_UNIVERSITARIO, o sin responsable, cuando se valida, entonces se rechaza (RN-085).

**Errores**

- **CA-090.** Todo rechazo de las reglas anteriores lanza una excepción de la jerarquía `EcoPointException` con un mensaje en español que explica qué falló: dato con formato o rango inválido → `InvalidDataException`; regla de negocio violada → `BusinessRuleException`; registro inexistente → `ResourceNotFoundException`.

## 6. Fuera de alcance

- Puntos ecológicos, recipientes, nivel de llenado y señalización (D1).
- Cálculo de los indicadores de la línea base: spec 006 (esta spec define los datos y reglas que esos cálculos usan).
- Almacenamiento, API y pantallas: specs 002, 003 y 004–007.
- Inicio de sesión y contraseñas: spec 003.

## 7. Preguntas abiertas

| # | Pregunta | Propuesta (se asume si nadie dice lo contrario) |
| --- | --- | --- |
| P1 | ¿Los 8 días son días calendario seguidos o días de clase seguidos? | Días de clase: fechas crecientes, sin exigir que sean seguidas (RN-011). |
| P2 | ¿Cada fuente tiene un código propio aunque haya varias del mismo tipo (ej. dos bloques de aulas: `AU1`, `AU2`)? | Sí. El código identifica la fuente; el tipo agrupa. |
| P3 | ¿Puede haber varias bolsas por fuente y turno? | Sí, con consecutivo en el rótulo (RN-031). |
| P4 | ¿La composición se calcula sobre la suma clasificada o sobre el peso de la submuestra? | Sobre la suma clasificada, para que sume 100 %. La diferencia con la submuestra ya la controla la regla del 2 %. |
| P5 | ¿Se hace un cuarteo por turno o uno por día? | Uno por turno (RN-050), como indica el flujo del póster. |
| P6 | ¿El volumen se registra en m³ o en litros? | Se guarda en m³; la pantalla puede pedir litros y convertir. |
| P7 | ¿Los límites de plausibilidad de RN-037 son razonables? | Validar con el equipo de Ingeniería Ambiental. |
| P8 | ¿Se incluye la verificación de EPP (RN-086)? | Como "Podría", solo si sobra tiempo. |
