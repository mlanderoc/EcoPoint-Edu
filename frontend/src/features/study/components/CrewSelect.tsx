/**
 * Crew of a study: chosen when it is created and changed in its detail while
 * it is planned (spec 004, R-331; RN-088; D14).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Select } from '@/components/ui'
import { adminService } from '@/features/admin/services/adminService'
import { Crew } from '@/types'

/** Loads the crews the teacher can assign. */
export function useCrews(): { crews: Crew[]; error: string | null } {
  const [crews, setCrews] = useState<Crew[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    adminService.getCrews()
      .then(setCrews)
      .catch(() => setError('No se pudieron cargar las cuadrillas.'))
  }, [])

  return { crews, error }
}

export interface CrewSelectProps {
  crews: Crew[]
  value: number | null
  /** Name of the current crew, shown even if it is not among the teacher's crews. */
  currentName?: string | null
  onChange: (id: number) => void
  disabled?: boolean
}

export function CrewSelect({ crews, value, currentName, onChange, disabled }: CrewSelectProps) {
  const isForeign = value !== null && !crews.some((crew) => crew.id === value)
  return (
    <Select
      id="study-crew"
      label="Cuadrilla del estudio"
      value={value ?? ''}
      disabled={disabled || (crews.length === 0 && !isForeign)}
      onChange={(event) => onChange(Number(event.target.value))}
    >
      {value === null && <option value="">Elija una cuadrilla</option>}
      {isForeign && <option value={value}>{currentName ?? 'Cuadrilla asignada'}</option>}
      {crews.map((crew) => (
        <option key={crew.id} value={crew.id}>{crew.name}</option>
      ))}
    </Select>
  )
}
