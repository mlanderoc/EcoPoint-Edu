/**
 * Unit and integration tests for role-based route guards and conditional permissions (T310 / CA-300 / R-311).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import {
  AuthProvider,
  RequireAuth,
  RequireRole,
  RequirePermission,
  hasPermission,
  Permission,
  ALL_PERMISSIONS,
} from '@/features/auth'
import { FieldDashboard } from '@/features/field/pages/FieldDashboard'
import { Sidebar, BottomNav, AppLayout } from '@/components/layout'
import { mockUsers } from '@/mocks/data/seedData'
import { User } from '@/types'

describe('Role Guards and Conditional Visibility (T310, CA-300, R-311, RN-081)', () => {
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
        <RequirePermission permission="MANAGE_SHIFTS" fallback={<div>No autorizado</div>}>
          <button>Botón Abrir Turno</button>
        </RequirePermission>
      </AuthProvider>
    )

    expect(screen.getByText('No autorizado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Botón Abrir Turno' })).toBeNull()
  })

  // RN-081 Permission Matrix exact test: 2 roles x 11 permissions = 22 assertions
  const expectedMatrix: Array<[Permission, boolean, boolean]> = [
    ['MANAGE_ADMINISTRATION', true, false],
    ['MANAGE_STUDIES', true, false],
    ['MANAGE_SHIFTS', true, false],
    ['REGISTER_POPULATION', true, true],
    ['REGISTER_BAGS', true, true],
    ['REGISTER_QUARTERING', true, true],
    ['REGISTER_HAZARDOUS_WASTE', true, true],
    ['VERIFY_RECORDS', true, true],
    ['ANNUL_QUARTERING', true, true],
    ['ATTEND_ALERTS', true, true],
    ['VIEW_INDICATORS', true, true],
  ]

  it.each(expectedMatrix)(
    'rn081_permissionMatrix: %s => DOCENTE=%s, ESTUDIANTE_UNIVERSITARIO=%s',
    (perm, teacher, student) => {
      expect(hasPermission('DOCENTE', perm)).toBe(teacher)
      expect(hasPermission('ESTUDIANTE_UNIVERSITARIO', perm)).toBe(student)
    }
  )

  it('rn081_matrixCoversAll11Permissions', () => {
    expect(ALL_PERMISSIONS).toHaveLength(11)
  })

  it('rn081_nullRoleOrPermissionReturnsFalse', () => {
    expect(hasPermission(null, 'MANAGE_SHIFTS')).toBe(false)
    expect(hasPermission(undefined, 'MANAGE_SHIFTS')).toBe(false)
    // @ts-expect-error test null permission
    expect(hasPermission('DOCENTE', null)).toBe(false)
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

