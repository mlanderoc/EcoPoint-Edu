import React from 'react'
import { WasteGroup } from '@/types'
import { cn } from '@/utils/cn'

export interface WasteGroupBadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  group: WasteGroup
  showDescription?: boolean
  size?: 'sm' | 'default'
}

export const wasteGroupLabels: Record<WasteGroup, { name: string; description: string }> = {
  BLANCO: {
    name: 'Blanco',
    description: 'Aprovechables limpios',
  },
  VERDE: {
    name: 'Verde Orgánico',
    description: 'Restos de poda y comida',
  },
  NEGRO: {
    name: 'Negro',
    description: 'No aprovechables / Ordinarios',
  },
  SIN_COLOR: {
    name: 'Sin color',
    description: 'Especiales / Peligrosos',
  },
}

export function WasteGroupBadge({
  group,
  showDescription = false,
  size = 'default',
  className,
  ...props
}: WasteGroupBadgeProps) {
  const info = wasteGroupLabels[group]

  const styles: Record<WasteGroup, string> = {
    BLANCO: 'bg-white text-slate-900 border-2 border-slate-400 shadow-xs',
    VERDE: 'bg-[var(--color-waste-green)] text-white border border-emerald-700 shadow-xs',
    NEGRO: 'bg-[var(--color-waste-black)] text-white border border-slate-700 shadow-xs',
    SIN_COLOR: 'bg-[var(--color-waste-none)] text-white border border-slate-600 shadow-xs',
  }

  const dotStyles: Record<WasteGroup, string> = {
    BLANCO: 'bg-white border-2 border-slate-500',
    VERDE: 'bg-emerald-200 border border-white',
    NEGRO: 'bg-slate-400 border border-white',
    SIN_COLOR: 'bg-slate-300 border border-white',
  }

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 font-bold rounded-full transition-colors',
        size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-xs md:text-sm',
        styles[group],
        className
      )}
      {...props}
    >
      <span className={cn('w-2 h-2 rounded-full shrink-0', dotStyles[group])} />
      <span>{info.name}</span>
      {showDescription && (
        <span className="opacity-90 font-normal text-[11px] ml-1">({info.description})</span>
      )}
    </span>
  )
}
