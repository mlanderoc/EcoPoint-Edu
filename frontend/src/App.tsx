/**
 * Root Application Component with React Router and AuthProvider (T307 / T309 / T310).
 * Configures role-based route guards (RequireAuth, RequireRole) and responsive layout.
 *
 * @author craulgomez
 */

import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AuthProvider, RequireAuth, RequireRole, LoginPage, useAuth } from '@/features/auth'
import { AppLayout } from '@/components/layout'
import { FieldDashboard, NewBagPage, PopulationPage, ShiftPage } from '@/features/field'
import { InstitutionsPage, SourcesPage, UsersPage, CrewsPage, CategoriesPage } from '@/features/admin'
import { NewStudyPage, StudiesPage, StudyDetailPage } from '@/features/study'
import { IndicatorsDashboard } from '@/features/indicators'
import { AlertsPage, DensityPage, QuarteringPage, RecordPage, VerificationPage } from '@/features/quality'
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
          path="/campo/turno/:id"
          element={
            <RoutedAppLayout>
              <ShiftPage />
            </RoutedAppLayout>
          }
        />
        <Route
          path="/campo/turno/:id/poblacion"
          element={
            <RoutedAppLayout>
              <PopulationPage />
            </RoutedAppLayout>
          }
        />
        <Route
          path="/campo/turno/:id/bolsas/nueva"
          element={
            <RoutedAppLayout>
              <NewBagPage />
            </RoutedAppLayout>
          }
        />

        <Route
          path="/campo/turno/:id/cuarteo"
          element={
            <RoutedAppLayout>
              <QuarteringPage />
            </RoutedAppLayout>
          }
        />
        <Route
          path="/campo/turno/:id/densidad"
          element={
            <RoutedAppLayout>
              <DensityPage />
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
          path="/indicadores"
          element={
            <RoutedAppLayout>
              <IndicatorsDashboard />
            </RoutedAppLayout>
          }
        />
        <Route
          path="/alertas"
          element={
            <RoutedAppLayout>
              <AlertsPage />
            </RoutedAppLayout>
          }
        />
        <Route
          path="/calidad/verificacion"
          element={
            <RoutedAppLayout>
              <VerificationPage />
            </RoutedAppLayout>
          }
        />
        <Route
          path="/calidad/registro/:tipo/:id"
          element={
            <RoutedAppLayout>
              <RecordPage />
            </RoutedAppLayout>
          }
        />

        {/* Protected Routes for DOCENTE only (CA-300 / R-311) */}
        <Route element={<RequireRole allowedRoles={['DOCENTE']} fallbackPath="/campo" />}>
          <Route
            path="/estudios"
            element={
              <RoutedAppLayout>
                <StudiesPage />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/estudios/nuevo"
            element={
              <RoutedAppLayout>
                <NewStudyPage />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/estudios/:id"
            element={
              <RoutedAppLayout>
                <StudyDetailPage />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/admin/instituciones"
            element={
              <RoutedAppLayout>
                <InstitutionsPage />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/admin/fuentes"
            element={
              <RoutedAppLayout>
                <SourcesPage />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/admin/usuarios"
            element={
              <RoutedAppLayout>
                <UsersPage />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/admin/cuadrillas"
            element={
              <RoutedAppLayout>
                <CrewsPage />
              </RoutedAppLayout>
            }
          />
          <Route
            path="/admin/subcategorias"
            element={
              <RoutedAppLayout>
                <CategoriesPage />
              </RoutedAppLayout>
            }
          />
          <Route path="/admin" element={<Navigate to="/admin/instituciones" replace />} />
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
