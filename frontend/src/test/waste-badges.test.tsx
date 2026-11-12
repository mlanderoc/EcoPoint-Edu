import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { WasteGroupBadge, WasteGroupChip } from '@/components/waste'
import { WasteGroup } from '@/types'

describe('Waste Group Badges and Chips (T306, R-304)', () => {
  const groups: WasteGroup[] = ['BLANCO', 'VERDE', 'NEGRO', 'SIN_COLOR']

  it.each(groups)('WasteGroupBadge renders textual label for %s', (group) => {
    render(<WasteGroupBadge group={group} showDescription />)
    const badge = screen.getByText(
      group === 'BLANCO'
        ? 'Blanco'
        : group === 'VERDE'
          ? 'Verde Orgánico'
          : group === 'NEGRO'
            ? 'Negro'
            : 'Sin color'
    )
    expect(badge).toBeInTheDocument()
    // Must be span or div, never button
    expect(badge.closest('button')).toBeNull()
  })

  it.each(groups)('WasteGroupChip renders name and description for %s', (group) => {
    render(<WasteGroupChip group={group} count={5} />)
    const expectedName =
      group === 'BLANCO'
        ? 'Blanco'
        : group === 'VERDE'
          ? 'Verde Orgánico'
          : group === 'NEGRO'
            ? 'Negro'
            : 'Sin color'
    expect(screen.getByText(expectedName)).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
  })
})
