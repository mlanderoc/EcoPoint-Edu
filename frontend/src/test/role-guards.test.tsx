/**
 * Unit and integration tests for role-based route guards and conditional permissions (T310 / CA-300 / R-311).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider, RequireAuth, RequireRole, RequirePermission, hasPermission } from '@/features/auth'
import { FieldDashboard } from '@/features/field/pages/FieldDashboard'
import { Sidebar, BottomNav, AppLayout } from '@/components/layout'
import { mockUsers } from '@/mocks/data/seedData'
import { User } from '@/types'

describe('Role Guards and Conditional Visibility (T310, CA-300, R-311)', () => {
  const teacherUser: User = mockUsers.find((u) => u.role === 'DOCENTE')!
  const studentUser: User = mockUsers.find((u) => u.role === 'ESTUDIANTE_UNIVERSITARIO')!

  it('ca300_studentMenuAndButtonsRestricted', async () => {
    // 1. In FieldDashboard, student does NOT see shift control buttons ("Abrir Turno", "Cerrar Turno")
    render(
      <MemoryRouter initialEntries={['/campo']}>
        <AuthProvider initialUser={studentUser}>
          <FieldDashboard />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(screen.queryByRole('button', { name: /Abrir Turno/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Turno Abierto/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Cerrar Turno/i })).toBeNull()

    // 2. In Sidebar, student does NOT see administration or study configuration links
    const { unmount: unmountSidebar } = render(
      <MemoryRouter>
        <AuthProvider initialUser={studentUser}>
          <Sidebar />
        </AuthProvider>
      </MemoryRouter>
    )
    expect(screen.getByText('Registro en Campo')).toBeInTheDocument()
    expect(screen.queryByText('Configuración Estudio')).toBeNull()
    expect(screen.queryByText('Administración')).toBeNull()
    expect(screen.queryByText('Instituciones')).toBeNull()
    unmountSidebar()

    // 3. In BottomNav, student does NOT see admin or studies
    render(
      <MemoryRouter>
        <AuthProvider initialUser={studentUser}>
          <BottomNav />
        </AuthProvider>
      </MemoryRouter>
    )
    expect(screen.getByText('Campo')).toBeInTheDocument()
    expect(screen.getByText('Bolsas')).toBeInTheDocument()
    expect(screen.queryByText('Estudios')).toBeNull()
    expect(screen.queryByText('Admin')).toBeNull()
  })

  it('Teacher sees shift control buttons in FieldDashboard and full administrative menus', async () => {
    render(
      <MemoryRouter initialEntries={['/campo']}>
        <AuthProvider initialUser={teacherUser}>
          <FieldDashboard />
        </AuthProvider>
      </MemoryRouter>
    )

    // Docente CAN see shift buttons
    expect(
      screen.getByRole('button', { name: /Turno Abierto|Abrir Turno/i })
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cerrar Turno/i })).toBeInTheDocument()
  })

  it('RequireAuth redirects unauthenticated visitors to /login', async () => {
    render(
      <MemoryRouter initialEntries={['/campo']}>
        <AuthProvider initialUser={null} skipInitialFetch>
          <Routes>
            <Route path="/login" element={<div>Pantalla de Login</div>} />
            <Route element={<RequireAuth />}>
              <Route path="/campo" element={<div>Dashboard de Campo Protegido</div>} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    expect(await screen.findByText('Pantalla de Login')).toBeInTheDocument()
    expect(screen.queryByText('Dashboard de Campo Protegido')).toBeNull()
  })

  it('RequireRole redirects Student trying to access /admin to /campo with accessDenied state', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/fuentes']}>
        <AuthProvider initialUser={studentUser}>
          <Routes>
            <Route
              path="/campo"
              element={<FieldDashboard />}
            />
            <Route element={<RequireRole allowedRoles={['DOCENTE']} fallbackPath="/campo" />}>
              <Route path="/admin/fuentes" element={<div>Admin Fuentes Confidencial</div>} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    // Redirected to /campo and shows access denied warning
    expect(await screen.findByText('Acceso restringido')).toBeInTheDocument()
    expect(
      screen.getByText(/Acceso denegado: únicamente los docentes tienen permisos/i)
    ).toBeInTheDocument()
    expect(screen.queryByText('Admin Fuentes Confidencial')).toBeNull()
  })

  it('RequireRole allows Docente to access /admin routes directly', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/fuentes']}>
        <AuthProvider initialUser={teacherUser}>
          <Routes>
            <Route path="/campo" element={<div>Campo</div>} />
            <Route element={<RequireRole allowedRoles={['DOCENTE']} fallbackPath="/campo" />}>
              <Route path="/admin/fuentes" element={<div>Admin Fuentes Confidencial</div>} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    expect(await screen.findByText('Admin Fuentes Confidencial')).toBeInTheDocument()
  })

  it('RequirePermission renders fallback or null when user lacks required permission', () => {
    render(
      <AuthProvider initialUser={studentUser}>
        <RequirePermission permission="OPEN_SHIFT" fallback={<div>No autorizado</div>}>
          <button>Botón Abrir Turno</button>
        </RequirePermission>
      </AuthProvider>
    )

    expect(screen.getByText('No autorizado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Botón Abrir Turno' })).toBeNull()
  })

  it('hasPermission evaluates permissions strictly per AGENTS.md access policies', () => {
    expect(hasPermission('DOCENTE', 'OPEN_SHIFT')).toBe(true)
    expect(hasPermission('DOCENTE', 'CLOSE_SHIFT')).toBe(true)
    expect(hasPermission('DOCENTE', 'MANAGE_STUDIES')).toBe(true)
    expect(hasPermission('DOCENTE', 'MANAGE_ADMIN')).toBe(true)
    expect(hasPermission('DOCENTE', 'RECORD_HAZARDOUS_WASTE')).toBe(true)

    expect(hasPermission('ESTUDIANTE_UNIVERSITARIO', 'OPEN_SHIFT')).toBe(false)
    expect(hasPermission('ESTUDIANTE_UNIVERSITARIO', 'CLOSE_SHIFT')).toBe(false)
    expect(hasPermission('ESTUDIANTE_UNIVERSITARIO', 'MANAGE_STUDIES')).toBe(false)
    expect(hasPermission('ESTUDIANTE_UNIVERSITARIO', 'MANAGE_ADMIN')).toBe(false)
    expect(hasPermission('ESTUDIANTE_UNIVERSITARIO', 'RECORD_HAZARDOUS_WASTE')).toBe(false)

    expect(hasPermission(null, 'OPEN_SHIFT')).toBe(false)
  })

  it('AppLayout redirects to /login when user has no session', async () => {
    render(
      <MemoryRouter initialEntries={['/campo']}>
        <AuthProvider initialUser={null} skipInitialFetch>
          <Routes>
            <Route path="/login" element={<div>Pantalla de Login</div>} />
            <Route
              path="/campo"
              element={
                <AppLayout>
                  <div>Contenido Protegido</div>
                </AppLayout>
              }
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    expect(await screen.findByText('Pantalla de Login')).toBeInTheDocument()
    expect(screen.queryByText('Contenido Protegido')).toBeNull()
  })
})
