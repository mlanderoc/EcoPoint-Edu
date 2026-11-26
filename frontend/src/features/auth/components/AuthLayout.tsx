/**
 * Shared layout of the public access screens: login and password recovery
 * (D15; spec D18). Campus photo on one side and the card with Rokko on the other.
 *
 * @author craulgomez
 */

import { ChevronLeft } from 'lucide-react'
import { Link } from 'react-router-dom'

export interface AuthLayoutProps {
  title: string
  subtitle: string
  backTo: string
  backLabel: string
  children: React.ReactNode
}

export function AuthLayout({ title, subtitle, backTo, backLabel, children }: AuthLayoutProps) {
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
          <Link className="lg-back" to={backTo}>
            <ChevronLeft className="w-[18px] h-[18px]" aria-hidden="true" /> {backLabel}
          </Link>
          <div className="lg-center">
            <div className="lg-area">
              <div className="lg-peek" aria-hidden="true"><img src="/brand/eco-saluda.webp" alt="" /></div>
              <div className="lg-card">
                <div className="lg-head">
                  <h1>{title}</h1>
                  <p>{subtitle}</p>
                </div>
                {children}
              </div>
            </div>
          </div>
        </section>
      </main>
    </>
  )
}
