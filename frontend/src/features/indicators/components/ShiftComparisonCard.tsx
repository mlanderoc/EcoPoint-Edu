/**
 * Morning versus afternoon: averages per shift and which one generates more
 * (Spec 006, R-525; D15).
 *
 * @author craulgomez
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { ShiftComparison } from '../types/indicatorTypes'
import { SHIFT_LABELS, formatNumber } from '../utils/format'

function Gap({ label, morning, afternoon, decimals, unit = '' }: {
  label: string; morning: number | null; afternoon: number | null; decimals: number; unit?: string
}) {
  if (morning === null || afternoon === null) return null
  const leader = morning === afternoon ? 'Igual' : morning > afternoon ? 'Mañana' : 'Tarde'
  return (
    <div>
      <small>{label}</small>
      <b>+{formatNumber(Math.abs(morning - afternoon), decimals)}{unit}</b>
      <span>{leader}</span>
    </div>
  )
}

function ShiftVerdict({ morning, afternoon }: { morning: ShiftComparison; afternoon: ShiftComparison }) {
  const morningKg = morning.averageGeneration
  const afternoonKg = afternoon.averageGeneration
  if (morningKg === null || afternoonKg === null || morningKg + afternoonKg <= 0) return null
  const morningShare = (morningKg / (morningKg + afternoonKg)) * 100
  const gap = Math.abs(morningKg - afternoonKg)
  const base = Math.min(morningKg, afternoonKg)
  const morningLeads = morningKg > afternoonKg
  return (
    <div className="shift-compare" data-testid="shift-verdict">
      <h4 className="m-0 text-base font-black text-[var(--color-brand-dark)]">¿Qué jornada genera más?</h4>
      <p className="shift-verdict">
        {gap === 0 ? 'Ambas jornadas generan lo mismo.' : (
          <>
            <b>{morningLeads ? 'La mañana' : 'La tarde'}</b> genera {formatNumber(gap)} kg más por día
            {base > 0 ? `, un ${formatNumber((gap / base) * 100, 1)} % más que ${morningLeads ? 'la tarde' : 'la mañana'}` : ''}.
          </>
        )}
      </p>
      <div className="shift-split" role="img"
        aria-label={`Mañana ${formatNumber(morningShare, 1)} % y tarde ${formatNumber(100 - morningShare, 1)} % de la generación`}>
        <span style={{ width: `${morningShare}%` }}>Mañana {formatNumber(morningShare, 1)} %</span>
        <span>Tarde {formatNumber(100 - morningShare, 1)} %</span>
      </div>
      <div className="shift-gaps">
        <Gap label="Personas" morning={morning.averagePopulation} afternoon={afternoon.averagePopulation} decimals={0} />
        <Gap label="Por persona" morning={morning.averagePpc} afternoon={afternoon.averagePpc} decimals={3} unit=" kg" />
        <Gap label="Densidad" morning={morning.averageDensity} afternoon={afternoon.averageDensity} decimals={1} unit=" kg/m³" />
      </div>
    </div>
  )
}

export function ShiftComparisonCard({ shifts }: { shifts: ShiftComparison[] }) {
  const morning = shifts.find((shift) => shift.type === 'MANANA')
  const afternoon = shifts.find((shift) => shift.type === 'TARDE')
  return (
    <Card>
      <CardHeader><CardTitle>Mañana frente a tarde</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-4">
        <table className="data-table" data-testid="shift-comparison">
          <thead>
            <tr>
              <th>Promedio por turno</th>
              {shifts.map((shift) => <th key={shift.type} className="num">{SHIFT_LABELS[shift.type]}</th>)}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Generación (kg)</td>
              {shifts.map((shift) => <td key={shift.type} className="num">{formatNumber(shift.averageGeneration)}</td>)}
            </tr>
            <tr>
              <td>Población</td>
              {shifts.map((shift) => <td key={shift.type} className="num">{formatNumber(shift.averagePopulation, 0)}</td>)}
            </tr>
            <tr>
              <td>PPC (kg/persona)</td>
              {shifts.map((shift) => <td key={shift.type} className="num">{formatNumber(shift.averagePpc, 3)}</td>)}
            </tr>
            <tr>
              <td>Densidad (kg/m³)</td>
              {shifts.map((shift) => <td key={shift.type} className="num">{formatNumber(shift.averageDensity)}</td>)}
            </tr>
          </tbody>
        </table>
        {morning && afternoon && <ShiftVerdict morning={morning} afternoon={afternoon} />}
      </CardContent>
    </Card>
  )
}
