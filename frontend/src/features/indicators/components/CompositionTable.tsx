/**
 * Subcategory detail of the composition with the note of the shifts used
 * (Spec 006, R-523, R-512, CA-505).
 *
 * @author craulgomez
 */

import { useState } from 'react'
import { Button, Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { Composition } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'

const VISIBLE_ROWS = 5

export function CompositionTable({ composition }: { composition: Composition }) {
  const [expanded, setExpanded] = useState(false)
  const rows = expanded ? composition.subcategories : composition.subcategories.slice(0, VISIBLE_ROWS)
  return (
    <Card>
      <CardHeader><CardTitle>Detalle por subcategoría</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm font-semibold text-slate-700" data-testid="composition-note">{composition.note}</p>
        {composition.subcategories.length > 0 && (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-500">
                <th className="py-1 font-semibold">Subcategoría</th>
                <th className="py-1 font-semibold text-right">kg</th>
                <th className="py-1 font-semibold text-right">%</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.categoryId} className="border-t border-slate-100">
                  <td className="py-2 pr-2">{row.name}</td>
                  <td className="py-2 text-right">{formatNumber(row.weight)}</td>
                  <td className="py-2 text-right font-semibold">{formatNumber(row.percentage)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {composition.subcategories.length > VISIBLE_ROWS && (
          <Button variant="outline" className="min-h-[48px]" onClick={() => setExpanded(!expanded)}>
            {expanded ? 'Ver menos' : `Ver las ${composition.subcategories.length} subcategorías`}
          </Button>
        )}
      </CardContent>
    </Card>
  )
}
