import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: '/admin/',
  server: {
    port: 3000,
    proxy: {
      '/admin/api': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/admin': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/common': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/upload': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': '/src',
    },
  },
})
