/**
 * Average daily generation by source, highest first (Spec 006, R-524).
 *
 * @author craulgomez
 */

import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { SourceGeneration } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'

const ROW_HEIGHT = 36

export function SourceGenerationChart({ sources }: { sources: SourceGeneration[] }) {
  return (
    <Card>
      <CardHeader><CardTitle>Generación por fuente (kg/día)</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-2">
        {sources.length === 0 ? (
          <p className="text-sm text-slate-500">Aún no hay bolsas en los días 2 a 8.</p>
        ) : (
          <div className="w-full" style={{ height: Math.max(sources.length * ROW_HEIGHT + 32, 120) }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={sources} layout="vertical" margin={{ top: 0, right: 16, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="code" tick={{ fontSize: 11 }} width={48} />
                <Tooltip formatter={(value) => [`${formatNumber(Number(value))} kg/día`, 'Promedio']}
                  labelFormatter={(code) => sources.find((source) => source.code === code)?.name ?? String(code)} />
                <Bar dataKey="averageDailyGeneration" fill="var(--color-primary, #059669)" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
        <ol className="text-sm flex flex-col gap-1" data-testid="source-ranking">
          {sources.map((source) => (
            <li key={source.sourceId} className="flex justify-between gap-2">
              <span>{source.code} · {source.name}</span>
              <strong>{formatNumber(source.averageDailyGeneration)} kg/día</strong>
            </li>
          ))}
        </ol>
      </CardContent>
    </Card>
  )
}
