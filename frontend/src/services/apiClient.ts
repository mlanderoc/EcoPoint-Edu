/**
 * Base HTTP client configured with credentials, the CSRF token of Spec 003 (decision T3)
 * and Spec 003 error normalization.
 *
 * @author craulgomez
 */

import { ApiErrorResponse, FieldError } from '@/types/api'

/**
 * Normalized API error conforming to Spec 003 standard error responses.
 */
export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string
  readonly path: string
  readonly fieldErrors: FieldError[]

  constructor(errorResponse: ApiErrorResponse) {
    super(errorResponse.message || 'Error en la petición al servidor.')
    this.name = 'ApiError'
    this.status = errorResponse.status
    this.errorCode = errorResponse.errorCode || `ERR_HTTP_${errorResponse.status}`
    this.path = errorResponse.path || ''
    this.fieldErrors = errorResponse.fieldErrors || []
  }

  /**
   * Retrieves the specific error message for a form field if present.
   */
  getFieldError(fieldName: string): string | undefined {
    return this.fieldErrors.find((fe) => fe.field === fieldName)?.message
  }
}

export type UnauthorizedListener = (error: ApiError) => void

const unauthorizedListeners = new Set<UnauthorizedListener>()

/**
 * Subscribes to HTTP 401 unauthorized / session expiration events.
 * Returns an unsubscribe function.
 */
export function onUnauthorized(listener: UnauthorizedListener): () => void {
  unauthorizedListeners.add(listener)
  return () => {
    unauthorizedListeners.delete(listener)
  }
}

/**
 * Dispatches notification to all registered unauthorized listeners.
 */
export function notifyUnauthorized(error: ApiError): void {
  unauthorizedListeners.forEach((listener) => {
    try {
      listener(error)
    } catch (err) {
      console.error('Error executing unauthorized listener:', err)
    }
  })
}

/** Cookie where the backend leaves the CSRF token (spec 003, decision T3). */
export const CSRF_COOKIE = 'XSRF-TOKEN'
/** Header where every state-changing request sends the CSRF token back. */
export const CSRF_HEADER = 'X-XSRF-TOKEN'
const CSRF_URL = '/api/v1/auth/csrf'
const STATE_CHANGING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

let pendingCsrfRequest: Promise<void> | null = null

/**
 * Reads a cookie visible to JavaScript. The session cookie is HttpOnly and is
 * never visible here; only the CSRF token is.
 */
export function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  const found = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix))
  return found ? decodeURIComponent(found.substring(prefix.length)) : undefined
}

/**
 * Returns the CSRF token, asking the backend for it the first time
 * (GET /api/v1/auth/csrf leaves it in the XSRF-TOKEN cookie).
 */
export async function ensureCsrfToken(): Promise<string | undefined> {
  const current = readCookie(CSRF_COOKIE)
  if (current) {
    return current
  }
  pendingCsrfRequest ??= fetch(CSRF_URL, { credentials: 'include' })
    .then(() => undefined)
    .catch(() => undefined)
    .finally(() => {
      pendingCsrfRequest = null
    })
  await pendingCsrfRequest
  return readCookie(CSRF_COOKIE)
}

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  params?: Record<string, string | number | boolean | undefined | null>
  body?: unknown
}

/**
 * Performs an HTTP request with HttpOnly session credentials and Spec 003 error handling.
 */
async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { params, headers, body, ...restOptions } = options

  let fullUrl = url
  if (params) {
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.append(key, String(value))
      }
    })
    const queryString = searchParams.toString()
    if (queryString) {
      fullUrl += (url.includes('?') ? '&' : '?') + queryString
    }
  }

  const isJsonBody = body !== undefined && !(body instanceof FormData)
  const method = (restOptions.method ?? 'GET').toUpperCase()
  const csrfToken = STATE_CHANGING_METHODS.has(method) ? await ensureCsrfToken() : undefined
  const defaultHeaders: HeadersInit = {
    Accept: 'application/json',
    ...(isJsonBody ? { 'Content-Type': 'application/json' } : {}),
    ...(csrfToken ? { [CSRF_HEADER]: csrfToken } : {}),
  }

  const response = await fetch(fullUrl, {
    credentials: 'include', // R-303: HttpOnly session cookie without localStorage tokens
    headers: {
      ...defaultHeaders,
      ...headers,
    },
    body: isJsonBody ? JSON.stringify(body) : (body as BodyInit | undefined),
    ...restOptions,
  })

  if (!response.ok) {
    let errorData: ApiErrorResponse
    try {
      errorData = (await response.json()) as ApiErrorResponse
    } catch {
      errorData = {
        timestamp: new Date().toISOString(),
        status: response.status,
        errorCode: `ERR_HTTP_${response.status}`,
        message: response.statusText || 'Error inesperado en la comunicación con el servidor.',
        path: url,
        fieldErrors: [],
      }
    }

    const apiError = new ApiError(errorData)

    if (response.status === 401) {
      notifyUnauthorized(apiError)
    }

    throw apiError
  }

  if (response.status === 204) {
    return undefined as unknown as T
  }

  return (await response.json()) as T
}

export const apiClient = {
  get: <T>(url: string, options?: Omit<RequestOptions, 'body'>) =>
    request<T>(url, { ...options, method: 'GET' }),

  post: <T>(url: string, body?: unknown, options?: Omit<RequestOptions, 'body'>) =>
    request<T>(url, { ...options, method: 'POST', body }),

  put: <T>(url: string, body?: unknown, options?: Omit<RequestOptions, 'body'>) =>
    request<T>(url, { ...options, method: 'PUT', body }),

  patch: <T>(url: string, body?: unknown, options?: Omit<RequestOptions, 'body'>) =>
    request<T>(url, { ...options, method: 'PATCH', body }),

  delete: <T>(url: string, options?: Omit<RequestOptions, 'body'>) =>
    request<T>(url, { ...options, method: 'DELETE' }),
}
