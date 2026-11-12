import React from 'react'
import { Header } from './Header'
import { Sidebar } from './Sidebar'
import { BottomNav } from './BottomNav'
import { User } from '@/types'

export interface AppLayoutProps {
  user?: User | null
  currentPath?: string
  onNavigate?: (href: string) => void
  onLogout?: () => void
  children: React.ReactNode
}

export function AppLayout({
  user,
  currentPath = '/campo',
  onNavigate,
  onLogout,
  children,
}: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-[var(--color-bg-app)] text-[var(--color-text-main)] flex flex-col overflow-x-hidden w-full">
      {/* Header fijo superior */}
      <Header user={user} onLogout={onLogout} />

      {/* Cuerpo principal con Sidebar y Contenido */}
      <div className="flex-1 flex w-full overflow-x-hidden">
        {/* Sidebar visible solo en md:flex (>= 768px) */}
        <Sidebar
          role={user?.role}
          currentPath={currentPath}
          onNavigate={onNavigate}
          className="hidden md:flex"
        />

        {/* Contenedor de contenido principal */}
        <main
          id="main-content"
          className="flex-1 w-full max-w-full p-4 md:p-6 pb-24 md:pb-6 overflow-x-hidden flex flex-col"
        >
          {children}
        </main>
      </div>

      {/* Barra de navegación inferior visible en celulares (< 768px) */}
      <BottomNav role={user?.role} currentPath={currentPath} onNavigate={onNavigate} />
    </div>
  )
}
