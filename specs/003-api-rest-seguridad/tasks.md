# Tareas — Spec 003: API REST y seguridad

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (12 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/003-api-rest-seguridad` (desde `develop`) |

Reglas:

- Las tareas se hacen **en orden**. Cada una termina con su verificación cumplida y **un commit por responsable** (autor y committer según la sección 9 del plan).
- `./mvnw test` (unitarias) y `./mvnw verify` (integración contra `ecopoint_test`) deben pasar al cerrar cada tarea.
- Las pruebas se nombran con su criterio: `caNNN_descripcion()`.
- Antigravity revisa al terminar cada fase.

## Fase 0 — Base web

- [x] **T00. Reparto en `TEAM.md`.** Actualizar `src/TEAM.md` a la versión actual (React, PostgreSQL, sin Swing ni archivos planos) con los paquetes nuevos `config`, `security`, `service`, `controller` y `dto`, según la sección 9 del plan. *(Merlín)*
  *Verificación:* cada paquete del plan tiene un responsable en `TEAM.md`.

- [x] **T01. Servidor web.** `pom.xml` con `spring-boot-starter-web`, `spring-boot-starter-security`, `springdoc-openapi-starter-webmvc-ui` y `spring-security-test`. `application.properties`: sin `web-application-type=none`, puerto 8080, sesión de 8 h, cookie `HttpOnly` y `SameSite=Lax`, `ECOPOINT_FRONTEND_ORIGIN` y `ECOPOINT_UPLOAD_DIR`; Swagger apagado salvo en el perfil `dev` (`application-dev.properties`). `.env.example` y `.gitignore` (carpeta de fotos). *(Merlín)*
  *Verificación:* `./mvnw spring-boot:run` arranca en el puerto 8080; todas las pruebas existentes siguen en verde.

## Fase 1 — Seguridad y errores

- [x] **T02. Motivo del error sin prefijo.** `InvalidDataException.getProblemDescription()`, con prueba unitaria. *(Merlín)*
  *Verificación:* las pruebas actuales de `exceptions` y `validation` siguen pasando.

- [x] **T03. Contraseñas en el repositorio.** `UserRepository.findPasswordHashByEmail` y `updatePasswordHash` con su implementación JDBC y prueba de integración. *(Merlín)*
  *Verificación:* `CatalogAndPeopleRepositoryIT` cubre los dos métodos.

- [x] **T04. Formato de error.** `ClockConfig` y la configuración de JSON (fechas ISO y decimales sin notación científica) *(Merlín)*. DTO `ApiError`, `FieldErrorDto` y `PageResponse`; `ApiExceptionHandler` con la tabla de la sección 4.2 del plan, el diccionario de campos y el identificador en el log para los 500. *(Carlos)*
  *Verificación:* `@WebMvcTest` con un controlador de prueba: un caso por fila de la tabla; CA-208 (500 sin SQL ni traza).

- [x] **T05. Configuración de seguridad.** `SecurityConfig` (sesión, CSRF con `CookieCsrfTokenRepository`, CORS, rutas públicas: login y csrf), `EcoPointUserDetails`, `BCryptPasswordEncoder`, respuestas JSON para 401 y 403. *(Merlín)*
  *Verificación:* una ruta protegida sin sesión responde 401 con `ERR_UNAUTHORIZED` (CA-200); un `POST` sin cabecera CSRF responde 403.

- [x] **T06. Bloqueo por intentos.** `LoginAttemptService`: 5 fallos seguidos bloquean el correo 15 minutos; un acierto reinicia el contador. Con `Clock` para probar el paso del tiempo. *(Merlín)*
  *Verificación:* pruebas unitarias del bloqueo y del desbloqueo a los 15 minutos.

- [x] **T07. Servicio de autenticación.** `UnauthorizedException` (401) en la jerarquía de excepciones, documentada en `AGENTS.md` *(Merlín)*. `AuthService`: login con mensaje único (T7 del plan), usuario actual con su institución y sus permisos. *(Estefanía)*
  *Verificación:* pruebas unitarias con repositorios simulados: correo inexistente, contraseña errada, cuenta inactiva y cuenta bloqueada dan el mismo mensaje.

- [x] **T08. Rutas de autenticación.** `AuthController` (`login`, `logout`, `me`, `csrf`) y `UserResponse`. `AuthIT` contra la semilla. *(Carlos)*
  *Verificación:* CA-200, CA-201, CA-202 y el bloqueo de R-126 en `AuthIT`.

## Fase 2 — Administración

- [x] **T09. Servicios de institución y fuentes.** `InstitutionService` y `GenerationSourceService` (RN-001 a RN-005, permisos). *(Estefanía)*
  *Verificación:* pruebas unitarias, incluido el 403 para el estudiante y la fuente con bolsas que solo se desactiva.

- [x] **T10. Servicios de usuarios, cuadrillas y subcategorías.** `UserService` (contraseña con BCrypt, correo único RN-084), `CrewService` (RN-085, en una transacción) y `WasteCategoryService`. *(Estefanía)*
  *Verificación:* pruebas unitarias de cada regla.

- [x] **T11. Rutas de administración.** Controladores y DTO de `institutions`, `sources`, `users`, `crews` y `waste-categories`, con paginación de usuarios. `AdministrationIT` (incluye CA-210). *(Carlos)*
  *Verificación:* `@WebMvcTest` por controlador; CA-210 en `AdministrationIT`.

## Fase 3 — Estudios, turnos y bolsas

- [x] **T12. Servicio de estudios.** `StudyService`: crear con 8 días, cambiar fechas y fuentes, cambiar de estado (RN-014, RN-016) y reabrir con motivo, en transacciones. *(Estefanía)*
  *Verificación:* pruebas unitarias de cada transición permitida y rechazada.

- [x] **T13. Servicio de turnos.** `ShiftService`: abrir, cerrar (RN-024) y reabrir; población; "sin generación"; los 16 turnos posibles del estudio con su avance. *(Estefanía)*
  *Verificación:* pruebas unitarias, incluido cerrar con fuentes pendientes.

- [x] **T14. Servicio de bolsas.** `WasteBagService`: registrar (RN-023, rótulo, atípica con su alerta en la misma transacción) y corregir mientras esté PENDIENTE. *(Estefanía)*
  *Verificación:* pruebas unitarias; la alerta de valor atípico se crea junto con la bolsa.

- [x] **T15. Rutas de estudios, turnos y bolsas.** Controladores y DTO de la sección 5.2 del plan, con paginación de bolsas. *(Carlos)*
  *Verificación:* `FieldFlowIT`: CA-204, CA-205, CA-206 y CA-207, y el flujo completo: abrir turno → población → bolsa → sin generación → cerrar.

## Fase 4 — Cuarteo, verificación, evidencias y alertas

- [x] **T16. Cuarteo y densidad.** `QuarteringService` (calidad del 2 %, alerta si es NO_CONFORME, RN-056 para SIN_COLOR, anulación) y `DensityService`. *(Carlos)*
  *Verificación:* pruebas unitarias; cuarteo y resultados se guardan juntos o no se guarda nada.

- [x] **T17. Alertas.** `AlertService`: listar con filtros y paginación, atender con observación (RN-073). *(Carlos)*
  *Verificación:* pruebas unitarias.

- [x] **T18. Verificación y evidencias.** `VerificationService` (RN-062: quien verifica es distinto de quien digitó; corrección con historial en una transacción) y `EvidenceService` (5 MB, JPG o PNG, carpeta local). *(Estefanía)*
  *Verificación:* pruebas unitarias; la corrección guarda el valor anterior.

- [x] **T19. Rutas de la sección 5.3.** Controladores y DTO de cuarteo, densidad, verificaciones, historial, evidencias y alertas. *(Carlos)*
  *Verificación:* `@WebMvcTest` por controlador y una prueba de integración de verificar y corregir una bolsa.

## Fase 5 — Cierre

- [x] **T20. Matriz de permisos.** `PermissionMatrixIT`: por cada permiso, una ruta representativa con cada rol (sección 7 del plan). *(Carlos)*
  *Verificación:* CA-203, CA-204 y CA-211.

- [x] **T21. OpenAPI.** `OpenApiConfig` con título, versión y descripción de la sesión; `OpenApiIT` con el perfil `dev`. *(Merlín)*
  *Verificación:* CA-212: el documento tiene las rutas de las secciones 5.1 a 5.3.

- [ ] **T22. Ejemplos y documentación.** `docs/api/*.http` por recurso, README (cómo arrancar el backend y el frontend juntos) y `docs/layer-diagram.md` con las capas nuevas. *(Merlín)*
  *Verificación:* los `.http` corren contra la base de desarrollo con la semilla.

- [ ] **T23. Revisión final y PR.** Trazabilidad CA-200 a CA-212, `git grep` de contraseñas, `./mvnw verify` en verde, revisión de Antigravity y PR a `develop`. *(Merlín)*
  *Verificación:* veredicto "APROBADO PARA PR".
