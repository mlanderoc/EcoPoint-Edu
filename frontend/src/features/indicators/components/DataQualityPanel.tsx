/**
 * Data quality of the study: verified share, corrections and non-conforming
 * quarterings (Spec 006, R-526).
 *
 * @author craulgomez
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { DataQuality } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'

export function DataQualityPanel({ quality }: { quality: DataQuality }) {
  const percent = Math.min(quality.verifiedPercentage ?? 0, 100)
  return (
    <Card>
      <CardHeader><CardTitle>Calidad del dato</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-3" data-testid="data-quality">
        <div className="flex justify-between text-sm">
          <span>Registros verificados</span>
          <strong>{quality.verifiedRecords} de {quality.totalRecords} ({formatNumber(quality.verifiedPercentage)} %)</strong>
        </div>
        <div className="h-3 w-full rounded-full bg-slate-100 overflow-hidden" aria-hidden="true">
          <div className="h-full bg-emerald-600" style={{ width: `${percent}%` }} />
        </div>
        <div className="flex justify-between text-sm">
          <span>Correcciones contra la planilla</span><strong>{quality.correctedRecords}</strong>
        </div>
        <div className="flex justify-between text-sm">
          <span>Cuarteos no conformes</span>
          <strong className={quality.nonConformingQuarterings > 0 ? 'text-red-700' : ''}>
            {quality.nonConformingQuarterings}
          </strong>
        </div>
      </CardContent>
    </Card>
  )
}
