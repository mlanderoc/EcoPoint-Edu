/**
 * Studies of an institution, chosen with a filter kept in the URL
 * (Spec 004, R-332, R-337; D14).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ClipboardList, Plus } from 'lucide-react'
import { AlertBanner, Button, Card, CardContent } from '@/components/ui'
import { CharacterizationStudy } from '@/types'
import { studyService } from '../services/studyService'
import { InstitutionSelect, useInstitutionParam, useInstitutions } from '@/features/admin/components/InstitutionSelect'
import { formatDate } from '../utils/samplingDays'
import { STATUS_LABELS, studyTitle } from '../utils/studyStatus'

export function StudiesPage() {
  const navigate = useNavigate()
  const [studies, setStudies] = useState<CharacterizationStudy[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [institutionId, setInstitutionId] = useInstitutionParam()
  const { institutions } = useInstitutions(false)
  const institutionName = (id: number) => institutions.find((item) => item.id === id)?.name

  useEffect(() => {
    if (institutionId === null) return
    setIsLoading(true)
    setError(null)
    studyService.list(institutionId)
      .then(setStudies)
      .catch(() => setError('No se pudieron cargar los estudios.'))
      .finally(() => setIsLoading(false))
  }, [institutionId])

  return (
    <div className="max-w-4xl w-full mx-auto flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-[var(--color-brand-dark)]">Estudios de caracterización</h1>
          <p className="text-sm text-[var(--color-text-muted)]">Planificación, ejecución, depuración y cierre.</p>
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

      {error && <AlertBanner variant="danger" message={error} />}
      {isLoading && <p className="text-sm text-[var(--color-text-muted)]">Cargando estudios...</p>}
      {!isLoading && studies.length === 0 && !error && (
        <p className="text-sm text-[var(--color-text-muted)]">Esta institución todavía no tiene estudios.</p>
      )}

      <div className="flex flex-col gap-3">
        {studies.map((study) => (
          <button key={study.id} type="button" onClick={() => navigate(`/estudios/${study.id}`)} className="text-left">
            <Card className="hover:border-[var(--color-primary)] transition-colors">
              <CardContent className="flex flex-wrap items-center justify-between gap-3 py-4">
                <div className="flex items-center gap-3">
                  <ClipboardList className="w-5 h-5 text-[var(--color-primary)]" />
                  <div>
                    <p className="font-semibold">{studyTitle(study)}</p>
                    <p className="text-xs text-[var(--color-text-muted)]">
                      {formatDate(study.startDate)} — {formatDate(study.endDate)}
                    </p>
                    {institutionName(study.institutionId) && (
                      <p className="text-xs text-[var(--color-text-muted)]">{institutionName(study.institutionId)}</p>
                    )}
                  </div>
                </div>
                <span className={study.status === 'CANCELADO'
                  ? 'text-xs font-semibold px-3 py-1 rounded-full bg-slate-100 text-slate-600'
                  : 'text-xs font-semibold px-3 py-1 rounded-full bg-[var(--color-primary-light)] text-[var(--color-primary-hover)]'}>
                  {STATUS_LABELS[study.status]}
                </span>
              </CardContent>
            </Card>
          </button>
        ))}
      </div>
    </div>
  )
}
