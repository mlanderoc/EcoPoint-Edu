import { describe, it, expect } from 'vitest'
import { resetMockState } from '@/mocks/handlers'

describe('Mock Service Worker API Contracts (T304)', () => {
  it('intercepts GET /api/v1/auth/me and returns active user', async () => {
    const res = await fetch('/api/v1/auth/me', { credentials: 'include' })
    expect(res.status).toBe(200)
    const data = await res.json()
    expect(data.email).toBe('docente.demo@unicesar.edu.co')
    expect(data.role).toBe('DOCENTE')
  })

  it('intercepts GET /api/v1/institutions and returns I.E. Prudencia Daza', async () => {
    const res = await fetch('/api/v1/institutions')
    expect(res.status).toBe(200)
    const data = await res.json()
    expect(data).toHaveLength(1)
    expect(data[0].name).toBe('I.E. Prudencia Daza')
  })

  it('rejects a duplicate email with 422 like the real API (RN-084, CA-302 contract)', async () => {
    const res = await fetch('/api/v1/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        firstName: 'Carlos',
        lastName: 'Gómez',
        email: 'docente.demo@unicesar.edu.co', // Already exists
        role: 'DOCENTE',
        password: 'ClaveSegura2026',
      }),
    })
    expect(res.status).toBe(422)
    const data = await res.json()
    expect(data.errorCode).toBe('ERR_BUSINESS_RULE')
    expect(data.message).toContain('ya está registrado')
  })

  it('rejects tare >= gross weight with 400 and tare fieldError (CA-305 contract)', async () => {
    const res = await fetch('/api/v1/shifts/14/bags', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sourceId: 1,
        grossWeight: 5.0,
        tare: 6.0, // tare > gross
        volume: 0.05,
      }),
    })
    expect(res.status).toBe(400)
    const data = await res.json()
    expect(data.errorCode).toBe('ERR_INVALID_DATA')
    expect(data.fieldErrors?.[0].field).toBe('tare')
  })

  it('calculates label AU-D3-M on valid bag entry (CA-304 contract)', async () => {
    const res = await fetch('/api/v1/shifts/14/bags', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sourceId: 1, // AU
        grossWeight: 12.4,
        tare: 0.4,
        volume: 0.08,
      }),
    })
    expect(res.status).toBe(201)
    const data = await res.json()
    expect(data.label).toBe('AU-D3-M')
    expect(data.netWeight).toBe(12.0)
    expect(data.density).toBe(150.0)
  })

  it('rejects closing a study with pending verifications (CA-309 contract)', async () => {
    resetMockState('DOCENTE')
    const toCleaning = await fetch('/api/v1/studies/2/status', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'EN_DEPURACION' }),
    })
    expect(toCleaning.status).toBe(200)

    const res = await fetch('/api/v1/studies/2/status', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'CERRADO' }),
    })
    expect(res.status).toBe(422)
    const data = await res.json()
    expect(data.errorCode).toBe('ERR_BUSINESS_RULE')
    expect(data.message).toContain('quedan 2 registros pendientes de verificación')
  })
})
