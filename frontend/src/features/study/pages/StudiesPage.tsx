/**
 * Studies of an institution, chosen with a filter kept in the URL. A closed
 * study is shown without the button to open it (Spec 004, R-332, R-337; D14, D15).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { AlertBanner, Button } from '@/components/ui'
import { CharacterizationStudy, ShiftSlot, StudyStatus } from '@/types'
import { studyService } from '../services/studyService'
import { InstitutionSelect, useInstitutionParam, useInstitutions } from '@/features/admin/components/InstitutionSelect'
import { formatDate } from '../utils/samplingDays'
import { STATUS_LABELS, studyTitle } from '../utils/studyStatus'

const STATUS_TONE: Record<StudyStatus, string> = {
  PLANIFICADO: 'pill grey',
  EN_EJECUCION: 'pill',
  EN_DEPURACION: 'pill warm',
  CERRADO: 'pill dark',
  CANCELADO: 'pill grey',
}

const STATUS_FILTERS: Array<{ value: StudyStatus | ''; label: string }> = [
  { value: '', label: 'Todos' },
  { value: 'PLANIFICADO', label: 'Planificados' },
  { value: 'EN_EJECUCION', label: 'En ejecución' },
  { value: 'EN_DEPURACION', label: 'En depuración' },
  { value: 'CERRADO', label: 'Cerrados' },
  { value: 'CANCELADO', label: 'Cancelados' },
]

function MiniTrail({ slots, running }: { slots: ShiftSlot[]; running: boolean }) {
  const closed = slots.filter((slot) => slot.status === 'CERRADO').length
  const cells = Array.from({ length: 16 }, (_, index) => {
    if (index < closed) return 'done'
    return running && index === closed ? 'now' : ''
  })
  return (
    <div className="mini-trail" role="img" aria-label={`${closed} de 16 turnos cerrados`}>
      {cells.map((cell, index) => <i key={index} className={cell} />)}
    </div>
  )
}

export function StudiesPage() {
  const navigate = useNavigate()
  const [studies, setStudies] = useState<CharacterizationStudy[]>([])
  const [shiftsByStudy, setShiftsByStudy] = useState<Record<number, ShiftSlot[]>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [institutionId, setInstitutionId] = useInstitutionParam()
  const { institutions } = useInstitutions(false)
  const [params, setParams] = useSearchParams()
  const statusFilter = (params.get('estadoEstudio') ?? '') as StudyStatus | ''
  const visible = statusFilter ? studies.filter((study) => study.status === statusFilter) : studies
  const chooseStatus = (value: StudyStatus | '') => {
    const updated = new URLSearchParams(params)
    if (value) updated.set('estadoEstudio', value)
    else updated.delete('estadoEstudio')
    setParams(updated, { replace: true })
  }
  const institutionName = (id: number) => institutions.find((item) => item.id === id)?.name

  useEffect(() => {
    if (institutionId === null) return
    setIsLoading(true)
    setError(null)
    studyService.list(institutionId)
      .then((loaded) => {
        setStudies(loaded)
        loaded.forEach((study) => {
          studyService.shifts(study.id)
            .then((slots) => setShiftsByStudy((previous) => ({ ...previous, [study.id]: slots })))
            .catch(() => undefined)
        })
      })
      .catch(() => setError('No se pudieron cargar los estudios.'))
      .finally(() => setIsLoading(false))
  }, [institutionId])

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-5">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Estudios de caracterización</h1>
          <p className="m-0 mt-1">Planifica, ejecuta, depura y cierra cada estudio.</p>
        </div>
        <Button className="min-h-[48px]" onClick={() => navigate('/estudios/nuevo')}>
          <Plus className="w-4 h-4 mr-2" /> Nuevo estudio
        </Button>
      </div>

      <div className="sm:max-w-md">
        <InstitutionSelect
          institutions={institutions}
          value={institutionId}
          onChange={setInstitutionId}
          label="Ver los estudios de"
        />
      </div>

      <div className="segmented self-start max-w-full overflow-x-auto" role="group" aria-label="Filtrar por estado del estudio">
        {STATUS_FILTERS.map((option) => {
          const count = option.value ? studies.filter((study) => study.status === option.value).length : studies.length
          return (
            <button key={option.value} type="button" className="whitespace-nowrap" aria-pressed={statusFilter === option.value}
              onClick={() => chooseStatus(option.value)}>
              {option.label} ({count})
            </button>
          )
        })}
      </div>

      {error && <AlertBanner variant="danger" message={error} />}
      {isLoading && <p className="text-sm text-[var(--color-text-muted)]">Cargando estudios...</p>}
      {!isLoading && studies.length === 0 && !error && (
        <p className="text-sm text-[var(--color-text-muted)]">Esta institución todavía no tiene estudios.</p>
      )}

      {!isLoading && studies.length > 0 && visible.length === 0 && (
        <p className="text-sm text-[var(--color-text-muted)]">No hay estudios con este estado.</p>
      )}

      <div className="study-grid">
        {visible.map((study) => {
          const slots = shiftsByStudy[study.id] ?? []
          const closedShifts = slots.filter((slot) => slot.status === 'CERRADO').length
          return (
            <article key={study.id} className="study-card">
              <div>
                <span className={STATUS_TONE[study.status]}>{STATUS_LABELS[study.status]}</span>
                <h3>{studyTitle(study)}</h3>
                <span className="text-sm text-[var(--color-text-muted)]">
                  {formatDate(study.startDate)} a {formatDate(study.endDate)}
                </span>
              </div>
              {study.status !== 'CERRADO' && (
                <button className="study-open" type="button" aria-label={`Abrir ${studyTitle(study)}`}
                  onClick={() => navigate(`/estudios/${study.id}`)}>
                  Abrir
                </button>
              )}
              <MiniTrail slots={slots} running={study.status === 'EN_EJECUCION'} />
              <div className="study-meta">
                <span>{closedShifts} de 16 turnos</span>
                <span>{study.crewName ?? 'Sin cuadrilla'}</span>
                <span>{study.participatingSourceIds.length} fuentes</span>
                <span>{study.pendingRecords} por verificar</span>
                {institutionName(study.institutionId) && <span>{institutionName(study.institutionId)}</span>}
              </div>
            </article>
          )
        })}
      </div>
    </div>
  )
}
