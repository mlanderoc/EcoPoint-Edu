/**
 * Main application responsive layout with Header, Sidebar, and BottomNav (T307 / T310).
 * Connects with AuthContext to enforce role-based menu display and session redirect.
 *
 * @author craulgomez
 */

import React from 'react'
import { Navigate } from 'react-router-dom'
import { Header } from './Header'
import { Sidebar } from './Sidebar'
import { BottomNav } from './BottomNav'
import { User } from '@/types'
import { useAuth } from '@/features/auth/hooks/useAuth'

export interface AppLayoutProps {
  user?: User | null
  currentPath?: string
  onNavigate?: (href: string) => void
  onLogout?: () => void
  children: React.ReactNode
}

export function AppLayout({
  user: userProp,
  currentPath = '/campo',
  onNavigate,
  onLogout,
  children,
}: AppLayoutProps) {
  let authUser: User | null = null
  let authLogout: (() => Promise<void>) | undefined
  let isAuthLoading = false
  let isAuthenticated = false

  try {
    const auth = useAuth()
    authUser = auth.user
    authLogout = auth.logout
    isAuthLoading = auth.isLoading
    isAuthenticated = auth.isAuthenticated
  } catch {
    // Outside AuthProvider (e.g. standalone tests)
  }

  const user = userProp !== undefined ? userProp : authUser
  const handleLogout = onLogout ?? authLogout

  // If inside an AuthProvider, user not passed as prop, not loading, and not authenticated, redirect to /login
  if (userProp === undefined && !isAuthLoading && !isAuthenticated && !user) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="min-h-screen bg-[var(--color-bg-app)] text-[var(--color-text-main)] flex flex-col overflow-x-hidden w-full">
      {/* Header fijo superior */}
      <Header user={user} onLogout={handleLogout} />

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
