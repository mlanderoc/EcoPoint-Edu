/**
 * Editing a crew with free students and filtering studies by status (D15, RN-091).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router-dom'
import { CrewsPage } from '@/features/admin/pages/CrewsPage'
import { AuthProvider } from '@/features/auth'
import { StudiesPage } from '@/features/study'
import { mockCrews, mockUsers } from '@/mocks/data/seedData'
import { resetMockState, setActiveUser } from '@/mocks/handlers'
import { server } from '@/mocks/server'

const teacher = mockUsers[0]

function renderPage(page: React.ReactNode, path = '/') {
  resetMockState('DOCENTE')
  setActiveUser(teacher)
  return render(
    <MemoryRouter initialEntries={[path]}><AuthProvider initialUser={teacher}>{page}</AuthProvider></MemoryRouter>
  )
}

describe('Crews and studies (D15)', () => {
  it('ca085_busyStudentCannotBeCheckedWhenEditingACrew', async () => {
    const student = mockUsers.find((item) => item.role === 'ESTUDIANTE_UNIVERSITARIO'
      && !mockCrews[0].memberIds.includes(item.id))
    expect(student).toBeDefined()
    renderPage(<CrewsPage />)
    server.use(http.get('/api/v1/crews/busy-members', () => HttpResponse.json([{ userId: student!.id, studyId: 9 }])))
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: `Editar ${mockCrews[0].name}` }))

    const row = (await screen.findByText('En Estudio N.º 9')).closest('label') as HTMLElement
    expect(within(row).getByRole('checkbox')).toBeDisabled()
  })

  it('ca328_studiesAreFilteredByStatus', async () => {
    renderPage(<StudiesPage />, '/estudios?institucion=1')
    server.use(http.get('/api/v1/studies', () => HttpResponse.json([
      { id: 7, institutionId: 1, status: 'CERRADO', startDate: '2026-03-02', endDate: '2026-03-11', days: [], participatingSourceIds: [], pendingRecords: 0 },
      { id: 8, institutionId: 1, status: 'CANCELADO', startDate: '2026-04-06', endDate: '2026-04-15', days: [], participatingSourceIds: [], pendingRecords: 0 },
    ])))
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Cerrados (1)' }))

    expect(screen.getByRole('heading', { name: /Estudio N.º 7/ })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /Estudio N.º 8/ })).toBeNull()
  })
})
