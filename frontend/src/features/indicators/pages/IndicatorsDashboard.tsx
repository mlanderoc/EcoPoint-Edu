/**
 * Baseline dashboard of a study, chosen among every institution the user can
 * see (Spec 006, T506 and T507; R-520 to R-525; D15).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { AlertBanner } from '@/components/ui'
import { useInstitutions } from '@/features/admin/components/InstitutionSelect'
import { studyService } from '@/features/study/services/studyService'
import { CharacterizationStudy } from '@/types'
import { CompositionDonutChart } from '../components/CompositionDonutChart'
import { CompositionTable } from '../components/CompositionTable'
import { DailyGenerationChart } from '../components/DailyGenerationChart'
import { IndicatorsHeader } from '../components/IndicatorsHeader'
import { KpiCardsGrid } from '../components/KpiCardsGrid'
import { ShiftComparisonCard } from '../components/ShiftComparisonCard'
import { SourceGenerationChart } from '../components/SourceGenerationChart'
import { useIndicators } from '../hooks/useIndicators'

export function IndicatorsDashboard() {
  const { institutions } = useInstitutions(false)
  const [studies, setStudies] = useState<CharacterizationStudy[]>([])
  const [studyId, setStudyId] = useState<number | null>(null)
  const [listError, setListError] = useState<string | null>(null)
  const { indicators, isLoading, error } = useIndicators(studyId)

  useEffect(() => {
    studyService.visible(institutions.map((institution) => institution.id))
      .then((loaded) => {
        setListError(null)
        // A planned study has no data yet, and a cancelled one has no baseline (D13, A20).
        const withData = loaded.filter((study) => study.status !== 'PLANIFICADO' && study.status !== 'CANCELADO')
        setStudies(withData)
        setStudyId((current) => current ?? (withData.length === 1 ? withData[0].id : null))
      })
      .catch(() => setListError('No se pudieron cargar los estudios.'))
  }, [institutions])

  const institutionName = (id: number) => institutions.find((item) => item.id === id)?.name

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-5">
      <IndicatorsHeader studies={studies} studyId={studyId} onStudyChange={setStudyId} indicators={indicators}
        institutionName={institutionName} />
      {(listError || error) && <AlertBanner variant="danger" message={listError ?? error ?? ''} />}
      {!listError && studies.length === 0 && (
        <AlertBanner variant="info" message="Aún no hay estudios con datos de campo." />
      )}
      {studyId === null && studies.length > 1 && (
        <AlertBanner variant="info" title="Escoja un estudio" message="Elija arriba el estudio cuyos indicadores quiere ver." />
      )}
      {isLoading && !indicators && <p className="text-sm text-slate-500">Calculando indicadores...</p>}
      {indicators && (
        <>
          <KpiCardsGrid summary={indicators.summary} compositionNote={indicators.composition.note} />
          <div className="grid-side" style={{ alignItems: 'stretch' }}>
            <DailyGenerationChart days={indicators.days} />
            <CompositionDonutChart composition={indicators.composition}
              recoverable={indicators.summary.recoverableFraction} />
          </div>
          <div className="grid-2">
            <SourceGenerationChart sources={indicators.sources} />
            <ShiftComparisonCard shifts={indicators.shifts} />
          </div>
          <div id="composicion" className="scroll-mt-4">
            <CompositionTable composition={indicators.composition} />
          </div>
        </>
      )}
    </div>
  )
}
