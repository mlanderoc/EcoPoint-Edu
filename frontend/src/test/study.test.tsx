/**
 * Study configuration and lifecycle (Spec 004, phase 4: T314, T315, T316).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { AuthProvider } from '@/features/auth'
import { NewStudyPage, StudyDetailPage, calculateSamplingDays } from '@/features/study'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'
import { server } from '@/mocks/server'

const teacher = mockUsers[0]

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={teacher}>
        <Routes>
          <Route path="/estudios/nuevo" element={<NewStudyPage />} />
          <Route path="/estudios/:id" element={<StudyDetailPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Sampling days planner (T314, R-330)', () => {
  it('ca301_samplingDaysProposalSkipsWeekends', () => {
    expect(calculateSamplingDays('2026-03-02')).toEqual([
      '2026-03-03', '2026-03-04', '2026-03-05', '2026-03-06', '2026-03-09', '2026-03-10', '2026-03-11',
    ])
  })

  it('ca301_fridayStartJumpsToMonday', () => {
    expect(calculateSamplingDays('2026-03-06')[0]).toBe('2026-03-09')
  })
})

describe('New study form (T314, T315, R-331, R-333)', () => {
  it('ca301_choosingDayOneFillsTheNextSevenBusinessDays', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAt('/estudios/nuevo')

    const dayOne = await screen.findByLabelText('Día 1 (descarte)')
    await user.type(dayOne, '2026-03-02')

    expect(screen.getByLabelText('Día 2')).toHaveValue('2026-03-03')
    expect(screen.getByLabelText('Día 6')).toHaveValue('2026-03-09')
    expect(screen.getByLabelText('Día 8')).toHaveValue('2026-03-11')
  })

  it('r333_dayOneShowsTheDiscardBannerAndAllActiveSourcesAreChosen', async () => {
    resetMockState('DOCENTE')
    renderAt('/estudios/nuevo')

    expect(await screen.findByText('Día 1: Día de descarte: no entra en los cálculos')).toBeInTheDocument()
    const classrooms = await screen.findByRole('checkbox', { name: /AU Aulas de Clase/i })
    expect(classrooms).toBeChecked()
    expect(screen.getAllByRole('checkbox').every((box) => (box as HTMLInputElement).checked)).toBe(true)
  })

  it('r331_createsTheStudyWithItsDatesAndChosenSourcesAndOpensIt', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    let sentSources: number[] | null = null
    server.use(
      http.put('/api/v1/studies/:id/sources', async ({ request }) => {
        sentSources = ((await request.json()) as { sourceIds: number[] }).sourceIds
        return HttpResponse.json({
          id: 1001, institutionId: 1, status: 'PLANIFICADO', startDate: '2026-03-02', endDate: '2026-03-11',
          days: [], participatingSourceIds: sentSources, pendingRecords: 0,
        })
      })
    )
    renderAt('/estudios/nuevo')

    await user.type(await screen.findByLabelText('Día 1 (descarte)'), '2026-03-02')
    await user.click(await screen.findByRole('checkbox', { name: /ZV Patios y Zonas Verdes/i }))
    await user.click(screen.getByRole('button', { name: 'Crear estudio' }))

    expect(await screen.findByText(/Estudio N.º 1001/)).toBeInTheDocument()
    expect(sentSources).not.toContain(6)
    expect(sentSources).toHaveLength(5)
  })
})

describe('Study lifecycle (T316, R-332, CA-309)', () => {
  it('r332_showsTheEightDaysWithTheirShifts', async () => {
    resetMockState('DOCENTE')
    renderAt('/estudios/2')

    expect(await screen.findByText(/Estudio N.º 2/)).toBeInTheDocument()
    const dayThree = await screen.findByTestId('study-day-3')
    expect(within(dayThree).getByText(/M · Abierto/)).toBeInTheDocument()
    expect(within(dayThree).getByText(/T · Sin abrir/)).toBeInTheDocument()
    expect(within(screen.getByTestId('study-day-1')).getByText(/Día de descarte/)).toBeInTheDocument()
  })

  it('ca309_studyClosingRejectionDisplaysExactReason', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAt('/estudios/2')

    await user.click(await screen.findByRole('button', { name: 'Pasar a depuración' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar' }))
    expect(await screen.findByText(/En depuración/)).toBeInTheDocument()

    await user.click(await screen.findByRole('button', { name: 'Cerrar estudio' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar' }))

    expect(
      await screen.findByText('No se puede cerrar el estudio: quedan 2 registros pendientes de verificación.')
    ).toBeInTheDocument()
  })

  it('rn017_reopeningAClosedStudyAsksForAReason', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    server.use(
      http.get('/api/v1/studies/:id', () => HttpResponse.json({
        id: 2, institutionId: 1, status: 'CERRADO', startDate: '2026-03-02', endDate: '2026-03-11',
        days: [], participatingSourceIds: [], pendingRecords: 0,
      }))
    )
    renderAt('/estudios/2')

    await user.click(await screen.findByRole('button', { name: 'Reabrir para depuración' }))
    const confirm = await screen.findByRole('button', { name: 'Confirmar' })
    expect(confirm).toBeDisabled()

    await user.type(screen.getByLabelText('Motivo'), 'Error de digitación en el día 4')
    expect(confirm).toBeEnabled()
  })
})
