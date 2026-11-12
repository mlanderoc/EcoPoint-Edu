/**
 * Weighing of a bag: source, gross weight, tare and volume, with the net weight
 * and density calculated while typing, and the giant label after saving
 * (Spec 004, T318 to T321; R-302, R-305, R-343, R-344, CA-303 to CA-305, CA-310).
 *
 * @author craulgomez
 */

import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Lock } from 'lucide-react'
import { AlertBanner, Button, Card, CardContent, Input } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { adminService } from '@/features/admin/services/adminService'
import { ApiError } from '@/services/apiClient'
import { CharacterizationSettings, SamplingShift, WasteBag } from '@/types'
import { cn } from '@/utils/cn'
import { fieldService } from '../services/fieldService'
import { DEFAULT_LIMITS, density, formatNumber, isAtypical, netWeight, parseDecimal } from '../utils/fieldMath'

type Limits = Pick<CharacterizationSettings, 'maxBagNetWeight' | 'minBagDensity' | 'maxBagDensity'>

export function NewBagPage() {
  const { id } = useParams()
  const shiftId = Number(id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const [shift, setShift] = useState<SamplingShift | null>(null)
  const [limits, setLimits] = useState<Limits>(DEFAULT_LIMITS)
  const [sourceId, setSourceId] = useState<number | null>(null)
  const [gross, setGross] = useState('')
  const [tare, setTare] = useState('')
  const [volume, setVolume] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [saved, setSaved] = useState<WasteBag | null>(null)
  const submitting = useRef(false)

  useEffect(() => {
    fieldService.getShift(shiftId)
      .then(setShift)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar el turno.'))
    adminService.getInstitutions()
      .then((institutions) => {
        const settings = institutions.find((item) => item.id === user?.institutionId)?.settings
        if (settings) setLimits(settings)
      })
      .catch(() => undefined)
  }, [shiftId, user?.institutionId])

  const grossValue = parseDecimal(gross)
  const tareValue = parseDecimal(tare)
  const volumeValue = parseDecimal(volume)
  const tareError = grossValue !== null && tareValue !== null && tareValue >= grossValue
    ? 'La tara no puede superar el peso bruto.'
    : undefined
  const net = grossValue !== null && tareValue !== null && !tareError ? netWeight(grossValue, tareValue) : null
  const bagDensity = net !== null && volumeValue !== null ? density(net, volumeValue) : null
  const atypical = net !== null && isAtypical(net, bagDensity, limits)
  const complete = sourceId !== null && net !== null && volumeValue !== null && volumeValue > 0

  const handleSave = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!complete || submitting.current) {
      return
    }
    submitting.current = true
    setIsSaving(true)
    setError(null)
    try {
      setSaved(await fieldService.registerBag(shiftId, {
        sourceId: sourceId as number, grossWeight: grossValue as number, tare: tareValue as number,
        volume: volumeValue as number,
      }))
    } catch (err) {
      setError(err instanceof ApiError ? (err.getFieldError('tare') ?? err.message) : 'No se pudo guardar la bolsa.')
    } finally {
      submitting.current = false
      setIsSaving(false)
    }
  }

  const nextBag = () => {
    setSaved(null)
    setGross('')
    setTare('')
    setVolume('')
  }

  if (!shift) {
    return error
      ? <AlertBanner variant="danger" title="Turno no disponible" message={error} />
      : <p className="text-sm text-[var(--color-text-muted)]">Cargando turno...</p>
  }

  if (!shift.population) {
    return (
      <div className="max-w-xl w-full mx-auto">
        <Card>
          <CardContent className="py-8 flex flex-col items-center gap-4 text-center" data-testid="bags-locked">
            <Lock className="w-10 h-10 text-amber-600" />
            <p className="text-lg font-semibold">Registre primero la población del turno para habilitar el pesaje de bolsas</p>
            <Button className="min-h-[56px]" onClick={() => navigate(`/campo/turno/${shiftId}/poblacion`)}>
              Registrar población
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (saved) {
    return (
      <div className="max-w-xl w-full mx-auto flex flex-col gap-5 items-stretch">
        <Card>
          <CardContent className="py-10 flex flex-col items-center gap-3 text-center">
            <p className="text-sm uppercase tracking-wide text-[var(--color-text-muted)]">Rótulo de la bolsa</p>
            <p className="font-mono text-5xl sm:text-6xl font-black break-all" data-testid="bag-label">{saved.label}</p>
            <p className="text-sm">
              {formatNumber(saved.netWeight)} kg netos · {formatNumber(saved.density)} kg/m³
            </p>
            {saved.atypical && (
              <AlertBanner variant="warning" message="Valor fuera de lo esperado: se generó una alerta para revisarlo." />
            )}
          </CardContent>
        </Card>
        <Button className="min-h-[56px] text-lg" onClick={nextBag}>Siguiente bolsa</Button>
        <Button variant="outline" className="min-h-[48px]" onClick={() => navigate(`/campo/turno/${shiftId}`)}>
          Ver turno
        </Button>
      </div>
    )
  }

  return (
    <div className="max-w-xl w-full mx-auto flex flex-col gap-5">
      <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Registrar bolsa · {shift.code}</h1>
      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}

      <form onSubmit={handleSave} className="flex flex-col gap-5">
        <fieldset className="flex flex-col gap-2">
          <legend className="text-sm font-semibold mb-2">Fuente</legend>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {shift.sources.map((source) => (
              <button
                key={source.sourceId}
                type="button"
                onClick={() => setSourceId(source.sourceId)}
                aria-pressed={sourceId === source.sourceId}
                aria-label={`${source.code} ${source.name}`}
                disabled={source.status === 'SIN_GENERACION'}
                className={cn(
                  'min-h-[64px] rounded-xl border-2 px-2 py-2 flex flex-col items-center justify-center text-center',
                  sourceId === source.sourceId
                    ? 'border-[var(--color-primary)] bg-[var(--color-primary-light)]'
                    : 'border-slate-200 bg-white',
                  source.status === 'SIN_GENERACION' && 'opacity-50'
                )}
              >
                <span className="font-mono text-xl font-black">{source.code}</span>
                <span className="text-xs leading-tight">{source.name}</span>
              </button>
            ))}
          </div>
        </fieldset>

        <Input id="bag-gross" label="Peso bruto (kg)" inputMode="decimal" placeholder="Ej. 12,40" value={gross}
          onChange={(event) => setGross(event.target.value)} disabled={isSaving} className="min-h-[56px] text-xl" />
        <Input id="bag-tare" label="Tara del recipiente (kg)" inputMode="decimal" placeholder="Ej. 0,40" value={tare}
          onChange={(event) => setTare(event.target.value)} error={tareError} disabled={isSaving}
          className="min-h-[56px] text-xl" />
        <Input id="bag-volume" label="Volumen de la bolsa (m³)" inputMode="decimal" placeholder="Ej. 0,080"
          value={volume} onChange={(event) => setVolume(event.target.value)} disabled={isSaving}
          className="min-h-[56px] text-xl" />

        <Card>
          <CardContent className="grid grid-cols-2 gap-3 py-4">
            <div>
              <p className="text-xs text-[var(--color-text-muted)]">Peso neto</p>
              <p className="text-2xl font-bold" data-testid="live-net">{formatNumber(net)} kg</p>
            </div>
            <div>
              <p className="text-xs text-[var(--color-text-muted)]">Densidad</p>
              <p className="text-2xl font-bold" data-testid="live-density">{formatNumber(bagDensity)} kg/m³</p>
            </div>
          </CardContent>
        </Card>

        {atypical && (
          <AlertBanner variant="warning" title="Valor fuera de lo esperado"
            message="Revise antes de continuar. Si el valor es correcto, puede guardar la bolsa." />
        )}

        <Button type="submit" className="min-h-[56px] text-lg" isLoading={isSaving}
          disabled={!complete || isSaving}>
          Guardar bolsa
        </Button>
      </form>
    </div>
  )
}
