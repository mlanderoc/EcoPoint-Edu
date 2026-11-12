/**
 * Studies of the institution (Spec 004, R-332).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ClipboardList, Plus } from 'lucide-react'
import { AlertBanner, Button, Card, CardContent } from '@/components/ui'
import { CharacterizationStudy } from '@/types'
import { studyService } from '../services/studyService'
import { formatDate } from '../utils/samplingDays'
import { STATUS_LABELS, studyTitle } from '../utils/studyStatus'

export function StudiesPage() {
  const navigate = useNavigate()
  const [studies, setStudies] = useState<CharacterizationStudy[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    studyService.list()
      .then(setStudies)
      .catch(() => setError('No se pudieron cargar los estudios.'))
      .finally(() => setIsLoading(false))
  }, [])

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

      {error && <AlertBanner variant="danger" message={error} />}
      {isLoading && <p className="text-sm text-[var(--color-text-muted)]">Cargando estudios...</p>}
      {!isLoading && studies.length === 0 && !error && (
        <p className="text-sm text-[var(--color-text-muted)]">Todavía no hay estudios. Cree el primero.</p>
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
                  </div>
                </div>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-[var(--color-primary-light)] text-[var(--color-primary-hover)]">
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
