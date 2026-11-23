/**
 * Double-entry tray: data cleaning summary by shift and the records of a shift
 * to check against the paper form (Spec 005, T405; R-420 to R-424, R-426,
 * CA-408, CA-410).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertBanner, Button, Select } from '@/components/ui'
import { useInstitutions } from '@/features/admin/components/InstitutionSelect'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { studyService } from '@/features/study/services/studyService'
import { studyTitle } from '@/features/study/utils/studyStatus'
import { ApiError } from '@/services/apiClient'
import { CharacterizationStudy, RecordType, VerifiableRecord, VerificationStatus, VerificationSummary } from '@/types'
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
  const { institutions } = useInstitutions(false)
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
    studyService.verifiable()
      .then((open) => {
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

  const finished = summary ? summary.verified + summary.corrected : 0
  const total = summary ? finished + summary.pending : 0
  const percent = total > 0 ? Math.round((finished / total) * 100) : 0

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-4">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Verificación del doble registro</h1>
          <p className="m-0 mt-1">Compara la planilla en papel con lo digitado.</p>
        </div>
      </div>
      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}

      {studies.length === 0 && !error && (
        <AlertBanner variant="info" message="No tienes estudios en ejecución ni en depuración para verificar." />
      )}
      {studies.length > 1 && (
        <div className="sm:max-w-md">
          <Select id="verification-study" label="Estudio" value={String(studyId ?? '')}
            onChange={(event) => { setStudyId(Number(event.target.value)); setShiftId(null) }}
            options={studies.map((study) => {
              const name = institutions.find((item) => item.id === study.institutionId)?.name
              return { value: String(study.id), label: name ? `${studyTitle(study)} · ${name}` : studyTitle(study) }
            })} />
        </div>
      )}

      <div className="grid-left">
        {summary && (
          <section className="panel">
            <h3>Avance de la depuración</h3>
            <div className="ring" style={{ background: `conic-gradient(var(--brand) ${percent}%, #e4e8db 0)` }}>
              <div><b>{percent} %</b><span className="text-[var(--muted)]">{finished} de {total}</span></div>
            </div>
            <AlertBanner variant={summary.readyToClose ? 'success' : 'warning'}
              title={summary.readyToClose ? 'Listo para cerrar' : 'El estudio aún no puede cerrarse'}
              message={`${summary.pending} pendientes · ${summary.verified} verificados · ${summary.corrected} corregidos · ${summary.nonConformingQuarterings} cuarteos no conformes`} />
            <div className="flex flex-col gap-1" data-testid="shift-progress">
              {summary.shifts.map((shift) => {
                const all = shift.pending + shift.verified + shift.corrected
                const done = shift.verified + shift.corrected
                return (
                  <button key={shift.shiftId} type="button" aria-pressed={shiftId === shift.shiftId}
                    onClick={() => setShiftId(shift.shiftId)} className="shift-pick">
                    <span className="shift-row">
                      <span className="code">{shift.code}</span>
                      <span className="track"><i style={{ width: `${all ? (done / all) * 100 : 0}%` }} /></span>
                      <span className="text-[var(--muted)]">{shift.pending} pend.</span>
                    </span>
                    {shift.nonConformingQuartering && <span className="block text-xs text-red-700 font-semibold">Cuarteo no conforme</span>}
                  </button>
                )
              })}
            </div>
          </section>
        )}

        {shiftId !== null && (
          <section className="panel">
            <h3>Registros del turno</h3>
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
            {visible.length === 0 && <p className="m-0 text-sm text-slate-500">No hay registros con estos filtros.</p>}
            {visible.map((record) => (
              <RecordCard key={key(record)} record={record}>
                {canAct(record) ? (
                  <div className="flex flex-wrap items-center gap-2">
                    <label className="flex items-center gap-2 min-h-[48px] text-sm">
                      <input type="checkbox" className="w-5 h-5 accent-[var(--brand)]" checked={selected.has(key(record))}
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
          </section>
        )}
      </div>
    </div>
  )
}
