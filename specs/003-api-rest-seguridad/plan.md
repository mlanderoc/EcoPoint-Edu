# Plan — Spec 003: API REST y seguridad

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (12 nov 2026) |
| Spec | [spec.md](spec.md) (aprobada) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo |
| Rama | `feature/003-api-rest-seguridad` |
| Fecha | 2026-11-12 |

## 1. Resumen

La spec 003 convierte el backend en una **aplicación web Spring Boot** que expone el dominio (spec 001) y la base de datos (spec 002) como una **API REST con JSON** en `/api/v1/*`, con inicio de sesión, permisos por rol y un formato de error único en español.

Tiene cinco partes:

1. **Seguridad**: inicio de sesión con correo y contraseña, sesión con cookie `HttpOnly` de 8 horas, permisos con la matriz RN-081, protección CSRF y CORS.
2. **Servicios** (`com.ecopoint.service`): orquestan cada caso de uso. Validan con los validadores de la spec 001, aplican los permisos con `AccessPolicy`, usan los repositorios por sus interfaces y abren las transacciones.
3. **Controladores y DTO** (`com.ecopoint.controller`, `com.ecopoint.dto`): reciben JSON, delegan en los servicios y responden con DTO. No tienen reglas de negocio.
4. **Manejo de errores**: un solo `@RestControllerAdvice` convierte cada excepción en el JSON de R-110 con su código HTTP.
5. **Documentación**: OpenAPI con Swagger UI (solo en desarrollo) y archivos `.http` de ejemplo.

**Alcance:** la API completa de la sección 5.4 de la spec, **menos tres recursos cuyo cálculo pertenece a otras specs**: `studies/{id}/indicators` y `studies/{id}/reports` (spec 006) e `institutions/{id}/map` (spec 007). Aquí se deja su contrato y los implementa su spec.

## 2. Decisiones técnicas

| # | Decisión | Por qué |
| --- | --- | --- |
| T1 | **Sesión con cookie del servidor** (`JSESSIONID`, `HttpOnly`, `SameSite=Lax`, `Secure` en producción), no JWT (spec, P1). Vence a las **8 horas** (`server.servlet.session.timeout=8h`) | R-124. Es lo que el frontend de la 004 ya usa (`credentials: 'include'` y `GET /auth/me`); no hay tokens en JavaScript |
| T2 | **Spring Security 6** solo para **autenticar** (quién es). Los **permisos** (qué puede hacer) los aplica cada servicio con `AccessPolicy.require(usuario, permiso)` de la spec 001 | La matriz RN-081 vive en un solo lugar (`UserRole`); los servicios se prueban sin servidor web. R-122 |
| T3 | **CSRF activo** con `CookieCsrfTokenRepository`: el servidor envía la cookie `XSRF-TOKEN` y el frontend la devuelve en la cabecera `X-XSRF-TOKEN` en cada `POST`, `PUT`, `PATCH` y `DELETE` | Con sesión por cookie, sin CSRF otra página podría hacer peticiones en nombre del usuario. **El `apiClient` de la 004 debe agregar esa cabecera** (un cambio pequeño) |
| T4 | **En desarrollo, Vite reenvía `/api` al backend** (`server.proxy` → `http://localhost:8080`), así el navegador ve un solo origen. CORS queda configurado solo para el origen del frontend (`ECOPOINT_FRONTEND_ORIGIN`, por defecto `http://localhost:5173`), nunca `*` | R-127. El *proxy* evita problemas de cookies entre puertos |
| T5 | **Contraseñas con BCrypt** (`BCryptPasswordEncoder`, costo 10). El hash nunca sale del repositorio: `UserRepository` gana `findPasswordHashByEmail` y `updatePasswordHash`; la clase `User` no lo guarda | R-120, CA-210. La semilla de la spec 002 ya usa BCrypt |
| T6 | **Bloqueo tras 5 intentos fallidos** durante 15 minutos, contado **en memoria** (`LoginAttemptService`, por correo en minúsculas) | R-126 es "Debería". Sin cambiar el esquema; si el servidor se reinicia, el contador vuelve a cero. Se documenta |
| T7 | Login con **mensaje único** para correo inexistente, contraseña errada y cuenta inactiva: "Correo o contraseña incorrectos." | CA-202: no revela si el correo existe |
| T8 | **DTO como `record`** en `com.ecopoint.dto`, con métodos `from(...)` que copian desde el modelo. Las entradas se validan con los **validadores de la spec 001** dentro del servicio, no con anotaciones `@Valid` | R-102. Las reglas ya están probadas en `validation`; no se duplican |
| T9 | **Números decimales como `BigDecimal`** en los DTO. Jackson los escribe como número JSON con punto decimal, sin notación científica (`WRITE_BIGDECIMAL_AS_PLAIN`). Fechas ISO 8601 (`JavaTimeModule`, sin *timestamps*) | R-101 y AGENTS.md |
| T10 | **Paginación** en bolsas, alertas y usuarios con `?page=0&size=50` (máximo 200). Respuesta `{ "items": [], "page": 0, "size": 50, "totalItems": 0 }` | R-103 |
| T11 | Los servicios reciben el **usuario actual** como parámetro (`User actor`); el controlador lo obtiene de la sesión con `@AuthenticationPrincipal` | Los servicios no dependen de Spring Security y se prueban con usuarios de prueba |
| T12 | Las fechas y horas actuales las da un **`Clock` inyectado** en los servicios | AGENTS.md: el dominio recibe la hora por parámetro; las pruebas fijan la hora |
| T13 | **OpenAPI con `springdoc-openapi`** (versión compatible con Spring Boot 3.5). Swagger UI en `/swagger-ui.html`, encendido solo con el perfil `dev` | R-130, CA-212 |
| T14 | **`.http` de ejemplo** por recurso en `docs/api/` (se ejecutan desde IntelliJ o VS Code) | R-131 |
| T15 | Se quita `spring.main.web-application-type=none` y se agrega `spring-boot-starter-web` y `spring-boot-starter-security`. Puerto **8080** | La aplicación pasa a ser un servidor web |
| T16 | **El servidor no sirve todavía el frontend compilado** (spec, P2). En desarrollo van separados; empaquetar los dos en un solo servidor se hace al final, cuando la 004 esté fusionada | El frontend aún no está en `develop` |

## 3. Arquitectura

```
controller ──► service ──► validation ──► model
    │             └──────► persistence (interfaces) ──► model
    └──► dto ──► model
security ──► service (UserAuthService), persistence (UserRepository)
exceptions: transversal
```

| Paquete | Contenido |
| --- | --- |
| `com.ecopoint.config` | `SecurityConfig`, `CorsConfig`, `JacksonConfig`, `OpenApiConfig`, `ClockConfig` |
| `com.ecopoint.security` | `EcoPointUserDetails` (envuelve a `User`), `EcoPointUserDetailsService`, `LoginAttemptService`, `JsonAuthenticationEntryPoint` (401) y `JsonAccessDeniedHandler` (403) |
| `com.ecopoint.service` | Un servicio por agregado (sección 5) |
| `com.ecopoint.controller` | Un controlador por recurso + `ApiExceptionHandler` |
| `com.ecopoint.dto` | `*Request` y `*Response` como `record`, `PageResponse`, `ApiError`, `FieldErrorDto` |

Reglas de AGENTS.md que se verifican en la revisión: `controller` no contiene reglas de negocio; `service` solo usa interfaces de `persistence`; ningún controlador devuelve clases de `model`.

## 4. Seguridad y errores

### 4.1 Inicio de sesión

| Ruta | Entrada | Salida |
| --- | --- | --- |
| `POST /api/v1/auth/login` | `{ "email", "password" }` | 200 + `UserResponse`, y la cookie de sesión. 401 `ERR_UNAUTHORIZED` si falla; 401 con "Cuenta bloqueada temporalmente. Intente en 15 minutos." si se superan los 5 intentos |
| `POST /api/v1/auth/logout` | — | 204; invalida la sesión |
| `GET /api/v1/auth/me` | — | 200 + `UserResponse` con su institución |
| `GET /api/v1/auth/csrf` | — | 204; solo entrega la cookie `XSRF-TOKEN` (el frontend la llama al iniciar) |

`UserResponse`: `id`, `firstName`, `lastName`, `email`, `role`, `active`, `institutionId`, `institutionName`, `permissions` (lista de los permisos del rol, para que el menú no repita la matriz).

### 4.2 Formato de error (R-110, R-111)

`ApiExceptionHandler` traduce:

| Origen | HTTP | `errorCode` |
| --- | --- | --- |
| `InvalidDataException` | 400 | `ERR_INVALID_DATA` (con `fieldErrors`) |
| JSON mal formado, tipo de dato equivocado | 400 | `ERR_INVALID_DATA` |
| Sin sesión o sesión vencida | 401 | `ERR_UNAUTHORIZED` |
| `AccessDeniedException` (nuestra) o CSRF inválido | 403 | `ERR_FORBIDDEN` |
| `ResourceNotFoundException`, ruta inexistente | 404 | `ERR_NOT_FOUND` |
| `BusinessRuleException` | 422 | `ERR_BUSINESS_RULE` |
| `QualityControlException` | 422 | `ERR_QUALITY_CONTROL` |
| `PersistenceException` | 500 | `ERR_PERSISTENCE` |
| Cualquier otra | 500 | `ERR_INTERNAL` |

En los 500, el mensaje es siempre "Ocurrió un error interno. Contacte al administrador." y el detalle va al log con un identificador que se repite en el log (R-112, CA-208).

**`fieldErrors`:** `InvalidDataException` ya guarda el nombre del campo, pero los validadores lo nombran en español (`tara`, `correo`, `nombre`), porque ese nombre aparece en el mensaje al usuario. La API, en cambio, debe señalar el campo del JSON (`tare`, `email`, `name`). Para no cambiar los validadores:

- Se agrega a `InvalidDataException` el método `getProblemDescription()` (el motivo sin el prefijo "Dato inválido en el campo…"). Es compatible con lo existente.
- Cada controlador declara el diccionario de sus campos (`tara → tare`, `pesoBruto → grossWeight`…), y `ApiExceptionHandler` lo usa para armar `fieldErrors` con el campo del JSON y el motivo en español (CA-205).
- Si un campo no está en el diccionario o es `general`, el error va solo en `message`, sin `fieldErrors`.

## 5. Recursos

Todas las rutas empiezan con `/api/v1`. "Permiso" es el de RN-081 que exige el servicio; "Sesión" significa cualquier usuario con sesión. Las listas filtran por la institución del usuario.

### 5.1 Administración

| Ruta | Permiso | Notas |
| --- | --- | --- |
| `GET /institutions`, `GET /institutions/{id}` | Sesión | |
| `POST /institutions`, `PUT /institutions/{id}` | `MANAGE_ADMINISTRATION` | `name`, `address`, `mapCenter {latitude, longitude}`, `mapZoom`, `settings` |
| `GET /institutions/{id}/sources?active=true` | Sesión | |
| `POST /institutions/{id}/sources`, `PUT …/sources/{sourceId}` | `MANAGE_ADMINISTRATION` | `code`, `name`, `type`; RN-002 (código único) |
| `POST …/sources/{sourceId}/deactivate` y `/activate` | `MANAGE_ADMINISTRATION` | RN-005: con bolsas solo se desactiva |
| `GET /users?page&size&role&active` | `MANAGE_ADMINISTRATION` | |
| `POST /users`, `PUT /users/{id}` | `MANAGE_ADMINISTRATION` | `firstName`, `lastName`, `email`, `role`, `password` (solo al crear o al cambiarla); P4: solo el docente crea usuarios |
| `POST /users/{id}/deactivate` | `MANAGE_ADMINISTRATION` | |
| `GET /crews`, `POST /crews`, `PUT /crews/{id}` | Leer: sesión. Escribir: `MANAGE_ADMINISTRATION` | `name`, `responsibleId`, `memberIds`; RN-085 |
| `GET /waste-categories?group` | Sesión | |
| `POST /waste-categories`, `PUT /waste-categories/{id}` | `MANAGE_ADMINISTRATION` | `name`, `group`, `description` |

### 5.2 Estudios y turnos

| Ruta | Permiso | Notas |
| --- | --- | --- |
| `GET /studies?institutionId`, `GET /studies/{id}` | Sesión | `StudyResponse`: `id`, `institutionId`, `status`, `days [{dayNumber, date, discarded}]`, `participatingSourceIds`, `pendingRecords` |
| `POST /studies` | `MANAGE_STUDIES` | `institutionId`, `days [{dayNumber, date}]`. El frontend propone los días hábiles (R-330) |
| `PUT /studies/{id}/days` | `MANAGE_STUDIES` | Cambiar fechas |
| `PUT /studies/{id}/sources` | `MANAGE_STUDIES` | Fuentes participantes (R-331) |
| `POST /studies/{id}/status` | `MANAGE_STUDIES` | `{ "status", "reason" }`. RN-014 y RN-016; la reapertura exige motivo. Transacción |
| `GET /studies/{id}/shifts` | Sesión | Los 16 turnos posibles: los que existen con su estado y avance, y los que no, con `status: null` ("no iniciado") |
| `POST /studies/{id}/shifts` | `MANAGE_SHIFTS` | Abrir turno: `dayNumber`, `type`, `crewId` |
| `GET /shifts/{id}` | Sesión | Turno con población, resumen por fuente (con bolsas, sin generación o pendiente), cuarteo y densidad |
| `POST /shifts/{id}/close`, `POST /shifts/{id}/reopen` | `MANAGE_SHIFTS` | RN-024; la reapertura exige motivo |
| `GET`, `PUT /shifts/{id}/population` | Escribir: `REGISTER_POPULATION` | `students`, `teachers`, `staff`. Transacción |
| `GET /shifts/{id}/bags?page&size` | Sesión | |
| `POST /shifts/{id}/bags` | `REGISTER_BAGS` | `sourceId`, `grossWeight`, `tare`, `volume`. Responde la bolsa con rótulo, neto, densidad y si es atípica (RN-037). Si es atípica, crea su alerta en la misma transacción |
| `POST /shifts/{id}/sources/{sourceId}/no-generation` y `DELETE` | `REGISTER_BAGS` | Marcar o quitar "sin generación" |
| `GET /bags/{id}`, `PUT /bags/{id}` | Corregir: `REGISTER_BAGS` mientras esté PENDIENTE (R-348) | |

### 5.3 Cuarteo, densidad, verificación y alertas (los consume la spec 005)

| Ruta | Permiso | Notas |
| --- | --- | --- |
| `GET`, `POST /shifts/{id}/quartering` | Escribir: `REGISTER_QUARTERING`; resultados `SIN_COLOR` además `REGISTER_HAZARDOUS_WASTE` (RN-056) | `subsampleWeight`, `results [{categoryId, weight}]`. Responde el estado de calidad. Fuera de tolerancia: se guarda como NO_CONFORME y se crea su alerta (no se rechaza) |
| `POST /shifts/{id}/quartering/annul` | `ANNUL_QUARTERING` | `{ "reason" }` |
| `GET`, `POST /shifts/{id}/density` | Escribir: `REGISTER_QUARTERING` (RN-081: "Registrar cuarteo y densidad") | `weight`, `volume` |
| `GET /verifications?studyId&shiftId` | `VERIFY_RECORDS` | Usa `V_REGISTROS_PENDIENTES` |
| `POST /verifications/{recordType}/{id}` | `VERIFY_RECORDS` | Verificar sin cambios. Quien verifica debe ser distinto de quien digitó (RN-062) |
| `POST /verifications/{recordType}/{id}/correct` | `VERIFY_RECORDS` | Valores nuevos + `reason`. Guarda la corrección y actualiza el registro en una sola transacción (T12 de la spec 002) |
| `GET /records/{recordType}/{id}/history` | Sesión | Historial de correcciones |
| `POST /evidences` (multipart), `GET /evidences?targetType&targetId`, `GET /evidences/{id}/file` | Subir: el permiso del registro al que se adjunta | Máximo 5 MB, JPG o PNG (RN-090). Los archivos se guardan en una carpeta local configurable (`ECOPOINT_UPLOAD_DIR`), no en la base |
| `GET /alerts?studyId&type&attended&page&size` | Sesión | |
| `POST /alerts/{id}/attend` | `ATTEND_ALERTS` | `{ "note" }` |

### 5.4 Reservados para otras specs

| Ruta | Spec | Permiso |
| --- | --- | --- |
| `GET /studies/{id}/indicators` | 006 | `VIEW_INDICATORS` |
| `GET /studies/{id}/reports?format=pdf\|xlsx` | 006 | `VIEW_INDICATORS` |
| `GET /institutions/{id}/map`, `PUT …/sources/geometries`, `PUT`/`DELETE …/sources/{id}/geometry` | 007 | Ver: sesión. Editar: `MANAGE_ADMINISTRATION` |

## 6. Servicios

| Servicio | Responsabilidad | Transacción |
| --- | --- | --- |
| `AuthService` | Login, bloqueo por intentos, usuario actual | — |
| `InstitutionService`, `GenerationSourceService` | Administración de instituciones y fuentes | — |
| `UserService` | Usuarios, contraseña inicial con BCrypt | Sí, al crear |
| `CrewService` | Cuadrillas con integrantes | Sí |
| `WasteCategoryService` | Subcategorías | — |
| `StudyService` | Crear estudio con sus 8 días, fechas, fuentes, cambios de estado y reaperturas | Sí |
| `ShiftService` | Abrir, cerrar y reabrir turnos; población; "sin generación"; resumen por fuente | Sí |
| `WasteBagService` | Registrar y corregir bolsas; crear la alerta si es atípica | Sí |
| `QuarteringService`, `DensityService` | Cuarteo con resultados y calidad; anulación; densidad | Sí |
| `VerificationService` | Verificar y corregir con historial | Sí |
| `AlertService` | Listar y atender alertas | — |
| `EvidenceService` | Guardar archivos y sus datos | Sí |

Los servicios **no** tienen reglas que ya estén en el modelo o en los validadores: las llaman.

## 7. Pruebas

| Tipo | Qué cubre | Cómo corre |
| --- | --- | --- |
| Unitarias de servicios | Cada caso de uso con repositorios simulados (Mockito) y un `Clock` fijo | `./mvnw test` |
| `@WebMvcTest` de controladores | Rutas, JSON de entrada y salida, formato de error, códigos HTTP: CA-205, CA-206, CA-207, CA-208 (repositorio simulado que lanza `PersistenceException`) | `./mvnw test` |
| `AuthIT` | CA-200, CA-201, CA-202, bloqueo por intentos, sesión y CSRF, contra `ecopoint_test` con la semilla | `./mvnw verify` |
| `PermissionMatrixIT` | CA-203, CA-204, CA-211: por cada permiso, una ruta representativa llamada con cada rol | `./mvnw verify` |
| `AdministrationIT` | CA-210: después de crear un usuario por la API, la columna `clave_hash` empieza por `$2a$` y no contiene la contraseña | `./mvnw verify` |
| `OpenApiIT` | CA-212: el documento OpenAPI tiene las rutas de la sección 5 | `./mvnw verify` con perfil `dev` |

**CA-211:** la matriz tiene 11 permisos. Para los 3 que el estudiante no tiene (`MANAGE_ADMINISTRATION`, `MANAGE_STUDIES`, `MANAGE_SHIFTS`) se prueba "Sí" con el docente y "No" (403) con el estudiante. Los otros 8 los tienen los dos roles (D9), así que se prueba "Sí" con ambos; el "No" de esos permisos no existe con los roles actuales. `VIEW_INDICATORS` se prueba en la spec 006, que crea su ruta.

## 8. Fases

| Fase | Contenido |
| --- | --- |
| 0 | `TEAM.md` con los paquetes nuevos; dependencias web, security y springdoc; configuración; la aplicación arranca en el puerto 8080 |
| 1 | Seguridad y errores: login, sesión, CSRF, CORS, bloqueo, `ApiExceptionHandler`, `GET /auth/me`. CA-200, CA-201, CA-202, CA-208, CA-210 |
| 2 | Administración (5.1) |
| 3 | Estudios y turnos (5.2). Aquí el frontend de la 004 ya puede dejar MSW |
| 4 | Cuarteo, densidad, verificación, evidencias y alertas (5.3) |
| 5 | Matriz de permisos (CA-211), OpenAPI (CA-212), `.http`, README y revisión final |

## 9. Responsables (autor de los commits)

`src/TEAM.md` todavía describe la versión anterior (Swing y archivos planos). En la fase 0 se actualiza con este reparto, siguiendo lo que cada integrante ya tenía:

| Parte | Responsable |
| --- | --- |
| `pom.xml`, `config`, `security`, `.http`, README y documentación | Merlín (`mlanderoc`), arquitectura |
| Servicios (`service`) y sus pruebas, salvo los de Carlos | Estefanía (`EstefaniaMarquez`), servicios |
| `AlertService`, `QuarteringService` y `DensityService` y sus pruebas | Carlos (`craulgomez`), como en el reparto anterior (alertas y cuarteo) |
| Controladores, DTO y manejo de errores (`controller`, `dto`) y sus pruebas | Carlos (`craulgomez`), interfaz |
| Cambios en `exceptions` (`getProblemDescription`) | Merlín (`mlanderoc`) |
| Métodos nuevos de `UserRepository` | Merlín (`mlanderoc`) |

## 10. Riesgos y puntos a confirmar

| # | Tema | Propuesta |
| --- | --- | --- |
| R1 | **El frontend de la 004 usa tipos distintos al dominio** (tipos de fuente, estados, `name` en vez de `firstName`…) | Ya se le pidió a Antigravity que `domain.ts` sea un espejo del modelo. La API usa los nombres y valores del dominio |
| R2 | **El estudio no tiene nombre** en el dominio y el frontend lo muestra | Propuesta: la pantalla lo arma como "Estudio N.º {id} · {fecha del día 1}". Si el equipo quiere un nombre editable, se agrega al modelo y a la tabla `ESTUDIO` en esta spec |
| R3 | **CSRF obliga a cambiar el `apiClient`** de la 004 | Cambio de pocas líneas: leer la cookie `XSRF-TOKEN` y enviarla en `X-XSRF-TOKEN`. Se coordina con Antigravity |
| R4 | **El bloqueo por intentos se pierde al reiniciar** | Aceptable para un "Debería". Si se quiere persistente, dos columnas en `USUARIO` |
| R5 | **Fotos en disco local** | Suficiente para el proyecto. La carpeta queda fuera del repositorio (`.gitignore`) |
| R6 | **La spec es grande** (un solo PR) | Un commit por tarea y la revisión de Antigravity por fases, como en las specs 001 y 002 |
