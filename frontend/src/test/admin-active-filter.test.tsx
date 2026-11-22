/**
 * Activate, deactivate and the active/inactive filter of the administration
 * lists (spec 004, R-320, R-321, R-324, R-325, CA-312).
 *
 * @author craulgomez
 */
import { describe, it, expect } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import type { ReactElement } from 'react'
import { AuthProvider } from '@/features/auth'
import { CategoriesPage, InstitutionsPage, SourcesPage } from '@/features/admin'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

function renderPage(page: ReactElement) {
  return render(
    <MemoryRouter>
      <AuthProvider initialUser={mockUsers[0]}>{page}</AuthProvider>
    </MemoryRouter>
  )
}

describe('Activar, desactivar y filtrar (R-325, CA-312)', () => {
  it('ca312_sourceCreatedInactiveIsHiddenShownWithTheFilterAndActivatedThere', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderPage(<SourcesPage />)

    await user.click(await screen.findByRole('button', { name: /Nueva Fuente/ }))
    await user.type(screen.getByLabelText(/Código de Fuente/), 'CAF')
    await user.type(screen.getByLabelText('Nombre Descriptivo'), 'Cafetería')
    await user.click(screen.getByRole('checkbox', { name: /Fuente activa para muestreo/ }))
    await user.click(screen.getByRole('button', { name: 'Guardar Fuente' }))

    await screen.findByRole('cell', { name: 'Aulas de Clase' })
    expect(screen.queryByTestId('source-row-CAF')).toBeNull()

    await user.click(screen.getByRole('button', { name: /Inactivos/ }))
    const row = await screen.findByTestId('source-row-CAF')
    expect(within(row).getByRole('switch', { name: 'Activar CAF' })).not.toBeChecked()

    await user.click(within(row).getByRole('switch', { name: 'Activar CAF' }))
    await user.click(screen.getByRole('button', { name: 'Activos' }))
    expect(within(await screen.findByTestId('source-row-CAF')).getByRole('switch', { name: 'Desactivar CAF' })).toBeChecked()
  })

  it('r320_institutionIsDeactivatedAndHiddenFromTheDefaultList', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderPage(<InstitutionsPage />)

    await user.click(await screen.findByRole('switch', { name: /Desactivar I\.E\. Prudencia Daza/ }))

    expect(await screen.findByText('No hay instituciones con este filtro.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /^Inactivos/ }))
    expect(await screen.findByRole('switch', { name: /Activar I\.E\. Prudencia Daza/ })).not.toBeChecked()
  })

  it('ca320_filterHasOnlyTheThreeOptionsWithoutEyeIcon', async () => {
    resetMockState('DOCENTE')
    renderPage(<InstitutionsPage />)

    const group = await screen.findByRole('group', { name: 'Mostrar por estado' })
    const labels = within(group).getAllByRole('button').map((button) => button.textContent?.replace(/\s*\(\d+\)$/, ''))
    expect(labels).toEqual(['Activos', 'Inactivos', 'Todos'])
    expect(screen.queryByRole('button', { name: /^(Mostrar|Ocultar) inactivos$/ })).toBeNull()
  })

  it('r324_subcategoryIsDeactivatedAndActivatedAgain', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    renderPage(<CategoriesPage />)

    const deactivate = await screen.findAllByRole('switch', { name: /^Desactivar / })
    const name = deactivate[0].getAttribute('aria-label')!.replace('Desactivar ', '')
    await user.click(deactivate[0])
    await waitFor(() => expect(screen.queryByRole('switch', { name: `Desactivar ${name}` })).toBeNull())

    await user.click(screen.getByRole('button', { name: 'Todos' }))
    await user.click(await screen.findByRole('switch', { name: `Activar ${name}` }))
    expect(await screen.findByRole('switch', { name: `Desactivar ${name}` })).toBeInTheDocument()
  })
})
