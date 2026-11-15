/**
 * "Atrás" and "Descartar" on the field forms (spec 004, R-307, R-308, CA-316).
 *
 * @author craulgomez
 */
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { NewBagPage, PopulationPage, ShiftPage } from '@/features/field'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

/** Shift 14 of the mock: day 3 morning, open, with population. */
const OPEN_SHIFT = 14

function renderBagScreen() {
  return render(
    <MemoryRouter initialEntries={[`/campo/turno/${OPEN_SHIFT}/bolsas/nueva`]}>
      <AuthProvider initialUser={mockUsers[0]}>
        <Routes>
          <Route path="/campo/turno/:id" element={<ShiftPage />} />
          <Route path="/campo/turno/:id/poblacion" element={<PopulationPage />} />
          <Route path="/campo/turno/:id/bolsas/nueva" element={<NewBagPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Atrás y Descartar (R-307, R-308, CA-316)', () => {
  it('ca316_leavingAFormWithDataAsksFirstAndKeepsTheDataWhenTheAnswerIsNo', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderBagScreen()

    await user.type(await screen.findByLabelText('Peso bruto (kg)'), '12,40')
    await user.click(screen.getByRole('button', { name: 'Atrás al turno' }))
    expect(await screen.findByText('¿Descartar los cambios?')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Seguir editando' }))
    expect(screen.getByLabelText('Peso bruto (kg)')).toHaveValue('12,40')

    await user.click(screen.getAllByRole('button', { name: 'Descartar' })[0])
    await user.click(await screen.findByRole('button', { name: 'Descartar' }))
    expect(await screen.findByRole('heading', { name: /^Turno / })).toBeInTheDocument()
  })

  it('r307_emptyFormGoesBackWithoutAsking', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderBagScreen()

    await user.click(await screen.findByRole('button', { name: 'Atrás al turno' }))

    expect(await screen.findByRole('heading', { name: /^Turno / })).toBeInTheDocument()
    expect(screen.queryByText('¿Descartar los cambios?')).toBeNull()
  })
})
