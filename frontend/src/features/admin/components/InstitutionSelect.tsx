/**
 * List to choose an institution, used to create a study, to filter the studies
 * and to manage the sources of each institution (spec 004, R-321, R-330, R-337; D14).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Select } from '@/components/ui'
import { useAuth } from '@/features/auth'
import { Institution } from '@/types'
import { adminService } from '../services/adminService'

/** Institution kept in the URL (?institucion=); by default, the one of the user. */
export function useInstitutionParam(): [number | null, (id: number) => void] {
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const fromUrl = Number(params.get('institucion'))
  const value = fromUrl > 0 ? fromUrl : user?.institutionId ?? null
  const change = (id: number) => {
    const updated = new URLSearchParams(params)
    if (id === user?.institutionId) updated.delete('institucion')
    else updated.set('institucion', String(id))
    setParams(updated, { replace: true })
  }
  return [value, change]
}

/** Loads the institutions; with onlyActive, only those that admit new studies (RN-006). */
export function useInstitutions(onlyActive: boolean): { institutions: Institution[]; error: string | null } {
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    adminService.getInstitutions()
      .then((all) => setInstitutions(onlyActive ? all.filter((item) => item.active !== false) : all))
      .catch(() => setError('No se pudieron cargar las instituciones.'))
  }, [onlyActive])

  return { institutions, error }
}

export interface InstitutionSelectProps {
  institutions: Institution[]
  value: number | null
  onChange: (id: number) => void
  label?: string
  disabled?: boolean
}

export function InstitutionSelect({ institutions, value, onChange, label = 'Institución', disabled }: InstitutionSelectProps) {
  return (
    <Select
      id="institution-select"
      label={label}
      value={value ?? ''}
      disabled={disabled || institutions.length === 0}
      onChange={(event) => onChange(Number(event.target.value))}
    >
      {value === null && <option value="">Elija una institución</option>}
      {institutions.map((institution) => (
        <option key={institution.id} value={institution.id}>
          {institution.name}{institution.active === false ? ' (inactiva)' : ''}
        </option>
      ))}
    </Select>
  )
}
