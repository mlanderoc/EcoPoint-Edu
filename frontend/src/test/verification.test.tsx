/**
 * Double entry, history and alerts (Spec 005, T405 to T407; CA-404, CA-408 to CA-412, R-426, R-432).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { AlertsPage, RecordPage, VerificationPage } from '@/features/quality'
import { Sidebar } from '@/components/layout'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState, setActiveUser } from '@/mocks/handlers'
import { User } from '@/types'

const teacher = mockUsers[0]
const student = mockUsers[1]
/** Shift 14 of the mock: its population and bag OF-D3-M were typed by the student. */
const BAG = 'record-bag-101'

function renderAs(user: User, path: string) {
  setActiveUser(user)
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={user}>
        <Routes>
          <Route path="/calidad/verificacion" element={<VerificationPage />} />
          <Route path="/calidad/registro/:tipo/:id" element={<RecordPage />} />
          <Route path="/alertas" element={<AlertsPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

async function createQuartering(sample: number, paper: number, food: number) {
  const response = await fetch('/api/v1/shifts/14/quartering', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sampleWeight: sample, results: [{ categoryId: 1, weight: paper }, { categoryId: 4, weight: food }] }),
  })
  return (await response.json()) as { id: number }
}

describe('Verification tray (T405, CA-408, CA-410, R-426)', () => {
  it('ca408_personWhoTypedTheRecordSeesNoVerificationButtons', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    renderAs(student, '/calidad/verificacion')

    const bag = await screen.findByTestId(BAG)
    expect(within(bag).queryByRole('button', { name: 'Coincide con el papel' })).toBeNull()
    expect(within(bag).getByText(/Lo digitó usted/)).toBeInTheDocument()
  })

  it('ca408_anotherPersonCanVerifyTheRecord', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAs(teacher, '/calidad/verificacion')

    const bag = await screen.findByTestId(BAG)
    await user.click(within(bag).getByRole('button', { name: 'Coincide con el papel' }))
    expect(await screen.findByText(/1 verificados/)).toBeInTheDocument()
  })

  it('ca410_summaryTellsWhenTheStudyCanBeClosed', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAs(teacher, '/calidad/verificacion')

    expect(await screen.findByText('El estudio aún no puede cerrarse')).toBeInTheDocument()
    await user.click(await screen.findByRole('checkbox', { name: /Seleccionar Población del turno/ }))
    await user.click(screen.getByRole('checkbox', { name: /Seleccionar Bolsa OF-D3-M/ }))
    await user.click(screen.getByRole('button', { name: 'Coinciden con el papel (2)' }))

    expect(await screen.findByText('Listo para cerrar')).toBeInTheDocument()
  })
})

describe('Record correction and history (T406, CA-404, CA-409, CA-412)', () => {
  it('ca409_correctionWithoutReasonIsNotSaved', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAs(teacher, '/calidad/registro/bag/101')

    const gross = await screen.findByLabelText('Peso bruto (kg)')
    await user.clear(gross)
    await user.type(gross, '4,05')
    await user.click(screen.getByRole('button', { name: 'Guardar corrección' }))

    expect(screen.getByText('El motivo de la corrección es obligatorio.')).toBeInTheDocument()
    expect(screen.getByText('Sin correcciones.')).toBeInTheDocument()
  })

  it('ca412_correctionAppearsInTheHistoryWithOldAndNewValue', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAs(teacher, '/calidad/registro/bag/101')

    const gross = await screen.findByLabelText('Peso bruto (kg)')
    await user.clear(gross)
    await user.type(gross, '4,05')
    await user.type(screen.getByLabelText('Motivo de la corrección'), 'En la planilla dice 4,05')
    await user.click(screen.getByRole('button', { name: 'Guardar corrección' }))

    const history = await screen.findByTestId('correction-history')
    expect(history).toHaveTextContent('Peso bruto (kg): 4,50 → 4,05')
    expect(history).toHaveTextContent('Motivo: En la planilla dice 4,05')
    expect(screen.getByTestId('record-bag-101')).toHaveTextContent('Corregido')
  })

  it('ca404_reweighingANonConformingQuarteringMakesItConforming', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const quartering = await createQuartering(50, 30, 18.5)
    const user = userEvent.setup()
    renderAs(teacher, `/calidad/registro/quartering/${quartering.id}`)

    expect(await screen.findByTestId(`record-quartering-${quartering.id}`)).toHaveTextContent('No conforme')
    const food = screen.getByLabelText('Restos de comida (kg)')
    await user.clear(food)
    await user.type(food, '19,5')
    await user.type(screen.getByLabelText('Motivo de la corrección'), 'Se repesó la submuestra')
    await user.click(screen.getByRole('button', { name: 'Guardar corrección' }))

    expect(await screen.findByTestId('correction-history')).toHaveTextContent('18,50 → 19,50')
    expect(screen.getByTestId(`record-quartering-${quartering.id}`)).toHaveTextContent('Conforme')
  })
})

describe('Alerts (T407, CA-411, R-432)', () => {
  it('ca411_alertIsAttendedOnlyWithANote', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    await createQuartering(50, 30, 18.5)
    const user = userEvent.setup()
    renderAs(teacher, '/alertas')

    await user.click(await screen.findByRole('button', { name: 'Marcar atendida' }))
    await user.click(await screen.findByRole('button', { name: 'Guardar' }))
    expect(screen.getByText('Escriba una observación sobre cómo se atendió la alerta.')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Observación'), 'Se volvió a pesar.')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(await screen.findByText('No hay alertas con estos filtros.')).toBeInTheDocument()
  })

  it('r432_menuShowsTheNumberOfPendingAlerts', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    await createQuartering(50, 30, 18.5)
    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacher}>
          <Sidebar />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(await screen.findByText('Alertas (1)')).toBeInTheDocument()
    expect(screen.getByText('Verificación')).toBeInTheDocument()
  })
})
