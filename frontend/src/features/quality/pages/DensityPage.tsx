/**
 * Density measurement of a shift with the density calculated while typing and
 * frequent measuring containers (Spec 005, T404; R-410, R-411, CA-407).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AlertBanner, BackButton, Button, Input } from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { ShiftSteps } from '@/features/field/components/ShiftSteps'
import { fieldService } from '@/features/field/services/fieldService'
import { formatNumber, parseDecimal } from '@/features/field/utils/fieldMath'
import { ApiError } from '@/services/apiClient'
import { DensityMeasurement, SamplingShift } from '@/types'
import { qualityService } from '../services/qualityService'

/** Measuring containers used by the crews (R-411); extend it with the list of Environmental Engineering (P2). */
export const FREQUENT_CONTAINERS = [{ name: 'Caneca de 55 galones', volume: 0.208 }]

export function DensityPage() {
  const { id } = useParams()
  const shiftId = Number(id)
  const navigate = useNavigate()
  const [saved, setSaved] = useState<DensityMeasurement | null | undefined>(undefined)
  const [shift, setShift] = useState<SamplingShift | null>(null)
  const [volume, setVolume] = useState('')
  const [weight, setWeight] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const shiftPath = `/campo/turno/${shiftId}`
  const { confirmLeave, discardDialog } = useUnsavedChanges(!saved && !isSaving && volume + weight !== '')

  useEffect(() => {
    fieldService.getShift(shiftId).then(setShift).catch(() => setShift(null))
    qualityService.getDensity(shiftId)
      .then(setSaved)
      .catch((err) => {
        setSaved(null)
        setError(err instanceof ApiError ? err.message : 'No se pudo cargar la densidad.')
      })
  }, [shiftId])

  const volumeValue = parseDecimal(volume)
  const weightValue = parseDecimal(weight)
  const density = volumeValue && weightValue && volumeValue > 0 ? Math.round((weightValue / volumeValue) * 100) / 100 : null

  const handleSave = async (event: React.FormEvent) => {
    event.preventDefault()
    if (volumeValue === null || weightValue === null) return
    setError(null)
    try {
      setIsSaving(true)
      setSaved(await qualityService.registerDensity(shiftId, volumeValue, weightValue))
      fieldService.getShift(shiftId).then(setShift).catch(() => undefined)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar la densidad.')
    } finally {
      setIsSaving(false)
    }
  }

  if (saved === undefined) {
    return <p className="text-sm text-[var(--color-text-muted)]">Cargando densidad...</p>
  }

  const fill = Math.min(100, (saved?.density ?? density ?? 0) / 3)

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-4">
      <BackButton to={shiftPath} label="Atrás al turno" onLeave={confirmLeave} />
      <div className="page-hero"><div><h1 className="page-hero-title">Densidad del turno</h1>
        <p className="m-0 mt-1">Peso volumétrico de los residuos.</p></div></div>
      {shift && <ShiftSteps shift={shift} current="densidad" />}
      {error && <AlertBanner variant="danger" message={error} />}
      <section className="panel">
        <h3>Peso volumétrico del turno</h3>
        <div className="grid-2" style={{ alignItems: 'center' }}>
          <div className="container-fill" aria-hidden="true"><i style={{ height: `${fill}%` }} /></div>
          {saved ? (
            <div className="flex flex-col gap-3">
              <p className="m-0 text-sm text-[var(--color-text-muted)]">
                {formatNumber(saved.netWeight)} kg en {formatNumber(saved.containerVolume, 3)} m³
              </p>
              <div className="total-band"><span>Densidad</span><b data-testid="saved-density">{formatNumber(saved.density)} kg/m³</b></div>
            </div>
          ) : (
            <form onSubmit={handleSave} className="flex flex-col gap-3">
              <div className="flex flex-wrap gap-2">
                {FREQUENT_CONTAINERS.map((container) => (
                  <Button key={container.name} type="button" variant="outline" size="sm" className="min-h-[48px]"
                    onClick={() => setVolume(String(container.volume).replace('.', ','))}>
                    {container.name} = {formatNumber(container.volume, 3)} m³
                  </Button>
                ))}
              </div>
              <Input id="density-volume" label="Volumen del recipiente (m³)" inputMode="decimal" value={volume}
                onChange={(event) => setVolume(event.target.value)} disabled={isSaving} className="min-h-[52px] text-lg" />
              <Input id="density-weight" label="Peso neto de los residuos (kg)" inputMode="decimal" value={weight}
                onChange={(event) => setWeight(event.target.value)} disabled={isSaving} className="min-h-[52px] text-lg" />
              <div className="total-band" data-testid="live-shift-density"><span>Densidad</span><b>{formatNumber(density)} kg/m³</b></div>
              <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
                <Button type="button" variant="outline" className="min-h-[48px]" disabled={isSaving}
                  onClick={() => confirmLeave(() => navigate(shiftPath))}>
                  Descartar
                </Button>
                <Button type="submit" className="min-h-[48px]" isLoading={isSaving}
                  disabled={isSaving || density === null}>
                  Guardar densidad
                </Button>
              </div>
            </form>
          )}
        </div>
      </section>
      {discardDialog}
    </div>
  )
}
