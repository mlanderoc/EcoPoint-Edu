/**
 * D13 adjustments of the study screens: business days with Colombian holidays,
 * recalculation of the later days, editing the dates of a planned study,
 * deleting it and cancelling a running one (spec 004, R-330, R-334 to R-336,
 * CA-301, CA-311, CA-315).
 *
 * @author craulgomez
 */
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { NewStudyPage, StudiesPage, StudyDetailPage } from '@/features/study'
import { easterSunday, holidayName } from '@/features/study/utils/holidays'
import { calculateSamplingDays, recalculateFrom } from '@/features/study/utils/samplingDays'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={mockUsers[0]}>
        <Routes>
          <Route path="/estudios" element={<StudiesPage />} />
          <Route path="/estudios/nuevo" element={<NewStudyPage />} />
          <Route path="/estudios/:id" element={<StudyDetailPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Festivos de Colombia y días hábiles (R-330, CA-311)', () => {
  it('r330_holidaysOf2026IncludeMovedAndEasterHolidays', () => {
    expect(easterSunday(2026).toISOString().slice(0, 10)).toBe('2026-04-05')
    expect(holidayName('2026-11-16')).toBe('Independencia de Cartagena')
    expect(holidayName('2026-11-02')).toBe('Todos los Santos')
    expect(holidayName('2026-04-02')).toBe('Jueves Santo')
    expect(holidayName('2026-04-03')).toBe('Viernes Santo')
    expect(holidayName('2026-05-18')).toBe('Ascensión del Señor')
    expect(holidayName('2026-07-20')).toBe('Día de la Independencia')
    expect(holidayName('2026-11-13')).toBeNull()
  })

  it('ca311_proposalSkipsTheWeekendAndTheMondayHoliday', () => {
    expect(calculateSamplingDays('2026-11-13')).toEqual([
      '2026-11-17', '2026-11-18', '2026-11-19', '2026-11-20', '2026-11-23', '2026-11-24', '2026-11-25',
    ])
  })

  it('ca311_changingDayFourRecalculatesTheLaterDaysEvenIfTheyWereChangedByHand', () => {
    const proposed = ['2026-11-13', ...calculateSamplingDays('2026-11-13')]
    const changedByHand = proposed.map((date, index) => (index === 6 ? '2026-12-01' : date))

    expect(recalculateFrom(changedByHand, 3, '2026-11-23')).toEqual([
      '2026-11-13', '2026-11-17', '2026-11-18', '2026-11-23', '2026-11-24', '2026-11-25', '2026-11-26', '2026-11-27',
    ])
  })

  it('ca301_marchProposalStillSkipsOnlyTheWeekend', () => {
    expect(calculateSamplingDays('2026-03-02')).toEqual([
      '2026-03-03', '2026-03-04', '2026-03-05', '2026-03-06', '2026-03-09', '2026-03-10', '2026-03-11',
    ])
  })

  it('ca311_changingADayOnTheScreenRecalculatesTheFollowingOnes', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAt('/estudios/nuevo')

    await user.type(await screen.findByLabelText('Día 1 (descarte)'), '2026-11-13')
    expect(screen.getByLabelText('Día 2')).toHaveValue('2026-11-17')
    await user.clear(screen.getByLabelText('Día 4'))
    await user.type(screen.getByLabelText('Día 4'), '2026-11-23')

    expect(screen.getByLabelText('Día 5')).toHaveValue('2026-11-24')
    expect(screen.getByLabelText('Día 8')).toHaveValue('2026-11-27')
  })
})

describe('Eliminar y cancelar estudio (R-334 a R-336, CA-315)', () => {
  async function createPlannedStudy() {
    const user = userEvent.setup()
    renderAt('/estudios/nuevo')
    await user.type(await screen.findByLabelText('Día 1 (descarte)'), '2030-03-04')
    await user.click(screen.getByRole('button', { name: 'Crear estudio' }))
    await screen.findByRole('heading', { name: /Estudio N\.º/ })
    return user
  }

  it('r334_plannedStudyDatesAreEditedAndSaved', async () => {
    resetMockState('DOCENTE')
    const user = await createPlannedStudy()

    await user.clear(screen.getByLabelText('Día 3'))
    await user.type(screen.getByLabelText('Día 3'), '2030-03-11')
    await user.click(screen.getByRole('button', { name: 'Guardar fechas' }))

    expect(await screen.findByText('Fechas guardadas.')).toBeInTheDocument()
    expect(screen.getByTestId('study-day-8')).toHaveTextContent('2030')
  })

  it('r335_plannedStudyIsDeletedAfterConfirming', async () => {
    resetMockState('DOCENTE')
    const user = await createPlannedStudy()

    await user.click(screen.getByRole('button', { name: /Eliminar estudio/ }))
    expect(await screen.findByText(/No se puede deshacer/)).toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: 'Eliminar estudio' }).at(-1)!)

    expect(await screen.findByRole('heading', { name: /Estudios/ })).toBeInTheDocument()
  })

  it('ca315_runningStudyIsCancelledOnlyWithAReason', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAt('/estudios/2')

    await user.click(await screen.findByRole('button', { name: /Cancelar estudio/ }))
    const confirm = () => screen.getAllByRole('button', { name: 'Cancelar estudio' }).at(-1)!
    expect(confirm()).toBeDisabled()

    await user.type(screen.getByLabelText(/Motivo de la cancelación/), 'Fechas equivocadas')
    await user.click(confirm())

    expect(await screen.findByText('Estudio cancelado')).toBeInTheDocument()
    expect(screen.getByText(/Motivo: Fechas equivocadas/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Cancelar estudio/ })).toBeNull()
  })
})
