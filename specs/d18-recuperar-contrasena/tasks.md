# Tareas — D18: recuperar contraseña por correo

| Campo | Valor |
| --- | --- |
| Estado | **Propuesta**, pendiente de aprobación del equipo y revisión de Antigravity |
| Spec | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/d18-recuperar-contrasena` |

## Antes de implementar

- [ ] **T00. Aprobación del equipo** de spec, plan y tareas, y revisión de Antigravity.
- [ ] **T01. Cuenta de Brevo.** El equipo crea la cuenta, verifica el Gmail remitente y genera la clave de la API. *(Equipo)*

## Backend

- [ ] **T02. Base de datos.** Tabla `TOKEN_RECUPERACION` en `001_schema.sql` y script `006_recuperar_contrasena.sql` (RP-04). *(Merlín)*
- [ ] **T03. Modelo.** `PasswordResetToken` con vencimiento y uso único, y sus pruebas (RP-03). *(Estefanía)*
- [ ] **T04. Repositorio.** Guardar, buscar por hash, contar por hora y anular los de una cuenta, con pruebas de integración (RP-03, RP-05). *(Merlín)*
- [ ] **T05. Correo.** Paquete `notification` en `TEAM.md`, envío por Brevo y versión de consola solo fuera de `prod` (RP-09, RP-10, RP-11). *(Merlín)*
- [ ] **T06. Servicio.** Pedir y confirmar, con límite, cierre de sesiones y desbloqueo, y sus pruebas (RP-01 a RP-08). *(Estefanía)*
- [ ] **T07. API.** Endpoints `request` y `confirm`, rutas públicas con CSRF y pruebas CP-01 a CP-09. *(Carlos y Merlín)*

## Frontend

- [ ] **T08. Pantallas.** Enlace en el login, "Recuperar contraseña" y "Contraseña nueva", con simulaciones MSW y pruebas. *(Carlos)*

## Cierre

- [ ] **T09. Documentación.** Quitar la recuperación de "Fuera de alcance" en la spec 003 y agregar Brevo a `docs/despliegue-demo.md`. *(Merlín)*
- [ ] **T10. Verificación local.** Pasar backend y frontend completos y revisar que no haya secretos (CP-10).
- [ ] **T11. Publicación.** Ejecutar el script 006 en Neon, agregar las variables en Render, desplegar y probar con un correo real del equipo. *(Equipo con Claude)*
