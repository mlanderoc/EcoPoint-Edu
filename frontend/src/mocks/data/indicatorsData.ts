/**
 * Indicators the mock API returns, built from the common test data of the
 * spec (Spec 006, section 8): P(d) = 400 and G(2..8) = 60, 62, 58, 64, 56, 60, 60 kg.
 *
 * @author craulgomez
 */

import type { StudyIndicators } from '@/features/indicators/types/indicatorTypes'
import type { CharacterizationStudy } from '@/types'

const GENERATION = [150, 60, 62, 58, 64, 56, 60, 60]

export function mockIndicators(study: CharacterizationStudy, verifiedPercentage: number): StudyIndicators {
  const official = study.status === 'CERRADO'
  const composition = {
    groups: [
      { group: 'BLANCO' as const, name: 'Blanco', description: 'Aprovechables', weight: 25, percentage: 33.33 },
      { group: 'VERDE' as const, name: 'Verde', description: 'Orgánicos aprovechables', weight: 30, percentage: 40 },
      { group: 'NEGRO' as const, name: 'Negro', description: 'No aprovechables', weight: 20, percentage: 26.67 },
      { group: 'SIN_COLOR' as const, name: 'Sin color', description: 'Peligrosos y especiales', weight: 0, percentage: 0 },
    ],
    subcategories: [
      { categoryId: 4, name: 'Restos de comida', group: 'VERDE' as const, weight: 30, percentage: 40 },
      { categoryId: 1, name: 'Papel y cartón', group: 'BLANCO' as const, weight: 15, percentage: 20 },
      { categoryId: 6, name: 'Sanitarios', group: 'NEGRO' as const, weight: 20, percentage: 26.67 },
      { categoryId: 2, name: 'Plástico', group: 'BLANCO' as const, weight: 10, percentage: 13.33 },
    ],
    shiftsWithQuartering: 12,
    expectedShifts: 14,
    note: 'Composición calculada con 12 de 14 turnos',
  }
  return {
    studyId: study.id,
    status: study.status,
    official,
    label: official ? 'Oficial' : 'Preliminar',
    summary: {
      averageDailyGeneration: 60, averagePpc: 0.15, ppcStandardDeviation: 0.006, ppcVariationCoefficient: 4.3,
      averageDensity: 152.4, recoverableFraction: 73.33, recoverableDailyGeneration: 44, countedDays: 7,
    },
    days: GENERATION.map((generation, index) => ({
      dayNumber: index + 1,
      date: null,
      excluded: index === 0,
      morningGeneration: generation / 2,
      afternoonGeneration: generation / 2,
      generation,
      population: 400,
      ppc: Math.round((generation / 400) * 1000) / 1000,
    })),
    sources: [
      { sourceId: 1, code: 'AU', name: 'Aulas', averageDailyGeneration: 32.5 },
      { sourceId: 3, code: 'TI', name: 'Tienda escolar', averageDailyGeneration: 15.2 },
      { sourceId: 2, code: 'OF', name: 'Oficinas', averageDailyGeneration: 7.8 },
      { sourceId: 4, code: 'PA', name: 'Patio', averageDailyGeneration: 4.5 },
    ],
    shifts: [
      { type: 'MANANA', averageGeneration: 30, averagePopulation: 200, averagePpc: 0.15, averageDensity: 150.1, composition },
      { type: 'TARDE', averageGeneration: 30, averagePopulation: 200, averagePpc: 0.15, averageDensity: 154.7, composition },
    ],
    composition,
    quality: {
      totalRecords: 120,
      verifiedRecords: Math.round(120 * verifiedPercentage / 100),
      verifiedPercentage,
      correctedRecords: 6,
      nonConformingQuarterings: official ? 0 : 1,
    },
  }
}
