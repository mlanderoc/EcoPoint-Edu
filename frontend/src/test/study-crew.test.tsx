/**
 * Crew of the study and choice of the study in the field (spec 004, R-331,
 * R-340, R-341, CA-321, CA-322; D14).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { FieldDashboard } from '@/features/field'
import { NewStudyPage, StudyDetailPage } from '@/features/study'
import { mockStudy, mockUsers } from '@/mocks/data/seedData'
import { addMockStudy, resetMockState } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { User } from '@/types'

const teacher = mockUsers[0]
const student = mockUsers[1]
/** Running study of a crew the demo student does not belong to. */
const OTHER_CREW_STUDY = { ...mockStudy, id: 30, crewId: 99 }

function renderAt(path: string, user: User = teacher) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={user}>
        <Routes>
          <Route path="/campo" element={<FieldDashboard now={new Date(2026, 2, 5, 8, 0)} />} />
          <Route path="/estudios/nuevo" element={<NewStudyPage />} />
          <Route path="/estudios/:id" element={<StudyDetailPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Estudio en Registro en campo (R-340, CA-321)', () => {
  it('ca321_studentOnlySeesTheStudiesOfHisCrew', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    addMockStudy(OTHER_CREW_STUDY)
    renderAt('/campo', student)

    expect(await screen.findByText(/Estudio N\.º 2/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Estudio en ejecución')).toBeNull()
    expect(screen.queryByText(/Estudio N\.º 30/)).toBeNull()
  })

  it('ca321_teacherChoosesAmongEveryStudyInExecution', async () => {
    resetMockState('DOCENTE')
    addMockStudy(OTHER_CREW_STUDY)
    const user = userEvent.setup()
    renderAt('/campo')

    const choice = await screen.findByLabelText('Estudio en ejecución')
    expect(within(choice).getAllByRole('option')).toHaveLength(2)
    await user.selectOptions(choice, '30')
    expect(screen.getByLabelText('Estudio en ejecución')).toHaveValue('30')
  })
})

describe('Cuadrilla del estudio (R-331, CA-322)', () => {
  it('ca083_runningStudyWithoutCrewCanReceiveOne', async () => {
    resetMockState('DOCENTE')
    addMockStudy({ ...mockStudy, id: 31, crewId: null, crewName: null })
    const user = userEvent.setup()
    renderAt('/estudios/31')

    await screen.findByRole('option', { name: 'Cuadrilla Tarde B' })
    await user.selectOptions(await screen.findByLabelText('Cuadrilla del estudio'), '2')

    expect(await screen.findByText('Cuadrilla guardada.')).toBeInTheDocument()
    expect(await screen.findByText('Cuadrilla Tarde B')).toBeInTheDocument()
    expect(screen.queryByLabelText('Cuadrilla del estudio')).toBeNull()
  })

  it('ca322_crewOfAnotherInstitutionShowsItsRealName', async () => {
    resetMockState('DOCENTE')
    addMockStudy({ ...mockStudy, id: 32, crewId: 99, crewName: 'Cuadrilla Sabanas' })
    renderAt('/estudios/32')

    expect(await screen.findByText('Cuadrilla Sabanas')).toBeInTheDocument()
    expect(screen.queryByText('Sin cuadrilla asignada')).toBeNull()
  })

  it('ca323_leavingAfterOnlyChoosingACrewWarns', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderAt('/estudios/nuevo')

    await screen.findByRole('option', { name: 'Cuadrilla Tarde B' })
    await user.selectOptions(await screen.findByLabelText('Cuadrilla del estudio'), '2')
    await user.click(screen.getByRole('button', { name: /Atrás a los estudios/ }))

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
  })

  it('ca095_busyCrewIsShownAsOccupiedAndCannotBeChosen', async () => {
    resetMockState('DOCENTE')
    renderAt('/estudios/nuevo')

    const busy = await screen.findByRole('option', { name: /Cuadrilla Mañana A \(ocupada: Estudio N\.º 2\)/ })
    expect(busy).toBeDisabled()
    expect(screen.getByRole('option', { name: 'Cuadrilla Tarde B' })).toBeEnabled()
  })

  it('ca322_studyNeedsACrewThatCanBeChangedWhilePlanned', async () => {
    resetMockState('DOCENTE')
    server.use(http.get('/api/v1/crews/unavailable', () => HttpResponse.json([])))
    const user = userEvent.setup()
    renderAt('/estudios/nuevo')

    await user.type(await screen.findByLabelText('Día 1 (descarte)'), '2027-02-01')
    await user.click(screen.getByRole('button', { name: 'Crear estudio' }))
    expect(await screen.findByText('Elija la cuadrilla del estudio.')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Cuadrilla del estudio'), '1')
    await user.click(screen.getByRole('button', { name: 'Crear estudio' }))
    await screen.findByRole('button', { name: /Eliminar estudio/ })
    const crew = screen.getByLabelText('Cuadrilla del estudio')
    expect(crew).toHaveValue('1')

    await user.selectOptions(crew, '2')
    expect(await screen.findByText('Cuadrilla guardada.')).toBeInTheDocument()
    expect(screen.getByLabelText('Cuadrilla del estudio')).toHaveValue('2')
  })
})
