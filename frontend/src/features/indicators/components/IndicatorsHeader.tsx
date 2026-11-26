/**
 * Study selector and the "Preliminar" or "Oficial" label (Spec 006, R-520,
 * R-502, CA-504; D15).
 *
 * @author craulgomez
 */

import { Select } from '@/components/ui'
import { STATUS_LABELS, studyTitle } from '@/features/study/utils/studyStatus'
import { CharacterizationStudy } from '@/types'
import { StudyIndicators } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'
import { ReportDownloads } from './ReportDownloads'

export interface IndicatorsHeaderProps {
  studies: CharacterizationStudy[]
  studyId: number | null
  onStudyChange: (studyId: number) => void
  indicators: StudyIndicators | null
  institutionName?: (institutionId: number) => string | undefined
}

export function IndicatorsHeader({ studies, studyId, onStudyChange, indicators, institutionName }: IndicatorsHeaderProps) {
  const verified = indicators ? `${formatNumber(indicators.quality.verifiedPercentage)} % de los registros verificados` : ''
  const optionLabel = (study: CharacterizationStudy) => {
    const name = institutionName?.(study.institutionId)
    return [studyTitle(study), name, STATUS_LABELS[study.status]].filter(Boolean).join(' · ')
  }
  const chosen = studies.find((study) => study.id === studyId)
  return (
    <div className="flex flex-col gap-3">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Indicadores de línea base</h1>
          {chosen && <p className="m-0 mt-1">{optionLabel(chosen)}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {indicators && (
            <span data-testid="indicators-label" title={verified}
              className={indicators.official ? 'pill' : 'pill warm'}>
              {indicators.label}
            </span>
          )}
          {indicators && studyId !== null && <ReportDownloads studyId={studyId} />}
        </div>
      </div>
      {indicators && !indicators.official && (
        <p className="m-0 text-sm text-amber-900">Datos sin depurar del todo: {verified}.</p>
      )}
      {studies.length > 0 && (
        <div className="sm:max-w-2xl">
          <Select id="indicators-study" label="Ver los indicadores de" value={String(studyId ?? '')}
            placeholder="Escoja un estudio"
            onChange={(event) => onStudyChange(Number(event.target.value))}
            options={studies.map((study) => ({ value: String(study.id), label: optionLabel(study) }))} />
        </div>
      )}
    </div>
  )
}
