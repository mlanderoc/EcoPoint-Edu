/**
 * Field registration (Spec 004, phase 5: T317 to T323; CA-303 to CA-306, CA-310).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { delay, http, HttpResponse } from 'msw'
import { AuthProvider } from '@/features/auth'
import { FieldDashboard, NewBagPage, PopulationPage, ShiftPage } from '@/features/field'
import { mockUsers } from '@/mocks/data/seedData'
import { handlers, resetMockState } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { User } from '@/types'

const teacher = mockUsers[0]
const student = mockUsers[1]
/** Day 4 of the mock study, morning: a shift not opened yet. */
const DAY_FOUR_MORNING = new Date(2026, 2, 5, 8, 0)
/** Shift 14 of the mock: day 3 morning, open, with population and one bag of OF. */
const OPEN_SHIFT = 14

function renderField(path: string, user: User = teacher) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={user}>
        <Routes>
          <Route path="/campo" element={<FieldDashboard now={DAY_FOUR_MORNING} />} />
          <Route path="/campo/turno/:id" element={<ShiftPage />} />
          <Route path="/campo/turno/:id/poblacion" element={<PopulationPage />} />
          <Route path="/campo/turno/:id/bolsas/nueva" element={<NewBagPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

async function openDayFourMorning() {
  const user = userEvent.setup()
  renderField('/campo')
  await user.click(await screen.findByRole('button', { name: /Abrir turno/i }))
  await user.selectOptions(await screen.findByLabelText('Cuadrilla'), '1')
  await user.click(screen.getByRole('button', { name: 'Confirmar apertura' }))
  expect(await screen.findByRole('heading', { name: 'Turno D4-M' })).toBeInTheDocument()
  return user
}

describe('Field dashboard and shift opening (T317, R-340, R-341)', () => {
  it('r341_teacherOpensTheSuggestedShiftWithItsCrew', async () => {
    resetMockState('DOCENTE')
    await openDayFourMorning()
    expect(screen.getByText(/Registre primero la población del turno/)).toBeInTheDocument()
  })

  it('ca300_studentSeesThatTheShiftIsNotStartedYet', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    renderField('/campo', student)

    expect(await screen.findByText('Turno no iniciado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Abrir turno/i })).toBeNull()
  })

  it('r340_openShiftIsContinuedFromTheDashboard', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = userEvent.setup()
    renderField('/campo', student)

    await user.selectOptions(await screen.findByLabelText('Día de muestreo'), '3')
    await user.click(screen.getByRole('button', { name: /Continuar turno/i }))

    expect(await screen.findByRole('heading', { name: 'Turno D3-M' })).toBeInTheDocument()
  })
})

describe('Population (T318, R-342, CA-303)', () => {
  it('ca303_shiftWithoutPopulationBlocksBagEntry', async () => {
    resetMockState('DOCENTE')
    const user = await openDayFourMorning()
    const registerPopulation = await screen.findByRole('button', { name: 'Registrar población' })
    expect(screen.queryByRole('button', { name: /Registrar bolsa/i })).toBeNull()

    await user.click(registerPopulation)
    await user.type(await screen.findByLabelText('Estudiantes presentes'), '300')
    await user.type(screen.getByLabelText('Docentes'), '12')
    await user.type(screen.getByLabelText('Personal administrativo y de servicios'), '6')
    expect(screen.getByTestId('population-total')).toHaveTextContent('318')

    await user.click(screen.getByRole('button', { name: 'Guardar población' }))
    expect(await screen.findByRole('button', { name: /Registrar bolsa/i })).toBeInTheDocument()
    expect(screen.getByText(/Población:/)).toHaveTextContent('318 personas (300 estudiantes, 12 docentes, 6 personal)')
  })

  it('ca303_bagScreenIsLockedUntilThePopulationIsSaved', async () => {
    resetMockState('DOCENTE')
    server.use(
      http.get('/api/v1/shifts/:shiftId', async (info) => {
        const response = await handlers.find((handler) => handler.info.header === 'GET /api/v1/shifts/:shiftId')!
          .run({ request: info.request, requestId: 'test' })
        const shift = await response!.response!.json()
        return HttpResponse.json({ ...shift, population: null })
      })
    )
    renderField(`/campo/turno/${OPEN_SHIFT}/bolsas/nueva`)

    expect(await screen.findByTestId('bags-locked')).toHaveTextContent(
      'Registre primero la población del turno para habilitar el pesaje de bolsas'
    )
  })
})

describe('Bag weighing (T319 to T321, R-343, R-344, CA-304, CA-305, CA-310)', () => {
  async function fillBag(gross: string, tare: string, volume: string) {
    const user = userEvent.setup()
    renderField(`/campo/turno/${OPEN_SHIFT}/bolsas/nueva`)
    await user.click(await screen.findByRole('button', { name: /AU Aulas de Clase/i }))
    await user.type(screen.getByLabelText('Peso bruto (kg)'), gross)
    await user.type(screen.getByLabelText('Tara del recipiente (kg)'), tare)
    await user.type(screen.getByLabelText('Volumen de la bolsa (m³)'), volume)
    return user
  }

  it('ca304_netWeightDensityAndLargeLabelCalculated', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = await fillBag('12,40', '0,40', '0,080')

    expect(screen.getByTestId('live-net')).toHaveTextContent('12,00 kg')
    expect(screen.getByTestId('live-density')).toHaveTextContent('150,00 kg/m³')

    await user.click(screen.getByRole('button', { name: 'Guardar bolsa' }))
    expect(await screen.findByTestId('bag-label')).toHaveTextContent('AU-D3-M')

    await user.click(screen.getByRole('button', { name: 'Siguiente bolsa' }))
    expect(screen.getByLabelText('Peso bruto (kg)')).toHaveValue('')
  })

  it('ca305_tareGreaterThanGrossDisplaysRedError', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    await fillBag('3,20', '3,50', '0,080')

    expect(screen.getByText('La tara no puede superar el peso bruto.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Guardar bolsa' })).toBeDisabled()
  })

  it('r344_atypicalValueShowsAWarningButCanBeSaved', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = await fillBag('62,40', '0,40', '0,080')

    expect(screen.getByText('Valor fuera de lo esperado')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Guardar bolsa' }))
    expect(await screen.findByTestId('bag-label')).toHaveTextContent('AU-D3-M')
  })

  it('ca310_doubleClickCreatesOnlyOneBag', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    let posts = 0
    server.use(
      http.post('/api/v1/shifts/:shiftId/bags', async () => {
        posts += 1
        await delay(150)
        return HttpResponse.json({
          id: 900, shiftId: OPEN_SHIFT, sourceId: 1, sourceCode: 'AU', label: 'AU-D3-M', grossWeight: 12.4, tare: 0.4,
          netWeight: 12, volume: 0.08, density: 150, atypical: false,
          verification: { status: 'PENDIENTE', enteredBy: 2, enteredAt: null, verifiedBy: null, verifiedAt: null },
        }, { status: 201 })
      })
    )
    const user = await fillBag('12,40', '0,40', '0,080')

    const save = screen.getByRole('button', { name: 'Guardar bolsa' })
    await user.dblClick(save)
    await user.click(save)

    expect(await screen.findByTestId('bag-label')).toHaveTextContent('AU-D3-M')
    expect(posts).toBe(1)
  })
})

describe('Shift checklist and closing (T322, T323, R-345, R-346, CA-306)', () => {
  it('r345_sourceChangesToNoGenerationWhenMarked', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = userEvent.setup()
    renderField(`/campo/turno/${OPEN_SHIFT}`, student)

    const store = await screen.findByTestId('source-TE')
    expect(within(store).getByText('Pendiente')).toBeInTheDocument()
    expect(within(await screen.findByTestId('source-OF')).getByText('Con bolsas')).toBeInTheDocument()

    await user.click(within(store).getByRole('button', { name: 'Marcar sin generación' }))
    expect(await within(screen.getByTestId('source-TE')).findByText('Sin generación')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cerrar turno' })).toBeNull()
  })

  it('ca306_shiftClosingDisabledUntilAllSourcesHandled', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderField(`/campo/turno/${OPEN_SHIFT}`)

    const close = await screen.findByRole('button', { name: 'Cerrar turno' })
    expect(close).toBeDisabled()

    for (const code of ['AU', 'BA', 'TE', 'PAE', 'ZV']) {
      await user.click(within(await screen.findByTestId(`source-${code}`))
        .getByRole('button', { name: 'Marcar sin generación' }))
      await within(screen.getByTestId(`source-${code}`)).findByText('Sin generación')
    }

    expect(screen.getByRole('button', { name: 'Cerrar turno' })).toBeEnabled()
    await user.click(screen.getByRole('button', { name: 'Cerrar turno' }))
    const summary = await screen.findByTestId('close-summary')
    expect(summary).toHaveTextContent('476')
    expect(summary).toHaveTextContent('AU, BA, TE, PAE, ZV')

    await user.click(screen.getByRole('button', { name: 'Confirmar cierre' }))
    expect(await screen.findByText(/Cerrado · 1 bolsas/)).toBeInTheDocument()
  })
})
