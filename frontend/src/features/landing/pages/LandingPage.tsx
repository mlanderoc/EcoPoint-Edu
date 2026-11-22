/**
 * Public welcome page shown before signing in (D15).
 *
 * @author craulgomez
 */

import { Link } from 'react-router-dom'

const LEAVES = ['f1', 'f2', 'f3', 'f4', 'f5', 'f6']

const STEPS = [
  { title: 'Registra', text: 'Durante 8 días se pesa cada bolsa por zona del campus.' },
  { title: 'Clasifica', text: 'Una muestra se separa en blanco, verde y negro.' },
  { title: 'Verifica', text: 'Otra persona revisa cada dato antes de usarlo.' },
  { title: 'Analiza', text: 'Indicadores, reportes y el plano del campus.' },
]

export function LandingPage() {
  return (
    <div className="ld">
      <header className="ld-top">
        <div className="ld-brand"><img src="/brand/logo.png" alt="" />EcoPoint Edu</div>
        <Link className="ld-btn ld-login" to="/login">Iniciar sesión</Link>
      </header>
      <div className="ld-body">
        <div className="ld-leaves" aria-hidden="true">
          {LEAVES.map((leaf) => (
            <svg key={leaf} className={`ld-leaf ${leaf}`} viewBox="0 0 24 24">
              <path d="M5 19c0-8 6-14 15-14 0 9-6 15-14 15l-1-1z" />
              <path d="M5 19l9-9" stroke="#f6f3ea" strokeWidth="1.4" fill="none" />
            </svg>
          ))}
        </div>
        <div className="ld-pad">
          <main className="ld-inner">
            <section className="ld-hero">
              <div>
                <h1>
                  La mejor gestión ambiental empieza por{' '}
                  <span className="ld-mark">
                    conocer
                    <svg viewBox="0 0 200 18" preserveAspectRatio="none" aria-hidden="true">
                      <path d="M3 12 C 50 3, 120 3, 197 9" />
                    </svg>
                  </span>{' '}
                  los residuos que generamos.
                </h1>
                <p className="ld-lead">
                  EcoPoint registra cuánto, de dónde y de qué tipo es tu basura para que decidas con datos reales.
                </p>
                <a className="ld-btn ld-ghost" href="#como-funciona">Cómo funciona</a>
              </div>
              <div className="ld-blob">
                <img src="/brand/eco-bienvenida.jpg" alt="Eco, el mapache mascota de EcoPoint Edu, saludando" />
              </div>
            </section>
            <section className="ld-steps" id="como-funciona">
              <h2>Así funciona</h2>
              <div className="ld-grid">
                {STEPS.map((step, index) => (
                  <article className="ld-card" key={step.title}>
                    <i>{index + 1}</i>
                    <h3>{step.title}</h3>
                    <p>{step.text}</p>
                  </article>
                ))}
              </div>
            </section>
            <footer className="ld-foot">
              <span>Universidad Popular del Cesar · Programación de Computadores III</span>
              <span>Res. 2184 de 2019</span>
            </footer>
          </main>
        </div>
      </div>
    </div>
  )
}
