# Spec D18: recuperar contraseña por correo

| Campo | Valor |
| --- | --- |
| Estado | **Propuesta**, pendiente de aprobación del equipo y revisión de Antigravity |
| Tipo | Función nueva de seguridad. Amplía la spec 003 |
| Redacta / implementa | Claude |
| Revisa | Antigravity y equipo (pendiente) |
| Rama | `feature/d18-recuperar-contrasena` |
| Fecha | 2026-11-24 |

## 1. Qué y por qué

Hoy, si un usuario olvida su contraseña, no puede recuperarla: el sistema solo permite cambiarla conociendo la actual (R-132). Los usuarios serán estudiantes universitarios de Ingeniería Ambiental con correo institucional, así que cada uno puede recuperar su acceso por correo, sin depender del docente.

Esta spec saca de "Fuera de alcance" de la spec 003 la recuperación de contraseña por correo.

## 2. Alcance

**Incluye:**

- Enlace "¿Olvidaste tu contraseña?" en el inicio de sesión.
- Envío de un correo con un enlace de un solo uso para crear una contraseña nueva.
- Pantalla para escribir la contraseña nueva desde ese enlace.
- Envío de correos con un servicio gratuito (Brevo) por su API HTTPS.

**No incluye:**

- Verificar el correo al crear un usuario ni cambiar el correo de una cuenta.
- Inicio de sesión con Google o Microsoft, ni recuperación por SMS.
- Restablecer la contraseña desde el administrador.

## 3. Historia de usuario

**HU-D18.** Como usuario que olvidó su contraseña, quiero recibir un enlace en mi correo para crear una nueva, para volver a entrar sin pedir ayuda.

## 4. Requisitos

- **RP-01.** Cualquier persona puede pedir el enlace escribiendo un correo. La respuesta es siempre la misma, exista o no la cuenta: "Si el correo está registrado, te llegará un enlace". Así no se revela quién tiene cuenta.
- **RP-02.** Solo se envía el correo si la cuenta existe y está activa.
- **RP-03.** El enlace contiene un código aleatorio de un solo uso, vence a los **30 minutos** y deja de servir al usarse. Al pedir uno nuevo, los anteriores de esa cuenta dejan de servir.
- **RP-04.** La base de datos guarda solo el hash del código, nunca el código.
- **RP-05.** Una cuenta recibe como máximo **3 enlaces por hora**. Las peticiones de más reciben la misma respuesta de RP-01, sin enviar correo.
- **RP-06.** La contraseña nueva cumple las mismas reglas que al crear el usuario (D13).
- **RP-07.** Al cambiar la contraseña con el enlace se cierran todas las sesiones abiertas de esa cuenta y se quita el bloqueo por intentos fallidos (R-126). El usuario vuelve al inicio de sesión para entrar con la contraseña nueva.
- **RP-08.** Un enlace vencido, usado o inventado muestra "El enlace no es válido o ya venció" y ofrece pedir uno nuevo.
- **RP-09.** El correo está en español. Dice para qué es, que vence en 30 minutos y que, si no lo pidió, puede ignorarlo. No incluye la contraseña ni otros datos de la cuenta.
- **RP-10.** La clave del servicio de correo y el remitente se configuran por variables de entorno. Nunca van en el repositorio ni en el chat.
- **RP-11.** En el computador local, sin servicio de correo configurado, el enlace se muestra en la consola del backend para poder probar. En producción esto está prohibido: sin servicio de correo, el backend no envía nada y lo registra como error, sin mostrar el enlace.

## 5. Limitaciones aceptadas

- El plan gratuito de Brevo permite 300 correos al día.
- El remitente es un Gmail verificado del equipo, sin dominio propio. Algunos correos pueden llegar a spam; la pantalla lo advierte.
- Los usuarios de la demostración tienen correos ficticios. Para probar en la web se crea una cuenta con el correo real de un integrante del equipo. Esto amplía RD-04 de la D17 solo para esas cuentas.

## 6. Criterios de aceptación

- **CP-01.** Dado un correo registrado y activo, cuando se pide el enlace, entonces llega un correo con un enlace que abre la pantalla de contraseña nueva.
- **CP-02.** Dado un correo no registrado o de una cuenta inactiva, cuando se pide el enlace, entonces la respuesta es la misma de CP-01 y no se envía correo.
- **CP-03.** Dado un enlace válido, cuando se escribe una contraseña nueva que cumple las reglas, entonces se guarda, el usuario entra con ella y la contraseña anterior deja de servir.
- **CP-04.** Dado un enlace ya usado, vencido o inventado, cuando se intenta usar, entonces se rechaza con el mensaje de RP-08.
- **CP-05.** Dado que se pidió un segundo enlace, cuando se usa el primero, entonces se rechaza.
- **CP-06.** Dada una cuenta con sesiones abiertas, cuando cambia la contraseña con el enlace, entonces esas sesiones se cierran.
- **CP-07.** Dada una cuenta que ya recibió 3 enlaces en la última hora, cuando pide otro, entonces recibe la misma respuesta y no se envía correo.
- **CP-08.** Dada una contraseña nueva que no cumple las reglas, cuando se envía, entonces se rechaza con el mismo mensaje que al crear el usuario y el enlace sigue sirviendo.
- **CP-09.** Cuando se consulta la base de datos, entonces ningún código de recuperación está en texto plano.
- **CP-10.** Dado el repositorio, cuando se revisan los cambios, entonces no contiene la clave del servicio de correo.
