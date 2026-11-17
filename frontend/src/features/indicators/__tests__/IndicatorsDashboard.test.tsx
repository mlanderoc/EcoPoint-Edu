/**
 * Baseline dashboard (Spec 006, T506 and T507; R-520 to R-526, CA-504, CA-505).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState, setActiveUser } from '@/mocks/handlers'
import { User } from '@/types'
import { IndicatorsDashboard } from '../pages/IndicatorsDashboard'

const teacher = mockUsers[0]
const student = mockUsers[1]

function renderAs(user: User) {
  setActiveUser(user)
  return render(
    <MemoryRouter>
      <AuthProvider initialUser={user}>
        <IndicatorsDashboard />
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Baseline dashboard (T506, T507)', () => {
  it('kpiCardsRenderAccurateValues', async () => {
    resetMockState('DOCENTE')
    renderAs(teacher)

    const cards = await screen.findByTestId('kpi-cards')
    expect(within(cards).getByLabelText('Generación diaria promedio')).toHaveTextContent('60,00 kg/día')
    expect(within(cards).getByLabelText('PPC promedio')).toHaveTextContent('0,150 kg/persona/día')
    expect(within(cards).getByLabelText('Fracción aprovechable')).toHaveTextContent('73,33 %')
    expect(within(cards).getByLabelText('CV de la PPC')).toHaveTextContent('4,30 %')
    expect(within(cards).getByLabelText('Densidad promedio')).toHaveTextContent('152,40 kg/m³')
    expect(within(cards).getByRole('link', { name: 'Ver detalle' })).toBeInTheDocument()
  })

  it('ca504_studyNotClosedShowsPreliminaryWithVerifiedShare', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    renderAs(student)

    expect(await screen.findByTestId('indicators-label')).toHaveTextContent('Preliminar')
    expect(screen.getByText(/62,50 % de los registros verificados/)).toBeInTheDocument()
  })

  it('ca505_compositionShowsHowManyShiftsEntered', async () => {
    resetMockState('DOCENTE')
    renderAs(teacher)

    expect(await screen.findByTestId('composition-note')).toHaveTextContent('Composición calculada con 12 de 14 turnos')
    expect(within(screen.getByTestId('group-shares')).getByText('33,33 %')).toBeInTheDocument()
    expect(screen.getByText('Restos de comida')).toBeInTheDocument()
  })

  it('r522_dayOneIsMarkedAsDiscardDay', async () => {
    resetMockState('DOCENTE')
    renderAs(teacher)

    const days = await screen.findByTestId('daily-generation')
    expect(days).toHaveTextContent('Día 1 (descarte): 150,00 kg')
    expect(screen.getByText(/Día de descarte/)).toBeInTheDocument()
  })

  it('r524_r525_r526_sourcesShiftsAndQualityAreShown', async () => {
    resetMockState('DOCENTE')
    renderAs(teacher)

    const ranking = await screen.findByTestId('source-ranking')
    expect(within(ranking).getAllByRole('listitem')[0]).toHaveTextContent('AU · Aulas')
    expect(screen.getByTestId('shift-comparison')).toHaveTextContent('Mañana')
    expect(screen.getByTestId('data-quality')).toHaveTextContent('Cuarteos no conformes')
  })
})
