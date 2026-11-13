/**
 * Baseline dashboard of a study (Spec 006, T506 and T507; R-520 to R-527).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { AlertBanner } from '@/components/ui'
import { studyService } from '@/features/study/services/studyService'
import { CharacterizationStudy } from '@/types'
import { CompositionDonutChart } from '../components/CompositionDonutChart'
import { CompositionTable } from '../components/CompositionTable'
import { DailyGenerationChart } from '../components/DailyGenerationChart'
import { DataQualityPanel } from '../components/DataQualityPanel'
import { IndicatorsHeader } from '../components/IndicatorsHeader'
import { KpiCardsGrid } from '../components/KpiCardsGrid'
import { ShiftComparisonCard } from '../components/ShiftComparisonCard'
import { SourceGenerationChart } from '../components/SourceGenerationChart'
import { useIndicators } from '../hooks/useIndicators'

export function IndicatorsDashboard() {
  const [studies, setStudies] = useState<CharacterizationStudy[]>([])
  const [studyId, setStudyId] = useState<number | null>(null)
  const [listError, setListError] = useState<string | null>(null)
  const { indicators, isLoading, error } = useIndicators(studyId)

  useEffect(() => {
    studyService.list()
      .then((all) => {
        const withData = all.filter((study) => study.status !== 'PLANIFICADO')
        setStudies(withData)
        setStudyId(withData[0]?.id ?? null)
      })
      .catch(() => setListError('No se pudieron cargar los estudios.'))
  }, [])

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-5">
      <IndicatorsHeader studies={studies} studyId={studyId} onStudyChange={setStudyId} indicators={indicators} />
      {(listError || error) && <AlertBanner variant="danger" message={listError ?? error ?? ''} />}
      {!listError && studies.length === 0 && (
        <AlertBanner variant="info" message="Aún no hay estudios con datos de campo." />
      )}
      {isLoading && !indicators && <p className="text-sm text-slate-500">Calculando indicadores...</p>}
      {indicators && (
        <>
          <KpiCardsGrid summary={indicators.summary} compositionNote={indicators.composition.note} />
          <DailyGenerationChart days={indicators.days} />
          <div id="composicion" className="grid grid-cols-1 lg:grid-cols-2 gap-5 scroll-mt-4">
            <CompositionDonutChart composition={indicators.composition} />
            <CompositionTable composition={indicators.composition} />
          </div>
          <SourceGenerationChart sources={indicators.sources} />
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            <ShiftComparisonCard shifts={indicators.shifts} />
            <DataQualityPanel quality={indicators.quality} />
          </div>
        </>
      )}
    </div>
  )
}
