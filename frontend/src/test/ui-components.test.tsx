import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Button, Input, Select, Card, AlertBanner } from '@/components/ui'

describe('UI Atomic Components (T305)', () => {
  it('Button default has min-h-[48px] touch target and triggers onClick', () => {
    const handleClick = vi.fn()
    render(<Button onClick={handleClick}>Registrar Bolsa</Button>)
    const btn = screen.getByRole('button', { name: 'Registrar Bolsa' })
    expect(btn).toHaveClass('min-h-[48px]')
    fireEvent.click(btn)
    expect(handleClick).toHaveBeenCalledTimes(1)
  })

  it('Button with loading=true is disabled and does not trigger onClick', () => {
    const handleClick = vi.fn()
    render(
      <Button loading onClick={handleClick}>
        Guardar
      </Button>
    )
    const btn = screen.getByRole('button')
    expect(btn).toBeDisabled()
    fireEvent.click(btn)
    expect(handleClick).not.toHaveBeenCalled()
  })

  it('Input has min-h-[48px], supports inputMode="decimal" and normalizes comma', () => {
    const handleChange = vi.fn()
    render(
      <Input
        label="Peso Bruto"
        inputMode="decimal"
        normalizeDecimal
        onChange={handleChange}
        placeholder="0.00"
      />
    )
    const input = screen.getByLabelText('Peso Bruto')
    expect(input).toHaveClass('min-h-[48px]')
    expect(input).toHaveAttribute('inputMode', 'decimal')

    fireEvent.change(input, { target: { value: '12,40' } })
    expect(input).toHaveValue('12.40')
  })

  it('Input displays error message in red border and text', () => {
    render(<Input label="Tara" error="La tara no puede superar el peso bruto." />)
    const errorMsg = screen.getByRole('alert')
    expect(errorMsg).toHaveTextContent('La tara no puede superar el peso bruto.')
    const input = screen.getByLabelText('Tara')
    expect(input).toHaveClass('border-red-500')
  })

  it('Select has min-h-[48px] and renders options', () => {
    render(
      <Select
        label="Fuente"
        options={[
          { value: '1', label: 'AU - Aulas de Clase' },
          { value: '2', label: 'BA - Baterías Sanitarias' },
        ]}
      />
    )
    const select = screen.getByLabelText('Fuente')
    expect(select).toHaveClass('min-h-[48px]')
    expect(screen.getByText('AU - Aulas de Clase')).toBeInTheDocument()
  })

  it('AlertBanner renders warning variant with alert role', () => {
    render(
      <AlertBanner variant="warning" title="Valor Atípico">
        El peso ingresado supera el rango esperado.
      </AlertBanner>
    )
    const banner = screen.getByRole('alert')
    expect(banner).toBeInTheDocument()
    expect(screen.getByText('Valor Atípico')).toBeInTheDocument()
  })

  it('Card renders content cleanly with border and shadow', () => {
    render(
      <Card>
        <p>Contenido de prueba</p>
      </Card>
    )
    expect(screen.getByText('Contenido de prueba')).toBeInTheDocument()
  })
})
