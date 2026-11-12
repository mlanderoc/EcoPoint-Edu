/**
 * Login page view (T309 / R-310).
 *
 * @author craulgomez
 */

import { Card, CardContent } from '@/components/ui'
import { User } from '@/types'
import { LoginForm } from '../components/LoginForm'

export interface LoginPageProps {
  onSuccess?: (user: User) => void
}

export function LoginPage({ onSuccess }: LoginPageProps) {
  return (
    <div className="min-h-screen bg-[var(--color-bg-app)] flex flex-col justify-center items-center p-4 select-none">
      <div className="w-full max-w-md flex flex-col items-center">
        {/* Institutional Branding */}
        <div className="flex flex-col items-center text-center mb-6">
          <img
            src="/logo_ecopoint.jpeg"
            alt="EcoPoint Edu Logo"
            className="w-20 h-20 object-contain rounded-2xl shadow-md border-2 border-white mb-3"
          />
          <h1 className="text-2xl md:text-3xl font-black text-[var(--color-brand-dark)] tracking-tight">
            EcoPoint Edu
          </h1>
          <p className="text-sm font-medium text-[var(--color-text-muted)] mt-1">
            Caracterización Participativa de Residuos Sólidos
          </p>
          <span className="inline-flex items-center text-xs font-semibold px-2.5 py-0.5 mt-2 rounded-full bg-[var(--color-primary-light)] text-[var(--color-primary)]">
            I.E. Prudencia Daza — UPC
          </span>
        </div>

        {/* Login Card */}
        <Card className="w-full shadow-lg border border-slate-200">
          <CardContent className="pt-6">
            <LoginForm onSuccess={onSuccess} />
          </CardContent>
        </Card>

        {/* Academic / Institutional Attribution */}
        <footer className="mt-8 text-center text-xs text-[var(--color-text-muted)]">
          <p>Universidad Popular del Cesar — Programación de Computadores III</p>
          <p className="mt-1">Protocolo de muestreo participativo de 8 días</p>
        </footer>
      </div>
    </div>
  )
}
