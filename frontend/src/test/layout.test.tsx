import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { AppLayout, Sidebar, BottomNav } from '@/components/layout'
import { User } from '@/types'

describe('Layout and Menu per Role (T307, CA-300, CA-307)', () => {
  const studentUser: User = {
    id: 2,
    firstName: 'Estudiante',
    lastName: 'Demo',
    email: 'estudiante.demo@unicesar.edu.co',
    role: 'ESTUDIANTE_UNIVERSITARIO',
    active: true,
  }

  const teacherUser: User = {
    id: 1,
    firstName: 'Docente',
    lastName: 'Demo',
    email: 'docente.demo@unicesar.edu.co',
    role: 'DOCENTE',
    active: true,
  }

  it('ca300_studentMenuAndButtonsRestricted: Student does NOT see Admin or Study Config in Sidebar', () => {
    render(<Sidebar role={studentUser.role} />)
    expect(screen.getByText('Registro en Campo')).toBeInTheDocument()
    expect(screen.queryByText('Configuración Estudio')).toBeNull()
    expect(screen.queryByText('Administración')).toBeNull()
    expect(screen.queryByText('Instituciones')).toBeNull()
  })

  it('ca300_studentMenuAndButtonsRestricted: Student does NOT see Admin or Studies in BottomNav', () => {
    render(<BottomNav role={studentUser.role} />)
    expect(screen.getByText('Campo')).toBeInTheDocument()
    expect(screen.getByText('Calidad')).toBeInTheDocument()
    expect(screen.queryByText('Estudios')).toBeNull()
    expect(screen.queryByText('Admin')).toBeNull()
  })

  it('Teacher sees Full Menu including Study Config and Admin in Sidebar and BottomNav', () => {
    render(<Sidebar role={teacherUser.role} />)
    expect(screen.getByText('Registro en Campo')).toBeInTheDocument()
    expect(screen.getByText('Configuración Estudio')).toBeInTheDocument()
    expect(screen.getByText('Administración')).toBeInTheDocument()
    expect(screen.getByText('Instituciones')).toBeInTheDocument()
    expect(screen.getByText('Fuentes de Residuos')).toBeInTheDocument()
  })

  it('ca318_teacherMenuStartsWithStudyConfiguration: Study Config comes before Field Registration', () => {
    const { unmount } = render(<Sidebar role={teacherUser.role} />)
    const sidebarLinks = screen.getAllByRole('link').map((link) => link.textContent?.trim())
    expect(sidebarLinks.indexOf('Configuración Estudio')).toBeGreaterThanOrEqual(0)
    expect(sidebarLinks.indexOf('Configuración Estudio')).toBeLessThan(sidebarLinks.indexOf('Registro en Campo'))
    expect(sidebarLinks.indexOf('Registro en Campo')).toBeLessThan(sidebarLinks.indexOf('Verificación'))
    unmount()

    render(<BottomNav role={teacherUser.role} />)
    const mobileLinks = screen.getAllByRole('link').map((link) => link.textContent?.trim())
    expect(mobileLinks.indexOf('Estudios')).toBeGreaterThanOrEqual(0)
    expect(mobileLinks.indexOf('Estudios')).toBeLessThan(mobileLinks.indexOf('Campo'))
  })

  it('ca307_mobile360ViewportNoHorizontalScroll: AppLayout container prevents horizontal overflow and ensures >= 48px touch targets', () => {
    const { container } = render(
      <div style={{ width: '360px' }}>
        <AppLayout user={studentUser}>
          <div className="p-4">
            <h1>Panel de Campo</h1>
          </div>
        </AppLayout>
      </div>
    )

    // The root clips horizontal overflow without breaking the sticky menu
    const layoutRoot = container.querySelector('.overflow-x-clip')
    expect(layoutRoot).toBeInTheDocument()

    // Verify touch targets in BottomNav are >= 48px
    const navItems = screen.getAllByRole('link')
    navItems.forEach((link) => {
      expect(link).toHaveClass('min-h-[48px]')
    })
  })
})
