/**
 * Study selector and the "Preliminar" or "Oficial" label (Spec 006, R-520,
 * R-502, CA-504).
 *
 * @author craulgomez
 */

import { Select } from '@/components/ui'
import { studyTitle } from '@/features/study/utils/studyStatus'
import { CharacterizationStudy } from '@/types'
import { StudyIndicators } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'

export interface IndicatorsHeaderProps {
  studies: CharacterizationStudy[]
  studyId: number | null
  onStudyChange: (studyId: number) => void
  indicators: StudyIndicators | null
}

export function IndicatorsHeader({ studies, studyId, onStudyChange, indicators }: IndicatorsHeaderProps) {
  const verified = indicators ? `${formatNumber(indicators.quality.verifiedPercentage)} % de los registros verificados` : ''
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Indicadores de línea base</h1>
        {indicators && (
          <span data-testid="indicators-label" title={verified}
            className={indicators.official
              ? 'text-sm font-bold px-3 py-1 rounded-full border bg-emerald-50 text-emerald-800 border-emerald-300'
              : 'text-sm font-bold px-3 py-1 rounded-full border bg-amber-50 text-amber-900 border-amber-300'}>
            {indicators.label}
          </span>
        )}
      </div>
      {indicators && !indicators.official && (
        <p className="text-sm text-amber-900">Datos sin depurar del todo: {verified}.</p>
      )}
      {studies.length > 1 && (
        <Select id="indicators-study" label="Estudio" value={String(studyId ?? '')}
          onChange={(event) => onStudyChange(Number(event.target.value))}
          options={studies.map((study) => ({ value: String(study.id), label: studyTitle(study) }))} />
      )}
    </div>
  )
}
