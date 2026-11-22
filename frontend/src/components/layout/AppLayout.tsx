/**
 * Main application responsive layout with Sidebar, top bar and BottomNav (T307 / T310; D15).
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
import { useOptionalAuth } from '@/features/auth/hooks/useAuth'

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
  const auth = useOptionalAuth()
  const authUser: User | null = auth?.user ?? null
  const authLogout: (() => Promise<void>) | undefined = auth?.logout
  const isAuthLoading = auth?.isLoading ?? false
  const isAuthenticated = auth?.isAuthenticated ?? false

  const user = userProp !== undefined ? userProp : authUser
  const handleLogout = onLogout ?? authLogout

  // If inside an AuthProvider, user not passed as prop, not loading, and not authenticated, redirect to /login
  if (userProp === undefined && !isAuthLoading && !isAuthenticated && !user) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="shell overflow-x-hidden w-full">
      <Sidebar role={user?.role} user={user} currentPath={currentPath} onNavigate={onNavigate} />
      <div className="shell-main">
        <Header user={user} currentPath={currentPath} onLogout={handleLogout} />
        <main id="main-content" className="shell-content">
          {children}
        </main>
      </div>
      <BottomNav role={user?.role} currentPath={currentPath} onNavigate={onNavigate} />
    </div>
  )
}
