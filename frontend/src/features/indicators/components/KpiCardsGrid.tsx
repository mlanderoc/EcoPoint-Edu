/**
 * The six main indicators of the poster (Spec 006, R-521).
 *
 * @author craulgomez
 */

import { SummaryKPIs } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'
import { KpiCard } from './KpiCard'

export function KpiCardsGrid({ summary, compositionNote }: { summary: SummaryKPIs; compositionNote: string }) {
  return (
    <div className="grid grid-cols-1 min-[400px]:grid-cols-2 lg:grid-cols-3 gap-3" data-testid="kpi-cards">
      <KpiCard label="Generación diaria promedio" value={formatNumber(summary.averageDailyGeneration)} unit="kg/día"
        hint={`Días 2 a 8 (${summary.countedDays} días)`} />
      <KpiCard label="PPC promedio" value={formatNumber(summary.averagePpc, 3)} unit="kg/persona/día"
        hint={`Desviación estándar ${formatNumber(summary.ppcStandardDeviation, 3)}`} />
      <KpiCard label="Densidad promedio" value={formatNumber(summary.averageDensity)} unit="kg/m³" />
      <KpiCard label="Fracción aprovechable" value={formatNumber(summary.recoverableFraction)} unit="%"
        hint={`${formatNumber(summary.recoverableDailyGeneration)} kg/día aprovechables`} />
      <KpiCard label="Composición física" value="Ver detalle" href="#composicion" hint={compositionNote} />
      <KpiCard label="CV de la PPC" value={formatNumber(summary.ppcVariationCoefficient)} unit="%" />
    </div>
  )
}
