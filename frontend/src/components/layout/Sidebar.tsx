import React, { useEffect, useState } from 'react'
import {
  BarChart3,
  Bell,
  Building,
  Calendar,
  ClipboardList,
  Home,
  MapPin,
  ShieldCheck,
  Tags,
  UserCheck,
  Users,
  UsersRound,
} from 'lucide-react'
import { User, UserRole } from '@/types'
import { cn } from '@/utils/cn'
import { useOptionalAuth } from '@/features/auth/hooks/useAuth'
import { alertService } from '@/features/quality/services/verificationService'

export interface NavItem {
  label: string
  href: string
  icon: React.ReactNode
  badge?: string
}

export interface SidebarProps {
  role?: UserRole
  user?: User | null
  currentPath?: string
  onNavigate?: (href: string) => void
  className?: string
}

const isActive = (currentPath: string, href: string) => currentPath === href || currentPath.startsWith(`${href}/`)

export function Sidebar({
  role: roleProp,
  user,
  currentPath = '/campo',
  onNavigate,
  className,
}: SidebarProps) {
  const authRole: UserRole | undefined = useOptionalAuth()?.role ?? undefined

  const role = roleProp ?? authRole

  const [pendingAlerts, setPendingAlerts] = useState(0)

  // Counter of pending alerts in the menu (spec 005, R-432)
  useEffect(() => {
    if (!role) return
    alertService.pendingCount().then(setPendingAlerts).catch(() => setPendingAlerts(0))
  }, [role, currentPath])

  // Without role or session, do not render menu
  if (!role) {
    return null
  }

  // Navigation structure adhering strictly to CA-300 and RN-081
  const isDocente = role === 'DOCENTE'

  // The teacher configures the study before registering in the field (R-311, D14)
  const mainItems: NavItem[] = [
    { label: 'Inicio', href: '/inicio', icon: <Home /> },
    ...(isDocente ? [{ label: 'Configuración Estudio', href: '/estudios', icon: <Calendar /> }] : []),
    { label: 'Registro en Campo', href: '/campo', icon: <ClipboardList /> },
    { label: 'Verificación', href: '/calidad/verificacion', icon: <ShieldCheck /> },
    { label: 'Indicadores', href: '/indicadores', icon: <BarChart3 /> },
    { label: pendingAlerts > 0 ? `Alertas (${pendingAlerts})` : 'Alertas', href: '/alertas', icon: <Bell /> },
    { label: 'Mi Perfil', href: '/perfil', icon: <UserCheck /> },
  ]

  const adminItems: NavItem[] = isDocente
    ? [
        { label: 'Instituciones', href: '/admin/instituciones', icon: <Building /> },
        { label: 'Fuentes de Residuos', href: '/admin/fuentes', icon: <MapPin /> },
        { label: 'Usuarios', href: '/admin/usuarios', icon: <Users /> },
        { label: 'Cuadrillas', href: '/admin/cuadrillas', icon: <UsersRound /> },
        { label: 'Subcategorías', href: '/admin/subcategorias', icon: <Tags /> },
      ]
    : []

  const renderLink = (item: NavItem) => {
    const active = isActive(currentPath, item.href)
    return (
      <a
        key={item.href}
        href={item.href}
        aria-current={active ? 'page' : undefined}
        onClick={(event) => {
          event.preventDefault()
          onNavigate?.(item.href)
        }}
        className={cn('shell-link min-h-[48px]', active && 'on')}
      >
        {item.icon}
        <span>{item.label}</span>
      </a>
    )
  }

  return (
    <aside className={cn('shell-side select-none', className)} aria-label="Menú principal">
      <div className="shell-logo">
        <span><img src="/brand/logo.png" alt="" /></span>
        EcoPoint Edu
      </div>
      <nav className="shell-nav">{mainItems.map(renderLink)}</nav>

      {/* Administration Section - Visible exclusively to DOCENTE (CA-300) */}
      {isDocente && (
        <nav className="shell-nav" aria-label="Administración">
          <p className="shell-nav-title">Administración</p>
          {adminItems.map(renderLink)}
        </nav>
      )}

      <div className="shell-person">
        <span className="shell-avatar">{`${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`.toUpperCase() || (isDocente ? 'D' : 'E')}</span>
        <div className="min-w-0">
          {user && <b>{user.firstName} {user.lastName}</b>}
          <small>{isDocente ? 'Docente' : 'Estudiante'}</small>
        </div>
      </div>
    </aside>
  )
}
