/**
 * Crew of a study: chosen when it is created and changed in its detail while
 * it is planned (spec 004, R-331; RN-088; D14).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { UsersRound } from 'lucide-react'
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
  /** Study being edited, so its own crew is not shown as busy. */
  studyId?: number
}

export function CrewSelect({ crews, value, currentName, onChange, disabled, studyId }: CrewSelectProps) {
  const [unavailable, setUnavailable] = useState<Map<number, number>>(new Map())

  useEffect(() => {
    adminService.getUnavailableCrews(studyId)
      .then((items) => setUnavailable(new Map(items.map((item) => [item.crewId, item.studyId]))))
      .catch(() => setUnavailable(new Map()))
  }, [studyId])

  const isForeign = value !== null && !crews.some((crew) => crew.id === value)
  return (
    <Select
      id="study-crew"
      label="Cuadrilla del estudio"
      value={value ?? ''}
      placeholder="Escoja una cuadrilla"
      icon={<UsersRound />}
      disabled={disabled || (crews.length === 0 && !isForeign)}
      onChange={(event) => onChange(Number(event.target.value))}
    >
      {isForeign && <option value={value}>{currentName ?? 'Cuadrilla asignada'}</option>}
      {crews.map((crew) => {
        const holder = crew.id === value ? undefined : unavailable.get(crew.id)
        return (
          <option key={crew.id} value={crew.id} disabled={holder !== undefined}>
            {holder === undefined ? crew.name : `${crew.name} (ocupada: Estudio N.º ${holder})`}
          </option>
        )
      })}
    </Select>
  )
}
