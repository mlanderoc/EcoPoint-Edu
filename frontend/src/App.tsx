/**
 * Root Application Component with React Router and AuthProvider (T307 / T309 / T310).
 * Configures role-based route guards (RequireAuth, RequireRole) and responsive layout.
 *
 * @author craulgomez
 */

import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AuthProvider, RequireAuth, RequireRole, LoginPage, useAuth } from '@/features/auth'
import { AppLayout } from '@/components/layout'
import { FieldDashboard } from '@/features/field/pages/FieldDashboard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { User } from '@/types'

/**
 * Layout wrapper that feeds current React Router location and navigation into AppLayout.
 */
function RoutedAppLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  return (
    <AppLayout
      user={user}
      currentPath={location.pathname}
      onNavigate={(href) => navigate(href)}
      onLogout={async () => {
        await logout()
        navigate('/login')
      }}
    >
      {children}
    </AppLayout>
  )
}

function LoginRoute() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  if (isAuthenticated) {
    return <Navigate to="/campo" replace />
  }

  return <LoginPage onSuccess={() => navigate('/campo', { replace: true })} />
}

function PlaceholderPage({ title, description }: { title: string; description: string }) {
  return (
    <div className="max-w-4xl w-full mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-[var(--color-text-muted)]">{description}</p>
        </CardContent>
      </Card>
    </div>
  )
}

export function AppRoutes() {
  return (
    <Routes>
      {/* Public Login Route */}
      <Route path="/login" element={<LoginRoute />} />

      {/* Protected Routes requiring authenticated session */}
      <Route element={<RequireAuth />}>
        {/* Field Dashboard (Common to Docente and Estudiante) */}
        <Route
          path="/campo"
          element={
            <RoutedAppLayout>
              <FieldDashboard />
            </RoutedAppLayout>
          }
        />

        <Route
          path="/perfil"
          element={
            <RoutedAppLayout>
              <PlaceholderPage
                title="Mi Perfil"
                description="Información personal y cuadrilla asignada en el estudio de caracterización."
              />
            </RoutedAppLayout>
          }
        />

        <Route
          path="/alertas"
          element={
            <RoutedAppLayout>
              <PlaceholderPage
                title="Alertas y Notificaciones"
                description="Registro histórico de alertas de calidad y pesos atípicos."
              />
            </RoutedAppLayout>
          }
        />

        {/* Protected Routes for DOCENTE only (CA-300 / R-311) */}
        <Route element={<RequireRole allowedRoles={['DOCENTE']} fallbackPath="/campo" />}>
          <Route
            path="/estudios"
            element={
              <RoutedAppLayout>
                <PlaceholderPage
                  title="Configuración del Estudio"
                  description="Planificación de los 8 días de muestreo y asignación de cuadrillas."
                />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/admin/*"
            element={
              <RoutedAppLayout>
                <PlaceholderPage
                  title="Módulo de Administración"
                  description="Gestión institucional, fuentes de generación, usuarios y cuadrillas."
                />
              </RoutedAppLayout>
            }
          />
        </Route>
      </Route>

      {/* Redirect root to /campo */}
      <Route path="/" element={<Navigate to="/campo" replace />} />
      <Route path="*" element={<Navigate to="/campo" replace />} />
    </Routes>
  )
}

export default function App({ initialUser }: { initialUser?: User | null } = {}) {
  return (
    <BrowserRouter>
      <AuthProvider initialUser={initialUser}>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
