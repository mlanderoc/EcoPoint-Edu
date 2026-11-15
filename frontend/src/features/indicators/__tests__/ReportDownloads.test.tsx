/**
 * Excel and PDF download buttons of the dashboard (Spec 006, T512).
 *
 * @author craulgomez
 */
import { afterEach, describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { AuthProvider } from '@/features/auth'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState, setActiveUser } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { IndicatorsDashboard } from '../pages/IndicatorsDashboard'

function renderDashboard() {
  setActiveUser(mockUsers[0])
  return render(
    <MemoryRouter>
      <AuthProvider initialUser={mockUsers[0]}>
        <IndicatorsDashboard />
      </AuthProvider>
    </MemoryRouter>
  )
}

function spyDownloads() {
  const createObjectURL = vi.fn((_file: Blob) => 'blob:informe')
  const revokeObjectURL = vi.fn()
  Object.assign(window.URL, { createObjectURL, revokeObjectURL })
  const names: string[] = []
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
    names.push(this.download)
  })
  return { createObjectURL, revokeObjectURL, names }
}

afterEach(() => vi.restoreAllMocks())

describe('Descarga de reportes (T512)', () => {
  it('t512_excelAndPdfAreDownloadedWithTheirFileNames', async () => {
    resetMockState('DOCENTE')
    const downloads = spyDownloads()
    const user = userEvent.setup()
    renderDashboard()

    await user.click(await screen.findByRole('button', { name: /Exportar Excel/ }))
    await waitFor(() => expect(downloads.names).toHaveLength(1))
    await user.click(screen.getByRole('button', { name: /Descargar PDF/ }))
    await waitFor(() => expect(downloads.names).toHaveLength(2))

    expect(downloads.names[0]).toMatch(/^ecopoint-reporte-estudio-\d+\.xlsx$/)
    expect(downloads.names[1]).toMatch(/^ecopoint-informe-linea-base-\d+\.pdf$/)
    expect(downloads.createObjectURL.mock.calls[1][0]).toBeInstanceOf(Blob)
    expect(downloads.revokeObjectURL).toHaveBeenCalledTimes(2)
  })

  it('t512_aFailedDownloadShowsTheMessageOfTheApi', async () => {
    resetMockState('DOCENTE')
    spyDownloads()
    server.use(http.get('/api/v1/studies/:id/reports/:format', () => HttpResponse.json(
      { status: 500, errorCode: 'ERR_INTERNAL', message: 'No se pudo generar el informe PDF.', fieldErrors: [] },
      { status: 500 })))
    const user = userEvent.setup()
    renderDashboard()

    await user.click(await screen.findByRole('button', { name: /Descargar PDF/ }))

    expect(await screen.findByText('No se pudo generar el informe PDF.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Descargar PDF/ })).toBeEnabled()
  })
})
