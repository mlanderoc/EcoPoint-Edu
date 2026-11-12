import { LogOut } from 'lucide-react'
import { User } from '@/types'

export interface HeaderProps {
  user?: User | null
  onLogout?: () => void
}

export function Header({ user, onLogout }: HeaderProps) {
  return (
    <header className="bg-[var(--color-brand-dark)] text-white px-4 py-2.5 shadow-md sticky top-0 z-30 flex items-center justify-between border-b border-emerald-950">
      <div className="flex items-center gap-2.5">
        <img
          src="/logo_ecopoint.jpeg"
          alt="EcoPoint Edu Logo"
          className="w-9 h-9 object-contain rounded-md bg-white p-0.5 shadow-xs"
        />
        <div className="flex flex-col">
          <span className="text-base font-black tracking-tight leading-tight">EcoPoint Edu</span>
          <span className="text-[10px] text-emerald-200 font-medium hidden sm:inline leading-none">
            I.E. Prudencia Daza
          </span>
        </div>
      </div>

      {user && (
        <div className="flex items-center gap-3">
          <div className="text-right flex flex-col items-end">
            <span className="text-xs font-bold leading-tight">
              {user.firstName} {user.lastName}
            </span>
            <span
              className={`text-[10px] font-extrabold px-1.5 py-0.2 rounded-sm tracking-wide uppercase ${
                user.role === 'DOCENTE'
                  ? 'bg-[var(--color-accent-leaf)] text-slate-900'
                  : 'bg-[var(--color-edu-blue)] text-white'
              }`}
            >
              {user.role === 'DOCENTE' ? 'Docente' : 'Estudiante'}
            </span>
          </div>

          <button
            type="button"
            onClick={onLogout}
            title="Cerrar sesión"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-lg bg-emerald-900/60 hover:bg-emerald-900 text-emerald-100 hover:text-white transition cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span className="sr-only">Cerrar sesión</span>
          </button>
        </div>
      )}
    </header>
  )
}
