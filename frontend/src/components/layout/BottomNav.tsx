import React from 'react'
import { ClipboardList, Calendar, Bell, UserCheck, Settings, ShoppingBag } from 'lucide-react'
import { UserRole } from '@/types'
import { cn } from '@/utils/cn'
import { useAuth } from '@/features/auth/hooks/useAuth'

export interface BottomNavProps {
  role?: UserRole
  currentPath?: string
  onNavigate?: (href: string) => void
}

export function BottomNav({
  role: roleProp,
  currentPath = '/campo',
  onNavigate,
}: BottomNavProps) {
  let authRole: UserRole | undefined
  try {
    const auth = useAuth()
    authRole = auth.role ?? undefined
  } catch {
    // Outside AuthProvider
  }

  const role = roleProp ?? authRole

  // Without role or session, do not render bottom navigation
  if (!role) {
    return null
  }

  const isDocente = role === 'DOCENTE'

  // Items per role strictly respecting CA-300
  const items = isDocente
    ? [
        { label: 'Campo', href: '/campo', icon: <ClipboardList className="w-5 h-5" /> },
        { label: 'Estudios', href: '/estudios', icon: <Calendar className="w-5 h-5" /> },
        { label: 'Admin', href: '/admin/instituciones', icon: <Settings className="w-5 h-5" /> },
        { label: 'Alertas', href: '/alertas', icon: <Bell className="w-5 h-5" /> },
      ]
    : [
        { label: 'Campo', href: '/campo', icon: <ClipboardList className="w-5 h-5" /> },
        { label: 'Bolsas', href: '/campo/bolsas', icon: <ShoppingBag className="w-5 h-5" /> },
        { label: 'Alertas', href: '/alertas', icon: <Bell className="w-5 h-5" /> },
        { label: 'Perfil', href: '/perfil', icon: <UserCheck className="w-5 h-5" /> },
      ]

  const handleNavigate = (e: React.MouseEvent, href: string) => {
    e.preventDefault()
    onNavigate?.(href)
  }

  return (
    <nav
      aria-label="Navegación móvil"
      className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white border-t border-slate-200 px-2 py-1 shadow-lg flex items-center justify-around select-none"
    >
      {items.map((item) => {
        const active = currentPath === item.href || currentPath.startsWith(`${item.href}/`)
        return (
          <a
            key={item.href}
            href={item.href}
            onClick={(e) => handleNavigate(e, item.href)}
            className={cn(
              'min-h-[48px] min-w-[48px] flex-1 flex flex-col items-center justify-center gap-0.5 rounded-lg py-1 transition cursor-pointer',
              active
                ? 'text-[var(--color-primary)] font-bold'
                : 'text-slate-500 hover:text-slate-800 font-medium'
            )}
          >
            <span className={active ? 'text-[var(--color-primary)]' : 'text-slate-500'}>
              {item.icon}
            </span>
            <span className="text-[10px] leading-tight tracking-tight">{item.label}</span>
          </a>
        )
      })}
    </nav>
  )
}
