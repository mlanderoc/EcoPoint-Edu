# Frontend de EcoPoint Edu

Aplicación web en **React + Vite + TypeScript** para el registro en campo de la caracterización de residuos (spec [004](../specs/004-registro-en-campo/spec.md)) y su control de calidad (spec [005](../specs/005-cuarteo-control-calidad/spec.md)). Consume la API REST de la spec [003](../specs/003-api-rest-seguridad/spec.md).

| Pantalla | Ruta | Quién |
| --- | --- | --- |
| Inicio de sesión | `/login` | Todos |
| Registro en campo: estudio en ejecución, día y turno, abrir o continuar | `/campo` | Todos (abrir turno: docente) |
| Turno: población, fuentes, "sin generación" y cierre | `/campo/turno/:id` | Todos (cerrar: docente) |
| Población del turno | `/campo/turno/:id/poblacion` | Todos |
| Pesaje de bolsa con rótulo gigante | `/campo/turno/:id/bolsas/nueva` | Todos |
| Cuarteo con control del 2 % y composición | `/campo/turno/:id/cuarteo` | Todos (anular: docente) |
| Densidad del turno | `/campo/turno/:id/densidad` | Todos |
| Verificación del doble registro por turno | `/calidad/verificacion` | Quien no digitó el registro |
| Registro: corrección con motivo e historial | `/calidad/registro/:tipo/:id` | Quien no digitó el registro |
| Alertas de calidad y su atención | `/alertas` | Docente |
| Estudios: lista, nuevo y ciclo de vida | `/estudios`, `/estudios/nuevo`, `/estudios/:id` | Docente |
| Administración: instituciones, fuentes, usuarios, cuadrillas y subcategorías | `/admin/*` | Docente |

## 1. Arrancar

Requiere **Node.js 20 o superior**.

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

Por defecto la aplicación usa **datos simulados** (Mock Service Worker) con el mismo contrato de la API real, así que funciona sin backend. Usuarios: `docente.demo@unicesar.edu.co` y `estudiante.demo@unicesar.edu.co`, contraseña `EcoPoint2026`.

### Con la API real

1. Prepara la base y arranca el backend (ver el [README principal](../README.md)): `./mvnw spring-boot:run` en la raíz.
2. Crea `frontend/.env.local` a partir de [`.env.example`](.env.example) con `VITE_USE_MOCK_API=false`.
3. `npm run dev`. Vite reenvía `/api` a `http://localhost:8080` (cambia el destino con `VITE_API_TARGET`), así el navegador y la API comparten origen y las cookies de sesión y CSRF funcionan sin CORS.

| Variable | Para qué | Por defecto |
| --- | --- | --- |
| `VITE_USE_MOCK_API` | `false` usa la API real | datos simulados |
| `VITE_API_TARGET` | Backend al que Vite reenvía `/api` | `http://localhost:8080` |

## 2. Probar

```bash
npm test             # pruebas con Vitest y Testing Library (CA-300 a CA-310)
npm run typecheck    # tipos de TypeScript
npm run lint         # oxlint
npm run build        # compilación de producción en dist/
npm run check:mobile # con npm run dev corriendo: Chrome real a 360 px (CA-307)
```

`check:mobile` abre las pantallas de campo en un celular de 360 px, falla si hay desplazamiento horizontal o controles de menos de 44 px y guarda capturas en `mobile-check/`. Usa Chrome en su ruta habitual de Windows; cámbiala con `CHROME_PATH`, y la dirección con `CHECK_BASE_URL`.

## 3. Cómo está organizado

```
src/
├── components/   layout (menú lateral, barra inferior), ui (botones, campos, avisos, Atrás, filtro de activos), waste (grupos de residuos)
├── hooks/        useUnsavedChanges: pregunta "¿Descartar los cambios?" antes de salir de un formulario
├── features/
│   ├── auth/     sesión, permisos RN-081 y guardas de ruta
│   ├── admin/    administración (spec 004, fase 3)
│   ├── study/    estudios y su ciclo de vida (fase 4)
│   ├── field/    registro en campo (fase 5)
│   ├── quality/  cuarteo, densidad, doble registro y alertas (spec 005)
│   └── profile/  Mi Perfil: datos, registros propios y cambio de contraseña (ajustes D13)
├── mocks/        API simulada con MSW (mismo contrato que la spec 003)
├── services/     cliente HTTP: cookies, token CSRF y errores en español
└── types/        tipos iguales a los DTO de la API
```

- **Sesión:** cookie `HttpOnly` del servidor; el frontend nunca guarda tokens. El cliente pide `GET /api/v1/auth/csrf` y envía `X-XSRF-TOKEN` en cada `POST`, `PUT`, `PATCH` y `DELETE`.
- **Errores:** la API responde siempre `{ status, errorCode, message, fieldErrors }`; los formularios muestran `fieldErrors` debajo del campo y el resto en un aviso.
- **Campo:** botones y campos de al menos 48 px, teclado numérico con coma o punto decimal, y un solo envío ante doble toque.
- **Ajustes D13:**
  - las fechas del estudio se recalculan con días hábiles y festivos de Colombia (`features/study/utils/holidays.ts`);
  - Instituciones, Fuentes y Subcategorías se activan, desactivan y filtran (Activos / Inactivos / Todos, en la URL con `?estado=`);
  - Usuarios es una tabla con filtros, buscador y paginación del servidor;
  - las pantallas internas tienen Atrás y los formularios Guardar y Descartar;
  - ninguna pantalla muestra códigos de reglas, y la prueba `no-rule-codes.test.tsx` lo vigila.
