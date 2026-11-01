import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/settlement-reports': 'http://localhost:8085',
      '/health': 'http://localhost:8085',
      '/orders': 'http://localhost:8085',
      '/ready' : 'http://localhost:8085',
      // یا اگر همه اندپوینت‌ها با /api شروع می‌شوند:
      // '/api': 'http://localhost:8085'
    }
  }
})
