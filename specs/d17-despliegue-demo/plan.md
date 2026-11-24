# Plan — D17: despliegue de demostración

| Campo | Valor |
| --- | --- |
| Estado | **PENDIENTE DE APROBACIÓN** del equipo |
| Spec | [spec.md](spec.md) |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo (pendiente) |
| Rama | `feature/d17-despliegue-demo` |
| Fecha | 2026-11-23 |

## 1. Arquitectura

```text
Navegador (HTTPS)
   │
   ▼
Frontend: Render Static Site (React compilado)
   │  /api/* se reenvía
   ▼
Backend: Render Web Service gratuito (Docker, Java 17)
   │  JDBC con SSL
   ▼
PostgreSQL: Neon, plan gratuito
```

## 2. Decisiones

| # | Decisión | Motivo |
| --- | --- | --- |
| P1 | Neon para la base de datos | PostgreSQL gratuito que no vence a los 30 días |
| P2 | Render para frontend y backend | Plan gratuito con despliegue desde GitHub |
| P3 | Una sola dirección pública mediante reenvío de `/api` | Conserva la sesión con cookie y la protección CSRF |
| P4 | Docker para el backend | Render no ofrece un entorno nativo de Java |
| P5 | Perfil `prod` de Spring | Separa producción sin alterar la configuración local |
| P6 | Pool de pocas conexiones y límite de memoria de la JVM | Se ajusta a los planes gratuitos |
| P7 | Contraseña nueva aplicada desde fuera del repositorio | Evita publicar la contraseña o su hash |

## 3. Cambios en el repositorio

| Archivo | Cambio |
| --- | --- |
| `Dockerfile` y `.dockerignore` | Compilan y ejecutan el backend. Excluyen `data/`, `target/`, PDF y archivos locales |
| `src/main/resources/application-prod.properties` | Cookies seguras, encabezados de proxy, pool pequeño y lectura de secretos por variables |
| `render.yaml` | Describe los dos servicios, el reenvío de `/api` y la ruta de React. Sin valores secretos |
| `docs/despliegue-demo.md` | Pasos de publicación, comprobación, plan B y apagado |
| `sql/preparar-demo-web.sh` | Carga esquema y demostración en la base indicada, y cambia las contraseñas sin escribirlas en archivos |

## 4. Variables de entorno

Se configuran en Render, sin escribir sus valores en el repositorio:

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `ECOPOINT_DB_URL` | Conexión JDBC a Neon con SSL |
| `ECOPOINT_DB_USER` | Usuario de Neon |
| `ECOPOINT_DB_PASSWORD` | Contraseña de Neon |
| `ECOPOINT_FRONTEND_ORIGIN` | Dirección pública de la demostración |

## 5. Riesgos

| Riesgo | Respuesta |
| --- | --- |
| El backend no cabe en la memoria gratuita | Ajustar JVM y pool. Si persiste, usar el plan B local |
| El reenvío de `/api` no conserva la sesión | Detener la tarea y proponer al equipo servir el frontend desde Spring Boot |
| La página está dormida al presentar | Abrirla 5 minutos antes |
| Falla internet en la feria | Presentar desde un computador con la aplicación local |
| Se filtra una contraseña | Cambiarla en Neon o en la aplicación y volver a desplegar |

## 6. Orden de trabajo

1. El equipo aprueba esta spec, el plan y las tareas.
2. Se fusiona a `develop` el PR de `fix/observaciones-pruebas`, para publicar la versión corregida.
3. Se implementan los archivos de la rama y se comprueban localmente.
4. El equipo crea las cuentas de Neon y Render, e ingresa los secretos en sus paneles.
5. Se publica y se comprueban los criterios CD-01 a CD-08.
