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
import { AlertBanner, BackButton, Button, Card, CardContent, Input } from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { ApiError } from '@/services/apiClient'
import { CharacterizationSettings, SamplingShift, WasteBag } from '@/types'
import { BagIcon } from '@/features/home/components/BagIcon'
import { ShiftSteps } from '../components/ShiftSteps'
import { fieldService } from '../services/fieldService'
import { settingsOfStudy } from '@/features/study/services/studySettings'
import { DEFAULT_LIMITS, density, formatNumber, isAtypical, netWeight, parseDecimal } from '../utils/fieldMath'

type Limits = Pick<CharacterizationSettings, 'maxBagNetWeight' | 'minBagDensity' | 'maxBagDensity'>

export function NewBagPage() {
  const { id } = useParams()
  const shiftId = Number(id)
  const navigate = useNavigate()
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
  const weighing = useRef<HTMLDivElement>(null)
  const shiftPath = `/campo/turno/${shiftId}`
  const isDirty = !saved && !isSaving && (sourceId !== null || gross + tare + volume !== '')
  const { confirmLeave, discardDialog } = useUnsavedChanges(isDirty)

  useEffect(() => {
    fieldService.getShift(shiftId)
      .then((loaded) => {
        setShift(loaded)
        settingsOfStudy(loaded.studyId)
          .then((settings) => { if (settings) setLimits(settings) })
          .catch(() => undefined)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar el turno.'))
  }, [shiftId])

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

  /** Chooses the source and brings the weighing form into view (D15). */
  const chooseSource = (id: number) => {
    setSourceId(id)
    requestAnimationFrame(() => {
      const form = weighing.current
      if (!form) return
      const { top, bottom } = form.getBoundingClientRect()
      if (top < 12 || bottom > window.innerHeight) {
        const calm = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches
        form.scrollIntoView?.({ behavior: calm ? 'auto' : 'smooth', block: 'start' })
      }
      if (window.matchMedia?.('(pointer: fine)')?.matches) {
        document.getElementById('bag-gross')?.focus({ preventScroll: true })
      }
    })
  }

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
      <div className="max-w-xl w-full mx-auto flex flex-col gap-4">
        <BackButton to={shiftPath} label="Atrás al turno" />
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
        <Button variant="outline" className="min-h-[48px]" onClick={() => navigate(shiftPath)}>
          Ver turno
        </Button>
      </div>
    )
  }

  return (
    <div className="max-w-xl w-full mx-auto flex flex-col gap-5">
      <BackButton to={shiftPath} label="Atrás al turno" onLeave={confirmLeave} />
      <ShiftSteps shift={shift} current="bolsas" />
      <div className="page-hero"><h1 className="page-hero-title">Registrar bolsa · {shift.code}</h1></div>
      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}

      <form onSubmit={handleSave} className="flex flex-col gap-5">
        <fieldset className="flex flex-col gap-2">
          <legend className="text-sm font-semibold mb-2">Fuente</legend>
          <div className="source-pick">
            {shift.sources.map((source) => (
              <button
                key={source.sourceId}
                type="button"
                onClick={() => chooseSource(source.sourceId)}
                aria-pressed={sourceId === source.sourceId}
                aria-label={`${source.code} ${source.name}`}
                disabled={source.status === 'SIN_GENERACION'}
                className="source-row min-h-[56px]"
              >
                <BagIcon kg={source.totalNetWeight} />
                <span className="min-w-0"><b>{source.code}</b> <small>{source.name}</small></span>
                <span>{source.bagCount > 0 ? `${formatNumber(source.totalNetWeight)} kg` : 'Pendiente'}</span>
              </button>
            ))}
          </div>
        </fieldset>

        <div ref={weighing} className="bag-form flex flex-col gap-5">
        <Input id="bag-gross" label="Peso bruto (kg)" inputMode="decimal" placeholder="Ej. 12,40" value={gross}
          onChange={(event) => setGross(event.target.value)} disabled={isSaving} className="min-h-[56px] text-xl" />
        <Input id="bag-tare" label="Tara del recipiente (kg)" inputMode="decimal" placeholder="Ej. 0,40" value={tare}
          onChange={(event) => setTare(event.target.value)} error={tareError} disabled={isSaving}
          className="min-h-[56px] text-xl" />
        <Input id="bag-volume" label="Volumen de la bolsa (m³)" inputMode="decimal" placeholder="Ej. 0,080"
          value={volume} onChange={(event) => setVolume(event.target.value)} disabled={isSaving}
          className="min-h-[56px] text-xl" />

        <div className="scale" aria-live="polite">
          <div><small>Peso neto</small><b data-testid="live-net">{formatNumber(net)} kg</b></div>
          <div><small>Densidad</small><b data-testid="live-density">{formatNumber(bagDensity)} kg/m³</b></div>
        </div>

        {atypical && (
          <AlertBanner variant="warning" title="Valor fuera de lo esperado"
            message="Revise antes de continuar. Si el valor es correcto, puede guardar la bolsa." />
        )}

        </div>

        <Button type="submit" className="min-h-[56px] text-lg" isLoading={isSaving}
          disabled={!complete || isSaving}>
          Guardar bolsa
        </Button>
        <Button type="button" variant="outline" className="min-h-[48px]" disabled={isSaving}
          onClick={() => confirmLeave(() => navigate(shiftPath))}>
          Descartar
        </Button>
      </form>
      {discardDialog}
    </div>
  )
}
