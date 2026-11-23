/**
 * Login page view with the campus photo and Eco greeting (T309 / R-310; D15).
 *
 * @author craulgomez
 */

import { ChevronLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import { User } from '@/types'
import { LoginForm } from '../components/LoginForm'

export interface LoginPageProps {
  onSuccess?: (user: User) => void
}

export function LoginPage({ onSuccess }: LoginPageProps) {
  return (
    <>
      <svg width="0" height="0" style={{ position: 'absolute' }} aria-hidden="true">
        <clipPath id="lg-leaf-edge" clipPathUnits="objectBoundingBox">
          <path d="M0,0 H.79 C.93,.16 1,.34 .93,.54 C.86,.74 .69,.86 .6,1 H0 Z" />
        </clipPath>
        <clipPath id="lg-leaf-bottom" clipPathUnits="objectBoundingBox">
          <path d="M0,0 H1 V.8 C.8,.98 .56,.88 .36,.91 C.2,.94 .1,1 0,.96 Z" />
        </clipPath>
      </svg>
      <main className="lg">
        <section className="lg-scene-wrap" aria-label="EcoPoint Edu">
          <div className="lg-band" />
          <div className="lg-scene">
            <img src="/brand/upc-vista.jpg" alt="Plazoleta y edificios de la Universidad Popular del Cesar" />
          </div>
          <div className="lg-story">
            <div className="lg-identity">
              <span className="lg-mark"><img src="/brand/logo.png" alt="EcoPoint Edu Logo" /></span>
              <h2>EcoPoint Edu</h2>
              <p>Caracterización participativa de residuos sólidos</p>
            </div>
            <p className="lg-foot">Universidad Popular del Cesar</p>
          </div>
        </section>
        <section className="lg-access">
          <Link className="lg-back" to="/">
            <ChevronLeft className="w-[18px] h-[18px]" aria-hidden="true" /> Volver al inicio
          </Link>
          <div className="lg-center">
            <div className="lg-area">
              <div className="lg-peek" aria-hidden="true"><img src="/brand/eco-saluda.webp" alt="" /></div>
              <div className="lg-card">
                <div className="lg-head">
                  <h1>Iniciar sesión</h1>
                  <p>¡Te damos la bienvenida!</p>
                </div>
                <LoginForm onSuccess={onSuccess} />
              </div>
            </div>
          </div>
        </section>
      </main>
    </>
  )
}
