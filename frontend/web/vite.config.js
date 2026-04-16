import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/blog/api': {
        target: 'http://localhost:28083',
        changeOrigin: true
      },
      '/common': {
        target: 'http://localhost:28083',
        changeOrigin: true
      },
      '/admin/upload': {
        target: 'http://localhost:28083',
        changeOrigin: true
      }
    }
  }
})
