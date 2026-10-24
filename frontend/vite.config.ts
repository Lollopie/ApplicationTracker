import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// The path the app is served under: '/' locally and in Compose, '/tracker/' behind the Render gateway.
const base = (process.env.VITE_BASE ?? '/').replace(/\/?$/, '/')
const apiPrefix = `${base}api`

// https://vite.dev/config/
export default defineConfig({
  base,
  plugins: [react(), tailwindcss()],
  server: {
    // The SPA talks to Spring through <base>api so its routes never clash with API paths
    // and cookies stay same-origin. nginx and the Render gateway do the same in production.
    proxy: {
      [apiPrefix]: {
        target: 'http://localhost:8080',
        rewrite: (path) => path.slice(apiPrefix.length) || '/',
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
