/**
 * First screen of the application after the session check (Phase 0, updated
 * for the field dashboard of T317).
 *
 * @author craulgomez
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import App from '@/App'
import { resetMockState } from '@/mocks/handlers'

describe('App initial render', () => {
  it('renders the layout with the institutional logo and the home screen', async () => {
    resetMockState('DOCENTE')
    render(<App />)

    expect(await screen.findByRole('heading', { name: /Buen(os|as) (días|tardes|noches)/ })).toBeInTheDocument()
    expect((await screen.findAllByAltText('EcoPoint Edu Logo')).length).toBeGreaterThanOrEqual(1)
  })

  it('shows the study in execution on the home screen', async () => {
    resetMockState('DOCENTE')
    render(<App />)

    expect(await screen.findByText(/Estudio N.º 2/)).toBeInTheDocument()
  })
})
