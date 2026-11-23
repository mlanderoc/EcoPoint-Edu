/**
 * Waste bag filled according to the kilograms weighed (D15).
 *
 * @author craulgomez
 */

import { useId } from 'react'

const BODY = 'M11 15 C5 24 5 41 11 45 H31 C37 41 37 24 31 15 Z'

export function BagIcon({ kg }: { kg: number }) {
  const clipId = useId()
  const fill = kg > 0 ? Math.min(30, 8 + kg * 1.75) : 0
  const line = kg > 0 ? '#1e8449' : '#a9b3a6'
  return (
    <svg viewBox="0 0 42 48" aria-hidden="true">
      <defs><clipPath id={clipId}><path d={BODY} /></clipPath></defs>
      <path d={BODY} fill="#fff" stroke={line} strokeWidth="2" strokeDasharray={kg > 0 ? undefined : '3 3'} />
      {kg > 0 && <rect y={45 - fill} width="42" height={fill} fill="#2aa35f" clipPath={`url(#${clipId})`} />}
      <path d="M15 15 L12 6 Q21 11 30 6 L27 15" fill={kg > 0 ? line : '#fff'} stroke={line} strokeWidth="2" strokeLinejoin="round" />
    </svg>
  )
}
