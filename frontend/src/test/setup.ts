import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { testConfig } from './config'
import { clearCookies, server } from './server'

// SERVER_SLEEPS is fixed at build time, so tests switch it through testConfig instead.
vi.mock('../config', async (importOriginal) => {
  const { testConfig } = await import('./config')
  return {
    ...(await importOriginal<typeof import('../config')>()),
    get SERVER_SLEEPS() {
      return testConfig.serverSleeps
    },
  }
})

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
  clearCookies()
  testConfig.serverSleeps = false
})
afterAll(() => server.close())
