import React from 'react'
import {
  ClipboardList,
  Calendar,
  Settings,
  Bell,
  UserCheck,
  Building,
  MapPin,
  Users,
  Tags,
  ShieldCheck,
  BarChart3,
} from 'lucide-react'
import { UserRole } from '@/types'
import { cn } from '@/utils/cn'
import { useEffect, useState } from 'react'
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
  currentPath?: string
  onNavigate?: (href: string) => void
  className?: string
}

export function Sidebar({
  role: roleProp,
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

  const mainItems: NavItem[] = [
    {
      label: 'Registro en Campo',
      href: '/campo',
      icon: <ClipboardList className="w-5 h-5" />,
    },
    ...(isDocente
      ? [
          {
            label: 'Configuración Estudio',
            href: '/estudios',
            icon: <Calendar className="w-5 h-5" />,
          },
        ]
      : []),
    {
      label: 'Verificación',
      href: '/calidad/verificacion',
      icon: <ShieldCheck className="w-5 h-5" />,
    },
    {
      label: 'Indicadores',
      href: '/indicadores',
      icon: <BarChart3 className="w-5 h-5" />,
    },
    {
      label: pendingAlerts > 0 ? `Alertas (${pendingAlerts})` : 'Alertas',
      href: '/alertas',
      icon: <Bell className="w-5 h-5" />,
    },
    {
      label: 'Mi Perfil',
      href: '/perfil',
      icon: <UserCheck className="w-5 h-5" />,
    },
  ]

  const adminItems: NavItem[] = isDocente
    ? [
        { label: 'Instituciones', href: '/admin/instituciones', icon: <Building className="w-4 h-4" /> },
        { label: 'Fuentes de Residuos', href: '/admin/fuentes', icon: <MapPin className="w-4 h-4" /> },
        { label: 'Usuarios', href: '/admin/usuarios', icon: <Users className="w-4 h-4" /> },
        { label: 'Cuadrillas', href: '/admin/cuadrillas', icon: <Users className="w-4 h-4" /> },
        { label: 'Subcategorías', href: '/admin/subcategorias', icon: <Tags className="w-4 h-4" /> },
      ]
    : []

  const handleItemClick = (e: React.MouseEvent, href: string) => {
    e.preventDefault()
    onNavigate?.(href)
  }

  return (
    <aside
      className={cn(
        'w-64 bg-white border-r border-slate-200 flex flex-col shrink-0 min-h-[calc(100vh-57px)] select-none',
        className
      )}
    >
      <div className="p-4 flex flex-col gap-6 flex-1">
        <div>
          <p className="text-[11px] font-black uppercase tracking-wider text-slate-600 px-3 mb-2">
            Operación de Muestreo
          </p>
          <nav className="flex flex-col gap-1">
            {mainItems.map((item) => {
              const active = currentPath === item.href || currentPath.startsWith(`${item.href}/`)
              return (
                <a
                  key={item.href}
                  href={item.href}
                  onClick={(e) => handleItemClick(e, item.href)}
                  className={cn(
                    'min-h-[48px] flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-bold transition cursor-pointer',
                    active
                      ? 'bg-[var(--color-primary-light)] text-[var(--color-primary-hover)] border-l-4 border-[var(--color-primary)]'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                  )}
                >
                  <span className={active ? 'text-[var(--color-primary)]' : 'text-slate-500'}>
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </a>
              )
            })}
          </nav>
        </div>

        {/* Administration Section - Visible exclusively to DOCENTE (CA-300) */}
        {isDocente && (
          <div>
            <div className="flex items-center gap-1.5 px-3 mb-2">
              <Settings className="w-3.5 h-3.5 text-slate-600" />
              <p className="text-[11px] font-black uppercase tracking-wider text-slate-600">
                Administración
              </p>
            </div>
            <nav className="flex flex-col gap-1">
              {adminItems.map((item) => {
                const active = currentPath === item.href
                return (
                  <a
                    key={item.href}
                    href={item.href}
                    onClick={(e) => handleItemClick(e, item.href)}
                    className={cn(
                      'min-h-[48px] flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-semibold transition cursor-pointer',
                      active
                        ? 'bg-slate-100 text-slate-900 font-bold border-l-4 border-[var(--color-brand-dark)]'
                        : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                    )}
                  >
                    <span className="text-slate-500">{item.icon}</span>
                    <span>{item.label}</span>
                  </a>
                )
              })}
            </nav>
          </div>
        )}
      </div>

      <div className="p-4 border-t border-slate-100 text-[11px] text-slate-600">
        Rol actual: <span className="font-bold">{isDocente ? 'Docente' : 'Estudiante'}</span>
      </div>
    </aside>
  )
}
