# Spec 003 — API REST y seguridad

| Campo | Valor |
| --- | --- |
| Estado | **APROBADA** (11 nov 2026) |
| Redacta | Claude |
| Implementa / revisa | Claude / Antigravity |
| Requerimientos | RF-11 (roles y permisos); expone RF-01 a RF-14 |
| Depende de | Spec 002 aprobada; decisiones D3, D9 |
| Fecha | 2026-11-11 |
| Ajustes | **D13** (13 nov 2026): observaciones de las pruebas de uso. **Aprobados** (13 nov 2026). |

## 1. Objetivo

Exponer las operaciones del sistema como una **API REST con JSON** bajo `/api/v1/*`, protegida con inicio de sesión y permisos por rol, para que el frontend web (specs 004–007) la consuma.

La API es el **contrato** entre backend y frontend. Una vez aprobada, los dos lados pueden trabajar en paralelo.

## 2. Requerimientos que cubre

- RF-11: roles y permisos.
- Exposición de RF-01 a RF-14 hacia el frontend.

## 3. Actores

- **Frontend web**: único consumidor de la API.
- **Usuarios** de los 4 roles (RN-080), a través del frontend.

## 4. Historias de usuario

- **HU-020.** Como usuario, quiero iniciar sesión con mi correo y contraseña, para usar el sistema según mi rol.
- **HU-021.** Como docente, quiero que cada rol solo pueda hacer lo que le corresponde, aunque alguien intente llamar a la API directamente.
- **HU-022.** Como desarrollador del frontend, quiero que todos los errores lleguen con el mismo formato y un mensaje en español, para mostrarlos al usuario sin traducirlos.

## 5. Requisitos

### 5.1 Convenciones generales

- **R-100** (Debe). Todas las rutas empiezan con `/api/v1/`.
- **R-101** (Debe). Todo se envía y recibe como JSON en UTF-8. Las fechas van en formato ISO 8601 (`2026-11-11`, `2026-11-11T07:30:00`), los pesos y volúmenes como números con punto decimal.
- **R-102** (Debe). La API nunca devuelve las clases del dominio directamente: usa objetos de entrada y salida propios (DTO).
- **R-103** (Debe). Los listados largos (bolsas, alertas, usuarios) se pueden paginar y filtrar.
- **R-104** (Debe). Los mensajes para el usuario van en español; los nombres de campos JSON, en inglés y en camelCase (`grossWeight`, `idSource`).

### 5.2 Formato de error

- **R-110** (Debe). Todo error responde con esta estructura, tomada del Requerimiento Técnico oficial:

  ```json
  {
    "timestamp": "2026-11-11T19:30:00",
    "status": 422,
    "errorCode": "ERR_BUSINESS_RULE",
    "message": "No se pueden registrar bolsas: registre primero la población del turno.",
    "path": "/api/v1/shifts/14/bags",
    "fieldErrors": []
  }
  ```

  `fieldErrors` lista los campos inválidos (`{ "field": "tare", "message": "La tara no puede superar el peso bruto." }`) cuando aplica.

- **R-111** (Debe). Cada tipo de excepción tiene su código HTTP:

  | Situación | Excepción | HTTP | `errorCode` |
  | --- | --- | --- | --- |
  | Formato, nulabilidad o rango inválido | `InvalidDataException` | 400 | `ERR_INVALID_DATA` |
  | No ha iniciado sesión o la sesión venció | — | 401 | `ERR_UNAUTHORIZED` |
  | Su rol no permite la acción | — | 403 | `ERR_FORBIDDEN` |
  | El recurso no existe | `ResourceNotFoundException` | 404 | `ERR_NOT_FOUND` |
  | Viola una regla de negocio | `BusinessRuleException` | 422 | `ERR_BUSINESS_RULE` |
  | Cuarteo fuera de la tolerancia del 2 % | `QualityControlException` | 422 | `ERR_QUALITY_CONTROL` |
  | Falla de base de datos u otra interna | `PersistenceException` u otra | 500 | `ERR_PERSISTENCE` / `ERR_INTERNAL` |

- **R-112** (Debe). Un error 500 nunca muestra detalles internos (SQL, trazas de pila). Dice "Ocurrió un error interno. Contacte al administrador." y el detalle queda en el log del servidor.

### 5.3 Inicio de sesión y seguridad

- **R-120** (Debe). Inicio de sesión con correo y contraseña. Las contraseñas se guardan cifradas con un algoritmo de hash seguro (BCrypt); nunca en texto plano.
- **R-121** (Debe). Todas las rutas exigen sesión, excepto `POST /api/v1/auth/login`.
- **R-122** (Debe). Cada ruta aplica la matriz de permisos de RN-081. Las reglas que dependen del dato (ej. RN-056, pesos SIN_COLOR) también se aplican en la API, no solo en la pantalla.
- **R-123** — Eliminado: no hay menores de edad en el sistema (D9).
- **R-124** (Debe). La sesión vence tras 8 horas, lo que cubre una jornada completa de muestreo.
- **R-125** (Debe). `GET /api/v1/auth/me` devuelve el usuario actual con su rol y su institución, para que el frontend arme el menú.
- **R-126** (Debería). Después de 5 intentos fallidos seguidos, la cuenta se bloquea 15 minutos.
- **R-127** (Debe). Solo se aceptan peticiones del origen del frontend (CORS configurado; nada de `*`).
- **R-132** (Debe). Cada usuario puede **cambiar su contraseña** indicando la actual. La nueva cumple las mismas reglas que al crear el usuario (D13).
- **R-133** (Debe). Cada usuario puede consultar **sus propios** totales de registros: poblaciones, bolsas, cuarteos y densidades digitados, verificaciones hechas y correcciones que recibieron sus datos. El docente puede consultar los de cualquier usuario; un estudiante, solo los suyos (D13).
- **R-134** (Debe). Los mensajes de error de la API no incluyen códigos de reglas (RN-, R-, CA-); explican el problema en palabras (D13).

### 5.4 Recursos de la API

Catálogo mínimo. El detalle de cada JSON va en `plan.md`.

| Recurso | Operaciones | Spec que lo usa |
| --- | --- | --- |
| `auth` | `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`; cambiar mi contraseña; ver mis totales de registros | Todas |
| `institutions` | Listar (filtro por estado), ver, crear, editar, activar, desactivar | 004, 007 |
| `institutions/{id}/sources` | Listar (filtro por estado), ver, crear (activa o inactiva), editar, activar, desactivar fuentes | 004, 007 |
| `institutions/{id}/map` | Ver el plano: GeoJSON de fuentes con sus indicadores | 007 |
| `waste-categories` | Listar grupos y subcategorías (filtro por estado); crear (activa o inactiva), editar, activar y desactivar (docente) | 004, 005 |
| `users` | Listar con filtros (rol, estado, texto) y paginación, ver, crear, editar; activar y desactivar estudiantes; ver totales de registros | 004 |
| `crews` | Listar, crear, editar integrantes | 004 |
| `studies` | Listar, ver, crear, editar, cambiar estado, cancelar (con motivo), eliminar si está planificado | 004, 006 |
| `studies/{id}/days` | Ver y editar fechas de los días | 004 |
| `studies/{id}/shifts` | Listar turnos con su estado y su avance | 004 |
| `shifts/{id}` | Ver, abrir, cerrar, reabrir | 004 |
| `shifts/{id}/population` | Registrar y ver población | 004 |
| `shifts/{id}/bags` | Listar y registrar bolsas; marcar fuente "sin generación" | 004 |
| `bags/{id}` | Ver, corregir | 004, 005 |
| `shifts/{id}/quartering` | Registrar y ver el cuarteo con sus resultados; anular | 005 |
| `shifts/{id}/density` | Registrar y ver la medición de densidad | 005 |
| `verifications` | Listar pendientes por estudio o turno; verificar; corregir | 005 |
| `records/{tipo}/{id}/history` | Ver historial de correcciones de un registro | 005 |
| `evidences` | Subir y ver fotos de un registro | 004, 005 |
| `alerts` | Listar con filtros; marcar como atendida | 005, 006 |
| `studies/{id}/indicators` | Indicadores de la línea base (total, por fuente, por turno) | 006 |
| `studies/{id}/reports` | Descargar reporte en PDF o Excel | 006 |

### 5.5 Documentación de la API

- **R-130** (Debe). La API se documenta automáticamente con OpenAPI y se puede probar desde un navegador (Swagger UI), disponible solo en desarrollo.
- **R-131** (Debería). Una colección de peticiones de ejemplo (Postman o archivo `.http`) para cada recurso.

## 6. Criterios de aceptación

- **CA-200.** Dada una persona sin sesión, cuando llama a `GET /api/v1/studies`, entonces recibe 401 con `ERR_UNAUTHORIZED`.
- **CA-201.** Dado un usuario con correo y contraseña correctos, cuando hace login, entonces recibe 200 y puede llamar a `GET /api/v1/auth/me`, que devuelve su nombre, rol e institución.
- **CA-202.** Dada una contraseña incorrecta, cuando hace login, entonces recibe 401 con un mensaje que no revela si el correo existe.
- **CA-203.** Dado un ESTUDIANTE_UNIVERSITARIO con sesión, cuando llama a la ruta de crear un estudio, entonces recibe 403 con `ERR_FORBIDDEN`.
- **CA-204.** Dado un ESTUDIANTE_UNIVERSITARIO, cuando intenta cerrar un turno que cumple todas las condiciones, entonces recibe 403.
- **CA-205.** Dada una bolsa con tara mayor que el bruto, cuando se envía, entonces recibe 400 con `ERR_INVALID_DATA` y `fieldErrors` señala `tare`.
- **CA-206.** Dado un turno sin población, cuando se envía una bolsa, entonces recibe 422 con `ERR_BUSINESS_RULE` y el mensaje de RN-023.
- **CA-207.** Dado un id de estudio que no existe, cuando se pide, entonces recibe 404 con `ERR_NOT_FOUND`.
- **CA-208.** Dada una falla simulada de base de datos, cuando se llama cualquier ruta, entonces recibe 500 sin texto de SQL ni traza en la respuesta, y el detalle queda en el log.
- **CA-209** — Eliminado: no hay menores de edad en el sistema (D9).
- **CA-210.** Cuando se consulta la base de datos, entonces ninguna contraseña está en texto plano.
- **CA-211.** Para cada fila de la matriz RN-081 existe al menos una prueba automática de API que confirma el "Sí" y una que confirma el "No".
- **CA-212.** Cuando se abre la documentación OpenAPI, entonces aparecen todos los recursos de la sección 5.4.

## 7. Fuera de alcance

- Pantallas: specs 004 a 007.
- Recuperación de contraseña por correo y acceso con cuentas externas (Google, Microsoft).
- Uso sin conexión a internet (ver pregunta P3).

## 8. Preguntas abiertas

| # | Pregunta | Propuesta |
| --- | --- | --- |
| P1 | ¿Sesión con cookie del servidor o con token JWT? | Cookie de sesión `HttpOnly` del servidor: más simple y segura para una aplicación web propia. Se decide en `plan.md`. |
| P2 | ¿El servidor sirve también el frontend compilado o van separados? | Separados en desarrollo; en la entrega, el backend sirve el frontend compilado, para tener un solo servidor que arrancar. |
| P3 | En campo puede no haber internet. ¿Se necesita trabajar sin conexión? | No en esta versión: el papel es el respaldo (doble registro) y se digita al volver la señal. Se anota como mejora futura. |
| P4 | ¿Quién crea los usuarios: el docente o cada uno se registra? | Solo el docente crea usuarios. No hay registro abierto. |
