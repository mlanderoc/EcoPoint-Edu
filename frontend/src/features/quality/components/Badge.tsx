/**
 * Small status label of the quality screens.
 *
 * @author craulgomez
 */

import { cn } from '@/utils/cn'

const TONES = {
  warning: 'bg-amber-50 text-amber-900 border-amber-300',
  success: 'bg-emerald-50 text-emerald-800 border-emerald-200',
  danger: 'bg-red-50 text-red-800 border-red-300',
  neutral: 'bg-slate-100 text-slate-700 border-slate-300',
}

export function Badge({ tone, children }: { tone: keyof typeof TONES; children: React.ReactNode }) {
  return (
    <span className={cn('inline-flex items-center text-xs font-semibold px-2 py-1 rounded-full border', TONES[tone])}>
      {children}
    </span>
  )
}
