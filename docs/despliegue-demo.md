# Despliegue de la demostración web (D17)

Publica EcoPoint Edu con **datos ficticios** en Render y Neon, en planes gratuitos. Ninguna contraseña ni cadena de conexión se escribe en este repositorio.

## 1. Preparar la base en Neon

1. En Neon, abre **Conectar** y copia la cadena de conexión.
2. En Git Bash, desde la raíz del repositorio, ejecuta `./sql/preparar-demo-web.sh`.
3. Pega la cadena y escribe la contraseña nueva de las cuentas demo. No se muestran en pantalla.

El script solo funciona sobre una base vacía.

## 2. Publicar el backend

En Render, crea un **Web Service** desde *Public Git Repository*:

| Campo | Valor |
| --- | --- |
| Repositorio | `https://github.com/mlanderoc/EcoPoint-Edu` |
| Rama | `develop` |
| Nombre | `ecopoint-edu-api` |
| Región | Ohio |
| Runtime | Docker |
| Plan | Free |
| Health Check Path | `/api/v1/auth/csrf` |

Variables de entorno:

| Variable | Valor |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `ECOPOINT_DB_URL` | `jdbc:postgresql://<host-de-neon>/<base>?sslmode=require` |
| `ECOPOINT_DB_USER` | Usuario de Neon |
| `ECOPOINT_DB_PASSWORD` | Contraseña de Neon |
| `ECOPOINT_FRONTEND_ORIGIN` | Dirección pública del frontend |
| `ECOPOINT_MAIL_API_KEY` | Clave de la API de Brevo, para recuperar contraseñas (D18) |
| `ECOPOINT_MAIL_FROM` | Correo remitente verificado en Brevo (D18) |

### Correo para recuperar contraseñas (D18)

1. Crea una cuenta gratuita en Brevo y verifica el Gmail del equipo en **Senders**.
2. En **SMTP & API → API Keys**, genera una clave y pégala solo en `ECOPOINT_MAIL_API_KEY` de Render.
3. En Neon, ejecuta una vez `sql/006_recuperar_contrasena.sql` en el **SQL Editor**.
4. Crea en la aplicación un usuario con el correo real de un integrante y prueba "¿Olvidaste tu contraseña?". Si el correo no llega, revisa spam.

Sin estas dos variables, la web no envía correos. En un computador local, el enlace aparece en la consola del backend.

## 3. Publicar el frontend

Crea un **Static Site** con el mismo repositorio y la misma rama:

| Campo | Valor |
| --- | --- |
| Nombre | `ecopoint-edu` |
| Build Command | `cd frontend && npm ci --include=dev && npm run build` |
| Publish Directory | `frontend/dist` |
| `VITE_USE_MOCK_API` | `false` |
| `NODE_VERSION` | `22` |

En **Redirects/Rewrites**, agrega en este orden:

| Origen | Destino | Acción |
| --- | --- | --- |
| `/api/*` | `https://<backend>.onrender.com/api/*` | Rewrite |
| `/*` | `/index.html` | Rewrite |

Después, actualiza `ECOPOINT_FRONTEND_ORIGIN` en el backend con la dirección real del frontend.

## 4. Comprobar

- La página abre por HTTPS en computador y celular.
- Docente y estudiante entran con la contraseña nueva y la sesión sigue al recargar.
- `EcoPoint2026` ya no permite entrar.
- Se completa un estudio y se descargan los reportes Excel y PDF.
- Después de reiniciar el backend, los datos siguen ahí.

## 5. Día de la sustentación

- Abre la página **5 minutos antes**, porque el backend se duerme tras 15 minutos sin visitas.
- **Plan B:** lleva la aplicación funcionando en un computador con la base local.

## 6. Apagar

En Render, suspende o elimina los dos servicios. En Neon, elimina el proyecto si ya no se usa.
