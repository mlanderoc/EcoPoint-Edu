/**
 * Quartering of a shift: subsample, weight of each subcategory by group, live
 * 2 % control bar, composition and annulment (Spec 005, T403; R-400 to R-407,
 * CA-400 to CA-406).
 *
 * @author craulgomez
 */

import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AlertTriangle } from 'lucide-react'
import { AlertBanner, Button, Card, CardContent, CardHeader, CardTitle, Input, Modal } from '@/components/ui'
import { WasteGroupBadge } from '@/components/waste'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { hasPermission } from '@/features/auth/utils/permissions'
import { adminService } from '@/features/admin/services/adminService'
import { fieldService } from '@/features/field/services/fieldService'
import { parseDecimal } from '@/features/field/utils/fieldMath'
import { ApiError } from '@/services/apiClient'
import { Quartering, SamplingShift, WasteCategory, WasteGroup } from '@/types'
import { cn } from '@/utils/cn'
import { qualityService } from '../services/qualityService'
import { CompositionChart } from '../components/CompositionChart'
import {
  DEFAULT_QUARTERING_SETTINGS, checkQuality, formatKg, proposedFullSample, sampleError,
} from '../utils/quarteringMath'

const GROUPS: WasteGroup[] = ['BLANCO', 'VERDE', 'NEGRO', 'SIN_COLOR']

export function QuarteringPage() {
  const { id } = useParams()
  const shiftId = Number(id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const canRegisterHazardous = hasPermission(user?.role, 'REGISTER_HAZARDOUS_WASTE')
  const canAnnul = hasPermission(user?.role, 'ANNUL_QUARTERING')

  const [shift, setShift] = useState<SamplingShift | null>(null)
  const [quartering, setQuartering] = useState<Quartering | null | undefined>(undefined)
  const [categories, setCategories] = useState<WasteCategory[]>([])
  const [settings, setSettings] = useState(DEFAULT_QUARTERING_SETTINGS)
  const [sample, setSample] = useState('')
  const [weights, setWeights] = useState<Record<number, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [isAnnulling, setIsAnnulling] = useState(false)
  const [reason, setReason] = useState('')

  useEffect(() => {
    Promise.all([fieldService.getShift(shiftId), qualityService.getQuartering(shiftId), adminService.getCategories()])
      .then(([loadedShift, loadedQuartering, loadedCategories]) => {
        setShift(loadedShift)
        setQuartering(loadedQuartering)
        setCategories(loadedCategories.filter((category) => category.active))
        const full = proposedFullSample(loadedShift.totalNetWeight)
        if (full !== null) setSample(String(full).replace('.', ','))
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar el cuarteo.'))
    adminService.getInstitutions()
      .then((institutions) => {
        const found = institutions.find((item) => item.id === user?.institutionId)?.settings
        if (found) setSettings(found)
      })
      .catch(() => undefined)
  }, [shiftId, user?.institutionId])

  const sampleValue = parseDecimal(sample)
  const weightValues = categories.map((category) => parseDecimal(weights[category.id] ?? ''))
  const check = useMemo(
    () => checkQuality(sampleValue ?? 0, weightValues, settings),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [sampleValue, weights, categories, settings]
  )
  const sampleMessage = shift ? sampleError(sampleValue, shift.totalNetWeight, settings) : null
  const hasWeights = weightValues.some((value) => value !== null && value > 0)

  const handleSave = async (event: React.FormEvent) => {
    event.preventDefault()
    if (sampleMessage || !hasWeights || sampleValue === null) return
    setError(null)
    try {
      setIsSaving(true)
      const results = categories
        .map((category, index) => ({ categoryId: category.id, weight: weightValues[index] }))
        .filter((item): item is { categoryId: number; weight: number } => item.weight !== null && item.weight > 0)
      setQuartering(await qualityService.registerQuartering(shiftId, sampleValue, results))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar el cuarteo.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleAnnul = async () => {
    setError(null)
    try {
      setQuartering(await qualityService.annulQuartering(shiftId, reason))
      setIsAnnulling(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo anular el cuarteo.')
      setIsAnnulling(false)
    }
  }

  if (!shift || quartering === undefined) {
    return error
      ? <AlertBanner variant="danger" title="Cuarteo no disponible" message={error} />
      : <p className="text-sm text-[var(--color-text-muted)]">Cargando cuarteo...</p>
  }

  if (quartering) {
    const nonConforming = quartering.qualityStatus === 'NO_CONFORME'
    return (
      <div className="max-w-3xl w-full mx-auto flex flex-col gap-5">
        <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Cuarteo · Turno {shift.code}</h1>
        {error && <AlertBanner variant="danger" message={error} />}
        {nonConforming && (
          <AlertBanner variant="danger" title="Cuarteo no conforme: repesar"
            message={`La diferencia es ${formatKg(quartering.differenceKg)} kg (${formatKg(quartering.differencePercent)} %), más del ${formatKg(settings.qualityTolerancePercent, 0)} %. Corrija los pesos desde la verificación o anule el cuarteo con su motivo.`} />
        )}
        {quartering.qualityStatus === 'ANULADO' && (
          <AlertBanner variant="info" title="Cuarteo anulado" message={`Motivo: ${quartering.annulReason}`} />
        )}
        {quartering.qualityStatus === 'CONFORME' && (
          <AlertBanner variant="success" title="Conforme"
            message={`Diferencia de ${formatKg(quartering.differenceKg)} kg (${formatKg(quartering.differencePercent)} %).`} />
        )}
        <Card>
          <CardHeader><CardTitle>Composición</CardTitle></CardHeader>
          <CardContent>
            <p className="text-sm mb-4">
              Submuestra {formatKg(quartering.sampleWeight)} kg · clasificado {formatKg(quartering.totalClassifiedWeight)} kg
            </p>
            <CompositionChart results={quartering.results} groups={quartering.groupComposition} />
          </CardContent>
        </Card>
        <div className="flex flex-col sm:flex-row gap-3">
          <Button variant="outline" className="min-h-[48px]" onClick={() => navigate(`/campo/turno/${shiftId}`)}>
            Volver al turno
          </Button>
          {nonConforming && canAnnul && (
            <Button variant="danger" className="min-h-[48px]" onClick={() => setIsAnnulling(true)}>Anular cuarteo</Button>
          )}
        </div>
        <Modal open={isAnnulling} onClose={() => setIsAnnulling(false)} title="Anular cuarteo"
          description="El cuarteo anulado no cuenta para la composición. El motivo queda registrado.">
          <div className="flex flex-col gap-4 mt-2">
            <Input id="annul-reason" label="Motivo" value={reason} onChange={(event) => setReason(event.target.value)} />
            <div className="flex justify-end gap-3">
              <Button variant="outline" className="min-h-[48px]" onClick={() => setIsAnnulling(false)}>Cancelar</Button>
              <Button variant="danger" className="min-h-[48px]" disabled={!reason.trim()} onClick={handleAnnul}>
                Confirmar anulación
              </Button>
            </div>
          </div>
        </Modal>
      </div>
    )
  }

  const conforming = check.status === 'CONFORME'
  return (
    <div className="max-w-3xl w-full mx-auto flex flex-col gap-5 pb-36">
      <div>
        <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Cuarteo · Turno {shift.code}</h1>
        <p className="text-sm text-[var(--color-text-muted)]" data-testid="shift-net">
          Peso neto del turno: <strong>{formatKg(shift.totalNetWeight)} kg</strong>
        </p>
      </div>
      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}
      {shift.status !== 'ABIERTO' && (
        <AlertBanner variant="warning" message="El turno está cerrado: no se puede registrar el cuarteo." />
      )}
      {proposedFullSample(shift.totalNetWeight) !== null && (
        <AlertBanner variant="info" title="Muestra completa"
          message={`El turno generó ${formatKg(shift.totalNetWeight)} kg, menos que la submuestra mínima: se clasifica todo.`} />
      )}

      <form onSubmit={handleSave} className="flex flex-col gap-5">
        <Input id="quartering-sample" label="Peso de la submuestra (kg)" inputMode="decimal" value={sample}
          onChange={(event) => setSample(event.target.value)} error={sample ? sampleMessage ?? undefined : undefined}
          helperText={`Entre ${formatKg(settings.minSampleWeight, 0)} y ${formatKg(settings.maxSampleWeight, 0)} kg`}
          disabled={isSaving} className="min-h-[56px] text-xl" />

        {GROUPS.map((group) => {
          const inGroup = categories.filter((category) => category.group === group)
          if (inGroup.length === 0) return null
          const locked = group === 'SIN_COLOR' && !canRegisterHazardous
          return (
            <Card key={group}>
              <CardHeader className="flex flex-row items-center justify-between gap-2">
                <WasteGroupBadge group={group} />
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                {group === 'SIN_COLOR' && (
                  <p className="text-sm text-amber-900 flex items-start gap-2">
                    <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                    {locked ? 'Solo un docente registra residuos peligrosos' : 'Residuos peligrosos: van a posconsumo'}
                  </p>
                )}
                {inGroup.map((category) => (
                  <Input key={category.id} id={`category-${category.id}`} label={`${category.name} (kg)`}
                    inputMode="decimal" placeholder="0" value={weights[category.id] ?? ''}
                    onChange={(event) => setWeights((previous) => ({ ...previous, [category.id]: event.target.value }))}
                    disabled={isSaving || locked} className="min-h-[48px]" />
                ))}
              </CardContent>
            </Card>
          )
        })}

        <div className="fixed bottom-16 md:bottom-0 inset-x-0 z-20 px-4 pb-3">
          <div className={cn('max-w-3xl mx-auto rounded-2xl border-2 p-3 shadow-lg bg-white flex flex-col gap-2',
            conforming ? 'border-emerald-500' : 'border-red-500')} data-testid="quality-bar">
            <div className="grid grid-cols-3 gap-2 text-center text-sm">
              <div><p className="text-xs text-slate-500">Clasificado</p><strong>{formatKg(check.classified)} kg</strong></div>
              <div><p className="text-xs text-slate-500">Submuestra</p><strong>{formatKg(sampleValue ?? 0)} kg</strong></div>
              <div>
                <p className="text-xs text-slate-500">Diferencia</p>
                <strong>{formatKg(check.differenceKg)} kg ({formatKg(check.differencePercent)} %)</strong>
              </div>
            </div>
            <p className={cn('text-center font-bold', conforming ? 'text-emerald-700' : 'text-red-700')}>
              {conforming
                ? `Conforme (≤ ${formatKg(settings.qualityTolerancePercent, 0)} %)`
                : `No conforme (> ${formatKg(settings.qualityTolerancePercent, 0)} %)`}
            </p>
            <Button type="submit" className="min-h-[48px]" isLoading={isSaving}
              disabled={isSaving || !!sampleMessage || !hasWeights || shift.status !== 'ABIERTO'}>
              Guardar cuarteo
            </Button>
          </div>
        </div>
      </form>
    </div>
  )
}
