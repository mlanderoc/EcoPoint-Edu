/**
 * Welcome page, home screen and the module changes of the approved design (D15).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { HomePage, MorePage } from '@/features/home'
import { LandingPage } from '@/features/landing'
import { StudiesPage } from '@/features/study'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState, setActiveUser } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { User } from '@/types'

const teacher = mockUsers[0]
const student = mockUsers[1]

function renderAs(user: User, path: string, page: React.ReactNode) {
  resetMockState(user.role === 'DOCENTE' ? 'DOCENTE' : 'ESTUDIANTE_UNIVERSITARIO')
  setActiveUser(user)
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={user}>
        <Routes><Route path={path.split('?')[0]} element={page} /></Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Approved design (D15)', () => {
  it('ca324_welcomePageLeadsToLogin', () => {
    render(<MemoryRouter><LandingPage /></MemoryRouter>)
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('La mejor gestión ambiental')
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toHaveAttribute('href', '/login')
  })

  it('ca325_homeShowsTheSixteenShiftsOfTheRunningStudy', async () => {
    renderAs(teacher, '/inicio', <HomePage />)
    expect(await screen.findByRole('button', { name: 'Día 8, Tarde' })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /^Día \d, (Mañana|Tarde)$/ })).toHaveLength(16)
    expect(screen.getByRole('heading', { name: 'Por atender' })).toBeInTheDocument()
  })

  it('ca326_closedStudyHasNoOpenButton', async () => {
    resetMockState('DOCENTE')
    setActiveUser(teacher)
    server.use(http.get('/api/v1/studies', () => HttpResponse.json([
      { id: 7, institutionId: 1, status: 'CERRADO', startDate: '2026-03-02', endDate: '2026-03-11', days: [], participatingSourceIds: [], pendingRecords: 0 },
      { id: 8, institutionId: 1, status: 'EN_EJECUCION', startDate: '2026-04-06', endDate: '2026-04-15', days: [], participatingSourceIds: [], pendingRecords: 0 },
    ])))
    render(
      <MemoryRouter initialEntries={['/estudios?institucion=1']}>
        <AuthProvider initialUser={teacher}><StudiesPage /></AuthProvider>
      </MemoryRouter>
    )
    expect(await screen.findByRole('button', { name: /Abrir Estudio N.º 8/ })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /Estudio N.º 7/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Abrir Estudio N.º 7/ })).toBeNull()
  })

  it('ca327_moreMenuHidesAdministrationFromStudents', () => {
    renderAs(student, '/mas', <MorePage />)
    expect(screen.getByRole('link', { name: 'Alertas' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Instituciones' })).toBeNull()
  })
})
