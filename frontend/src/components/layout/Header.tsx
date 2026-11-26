/**
 * Top bar: where the user is, the greeting with today's date (professor's
 * review, point 8) and the sign-out button.
 *
 * @author craulgomez
 */

import { LogOut } from 'lucide-react'
import { User } from '@/types'

const SECTIONS: Array<[string, string]> = [
  ['/inicio', 'Inicio'],
  ['/estudios', 'Configuración del estudio'],
  ['/campo', 'Registro en campo'],
  ['/calidad', 'Verificación'],
  ['/indicadores', 'Indicadores'],
  ['/alertas', 'Alertas'],
  ['/perfil', 'Mi perfil'],
  ['/mas', 'Más opciones'],
  ['/admin/instituciones', 'Administración · Instituciones'],
  ['/admin/fuentes', 'Administración · Fuentes'],
  ['/admin/usuarios', 'Administración · Usuarios'],
  ['/admin/cuadrillas', 'Administración · Cuadrillas'],
  ['/admin/subcategorias', 'Administración · Subcategorías'],
]

export interface HeaderProps {
  user?: User | null
  currentPath?: string
  onLogout?: () => void
  /** Current moment; tests pass a fixed one. */
  now?: Date
}

/** "Buenos días" until noon, "Buenas tardes" until 7 p. m. and "Buenas noches" after. */
export function greeting(now: Date): string {
  const hour = now.getHours()
  if (hour >= 5 && hour < 12) return 'Buenos días'
  if (hour >= 12 && hour < 19) return 'Buenas tardes'
  return 'Buenas noches'
}

/** Today's date as "martes, 25 de noviembre de 2026". */
export function longDate(now: Date): string {
  return new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
    .format(now)
}

export function Header({ user, currentPath = '', onLogout, now = new Date() }: HeaderProps) {
  const section = SECTIONS.find(([path]) => currentPath === path || currentPath.startsWith(`${path}/`))?.[1]
  return (
    <header className="shell-top">
      <div className="shell-crumb">
        <img src="/brand/logo.png" alt="EcoPoint Edu Logo" />
        <span>{section && section !== 'Inicio' ? `Inicio  ›  ${section}` : 'Inicio'}</span>
      </div>
      {user && currentPath !== '/inicio' && (
        <p className="shell-hello" data-testid="greeting">
          <b>{greeting(now)}, {user.firstName}</b>
          <time dateTime={now.toISOString().slice(0, 10)}>{longDate(now)}</time>
        </p>
      )}
      {user && (
        <button type="button" onClick={onLogout} title="Cerrar sesión" className="shell-logout">
          <LogOut className="w-4 h-4" aria-hidden="true" />
          <span className="max-sm:sr-only">Cerrar sesión</span>
        </button>
      )}
    </header>
  )
}
