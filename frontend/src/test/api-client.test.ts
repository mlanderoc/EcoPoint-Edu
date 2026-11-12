/**
 * Unit tests for base HTTP apiClient (T308 / R-303).
 *
 * @author craulgomez
 */

import { describe, it, expect, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '@/mocks/server'
import { apiClient, ApiError, onUnauthorized } from '@/services/apiClient'

describe('HTTP apiClient (T308, R-303)', () => {
  it('performs successful GET request returning parsed data', async () => {
    const result = await apiClient.get<Array<{ id: number; name: string }>>('/api/v1/institutions')
    expect(Array.isArray(result)).toBe(true)
    expect(result[0].name).toContain('I.E. Prudencia Daza')
  })

  it('performs successful POST request with credentials and body', async () => {
    server.use(
      http.post('/api/v1/test-echo', async ({ request }) => {
        const cookies = request.headers.get('cookie')
        const body = (await request.json()) as { test: string }
        return HttpResponse.json({ received: body, cookies })
      })
    )

    const res = await apiClient.post<{ received: { test: string } }>('/api/v1/test-echo', {
      test: 'valor',
    })
    expect(res.received.test).toBe('valor')
  })

  it('normalizes 400 error response with fieldErrors list in Spanish', async () => {
    server.use(
      http.post('/api/v1/test-validation', () => {
        return HttpResponse.json(
          {
            timestamp: '2026-03-02T10:00:00Z',
            status: 400,
            errorCode: 'ERR_VALIDATION',
            message: 'Datos de entrada inválidos.',
            path: '/api/v1/test-validation',
            fieldErrors: [
              { field: 'email', message: 'El correo institucional ya se encuentra registrado.' },
              { field: 'name', message: 'El nombre es obligatorio.' },
            ],
          },
          { status: 400 }
        )
      })
    )

    try {
      await apiClient.post('/api/v1/test-validation', {})
      expect.unreachable('Should have thrown an ApiError')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      const apiErr = err as ApiError
      expect(apiErr.status).toBe(400)
      expect(apiErr.errorCode).toBe('ERR_VALIDATION')
      expect(apiErr.message).toBe('Datos de entrada inválidos.')
      expect(apiErr.fieldErrors).toHaveLength(2)
      expect(apiErr.getFieldError('email')).toBe(
        'El correo institucional ya se encuentra registrado.'
      )
      expect(apiErr.getFieldError('name')).toBe('El nombre es obligatorio.')
      expect(apiErr.getFieldError('nonexistent')).toBeUndefined()
    }
  })

  it('normalizes 401 response and notifies onUnauthorized listeners', async () => {
    const unauthorizedSpy = vi.fn()
    const unsubscribe = onUnauthorized(unauthorizedSpy)

    server.use(
      http.get('/api/v1/test-auth-expired', () => {
        return HttpResponse.json(
          {
            timestamp: '2026-03-02T10:00:00Z',
            status: 401,
            errorCode: 'ERR_UNAUTHORIZED',
            message: 'Su sesión expiró o no ha iniciado sesión.',
            path: '/api/v1/test-auth-expired',
            fieldErrors: [],
          },
          { status: 401 }
        )
      })
    )

    try {
      await apiClient.get('/api/v1/test-auth-expired')
      expect.unreachable('Should have thrown an ApiError')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      const apiErr = err as ApiError
      expect(apiErr.status).toBe(401)
      expect(apiErr.message).toBe('Su sesión expiró o no ha iniciado sesión.')
      expect(unauthorizedSpy).toHaveBeenCalledTimes(1)
      expect(unauthorizedSpy).toHaveBeenCalledWith(apiErr)
    } finally {
      unsubscribe()
    }

    // After unsubscribe, listener should not be called again
    try {
      await apiClient.get('/api/v1/test-auth-expired')
    } catch {
      // Ignored
    }
    expect(unauthorizedSpy).toHaveBeenCalledTimes(1)
  })

  it('normalizes 422 business rule errors correctly', async () => {
    server.use(
      http.post('/api/v1/test-business-rule', () => {
        return HttpResponse.json(
          {
            timestamp: '2026-03-02T10:00:00Z',
            status: 422,
            errorCode: 'ERR_BUSINESS_RULE',
            message: 'No se puede cerrar el turno: existen fuentes pendientes de registro.',
            path: '/api/v1/test-business-rule',
            fieldErrors: [],
          },
          { status: 422 }
        )
      })
    )

    try {
      await apiClient.post('/api/v1/test-business-rule', {})
      expect.unreachable('Should have thrown an ApiError')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      const apiErr = err as ApiError
      expect(apiErr.status).toBe(422)
      expect(apiErr.message).toBe(
        'No se puede cerrar el turno: existen fuentes pendientes de registro.'
      )
    }
  })

  it('appends query parameters to GET URL properly', async () => {
    server.use(
      http.get('/api/v1/test-query', ({ request }) => {
        const url = new URL(request.url)
        return HttpResponse.json({
          filter: url.searchParams.get('filter'),
          page: url.searchParams.get('page'),
        })
      })
    )

    const result = await apiClient.get<{ filter: string; page: string }>('/api/v1/test-query', {
      params: { filter: 'active', page: 2 },
    })

    expect(result.filter).toBe('active')
    expect(result.page).toBe('2')
  })
})
