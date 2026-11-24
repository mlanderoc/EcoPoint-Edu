# Tareas — D17: despliegue de demostración

| Campo | Valor |
| --- | --- |
| Estado | **PENDIENTE DE APROBACIÓN** del equipo |
| Spec | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/d17-despliegue-demo` |

## Antes de implementar

- [ ] **T00. Aprobación del equipo** de spec, plan y tareas, y revisión de Antigravity.
- [ ] **T01. Fusionar** el PR de `fix/observaciones-pruebas` en `develop` y actualizar esta rama.

## Preparación del proyecto

- [ ] **T02. Docker.** Crear `Dockerfile` y `.dockerignore`, y comprobar que el backend inicia con PostgreSQL local (RD-06). *(Merlín)*
- [ ] **T03. Perfil de producción.** Crear `application-prod.properties` con cookies seguras, proxy y pool pequeño (RD-05). *(Merlín)*
- [ ] **T04. Render.** Crear `render.yaml` con los servicios, el reenvío de `/api` y la ruta de React (RD-01). *(Merlín)*
- [ ] **T05. Datos de la demostración.** Crear `sql/preparar-demo-web.sh`, que cambia las contraseñas sin guardarlas en archivos (RD-02, RD-03, RD-04). *(Merlín)*
- [ ] **T06. Documentación.** Crear `docs/despliegue-demo.md`, con publicación, comprobación, plan B y apagado (RD-07). *(Merlín)*
- [ ] **T07. Verificación local.** Pasar backend y frontend completos, y revisar que no haya secretos (CD-07, CD-08).

## Publicación

- [ ] **T08. Cuentas.** El equipo crea Neon y Render, y conecta el repositorio. *(Equipo)*
- [ ] **T09. Base de datos.** Cargar la demostración en Neon con contraseñas nuevas. *(Equipo con Claude)*
- [ ] **T10. Backend.** Publicar y comprobar la conexión con Neon (CD-06). *(Equipo con Claude)*
- [ ] **T11. Frontend.** Publicar y comprobar la dirección única (CD-01). *(Equipo con Claude)*

## Comprobación final

- [ ] **T12. Seguridad.** Comprobar inicio de sesión, contraseña antigua y CSRF (CD-02, CD-03, CD-04).
- [ ] **T13. Flujo completo.** Crear, registrar, cuartear, verificar, cerrar y descargar reportes en computador y celular (CD-05).
- [ ] **T14. Ensayo.** Abrir la página dormida, medir cuánto tarda y practicar el plan B local.
