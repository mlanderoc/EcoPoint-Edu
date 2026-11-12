import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import App from '@/App'

describe('App Initial Render (Phase 0)', () => {
  it('renders EcoPoint Edu title and institutional logo', () => {
    render(<App />)
    expect(screen.getByText('EcoPoint Edu')).toBeInTheDocument()
    const logos = screen.getAllByAltText('EcoPoint Edu Logo')
    expect(logos.length).toBeGreaterThanOrEqual(1)
  })

  it('displays the 4 waste groups from Res. 2184/2019', () => {
    render(<App />)
    expect(screen.getByText('Blanco')).toBeInTheDocument()
    expect(screen.getByText('Verde Orgánico')).toBeInTheDocument()
    expect(screen.getByText('Negro')).toBeInTheDocument()
    expect(screen.getByText('Sin color')).toBeInTheDocument()
  })
})
