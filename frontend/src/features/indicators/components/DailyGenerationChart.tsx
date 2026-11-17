/**
 * Generation of days 1 to 8, with day 1 greyed out as the discard day
 * (Spec 006, R-522).
 *
 * @author craulgomez
 */

import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { DailyGeneration } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'

export function DailyGenerationChart({ days }: { days: DailyGeneration[] }) {
  const data = days.map((day) => ({ ...day, name: `Día ${day.dayNumber}` }))
  return (
    <Card>
      <CardHeader><CardTitle>Generación por día</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-2">
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} />
              <YAxis tick={{ fontSize: 11 }} unit=" kg" width={64} />
              <Tooltip formatter={(value) => [`${formatNumber(Number(value))} kg`, 'Generación']} />
              <Bar dataKey="generation" radius={[4, 4, 0, 0]}>
                {data.map((day) => (
                  <Cell key={day.dayNumber} fill={day.excluded ? '#cbd5e1' : 'var(--color-primary, #059669)'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
        <p className="text-xs text-slate-500 flex items-center gap-2">
          <span className="inline-block w-3 h-3 rounded-sm bg-slate-300" aria-hidden="true" />
          Día de descarte: se muestra pero no entra en ningún cálculo.
        </p>
        <ul className="sr-only" data-testid="daily-generation">
          {data.map((day) => (
            <li key={day.dayNumber}>{day.name}{day.excluded ? ' (descarte)' : ''}: {formatNumber(day.generation)} kg</li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
