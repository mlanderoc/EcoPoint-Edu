/**
 * Field dashboard view for shift monitoring and waste recording (CA-300 / CA-307 / R-340).
 *
 * @author craulgomez
 */

import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import { PlayCircle, CheckCircle2, ShoppingBag } from 'lucide-react'
import { Button, Input, Card, CardHeader, CardTitle, CardContent, AlertBanner } from '@/components/ui'
import { WasteGroupBadge, WasteGroupChip } from '@/components/waste'
import { WasteGroup } from '@/types'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { RequirePermission } from '@/features/auth/components/RequirePermission'

export function FieldDashboard() {
  const { user } = useAuth()
  const location = useLocation()
  const accessDenied = location.state?.accessDenied as boolean | undefined

  const [grossWeight, setGrossWeight] = useState('')
  const [tare, setTare] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [shiftOpen, setShiftOpen] = useState(true)

  const wasteGroups: WasteGroup[] = ['BLANCO', 'VERDE', 'NEGRO', 'SIN_COLOR']

  const tareNum = parseFloat(tare) || 0
  const grossNum = parseFloat(grossWeight) || 0
  const tareError =
    tareNum > 0 && grossNum > 0 && tareNum >= grossNum
      ? 'La tara no puede superar el peso bruto.'
      : undefined

  const handleSimulateSave = () => {
    setIsSaving(true)
    setTimeout(() => setIsSaving(false), 600)
  }

  return (
    <div className="max-w-4xl w-full mx-auto flex flex-col gap-6">
      {/* Access Denied Warning when student is redirected from admin (CA-300) */}
      {accessDenied && (
        <AlertBanner
          variant="warning"
          title="Acceso restringido"
          message="Acceso denegado: únicamente los docentes tienen permisos para acceder al módulo de administración y configuración."
        />
      )}

      {/* Header Institucional y Estado del Turno */}
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
            <div className="flex items-center gap-2 mt-2">
              <span className="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-[var(--color-primary-light)] text-[var(--color-primary)]">
                <span className="w-2 h-2 rounded-full bg-[var(--color-primary)] animate-pulse" />
                Día 1 — Turno Mañana ({shiftOpen ? 'Abierto' : 'Cerrado'})
              </span>
              <span className="text-xs text-[var(--color-text-muted)]">
                Usuario: <strong className="text-slate-800">{user?.name} {user?.lastName}</strong> ({user?.role === 'DOCENTE' ? 'Docente' : 'Estudiante'})
              </span>
            </div>
          </div>
        </div>

        {/* Botones de Control de Turno: Visibles SOLO para Docentes (CA-300) */}
        <RequirePermission permission="OPEN_SHIFT">
          <div className="flex items-center gap-2 flex-wrap">
            <Button
              variant={shiftOpen ? 'outline' : 'primary'}
              size="default"
              className="min-h-[48px]"
              onClick={() => setShiftOpen((prev) => !prev)}
            >
              <PlayCircle className="w-5 h-5 mr-1.5" />
              {shiftOpen ? 'Turno Abierto' : 'Abrir Turno'}
            </Button>
            <Button
              variant="secondary"
              size="default"
              className="min-h-[48px]"
              onClick={() => alert('Turno cerrado por docente')}
            >
              <CheckCircle2 className="w-5 h-5 mr-1.5" />
              Cerrar Turno
            </Button>
          </div>
        </RequirePermission>
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
              <WasteGroupChip
                key={g}
                group={g}
                count={g === 'BLANCO' ? 12 : g === 'VERDE' ? 18 : 5}
              />
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
              helperText="Incluye peso de bolsa y residuo"
            />
            <Input
              label="Tara del recipiente (kg)"
              inputMode="decimal"
              normalizeDecimal
              value={tare}
              onChange={(e) => setTare(e.target.value)}
              placeholder="Ej. 0,40"
              error={tareError}
              helperText="Tara promedio de bolsa"
            />
          </div>

          <div className="p-4 bg-[var(--color-primary-light)] rounded-xl border border-emerald-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <span className="text-xs font-bold text-[var(--color-brand-dark)] uppercase tracking-wider block">
                Cálculo en vivo (R-343)
              </span>
              <p className="text-xl md:text-2xl font-black text-[var(--color-primary)]">
                Neto: {grossNum > tareNum ? (grossNum - tareNum).toFixed(2) : '0,00'} kg
              </p>
            </div>
            <Button
              variant="primary"
              size="lg"
              className="min-h-[48px]"
              disabled={Boolean(tareError) || grossNum <= 0}
              loading={isSaving}
              onClick={handleSimulateSave}
            >
              <ShoppingBag className="w-5 h-5 mr-1.5" />
              Guardar Bolsa
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
