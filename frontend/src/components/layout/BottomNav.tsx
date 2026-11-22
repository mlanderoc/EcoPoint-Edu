import React from 'react'
import { BarChart3, Calendar, ClipboardList, Grid2x2, Home, ShieldCheck } from 'lucide-react'
import { UserRole } from '@/types'
import { cn } from '@/utils/cn'
import { useOptionalAuth } from '@/features/auth/hooks/useAuth'

export interface BottomNavProps {
  role?: UserRole
  currentPath?: string
  onNavigate?: (href: string) => void
}

/** Paths reached through "Más" on the phone. */
const MORE_PATHS = ['/mas', '/alertas', '/perfil', '/admin']

export function BottomNav({
  role: roleProp,
  currentPath = '/campo',
  onNavigate,
}: BottomNavProps) {
  const authRole: UserRole | undefined = useOptionalAuth()?.role ?? undefined

  const role = roleProp ?? authRole

  // Without role or session, do not render bottom navigation
  if (!role) {
    return null
  }

  // Items per role strictly respecting CA-300
  const items = [
    { label: 'Inicio', href: '/inicio', icon: <Home className="w-5 h-5" /> },
    ...(role === 'DOCENTE' ? [{ label: 'Estudios', href: '/estudios', icon: <Calendar className="w-5 h-5" /> }] : []),
    { label: 'Campo', href: '/campo', icon: <ClipboardList className="w-5 h-5" /> },
    { label: 'Calidad', href: '/calidad/verificacion', icon: <ShieldCheck className="w-5 h-5" /> },
    { label: 'Indicadores', href: '/indicadores', icon: <BarChart3 className="w-5 h-5" /> },
    { label: 'Más', href: '/mas', icon: <Grid2x2 className="w-5 h-5" /> },
  ]

  const handleNavigate = (event: React.MouseEvent, href: string) => {
    event.preventDefault()
    onNavigate?.(href)
  }

  const isActive = (href: string) => href === '/mas'
    ? MORE_PATHS.some((path) => currentPath === path || currentPath.startsWith(`${path}/`))
    : currentPath === href || currentPath.startsWith(`${href}/`)

  return (
    <nav aria-label="Navegación móvil" className="shell-bottom select-none">
      {items.map((item) => (
        <a
          key={item.href}
          href={item.href}
          aria-current={isActive(item.href) ? 'page' : undefined}
          onClick={(event) => handleNavigate(event, item.href)}
          className={cn('min-h-[48px]', isActive(item.href) && 'on')}
        >
          {item.icon}
          <span>{item.label}</span>
        </a>
      ))}
    </nav>
  )
}
