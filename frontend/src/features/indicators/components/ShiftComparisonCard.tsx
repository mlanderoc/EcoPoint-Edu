/**
 * Morning versus afternoon: generation, population and PPC (Spec 006, R-525).
 *
 * @author craulgomez
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { ShiftComparison } from '../types/indicatorTypes'
import { SHIFT_LABELS, formatNumber } from '../utils/format'

export function ShiftComparisonCard({ shifts }: { shifts: ShiftComparison[] }) {
  return (
    <Card>
      <CardHeader><CardTitle>Mañana frente a tarde</CardTitle></CardHeader>
      <CardContent>
        <table className="w-full text-sm" data-testid="shift-comparison">
          <thead>
            <tr className="text-left text-xs text-slate-500">
              <th className="py-1 font-semibold">Promedio por turno</th>
              {shifts.map((shift) => <th key={shift.type} className="py-1 font-semibold text-right">{SHIFT_LABELS[shift.type]}</th>)}
            </tr>
          </thead>
          <tbody>
            <tr className="border-t border-slate-100">
              <td className="py-2">Generación (kg)</td>
              {shifts.map((shift) => <td key={shift.type} className="py-2 text-right">{formatNumber(shift.averageGeneration)}</td>)}
            </tr>
            <tr className="border-t border-slate-100">
              <td className="py-2">Población</td>
              {shifts.map((shift) => <td key={shift.type} className="py-2 text-right">{formatNumber(shift.averagePopulation, 0)}</td>)}
            </tr>
            <tr className="border-t border-slate-100">
              <td className="py-2">PPC (kg/persona)</td>
              {shifts.map((shift) => <td key={shift.type} className="py-2 text-right">{formatNumber(shift.averagePpc, 3)}</td>)}
            </tr>
            <tr className="border-t border-slate-100">
              <td className="py-2">Densidad (kg/m³)</td>
              {shifts.map((shift) => <td key={shift.type} className="py-2 text-right">{formatNumber(shift.averageDensity)}</td>)}
            </tr>
          </tbody>
        </table>
      </CardContent>
    </Card>
  )
}
