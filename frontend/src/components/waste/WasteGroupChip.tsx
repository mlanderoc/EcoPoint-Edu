import React from 'react'
import { WasteGroup } from '@/types'
import { cn } from '@/utils/cn'
import { wasteGroupLabels } from './WasteGroupBadge'

export interface WasteGroupChipProps extends React.HTMLAttributes<HTMLDivElement> {
  group: WasteGroup
  count?: number
}

export function WasteGroupChip({ group, count, className, ...props }: WasteGroupChipProps) {
  const info = wasteGroupLabels[group]

  const containerStyles: Record<WasteGroup, string> = {
    BLANCO: 'bg-white border-2 border-slate-300 text-slate-900',
    VERDE: 'bg-emerald-50 border-2 border-emerald-300 text-emerald-950',
    NEGRO: 'bg-slate-900 border-2 border-slate-700 text-white',
    SIN_COLOR: 'bg-slate-100 border-2 border-slate-300 text-slate-800',
  }

  const indicatorStyles: Record<WasteGroup, string> = {
    BLANCO: 'bg-white border-2 border-slate-400',
    VERDE: 'bg-[var(--color-waste-green)] border border-emerald-600',
    NEGRO: 'bg-[var(--color-waste-black)] border border-slate-600',
    SIN_COLOR: 'bg-[var(--color-waste-none)] border border-slate-500',
  }

  return (
    <div
      className={cn(
        'flex items-center justify-between p-3 rounded-xl shadow-xs select-none',
        containerStyles[group],
        className
      )}
      {...props}
    >
      <div className="flex items-center gap-2.5">
        <span className={cn('w-4 h-4 rounded-full shrink-0 shadow-xs', indicatorStyles[group])} />
        <div>
          <p className="text-xs md:text-sm font-bold leading-tight">{info.name}</p>
          <p className="text-[11px] opacity-80 leading-tight">{info.description}</p>
        </div>
      </div>
      {count !== undefined && (
        <span className="text-xs font-black px-2 py-0.5 rounded-md bg-black/10">
          {count}
        </span>
      )}
    </div>
  )
}
