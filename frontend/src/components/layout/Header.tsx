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
}

export function Header({ user, currentPath = '', onLogout }: HeaderProps) {
  const section = SECTIONS.find(([path]) => currentPath === path || currentPath.startsWith(`${path}/`))?.[1]
  return (
    <header className="shell-top">
      <div className="shell-crumb">
        <img src="/brand/logo.png" alt="EcoPoint Edu Logo" />
        <span>{section && section !== 'Inicio' ? `Inicio  ›  ${section}` : 'Inicio'}</span>
      </div>
      {user && (
        <button type="button" onClick={onLogout} title="Cerrar sesión" className="shell-logout">
          <LogOut className="w-4 h-4" aria-hidden="true" />
          <span className="max-sm:sr-only">Cerrar sesión</span>
        </button>
      )}
    </header>
  )
}
