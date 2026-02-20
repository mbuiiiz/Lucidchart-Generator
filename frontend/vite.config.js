import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // this sends the production build to Spring Boot's static resources
    outDir: '..aetherxmlbridge/src/main/resources/static',
    emptyOutDir: true, // cleans the folder before each build
  },
  server: {
    proxy: {
      // this redirects API calls to Spring Boot during development
      '/api': 'http://localhost:8080'
    }
  }
})
