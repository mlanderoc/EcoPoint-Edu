/**
 * Composition by the four groups of Resolution 2184 of 2019 as a donut
 * (Spec 006, R-523).
 *
 * @author craulgomez
 */

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { Composition } from '../types/indicatorTypes'
import { GROUP_COLORS, formatNumber } from '../utils/format'

export function CompositionDonutChart({ composition }: { composition: Composition }) {
  const data = composition.groups.filter((group) => (group.percentage ?? 0) > 0)
  return (
    <Card>
      <CardHeader><CardTitle>Composición por grupo</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-3">
        {data.length === 0 ? (
          <p className="text-sm text-slate-500">Aún no hay cuarteos válidos para calcular la composición.</p>
        ) : (
          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={data} dataKey="percentage" nameKey="name" innerRadius="55%" outerRadius="85%" isAnimationActive={false}
                  stroke="#ffffff" strokeWidth={2}>
                  {data.map((group) => <Cell key={group.group} fill={GROUP_COLORS[group.group]} />)}
                </Pie>
                <Tooltip formatter={(value, name) => [`${formatNumber(Number(value))} %`, String(name)]} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}
        <ul className="grid grid-cols-2 gap-2 text-sm" data-testid="group-shares">
          {composition.groups.map((group) => (
            <li key={group.group} className="flex items-center gap-2">
              <span className="inline-block w-3 h-3 rounded-sm border border-slate-300 shrink-0"
                style={{ backgroundColor: GROUP_COLORS[group.group] }} aria-hidden="true" />
              <span>{group.name}: <strong>{formatNumber(group.percentage)} %</strong></span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
