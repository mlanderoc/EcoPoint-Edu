/**
 * Live quality control of the quartering (Spec 005, T402; CA-400 to CA-403, CA-406).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { checkQuality, composition, proposedFullSample, sampleError } from '@/features/quality/utils/quarteringMath'

describe('Live quartering control (T402)', () => {
  it('ca400_subsampleOutsideTheRangeIsRejected', () => {
    expect(sampleError(60, 120)).toBe('Debe estar entre 45 y 55 kg')
    expect(sampleError(50, 120)).toBeNull()
  })

  it('ca401_smallShiftProposesAFullSample', () => {
    expect(proposedFullSample(32)).toBe(32)
    expect(proposedFullSample(120)).toBeNull()
    expect(sampleError(32, 32)).toBeNull()
    expect(sampleError(30, 32)).toContain('muestra completa de 32,00 kg')
  })

  it('ca402_differenceWithinTwoPercentIsConforming', () => {
    expect(checkQuality(50, [30, 19.2])).toEqual({
      classified: 49.2, differenceKg: 0.8, differencePercent: 1.6, status: 'CONFORME',
    })
  })

  it('ca403_differenceOverTwoPercentIsNonConforming', () => {
    expect(checkQuality(50, [30, 18.5, null])).toEqual({
      classified: 48.5, differenceKg: 1.5, differencePercent: 3, status: 'NO_CONFORME',
    })
  })

  it('rn052_exactlyTwoPercentIsStillConforming', () => {
    expect(checkQuality(50, [49]).status).toBe('CONFORME')
  })

  it('ca406_compositionBySubcategoryAndByGroup', () => {
    const result = composition([
      { categoryId: 1, name: 'Papel', group: 'BLANCO', weight: 10 },
      { categoryId: 2, name: 'Restos de comida', group: 'VERDE', weight: 25 },
      { categoryId: 3, name: 'Icopor', group: 'NEGRO', weight: 15 },
    ])
    expect(result.byCategory.map((item) => item.percent)).toEqual([20, 50, 30])
    expect(result.byGroup).toEqual({ BLANCO: 20, VERDE: 50, NEGRO: 30, SIN_COLOR: 0 })
  })
})
