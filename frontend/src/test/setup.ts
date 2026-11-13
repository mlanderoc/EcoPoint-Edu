import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from '@/mocks/server'
import { resetMockState } from '@/mocks/handlers'

// The suite runs files in parallel; typing in jsdom is slow under that load,
// so asynchronous queries wait up to 5 seconds instead of 1.
configure({ asyncUtilTimeout: 5000 })

// jsdom has no ResizeObserver; the Recharts responsive container needs one.
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
}

beforeAll(() => server.listen())

afterEach(() => {
  server.resetHandlers()
  resetMockState()
  cleanup()
})

afterAll(() => server.close())
