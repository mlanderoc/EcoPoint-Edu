/**
 * Phone menu with the modules that do not fit in the bottom bar (D15).
 *
 * @author craulgomez
 */

import { Bell, Building, MapPin, Tags, UserCheck, Users, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/features/auth/hooks/useAuth'

const COMMON = [
  { label: 'Alertas', href: '/alertas', icon: Bell },
  { label: 'Mi perfil', href: '/perfil', icon: UserCheck },
]
const TEACHER = [
  { label: 'Instituciones', href: '/admin/instituciones', icon: Building },
  { label: 'Fuentes', href: '/admin/fuentes', icon: MapPin },
  { label: 'Usuarios', href: '/admin/usuarios', icon: Users },
  { label: 'Cuadrillas', href: '/admin/cuadrillas', icon: UsersRound },
  { label: 'Subcategorías', href: '/admin/subcategorias', icon: Tags },
]

export function MorePage() {
  const { user } = useAuth()
  const links = user?.role === 'DOCENTE' ? [...COMMON, ...TEACHER] : COMMON
  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-4">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Más opciones</h1>
          <p className="m-0 mt-1">Todos los módulos a la mano.</p>
        </div>
      </div>
      <nav className="more-grid" aria-label="Más módulos">
        {links.map(({ label, href, icon: Icon }) => (
          <Link key={href} to={href}><Icon aria-hidden="true" />{label}</Link>
        ))}
      </nav>
    </div>
  )
}
