# Tareas — Spec 004: Configuración del estudio y registro en campo

| Campo | Valor |
| --- | --- |
| Estado | **APROBADO** (11 nov 2026) |
| Plan | [plan.md](plan.md) |
| Rama | `feature/004-registro-en-campo` (desde `develop`) |
| Responsable | Carlos Raúl Gómez Galvis (`craulgomez@unicesar.edu.co`) |

---

## Reglas de Implementación

1. **Orden estricto:** Las tareas se ejecutan en secuencia ordenada. Cada tarea concluye con su verificación automatizada exitosa.
2. **Commits atómicos:** Un commit por tarea con mensaje semántico en español (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
3. **Autoría obligatoria:** Todos los commits deben registrar como autor y committer a Carlos Gómez:
   `Carlos Gomez <craulgomez@unicesar.edu.co>` (sin menciones de IA ni coautorías en el commit o PR).
4. **Cero regresiones:** Ninguna tarea se marca completada (`[x]`) si las pruebas de Vitest (`npm test`) o el linter fallan.

---

## Fase 0 — Scaffolding, Tooling y Mock API

- [x] **T300. Scaffolding del proyecto frontend.**
  Inicializar la carpeta `frontend/` con Vite 8, React 19 y TypeScript 7 en modo estricto. Configurar `tsconfig.json` con alias de importación `@/*` y `vite.config.ts`.
  *Verificación:* `cd frontend && npm run build` genera el bundle sin advertencias ni errores.

- [x] **T301. Configuración de Tailwind CSS v4 y tokens de diseño.**
  Instalar y configurar Tailwind CSS v4 con `@tailwindcss/vite`. Definir la paleta de colores del logo de EcoPoint Edu (Verde Eco `#1E8449`, Verde Pino `#0F3D23`, Hoja `#8BC34A`, Azul del texto 'Edu' `#2F5DA8`), badges de la Res. 2184/2019 (Blanco, Verde Orgánico `#27AE60`, Negro, Sin color) y variables CSS para alto contraste solar.
  *Verificación:* Componente de prueba visualiza correctamente los colores institucionales y clases de utilidad de Tailwind v4.

- [x] **T302. Infraestructura de pruebas con Vitest y Testing Library.**
  Configurar Vitest, `@testing-library/react`, `@testing-library/jest-dom` y `jsdom`. Crear un test básico de humo.
  *Verificación:* `npm test` ejecuta y pasa la prueba inicial en verde.

- [x] **T303. Tipos TypeScript del dominio y contratos de la API.**
  Definir en `src/types/` las interfaces de entidades (`User`, `UserRole`, `Institution`, `GenerationSource`, `Study`, `SamplingDay`, `Shift`, `WasteBag`, `WasteGroup`) y el contrato de error de la Spec 003 (`ErrorResponse`, `FieldError`).
  *Verificación:* `npm run typecheck` valida los tipos sin inconsistencias.

- [x] **T304. Mock Service Worker (MSW v3) con simulación de sesión por cookie.**
  Instalar MSW v3 y configurar `src/mocks/browser.ts` y `src/mocks/handlers.ts`. Simular la autenticación mediante cookie de sesión `HttpOnly` en memoria evaluando `withCredentials` (sin tokens en LocalStorage). Cargar datos semilla realistas de la I.E. Prudencia Daza (fuentes `AU`, `BA`, `CAF`, `OF`, etc., usuarios docente y estudiante, estudio de ejemplo). Configurar interceptor para simular latencia de red y errores controlados (400, 401, 403, 404, 422).
  *Verificación:* En el navegador, las peticiones `fetch('/api/v1/auth/me', { credentials: 'include' })` y `fetch('/api/v1/institutions')` son interceptadas y devuelven los datos simulados con status 200 tras login.

---

## Fase 1 — Sistema de Diseño, Componentes Base y Layout Adaptativo

- [x] **T305. Componentes atómicos accesibles con touch target ≥ 48 px (R-300, R-301, R-302).**
  Crear componentes UI reutilizables en `src/components/ui/`: `Button` (con variantes, estado `loading` con spinner y altura mínima 48 px), `Input` (soporte `inputMode="decimal"`, formateo de coma/punto, manejo de error en borde rojo), `Select`, `Card`, `Modal` (accesible con Radix UI Dialog) y `AlertBadge`.
  *Verificación:* Pruebas unitarias en Vitest confirmando dimensiones mínimas $\ge 48\text{ px}$ y aceptación de separador decimal punto y coma.

- [x] **T306. Badges y chips de grupos de residuos (R-304).**
  Crear `WasteGroupBadge` y `WasteGroupChip` que representen los 4 grupos de la Res. 2184/2019 (Blanco, Verde Orgánico `#27AE60`, Negro, Sin color), asegurando que siempre incluyan el nombre textual visible junto al color y que se usen exclusivamente como chips informativos, nunca como botones.
  *Verificación:* Pruebas unitarias validando que el texto de cada grupo de residuo esté presente en el DOM y con contraste WCAG adecuado.

- [x] **T307. Layout principal adaptativo y menú según rol (R-311, CA-300, CA-307).**
  Crear `AppLayout`, `Header` con datos del usuario actual y botón de cierre de sesión, `Sidebar` para pantallas de escritorio/tableta y `BottomNav` para dispositivos móviles (< 768 px, optimizado para 360 px sin scroll horizontal).
  *Verificación:* `test('ca307_mobile360ViewportNoHorizontalScroll')` verifica que no exista desplazamiento horizontal en viewport de 360 px y que los botones de navegación sean accesibles.

---

## Fase 2 — Autenticación, Sesión y Control de Acceso

- [x] **T308. Cliente HTTP base con withCredentials y normalizador de errores (R-303).**
  Crear `src/services/apiClient.ts` con Axios o Fetch configurado con `withCredentials: true` para envío de cookies de sesión `HttpOnly` (sin manejo de tokens en LocalStorage). Normalizar las respuestas con el formato de la Spec 003 (`{ timestamp, status, errorCode, message, path, fieldErrors }`) y notificar vencimiento de sesión ante error 401.
  *Verificación:* Pruebas unitarias de cliente HTTP verificando la propagación de mensajes en español y la lista de `fieldErrors`.

- [x] **T309. Contexto de autenticación y pantalla de inicio de sesión (R-310, R-312).**
  Crear `AuthContext`, hook `useAuth` y la vista `/login` con formulario de correo y contraseña. Al autenticar con éxito, sincronizar con `GET /api/v1/auth/me`. Soporte para el aviso "Su sesión expiró" tras recibir error 401.
  *Verificación:* `test('loginFormSuccessAndSessionHandling')` valida el inicio de sesión exitoso y la sincronización con `/auth/me`.

- [x] **T310. Guardas de ruta por rol y visibilidad condicional (CA-300, R-311).**
  Implementar `RequireAuth`, `RequireRole` y `RequirePermission`. Restringir vistas administrativas y de configuración únicamente al rol `DOCENTE`. En el rol `ESTUDIANTE_UNIVERSITARIO`, ocultar los enlaces de administración y deshabilitar/ocultar botones de abrir y cerrar turnos, dejando la arquitectura preparada para las acciones de campo avanzadas (Specs 005/006).
  *Verificación:* `test('ca300_studentMenuAndButtonsRestricted')` confirma que el estudiante no ve opciones administrativas ni botones de turno.

---

## Fase 3 — Módulo de Administración (Solo Docente)

- [x] **T311. Administración de instituciones y fuentes de generación (R-320, R-321).**
  Vistas `/admin/instituciones` y `/admin/fuentes`. Listado, creación y edición de fuentes con código (ej. `AU`), nombre, tipo (Aulas, Oficinas, etc.) y estado activo/inactivo.
  *Verificación:* Pruebas de integración validando el listado y alta de fuentes vinculadas a una institución.

- [x] **T312. Administración de usuarios y validación de correo único (R-322, CA-302).**
  Vista `/admin/usuarios`. Formulario para registrar docentes y estudiantes. Si el correo institucional ya existe, capturar el error de API y mostrar el mensaje en rojo directamente bajo el campo correo.
  *Verificación:* `test('ca302_duplicateEmailDisplaysFieldError')` simula respuesta 400 con `fieldErrors: [{ field: 'email' }]` y confirma la marcación del campo.

- [x] **T313. Administración de cuadrillas y subcategorías (R-323, R-324).**
  Vistas `/admin/cuadrillas` (asignación de docente responsable obligatoria con alerta preventiva RN-085) y `/admin/subcategorias` (clasificación por grupos de color).
  *Verificación:* Pruebas de integración validando advertencia visual si falta el docente responsable en la cuadrilla.

---

## Fase 4 — Configuración del Estudio de Caracterización

- [ ] **T314. Planificador de fechas y propuesta de 7 días hábiles (R-330, CA-301).**
  Implementar la utilidad `calculateSamplingDays(startDate: string): string[]` que, dado el Día 1, calcula automáticamente los siguientes 7 días hábiles consecutivos (omitiendo sábados y domingos). Integrar en el formulario de creación de estudio (`/estudios/nuevo`).
  *Verificación:* `test('ca301_samplingDaysProposalSkipsWeekends')` verifica que al ingresar lunes 2 de marzo de 2026, proponga 3, 4, 5, 6, 9, 10 y 11 de marzo.

- [ ] **T315. Formulario de configuración del estudio y asignación de cuadrillas (R-331, R-333).**
  Completar el formulario de estudio permitiendo seleccionar fuentes participantes, asignar cuadrillas a los turnos mañana/tarde y mostrar el banner destacado: *"Día 1: Día de descarte: no entra en los cálculos"*.
  *Verificación:* Pruebas de integración validando la asignación de cuadrillas y el rótulo de descarte del día 1.

- [ ] **T316. Monitor de ciclo de vida del estudio y control de cierres (R-332, CA-309).**
  Vista `/estudios/:id` con la barra de progreso de los 8 días. Botón para avanzar o cerrar el estudio con diálogo de confirmación. Si el cierre es rechazado por la API debido a registros pendientes, mostrar el motivo exacto en un panel explicativo.
  *Verificación:* `test('ca309_studyClosingRejectionDisplaysExactReason')` simula rechazo 422 y verifica la visualización del motivo al usuario.

---

## Fase 5 — Flujo de Registro en Campo (Móvil First)

- [ ] **T317. Dashboard de campo y apertura de turno (R-340, R-341, CA-300).**
  Vista `/campo` con detección del turno actual según la fecha y hora. Botón táctil grande "Abrir turno" visible únicamente para docentes.
  *Verificación:* Pruebas validando que el docente puede abrir el turno y el estudiante visualiza el estado pasivo.

- [ ] **T318. Formulario de población del turno y bloqueo de bolsas (R-342, CA-303).**
  Vista `/campo/turno/:id/poblacion` con campos para estudiantes, docentes y personal administrativo, calculando el total en tiempo real. Bloqueo del registro de bolsas hasta que la población esté registrada.
  *Verificación:* `test('ca303_shiftWithoutPopulationBlocksBagEntry')` confirma que el formulario de pesaje muestra el mensaje de bloqueo mientras no haya población.

- [ ] **T319. Registro de pesaje de bolsa con cálculos en vivo (R-343, CA-304).**
  Formulario `/campo/turno/:id/bolsas/nueva` con selector ágil de fuentes y campos de peso bruto, tara y volumen. Cálculo dinámico en pantalla de peso neto ($\text{bruto} - \text{tara}$) y densidad ($\text{neto} / \text{volumen}$).
  *Verificación:* `test('ca304_netWeightDensityAndLargeLabelCalculated')` comprueba los cálculos para bruto 12,40, tara 0,40 y volumen 0,080.

- [ ] **T320. Validaciones de tara y detección de valores atípicos (R-305, R-344, CA-305).**
  Validación en cliente con Zod: si $\text{tara} \ge \text{bruto}$, mostrar mensaje de error en rojo bajo el campo tara y deshabilitar el guardado. Si los valores superan los umbrales históricos, desplegar aviso amarillo preventivo permitiendo continuar.
  *Verificación:* `test('ca305_tareGreaterThanGrossDisplaysRedError')` valida el rechazo visual y funcional cuando la tara supera al bruto.

- [ ] **T321. Prevención de envíos dobles y despliegue del rótulo gigante (R-305, R-343, CA-304, CA-310).**
  Al presionar "Guardar bolsa", deshabilitar inmediatamente el botón y mostrar spinner para impedir doble registro ante toques sucesivos. Tras el guardado exitoso, mostrar el rótulo generado en tipografía monoespaciada gigante (ej. `AU-D3-M`) para rotulación física de la bolsa.
  *Verificación:* `test('ca310_doubleClickCreatesOnlyOneBag')` simula clics repetidos y verifica una sola llamada; valida el rótulo en pantalla.

- [ ] **T322. Monitoreo del turno y checklist de fuentes (R-345).**
  Vista de monitoreo `/campo/turno/:id`: lista de todas las fuentes participantes con conteo de bolsas, peso acumulado y badge de estado (*Con bolsas*, *Sin generación*, *Pendiente*). Botón "Marcar sin generación" para fuentes sin residuos.
  *Verificación:* Pruebas de integración validando el cambio de estado de la fuente al registrar bolsa o marcarla sin generación.

- [ ] **T323. Cierre de turno con validación de fuentes pendientes (R-346, CA-306).**
  Botón "Cerrar turno" (solo docente). Mantener el botón deshabilitado mientras exista al menos una fuente en estado *Pendiente*. Al resolver todas las fuentes, habilitar el botón y mostrar el resumen consolidado de cierre.
  *Verificación:* `test('ca306_shiftClosingDisabledUntilAllSourcesHandled')` confirma que el botón permanece inactivo con fuentes pendientes y se activa al resolverlas.

---

## Fase 6 — Verificación de Usabilidad, Ergonomía y Cierre

- [ ] **T324. Verificación de ergonomía en 360 px y velocidad operativa (CA-307, CA-308).**
  Auditoría visual y funcional en emulador móvil (ancho 360 px): verificar ausencia de desplazamiento horizontal, objetivos táctiles $\ge 48\text{ px}$ y flujo continuo para registrar una bolsa en menos de 30 segundos.
  *Verificación:* Pruebas de integración end-to-end con Vitest/Testing Library verificando el flujo completo de registro de bolsa en condiciones ergonómicas.

- [ ] **T325. Documentación y revisión final del PR.**
  Actualizar `README.md` del frontend con instrucciones de arranque local (`npm install`, `npm run dev`), variables de entorno (`VITE_USE_MOCK_API`) y ejecución de pruebas (`npm test`). Verificar el cumplimiento de todos los criterios CA-300 a CA-310.
  *Verificación:* Suite completa de pruebas en verde y PR abierto hacia `develop`.
