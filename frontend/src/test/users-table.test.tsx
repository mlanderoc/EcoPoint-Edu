/**
 * Users table with server filters, search and pagination (spec 004, R-322,
 * CA-313; RN-087).
 *
 * @author craulgomez
 */
import { describe, it, expect } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { UsersPage, adminService } from '@/features/admin'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

async function addStudents(count: number) {
  const created = []
  for (let index = 1; index <= count; index++) {
    created.push(await adminService.createUser({
      firstName: 'Estudiante', lastName: `Prueba ${index}`, email: `estudiante${index}@unicesar.edu.co`,
      role: 'ESTUDIANTE_UNIVERSITARIO', password: 'ClaveSegura2026',
    }))
  }
  return created
}

function renderUsers() {
  return render(
    <MemoryRouter>
      <AuthProvider initialUser={mockUsers[0]}>
        <UsersPage />
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Tabla de usuarios (R-322, CA-313)', () => {
  it('ca313_fiftyStudentsArePaginatedAndFilteredByRoleAndState', async () => {
    resetMockState('DOCENTE')
    const students = await addStudents(50)
    await adminService.setUserActive(students[9].id, false)
    const user = userEvent.setup()
    renderUsers()

    const table = await screen.findByTestId('users-table')
    expect(within(table).getAllByRole('row')).toHaveLength(21)
    expect(screen.getByTestId('users-total')).toHaveTextContent(/página 1 de 3/)

    await user.click(screen.getByRole('button', { name: /Siguiente/ }))
    await waitFor(() => expect(screen.getByTestId('users-total')).toHaveTextContent(/página 2 de 3/))

    await user.selectOptions(screen.getByLabelText('Rol'), 'ESTUDIANTE_UNIVERSITARIO')
    await user.selectOptions(screen.getByLabelText('Estado'), 'false')
    await waitFor(() => expect(screen.getByTestId('users-total')).toHaveTextContent(/^1 usuario/))
    expect(screen.getByText('estudiante10@unicesar.edu.co')).toBeInTheDocument()
  })

  it('ca313_searchFindsByEmailAndTeachersHaveNoDeactivateButton', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderUsers()

    const teacherRow = await screen.findByTestId(`user-row-${mockUsers[0].id}`)
    expect(within(teacherRow).queryByRole('button', { name: /Desactivar/ })).toBeNull()

    await user.type(screen.getByLabelText('Buscar'), 'estudiante.demo')
    await waitFor(() => expect(screen.getByTestId('users-total')).toHaveTextContent(/^1 usuario/))
    const studentRow = screen.getByTestId(`user-row-${mockUsers[1].id}`)
    await user.click(within(studentRow).getByRole('button', { name: /Desactivar/ }))
    expect(await within(studentRow).findByRole('button', { name: /Activar/ })).toBeInTheDocument()
  })
})
