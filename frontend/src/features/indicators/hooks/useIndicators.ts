/**
 * Loads the indicators of a study and reloads them when the study changes
 * (Spec 006, T506).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '@/services/apiClient'
import { indicatorService } from '../services/indicatorService'
import { StudyIndicators } from '../types/indicatorTypes'

export function useIndicators(studyId: number | null) {
  const [indicators, setIndicators] = useState<StudyIndicators | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    if (studyId === null) return
    setIsLoading(true)
    setError(null)
    try {
      setIndicators(await indicatorService.get(studyId))
    } catch (err) {
      setIndicators(null)
      setError(err instanceof ApiError ? err.message : 'No se pudieron calcular los indicadores.')
    } finally {
      setIsLoading(false)
    }
  }, [studyId])

  useEffect(() => { reload() }, [reload])

  return { indicators, isLoading, error, reload }
}
