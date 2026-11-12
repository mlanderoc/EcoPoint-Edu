/**
 * Complete field flow on a 360 px phone (Spec 004, T324; CA-307, CA-308, R-301).
 * jsdom has no layout, so this test checks the flow and the touch-target classes;
 * the real width and sizes are measured with `npm run check:mobile` (Chrome at 360 px).
 *
 * @author craulgomez
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { FieldDashboard, NewBagPage, PopulationPage, ShiftPage } from '@/features/field'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

const MIN_TOUCH_PX = 48
const DAY_FOUR_MORNING = new Date(2026, 2, 5, 8, 0)

/** Smallest height a control declares with a Tailwind min-h-[Npx] class, or 0. */
function declaredHeight(element: HTMLElement): number {
  const match = /min-h-\[(\d+)px\]/.exec(element.className)
  return match ? Number(match[1]) : 0
}

/** Every visible button and text field of the screen declares a touch target of at least 48 px. */
function expectTouchTargets() {
  const controls = [...screen.queryAllByRole('button'), ...screen.queryAllByRole('textbox'),
    ...screen.queryAllByRole('spinbutton'), ...screen.queryAllByRole('combobox')]
  const small = controls.filter((control) => declaredHeight(control) < MIN_TOUCH_PX)
    .map((control) => control.getAttribute('aria-label') || control.textContent || control.id)
  expect(small).toEqual([])
}

describe('Field flow on a 360 px phone (T324, CA-307, CA-308)', () => {
  beforeEach(() => {
    window.innerWidth = 360
    window.dispatchEvent(new Event('resize'))
  })

  it('ca307_wholeShiftWithThreeBagsUsesOnlyLargeTouchTargets', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/campo']}>
        <AuthProvider initialUser={mockUsers[0]}>
          <Routes>
            <Route path="/campo" element={<FieldDashboard now={DAY_FOUR_MORNING} />} />
            <Route path="/campo/turno/:id" element={<ShiftPage />} />
            <Route path="/campo/turno/:id/poblacion" element={<PopulationPage />} />
            <Route path="/campo/turno/:id/bolsas/nueva" element={<NewBagPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    // Open the shift
    expectTouchTargets()
    await user.click(await screen.findByRole('button', { name: /Abrir turno/i }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar apertura' }))
    await screen.findByRole('heading', { name: 'Turno D4-M' })
    expectTouchTargets()

    // Population
    await user.click(screen.getByRole('button', { name: 'Registrar población' }))
    await user.type(await screen.findByLabelText('Estudiantes presentes'), '300')
    await user.type(screen.getByLabelText('Docentes'), '12')
    await user.type(screen.getByLabelText('Personal administrativo y de servicios'), '6')
    expectTouchTargets()
    await user.click(screen.getByRole('button', { name: 'Guardar población' }))

    // Three bags; each one takes 5 actions: source, gross, tare, volume and save (CA-308)
    await user.click(await screen.findByRole('button', { name: /Registrar bolsa/i }))
    const bags = [['AU', '12,40'], ['AU', '10,20'], ['TE', '4,10']]
    for (const [index, [code, gross]] of bags.entries()) {
      await user.click(await screen.findByRole('button', { name: new RegExp(`^${code} `) }))
      await user.type(screen.getByLabelText('Peso bruto (kg)'), gross)
      await user.type(screen.getByLabelText('Tara del recipiente (kg)'), '0,40')
      await user.type(screen.getByLabelText('Volumen de la bolsa (m³)'), '0,080')
      expectTouchTargets()
      await user.click(screen.getByRole('button', { name: 'Guardar bolsa' }))
      expect(await screen.findByTestId('bag-label')).toHaveTextContent(new RegExp(`^${code}-D4-M`))
      expectTouchTargets()
      const last = index === bags.length - 1
      await user.click(screen.getByRole('button', { name: last ? 'Ver turno' : 'Siguiente bolsa' }))
    }

    // Mark the other sources without generation and close the shift
    await screen.findByRole('heading', { name: 'Turno D4-M' })
    for (const code of ['BA', 'OF', 'PAE', 'ZV']) {
      await user.click(within(await screen.findByTestId(`source-${code}`))
        .getByRole('button', { name: 'Marcar sin generación' }))
      await within(screen.getByTestId(`source-${code}`)).findByText('Sin generación')
    }
    expectTouchTargets()
    await user.click(screen.getByRole('button', { name: 'Cerrar turno' }))
    const summary = await screen.findByTestId('close-summary')
    expect(summary).toHaveTextContent('318')
    expect(within(summary).getByText(/Bolsas:/)).toHaveTextContent('3')
    await user.click(screen.getByRole('button', { name: 'Confirmar cierre' }))
    expect(await screen.findByText(/Cerrado · 3 bolsas/)).toBeInTheDocument()
  }, 60000)
})
