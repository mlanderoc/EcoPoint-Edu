export default function App() {
  return (
    <div className="min-h-screen bg-[var(--color-bg-app)] text-[var(--color-text-main)] flex flex-col">
      {/* Header */}
      <header className="bg-[var(--color-brand-dark)] text-white px-4 py-3 shadow-md flex items-center justify-between">
        <div className="flex items-center gap-3">
          <img
            src="/logo_ecopoint.jpeg"
            alt="EcoPoint Edu Logo"
            className="w-10 h-10 object-contain rounded-md bg-white p-0.5"
          />
          <div>
            <h1 className="text-lg font-bold tracking-tight">EcoPoint Edu</h1>
            <p className="text-xs text-gray-300">Caracterización de Residuos Sólidos</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs px-2.5 py-1 rounded-full bg-[var(--color-edu-blue)] text-white font-medium">
            Fase 0: Base Operativa
          </span>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-4 md:p-6 flex flex-col gap-6">
        {/* Banner Institucional */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex flex-col md:flex-row items-center gap-5">
          <img
            src="/logo_ecopoint.jpeg"
            alt="EcoPoint Edu Logo"
            className="w-24 h-24 object-contain rounded-lg border border-slate-100 shadow-sm"
          />
          <div className="flex-1 text-center md:text-left">
            <h2 className="text-xl md:text-2xl font-bold text-[var(--color-brand-dark)]">
              Sistema de Caracterización Participativa
            </h2>
            <p className="text-sm text-[var(--color-text-muted)] mt-1">
              Institución Educativa Prudencia Daza — Valledupar, Cesar
            </p>
            <div className="mt-3 flex flex-wrap gap-2 justify-center md:justify-start">
              <span className="inline-flex items-center text-xs font-semibold px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-800 border border-emerald-200">
                ✓ React 18/19 + Vite 6
              </span>
              <span className="inline-flex items-center text-xs font-semibold px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-800 border border-emerald-200">
                ✓ Tailwind CSS v4
              </span>
              <span className="inline-flex items-center text-xs font-semibold px-2.5 py-1 rounded-md bg-blue-50 text-blue-800 border border-blue-200">
                ✓ MSW v2 Mock API Activo
              </span>
              <span className="inline-flex items-center text-xs font-semibold px-2.5 py-1 rounded-md bg-purple-50 text-purple-800 border border-purple-200">
                ✓ Vitest Suite Integrada
              </span>
            </div>
          </div>
        </div>

        {/* Grupos de Residuos Sólidos (Res. 2184/2019 + R-304) */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
          <h3 className="text-base font-bold text-[var(--color-text-main)] mb-3">
            Tokens de Residuos Sólidos (Res. 2184/2019)
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
            <div className="flex items-center gap-3 p-3 rounded-lg border border-slate-300 bg-white">
              <span className="w-5 h-5 rounded-full border border-slate-400 bg-white shrink-0 shadow-sm" />
              <div>
                <p className="text-xs font-bold text-slate-800">Blanco</p>
                <p className="text-[11px] text-slate-600">Aprovechables limpios</p>
              </div>
            </div>

            <div className="flex items-center gap-3 p-3 rounded-lg border border-emerald-300 bg-emerald-50">
              <span className="w-5 h-5 rounded-full bg-[var(--color-waste-green)] shrink-0 shadow-sm" />
              <div>
                <p className="text-xs font-bold text-emerald-950">Verde Orgánico</p>
                <p className="text-[11px] text-emerald-800">Restos de poda y comida</p>
              </div>
            </div>

            <div className="flex items-center gap-3 p-3 rounded-lg border border-slate-800 bg-slate-900 text-white">
              <span className="w-5 h-5 rounded-full bg-[var(--color-waste-black)] shrink-0 border border-slate-700" />
              <div>
                <p className="text-xs font-bold text-white">Negro</p>
                <p className="text-[11px] text-slate-300">No aprovechables</p>
              </div>
            </div>

            <div className="flex items-center gap-3 p-3 rounded-lg border border-slate-400 bg-slate-100">
              <span className="w-5 h-5 rounded-full bg-[var(--color-waste-none)] shrink-0" />
              <div>
                <p className="text-xs font-bold text-slate-800">Sin color</p>
                <p className="text-[11px] text-slate-600">Especiales / Peligrosos</p>
              </div>
            </div>
          </div>
        </div>

        {/* Verificación de Ergonomía Táctil Móvil */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
          <h3 className="text-base font-bold text-[var(--color-text-main)] mb-2">
            Ergonomía Móvil (≥ 48 px de objetivo táctil al sol)
          </h3>
          <p className="text-xs text-slate-600 mb-4">
            Botones y campos con altura mínima de 48 px para operarse de pie y con guantes de látex.
          </p>
          <div className="flex flex-col sm:flex-row gap-3">
            <button
              type="button"
              className="min-h-[48px] px-6 py-3 rounded-lg bg-[var(--color-primary)] hover:bg-[var(--color-primary-hover)] text-white font-bold text-sm tracking-wide shadow-sm transition active:scale-[0.98] cursor-pointer"
            >
              Botón Primario (48 px)
            </button>
            <input
              type="text"
              inputMode="decimal"
              pattern="[0-9]*[.,]?[0-9]*"
              placeholder="Entrada decimal (ej. 12,40)"
              className="min-h-[48px] px-4 py-2 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] text-sm bg-slate-50 font-medium"
            />
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-slate-100 border-t border-slate-200 py-3 text-center text-xs text-slate-600">
        EcoPoint Edu © 2026 — Programación de Computadores III (Universidad Popular del Cesar)
      </footer>
    </div>
  )
}
