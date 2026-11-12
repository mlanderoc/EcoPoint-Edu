import { useState } from 'react'
import { AppLayout } from '@/components/layout'
import { Button, Input, Card, CardHeader, CardTitle, CardContent, AlertBanner } from '@/components/ui'
import { WasteGroupBadge, WasteGroupChip } from '@/components/waste'
import { User, WasteGroup } from '@/types'

export default function App() {
  const [currentUser, setCurrentUser] = useState<User>({
    id: 1,
    name: 'Alfredo',
    lastName: 'Bautista',
    email: 'docente@unicesar.edu.co',
    role: 'DOCENTE',
    active: true,
  })

  const [currentPath, setCurrentPath] = useState('/campo')
  const [grossWeight, setGrossWeight] = useState('')
  const [tare, setTare] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  const toggleRole = () => {
    setCurrentUser((prev) => ({
      ...prev,
      role: prev.role === 'DOCENTE' ? 'ESTUDIANTE_UNIVERSITARIO' : 'DOCENTE',
    }))
  }

  const wasteGroups: WasteGroup[] = ['BLANCO', 'VERDE', 'NEGRO', 'SIN_COLOR']

  const tareNum = parseFloat(tare) || 0
  const grossNum = parseFloat(grossWeight) || 0
  const tareError = tareNum > 0 && grossNum > 0 && tareNum >= grossNum ? 'La tara no puede superar el peso bruto.' : undefined

  const handleSimulateSave = () => {
    setIsSaving(true)
    setTimeout(() => setIsSaving(false), 800)
  }

  return (
    <AppLayout
      user={currentUser}
      currentPath={currentPath}
      onNavigate={setCurrentPath}
      onLogout={() => alert('Sesión cerrada')}
    >
      <div className="max-w-4xl w-full mx-auto flex flex-col gap-6">
        {/* Banner de Bienvenida y Selector de Rol para demo */}
        <div className="bg-white rounded-2xl p-5 shadow-xs border border-slate-200 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <img
              src="/logo_ecopoint.jpeg"
              alt="EcoPoint Edu Logo"
              className="w-16 h-16 object-contain rounded-xl border border-slate-100 shadow-xs"
            />
            <div>
              <h1 className="text-xl md:text-2xl font-black text-[var(--color-brand-dark)] leading-tight">
                EcoPoint Edu — Sistema de Muestreo
              </h1>
              <p className="text-xs md:text-sm text-[var(--color-text-muted)] mt-0.5">
                I.E. Prudencia Daza — Caracterización participativa 2026
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={toggleRole}
              title="Alternar rol para verificar restricciones CA-300"
            >
              Cambiar rol a: {currentUser.role === 'DOCENTE' ? 'Estudiante' : 'Docente'}
            </Button>
          </div>
        </div>

        {/* Grupos de Residuos Sólidos (Res. 2184/2019 + R-304) */}
        <Card>
          <CardHeader>
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-2">
              <div>
                <CardTitle>Grupos de Residuos Sólidos</CardTitle>
                <p className="text-xs text-[var(--color-text-muted)] mt-1">
                  Resolución 2184 de 2019 — Identificación mediante color y nombre explícito (R-304)
                </p>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {wasteGroups.map((g) => (
                  <WasteGroupBadge key={g} group={g} size="sm" />
                ))}
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
              {wasteGroups.map((g) => (
                <WasteGroupChip key={g} group={g} count={g === 'BLANCO' ? 12 : g === 'VERDE' ? 18 : 5} />
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Demostración de Ergonomía Táctil y Prevención de Errores (R-300 a R-305) */}
        <Card>
          <CardHeader>
            <CardTitle>Ergonomía de Campo y Touch Targets (≥ 48 px)</CardTitle>
            <p className="text-xs text-[var(--color-text-muted)] mt-1">
              Diseño adaptado para operación en smartphone (360 px) bajo sol directo y con guantes
            </p>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Peso Bruto (kg)"
                inputMode="decimal"
                normalizeDecimal
                value={grossWeight}
                onChange={(e) => setGrossWeight(e.target.value)}
                placeholder="Ej. 12,40"
                helperText="Acepta punto o coma decimal (R-302)"
              />
              <Input
                label="Tara del Recipiente (kg)"
                inputMode="decimal"
                normalizeDecimal
                value={tare}
                onChange={(e) => setTare(e.target.value)}
                placeholder="Ej. 0,40"
                error={tareError}
              />
            </div>

            {tareError && (
              <AlertBanner variant="danger" title="Error de validación (CA-305)">
                {tareError}
              </AlertBanner>
            )}

            <div className="pt-2 flex flex-col sm:flex-row gap-3 items-center">
              <Button
                variant="primary"
                loading={isSaving}
                disabled={Boolean(tareError)}
                onClick={handleSimulateSave}
                className="w-full sm:w-auto"
              >
                Guardar Pesaje (Touch Target 48 px)
              </Button>
              <span className="text-xs text-slate-500">
                El botón se inhabilita durante el guardado para evitar envíos dobles (R-305, CA-310).
              </span>
            </div>
          </CardContent>
        </Card>
      </div>
    </AppLayout>
  )
}
