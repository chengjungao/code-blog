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
      '/admin/blogs': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/admin/categories': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/admin/tags': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/admin/comments': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/admin/links': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/admin/configurations': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/admin/upload': {
        target: 'http://localhost:28083',
        changeOrigin: true,
      },
      '/common': {
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
