# Plan — D18: recuperar contraseña por correo

| Campo | Valor |
| --- | --- |
| Estado | **Propuesta**, pendiente de aprobación del equipo y revisión de Antigravity |
| Spec | [spec.md](spec.md) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo (pendiente) |
| Rama | `feature/d18-recuperar-contrasena` |
| Fecha | 2026-11-24 |

## 1. Flujo

```text
1. Login → "¿Olvidaste tu contraseña?" → escribe su correo
2. POST /api/v1/auth/password-reset/request   → 202 siempre
3. Backend: si la cuenta existe, está activa y no pasó el límite,
   crea el código, guarda su hash y envía el correo por Brevo
4. Correo → <frontend>/restablecer-contrasena?token=...
5. POST /api/v1/auth/password-reset/confirm   → 204, o 400 si el enlace no sirve
6. Backend: guarda la contraseña, marca el código como usado,
   anula los demás, cierra las sesiones y quita el bloqueo
```

## 2. Decisiones

| # | Decisión | Motivo |
| --- | --- | --- |
| P1 | Brevo por API HTTPS | Gratuito, permite remitente Gmail sin dominio propio y evita el bloqueo de SMTP en Render |
| P2 | Código de 32 bytes aleatorios (`SecureRandom`), guardado como SHA-256 | Si se filtra la base, los códigos no sirven |
| P3 | El correo se envía después de responder | El tiempo de respuesta no revela si la cuenta existe |
| P4 | Interfaz `PasswordResetMailer` con dos implementaciones | El servicio no depende de Brevo (Variaciones Protegidas). La de consola solo existe fuera del perfil `prod` |
| P5 | Límite por cuenta contado en la misma tabla | No necesita infraestructura nueva |
| P6 | `Referrer-Policy: no-referrer` en la pantalla del enlace | El código no sale de la página hacia otros sitios |
| P7 | Las rutas nuevas son públicas pero conservan CSRF | Igual que el inicio de sesión |

## 3. Cambios en el repositorio

| Parte | Cambio | Responsable |
| --- | --- | --- |
| `sql/001_schema.sql` y `sql/006_recuperar_contrasena.sql` | Tabla `TOKEN_RECUPERACION` (usuario, hash único, creado, vence, usado). El 006 la agrega a bases existentes, como la de Neon | Merlín |
| `model` | `PasswordResetToken`: vencido, usado, `use(at)` que lanza excepción si no sirve | Estefanía |
| `persistence` | `PasswordResetTokenRepository` y su versión JDBC. `UserRepository` agrega la búsqueda por correo si falta | Merlín |
| `service` | `PasswordResetService`: pedir y confirmar | Estefanía |
| `notification` (paquete nuevo) | `BrevoPasswordResetMailer` y `ConsolePasswordResetMailer`. Se agrega a `TEAM.md` | Merlín |
| `security` y `config` | Rutas públicas, cierre de sesiones, desbloqueo y variables del correo | Merlín |
| `controller` y `dto` | Dos endpoints y sus DTO | Carlos |
| `frontend` | Enlace en el login y pantallas "Recuperar contraseña" y "Contraseña nueva", con sus simulaciones MSW | Carlos |
| `specs` y `docs` | Spec 003 sin la recuperación en "Fuera de alcance". Guía de Brevo en `docs/despliegue-demo.md` | Merlín |

## 4. Variables de entorno

| Variable | Uso |
| --- | --- |
| `ECOPOINT_MAIL_API_KEY` | Clave de la API de Brevo. Solo en el panel de Render |
| `ECOPOINT_MAIL_FROM` | Correo remitente verificado en Brevo |
| `ECOPOINT_FRONTEND_ORIGIN` | Ya existe. Se usa para armar el enlace |

## 5. Riesgos

| Riesgo | Respuesta |
| --- | --- |
| Los correos llegan a spam | Avisarlo en la pantalla y probar con correos institucionales antes de la sustentación |
| No alcanza para el 26 de noviembre | Se presenta sin esta función. No afecta el resto |
| Se filtra la clave de Brevo | Revocarla en Brevo, crear otra y actualizarla en Render |
| Alguien gasta los 300 correos diarios | El límite por cuenta lo reduce. Si pasa, la función se recupera al día siguiente |
