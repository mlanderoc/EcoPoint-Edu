/**
 * Double-entry tray: data cleaning summary by shift and the records of a shift
 * to check against the paper form (Spec 005, T405; R-420 to R-424, R-426,
 * CA-408, CA-410).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertBanner, Button, Card, CardContent, CardHeader, CardTitle, Select } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { studyService } from '@/features/study/services/studyService'
import { studyTitle } from '@/features/study/utils/studyStatus'
import { ApiError } from '@/services/apiClient'
import { CharacterizationStudy, RecordType, VerifiableRecord, VerificationStatus, VerificationSummary } from '@/types'
import { cn } from '@/utils/cn'
import { RecordCard } from '../components/RecordCard'
import { verificationService } from '../services/verificationService'

const TYPE_OPTIONS = [
  { value: '', label: 'Todos los tipos' },
  { value: 'population', label: 'Población' },
  { value: 'bag', label: 'Bolsas' },
  { value: 'quartering', label: 'Cuarteo' },
  { value: 'density', label: 'Densidad' },
]
const STATUS_OPTIONS = [
  { value: 'PENDIENTE', label: 'Pendientes' },
  { value: 'VERIFICADO', label: 'Verificados' },
  { value: 'CORREGIDO', label: 'Corregidos' },
  { value: '', label: 'Todos los estados' },
]

const key = (record: VerifiableRecord) => `${record.recordType}-${record.recordId}`

export function VerificationPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [studies, setStudies] = useState<CharacterizationStudy[]>([])
  const [studyId, setStudyId] = useState<number | null>(null)
  const [summary, setSummary] = useState<VerificationSummary | null>(null)
  const [shiftId, setShiftId] = useState<number | null>(null)
  const [records, setRecords] = useState<VerifiableRecord[]>([])
  const [type, setType] = useState('')
  const [status, setStatus] = useState<VerificationStatus | ''>('PENDIENTE')
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [error, setError] = useState<string | null>(null)
  const [isWorking, setIsWorking] = useState(false)

  useEffect(() => {
    studyService.list()
      .then((all) => {
        const open = all.filter((study) => study.status === 'EN_EJECUCION' || study.status === 'EN_DEPURACION')
        setStudies(open)
        setStudyId(open[0]?.id ?? null)
      })
      .catch(() => setError('No se pudieron cargar los estudios.'))
  }, [])

  const loadSummary = useCallback(async () => {
    if (studyId === null) return
    try {
      const loaded = await verificationService.summary(studyId)
      setSummary(loaded)
      setShiftId((current) => current ?? loaded.shifts.find((shift) => shift.pending > 0)?.shiftId ?? loaded.shifts[0]?.shiftId ?? null)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cargar el resumen.')
    }
  }, [studyId])

  const loadRecords = useCallback(async () => {
    if (shiftId === null) return
    setRecords(await verificationService.shiftRecords(shiftId))
    setSelected(new Set())
  }, [shiftId])

  useEffect(() => { loadSummary() }, [loadSummary])
  useEffect(() => { loadRecords().catch(() => setError('No se pudieron cargar los registros del turno.')) }, [loadRecords])

  const visible = useMemo(() => records
    .filter((record) => !type || record.recordType === type)
    .filter((record) => !status || record.verification.status === status), [records, type, status])

  const canAct = (record: VerifiableRecord) =>
    record.verification.status === 'PENDIENTE' && record.verification.enteredBy !== user?.id

  const verify = async (targets: VerifiableRecord[]) => {
    setError(null)
    setIsWorking(true)
    const failed: string[] = []
    for (const record of targets) {
      try {
        await verificationService.verify(record.recordType as RecordType, record.recordId)
      } catch {
        failed.push(record.title)
      }
    }
    if (failed.length > 0) setError(`No se pudieron verificar: ${failed.join(', ')}.`)
    await Promise.all([loadRecords(), loadSummary()])
    setIsWorking(false)
  }

  const toggle = (record: VerifiableRecord) => setSelected((previous) => {
    const next = new Set(previous)
    if (next.has(key(record))) next.delete(key(record))
    else next.add(key(record))
    return next
  })

  return (
    <div className="max-w-4xl w-full mx-auto flex flex-col gap-5">
      <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Verificación del doble registro</h1>
      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}

      {studies.length === 0 && !error && (
        <AlertBanner variant="info" message="No hay estudios en ejecución ni en depuración para verificar." />
      )}
      {studies.length > 1 && (
        <Select id="verification-study" label="Estudio" value={String(studyId ?? '')}
          onChange={(event) => { setStudyId(Number(event.target.value)); setShiftId(null) }}
          options={studies.map((study) => ({ value: String(study.id), label: studyTitle(study) }))} />
      )}

      {summary && (
        <Card>
          <CardHeader><CardTitle>Avance de la depuración</CardTitle></CardHeader>
          <CardContent className="flex flex-col gap-3">
            <AlertBanner variant={summary.readyToClose ? 'success' : 'warning'}
              title={summary.readyToClose ? 'Listo para cerrar' : 'El estudio aún no puede cerrarse'}
              message={`${summary.pending} pendientes · ${summary.verified} verificados · ${summary.corrected} corregidos · ${summary.nonConformingQuarterings} cuarteos no conformes`} />
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2" data-testid="shift-progress">
              {summary.shifts.map((shift) => (
                <button key={shift.shiftId} type="button" onClick={() => setShiftId(shift.shiftId)}
                  className={cn('min-h-[56px] rounded-lg border-2 px-2 py-1 text-left text-xs',
                    shiftId === shift.shiftId ? 'border-[var(--color-primary)] bg-[var(--color-primary-light)]' : 'border-slate-200')}>
                  <span className="block font-bold text-sm">{shift.code}</span>
                  {shift.pending} pend. · {shift.verified + shift.corrected} listos
                  {shift.nonConformingQuartering && <span className="block text-red-700 font-semibold">Cuarteo no conforme</span>}
                </button>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {shiftId !== null && (
        <Card>
          <CardHeader><CardTitle>Registros del turno</CardTitle></CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="grid grid-cols-2 gap-3">
              <Select id="verification-type" label="Tipo" value={type} onChange={(event) => setType(event.target.value)}
                options={TYPE_OPTIONS} />
              <Select id="verification-status" label="Estado" value={status}
                onChange={(event) => setStatus(event.target.value as VerificationStatus | '')} options={STATUS_OPTIONS} />
            </div>
            {selected.size > 0 && (
              <Button className="min-h-[48px]" isLoading={isWorking} disabled={isWorking}
                onClick={() => verify(records.filter((record) => selected.has(key(record))))}>
                Coinciden con el papel ({selected.size})
              </Button>
            )}
            {visible.length === 0 && <p className="text-sm text-slate-500">No hay registros con estos filtros.</p>}
            {visible.map((record) => (
              <RecordCard key={key(record)} record={record}>
                {canAct(record) ? (
                  <div className="flex flex-wrap items-center gap-2">
                    <label className="flex items-center gap-2 min-h-[48px] text-sm">
                      <input type="checkbox" className="w-5 h-5" checked={selected.has(key(record))}
                        onChange={() => toggle(record)} aria-label={`Seleccionar ${record.title}`} />
                      Seleccionar
                    </label>
                    <Button className="min-h-[48px]" disabled={isWorking} onClick={() => verify([record])}>
                      Coincide con el papel
                    </Button>
                    <Button variant="outline" className="min-h-[48px]"
                      onClick={() => navigate(`/calidad/registro/${record.recordType}/${record.recordId}`)}>
                      Corregir
                    </Button>
                  </div>
                ) : (
                  <div className="flex items-center justify-between gap-2 text-xs text-slate-500">
                    <span>{record.verification.enteredBy === user?.id && record.verification.status === 'PENDIENTE'
                      ? 'Lo digitó usted: debe verificarlo otra persona.' : ''}</span>
                    <Button variant="ghost" className="min-h-[48px]"
                      onClick={() => navigate(`/calidad/registro/${record.recordType}/${record.recordId}`)}>
                      Ver historial
                    </Button>
                  </div>
                )}
              </RecordCard>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
