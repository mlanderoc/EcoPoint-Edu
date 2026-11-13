/**
 * Density measurement of a shift with the density calculated while typing and
 * frequent measuring containers (Spec 005, T404; R-410, R-411, CA-407).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AlertBanner, BackButton, Button, Card, CardContent, Input } from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { formatNumber, parseDecimal } from '@/features/field/utils/fieldMath'
import { ApiError } from '@/services/apiClient'
import { DensityMeasurement } from '@/types'
import { qualityService } from '../services/qualityService'

/** Measuring containers used by the crews (R-411); extend it with the list of Environmental Engineering (P2). */
export const FREQUENT_CONTAINERS = [{ name: 'Caneca de 55 galones', volume: 0.208 }]

export function DensityPage() {
  const { id } = useParams()
  const shiftId = Number(id)
  const navigate = useNavigate()
  const [saved, setSaved] = useState<DensityMeasurement | null | undefined>(undefined)
  const [volume, setVolume] = useState('')
  const [weight, setWeight] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const shiftPath = `/campo/turno/${shiftId}`
  const { confirmLeave, discardDialog } = useUnsavedChanges(!saved && !isSaving && volume + weight !== '')

  useEffect(() => {
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
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar la densidad.')
    } finally {
      setIsSaving(false)
    }
  }

  if (saved === undefined) {
    return <p className="text-sm text-[var(--color-text-muted)]">Cargando densidad...</p>
  }

  return (
    <div className="max-w-xl w-full mx-auto flex flex-col gap-5">
      <BackButton to={shiftPath} label="Atrás al turno" onLeave={confirmLeave} />
      <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Densidad del turno</h1>
      {error && <AlertBanner variant="danger" message={error} />}
      {saved ? (
        <Card>
          <CardContent className="py-6 flex flex-col items-center gap-2 text-center">
            <p className="text-sm text-[var(--color-text-muted)]">
              {formatNumber(saved.netWeight)} kg en {formatNumber(saved.containerVolume, 3)} m³
            </p>
            <p className="text-4xl font-black" data-testid="saved-density">{formatNumber(saved.density)} kg/m³</p>
          </CardContent>
        </Card>
      ) : (
        <form onSubmit={handleSave} className="flex flex-col gap-4">
          <div className="flex flex-wrap gap-2">
            {FREQUENT_CONTAINERS.map((container) => (
              <Button key={container.name} type="button" variant="outline" className="min-h-[48px]"
                onClick={() => setVolume(String(container.volume).replace('.', ','))}>
                {container.name} = {formatNumber(container.volume, 3)} m³
              </Button>
            ))}
          </div>
          <Input id="density-volume" label="Volumen del recipiente (m³)" inputMode="decimal" value={volume}
            onChange={(event) => setVolume(event.target.value)} disabled={isSaving} className="min-h-[56px] text-xl" />
          <Input id="density-weight" label="Peso neto de los residuos (kg)" inputMode="decimal" value={weight}
            onChange={(event) => setWeight(event.target.value)} disabled={isSaving} className="min-h-[56px] text-xl" />
          <p className="text-lg" data-testid="live-shift-density">
            Densidad: <strong className="text-2xl">{formatNumber(density)} kg/m³</strong>
          </p>
          <Button type="submit" className="min-h-[56px] text-lg" isLoading={isSaving}
            disabled={isSaving || density === null}>
            Guardar densidad
          </Button>
          <Button type="button" variant="outline" className="min-h-[48px]" disabled={isSaving}
            onClick={() => confirmLeave(() => navigate(shiftPath))}>
            Descartar
          </Button>
        </form>
      )}
      {discardDialog}
    </div>
  )
}
