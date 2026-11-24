# Spec D17: despliegue de demostración

| Campo | Valor |
| --- | --- |
| Estado | **PENDIENTE DE APROBACIÓN** del equipo |
| Tipo | Ajuste de infraestructura. No cambia reglas de negocio |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo (pendiente) |
| Rama | `feature/d17-despliegue-demo` |
| Fecha | 2026-11-23 |

## 1. Qué y por qué

EcoPoint Edu solo funciona en el computador de cada integrante. Para la sustentación de ExpoSoftware del 26 de noviembre de 2026, el equipo necesita abrirlo desde un enlace público, en computador y celular, sin pagar.

Este despliegue es una **demostración con datos ficticios**. No es todavía el sistema en uso real de la I. E. Prudencia Daza.

## 2. Alcance

**Incluye:**

- Publicar frontend y backend en Render, en planes gratuitos.
- Crear la base de datos en Neon, en plan gratuito.
- Cargar el esquema y los datos de demostración, con contraseñas nuevas.
- Configurar la aplicación para producción con HTTPS y cookies seguras.
- Documentar cómo publicar, comprobar y apagar la demostración.

**No incluye:**

- Uso real ni datos reales de personas o de la institución.
- Pantalla o almacenamiento permanente de evidencias.
- Plano interactivo (spec 007).
- Planes de pago, dominio propio ni copias de seguridad automáticas.

## 3. Historia de usuario

**HU-D17.** Como integrante del equipo, quiero abrir EcoPoint Edu desde un enlace público, para demostrar el flujo completo en la sustentación.

## 4. Requisitos

- **RD-01.** La aplicación se abre desde una sola dirección HTTPS. El frontend reenvía `/api` al backend, para que la sesión y la protección CSRF funcionen.
- **RD-02.** Contraseñas, cadena de conexión y demás secretos solo se guardan en los paneles de Render y Neon. Nunca van en el repositorio, en archivos del proyecto ni en el chat.
- **RD-03.** Las cuentas publicadas no usan la contraseña de demostración que aparece en el repositorio.
- **RD-04.** La base publicada solo contiene datos ficticios.
- **RD-05.** En producción, la cookie de sesión es `Secure` y `HttpOnly`, y la API solo acepta el origen de la demostración.
- **RD-06.** La configuración local sigue funcionando igual que antes.
- **RD-07.** La documentación indica el plan B: presentar desde un computador con la aplicación local.

## 5. Limitaciones aceptadas

- El backend se duerme tras 15 minutos sin visitas y tarda cerca de un minuto en despertar. Se debe abrir unos 5 minutos antes de presentar.
- Al dormirse o reiniciarse, se cierran las sesiones. Los datos permanecen en Neon.
- La memoria del plan gratuito es limitada para Spring Boot. Se debe comprobar con la aplicación publicada.
- Un archivo enviado directamente a la API de evidencias no es permanente. La aplicación no ofrece pantalla para subirlos.

## 6. Criterios de aceptación

- **CD-01.** Dado el enlace público, cuando se abre en computador y celular, entonces carga la página de bienvenida por HTTPS.
- **CD-02.** Dadas las cuentas de docente y estudiante, cuando inician sesión con la contraseña nueva, entonces entran con los permisos de su rol y la sesión se mantiene al recargar.
- **CD-03.** Dada la contraseña de demostración del repositorio, cuando se intenta iniciar sesión en la web, entonces se rechaza.
- **CD-04.** Dada una petición que modifica datos sin token CSRF, cuando llega a la API, entonces se rechaza.
- **CD-05.** Dado el docente, cuando crea un estudio y completa registro, cuarteo, verificación y cierre, entonces los indicadores se muestran y se descargan los reportes Excel y PDF.
- **CD-06.** Dado un reinicio del backend, cuando vuelve a iniciar, entonces los datos guardados siguen disponibles.
- **CD-07.** Dado el repositorio, cuando se revisan los cambios, entonces no contiene secretos.
- **CD-08.** Dado un entorno local, cuando se ejecutan `./mvnw verify` y las pruebas del frontend, entonces pasan completas.
