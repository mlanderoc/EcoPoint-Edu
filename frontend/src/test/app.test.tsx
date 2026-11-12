import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import App from '@/App'

describe('App Initial Render (Phase 0)', () => {
  it('renders EcoPoint Edu title and institutional logo', async () => {
    render(<App />)
    expect(await screen.findByText('EcoPoint Edu — Sistema de Muestreo')).toBeInTheDocument()
    const logos = await screen.findAllByAltText('EcoPoint Edu Logo')
    expect(logos.length).toBeGreaterThanOrEqual(1)
  })

  it('displays the 4 waste groups from Res. 2184/2019', async () => {
    render(<App />)
    expect((await screen.findAllByText('Blanco')).length).toBeGreaterThanOrEqual(1)
    expect((await screen.findAllByText('Verde Orgánico')).length).toBeGreaterThanOrEqual(1)
    expect((await screen.findAllByText('Negro')).length).toBeGreaterThanOrEqual(1)
    expect((await screen.findAllByText('Sin color')).length).toBeGreaterThanOrEqual(1)
  })
})
