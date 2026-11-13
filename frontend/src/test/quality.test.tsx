/**
 * Quartering and density screens (Spec 005, T403 and T404; CA-400 to CA-407).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { AuthProvider } from '@/features/auth'
import { DensityPage, QuarteringPage } from '@/features/quality'
import { ShiftPage } from '@/features/field'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'
import { server } from '@/mocks/server'

const student = mockUsers[1]

function shiftWithNetWeight(totalNetWeight: number) {
  server.use(http.get('/api/v1/shifts/:shiftId', () => HttpResponse.json({
    id: 14, studyId: 2, dayNumber: 3, type: 'MANANA', code: 'D3-M', status: 'ABIERTO', crewId: 1,
    population: { id: 1, students: 450, teachers: 18, staff: 8, total: 476, verification: null },
    bagCount: 10, totalNetWeight, sourcesWithoutGeneration: [], hasQuartering: false, hasDensity: false, sources: [],
  })))
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider initialUser={student}>
        <Routes>
          <Route path="/campo/turno/:id" element={<ShiftPage />} />
          <Route path="/campo/turno/:id/cuarteo" element={<QuarteringPage />} />
          <Route path="/campo/turno/:id/densidad" element={<DensityPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Quartering screen (T403)', () => {
  it('ca400_subsampleOutOfRangeBlocksSaving', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    shiftWithNetWeight(120)
    const user = userEvent.setup()
    renderAt('/campo/turno/14/cuarteo')

    await user.type(await screen.findByLabelText('Peso de la submuestra (kg)'), '60')
    await user.type(screen.getByLabelText('Papel de archivo (kg)'), '60')

    expect(screen.getByText('Debe estar entre 45 y 55 kg')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Guardar cuarteo' })).toBeDisabled()
  })

  it('ca401_smallShiftProposesAFullSample', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    shiftWithNetWeight(32)
    renderAt('/campo/turno/14/cuarteo')

    expect(await screen.findByLabelText('Peso de la submuestra (kg)')).toHaveValue('32')
    expect(screen.getByText('Muestra completa')).toBeInTheDocument()
  })

  it('ca402_liveBarShowsAConformingDifference', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    shiftWithNetWeight(120)
    const user = userEvent.setup()
    renderAt('/campo/turno/14/cuarteo')

    await user.type(await screen.findByLabelText('Peso de la submuestra (kg)'), '50')
    await user.type(screen.getByLabelText('Papel de archivo (kg)'), '30')
    await user.type(screen.getByLabelText('Restos de comida (kg)'), '19,2')

    const bar = screen.getByTestId('quality-bar')
    expect(bar).toHaveTextContent('0,80 kg (1,60 %)')
    expect(within(bar).getByText('Conforme (≤ 2 %)')).toBeInTheDocument()
  })

  it('ca403_nonConformingQuarteringIsSavedWithItsWarning', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    shiftWithNetWeight(120)
    const user = userEvent.setup()
    renderAt('/campo/turno/14/cuarteo')

    await user.type(await screen.findByLabelText('Peso de la submuestra (kg)'), '50')
    await user.type(screen.getByLabelText('Papel de archivo (kg)'), '30')
    await user.type(screen.getByLabelText('Restos de comida (kg)'), '18,5')
    expect(within(screen.getByTestId('quality-bar')).getByText('No conforme (> 2 %)')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Guardar cuarteo' }))
    expect(await screen.findByText('Cuarteo no conforme: repesar')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Anular cuarteo' })).toBeInTheDocument()
  })

  it('ca405_studentRegistersHazardousWasteWithItsWarning', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    shiftWithNetWeight(120)
    renderAt('/campo/turno/14/cuarteo')

    expect(await screen.findByLabelText('Pilas y baterías (kg)')).toBeEnabled()
    expect(screen.getByText('Residuos peligrosos: van a posconsumo')).toBeInTheDocument()
  })

  it('ca406_savedQuarteringShowsItsCompositionByGroup', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    shiftWithNetWeight(120)
    const user = userEvent.setup()
    renderAt('/campo/turno/14/cuarteo')

    await user.type(await screen.findByLabelText('Peso de la submuestra (kg)'), '50')
    await user.type(screen.getByLabelText('Papel de archivo (kg)'), '10')
    await user.type(screen.getByLabelText('Restos de comida (kg)'), '25')
    await user.type(screen.getByLabelText('Papel higiénico y servilletas (kg)'), '15')
    await user.click(screen.getByRole('button', { name: 'Guardar cuarteo' }))

    const groups = await screen.findByTestId('group-composition')
    expect(groups).toHaveTextContent('20,00 %')
    expect(groups).toHaveTextContent('50,00 %')
    expect(groups).toHaveTextContent('30,00 %')
    expect(groups).toHaveTextContent('0,00 %')
  })
})

describe('Density screen (T404)', () => {
  it('ca407_densityIsCalculatedAndSaved', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = userEvent.setup()
    renderAt('/campo/turno/14/densidad')

    await user.click(await screen.findByRole('button', { name: /Caneca de 55 galones/ }))
    await user.type(screen.getByLabelText('Peso neto de los residuos (kg)'), '31,20')
    expect(screen.getByTestId('live-shift-density')).toHaveTextContent('150,00 kg/m³')

    await user.click(screen.getByRole('button', { name: 'Guardar densidad' }))
    expect(await screen.findByTestId('saved-density')).toHaveTextContent('150,00 kg/m³')
  })
})
