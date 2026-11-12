# Ejemplos de la API

Peticiones de ejemplo para cada recurso de la API (spec 003, R-131). Se ejecutan con el cliente HTTP de IntelliJ IDEA o con la extensión **REST Client** de VS Code, contra la base de desarrollo con los datos de demostración.

| Archivo | Recursos |
| --- | --- |
| [`01-sesion.http`](01-sesion.http) | Token CSRF, inicio y cierre de sesión, usuario actual |
| [`02-administracion.http`](02-administracion.http) | Instituciones, fuentes, usuarios, cuadrillas y subcategorías |
| [`03-estudios-turnos.http`](03-estudios-turnos.http) | Estudios, turnos, población, bolsas y "sin generación" |
| [`04-calidad.http`](04-calidad.http) | Cuarteo, densidad, verificación, historial, alertas y fotos |

## Antes de empezar

1. Recrea la base de desarrollo: `.\sql\recrear-base.ps1`.
2. Arranca el backend con el perfil de desarrollo:
   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "dev"
   .\mvnw.cmd spring-boot:run
   ```
3. Ejecuta primero `01-sesion.http`, en orden:
   - `GET /auth/csrf` entrega la cookie `XSRF-TOKEN`. Copia su valor en la variable `@xsrf` de cada archivo (los dos clientes guardan las cookies, pero la cabecera hay que enviarla a mano).
   - `POST /auth/login` abre la sesión; la cookie `JSESSIONID` queda guardada en el cliente.

Toda petición `POST`, `PUT`, `PATCH` o `DELETE` lleva la cabecera `X-XSRF-TOKEN: {{xsrf}}`; sin ella la API responde 403.

La documentación completa de cada ruta, con sus JSON, está en Swagger UI: http://localhost:8080/swagger-ui.html (solo con el perfil `dev`).

Usuarios de demostración (contraseña `EcoPoint2026`): `docente.demo@unicesar.edu.co` (DOCENTE) y `estudiante.demo@unicesar.edu.co` (ESTUDIANTE_UNIVERSITARIO).
