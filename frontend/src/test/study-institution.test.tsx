/**
 * The institution of a study is chosen when it is created, and the list of
 * studies is filtered by institution (spec 004, R-330, R-337, CA-319; D14).
 *
 * @author craulgomez
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { NewStudyPage, StudiesPage, StudyDetailPage } from '@/features/study'
import { SourcesPage } from '@/features/admin'
import { formatDate } from '@/features/study/utils/samplingDays'
import { mockUsers } from '@/mocks/data/seedData'
import { addMockInstitution, resetMockState } from '@/mocks/handlers'

const SABANAS = 'Universidad Popular del Cesar, sede Sabanas'
const DAY_ONE = formatDate('2027-02-01')

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={mockUsers[0]}>
        <Routes>
          <Route path="/estudios" element={<StudiesPage />} />
          <Route path="/estudios/nuevo" element={<NewStudyPage />} />
          <Route path="/estudios/:id" element={<StudyDetailPage />} />
          <Route path="/admin/fuentes" element={<SourcesPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Institución del estudio (R-330, R-337, CA-319)', () => {
  beforeEach(() => {
    resetMockState('DOCENTE')
    addMockInstitution(
      { id: 2, name: SABANAS, address: 'Sabanas del Valle, Valledupar', active: true },
      [{ id: 900, institutionId: 2, code: 'BL', name: 'Bloque de aulas', type: 'AULAS', active: true }]
    )
  })

  it('ca319_studyIsCreatedInTheChosenInstitutionAndFilteredByIt', async () => {
    const user = userEvent.setup()
    const { unmount } = renderAt('/estudios/nuevo')

    const institution = await screen.findByLabelText('Institución donde se hace el estudio')
    expect(await screen.findByRole('option', { name: SABANAS })).toBeInTheDocument()
    await user.selectOptions(institution, '2')

    expect(await screen.findByLabelText('BL Bloque de aulas')).toBeChecked()
    expect(screen.queryByLabelText(/^AU /)).toBeNull()

    await user.type(screen.getByLabelText('Día 1 (descarte)'), '2027-02-01')
    await user.selectOptions(await screen.findByLabelText('Cuadrilla del estudio'), '2')
    await user.click(screen.getByRole('button', { name: /Crear estudio/ }))
    expect(await screen.findByText(/Planificado/)).toBeInTheDocument()
    unmount()

    const second = renderAt('/estudios?institucion=2')
    expect((await screen.findAllByText(DAY_ONE, { exact: false })).length).toBeGreaterThan(0)
    second.unmount()

    renderAt('/estudios')
    expect(await screen.findByText('Estudios de caracterización')).toBeInTheDocument()
    expect(await screen.findByLabelText('Ver los estudios de')).toHaveValue('')
    expect(screen.queryAllByText(DAY_ONE, { exact: false })).toHaveLength(0)
  })

  it('ca319_inactiveInstitutionIsNotOfferedForNewStudies', async () => {
    addMockInstitution({ id: 3, name: 'Institución cerrada', address: 'Calle 1', active: false }, [])
    renderAt('/estudios/nuevo')

    expect(await screen.findByRole('option', { name: SABANAS })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: /Institución cerrada/ })).toBeNull()
  })

  it('r321_sourcesAreManagedPerInstitution', async () => {
    const user = userEvent.setup()
    renderAt('/admin/fuentes')

    await screen.findByRole('option', { name: SABANAS })
    await user.selectOptions(screen.getByLabelText('Institución'), '1')
    await screen.findByTestId('source-row-AU')
    await user.selectOptions(screen.getByLabelText('Institución'), '2')
    expect(await screen.findByTestId('source-row-BL')).toBeInTheDocument()
    expect(screen.queryByTestId('source-row-AU')).toBeNull()
  })
})
