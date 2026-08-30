<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面不存在</h2>
      <p class="error-message">
        抱歉，您访问的页面不存在或已被移除。
      </p>
      <div class="error-actions">
        <router-link to="/" class="btn-primary">返回首页</router-link>
        <router-link to="/notes" class="btn-secondary">浏览笔记</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { setPageMeta } from '../utils/seo'

onMounted(() => {
  setPageMeta({
    title: '404 - 页面不存在',
    description: '您访问的页面不存在。'
  })
})
</script>

<style scoped>
.not-found-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.not-found-content {
  text-align: center;
  max-width: 500px;
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  color: var(--color-primary);
  line-height: 1;
  margin: 0;
  letter-spacing: -4px;
}

.error-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin: 16px 0 12px;
}

.error-message {
  font-size: 16px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 32px;
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-primary,
.btn-secondary {
  padding: 12px 28px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-primary:hover {
  background: var(--color-accent);
  transform: translateY(-1px);
}

.btn-secondary {
  background: var(--color-surface-strong);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-accent-soft);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

@media (max-width: 640px) {
  .error-code {
    font-size: 80px;
  }
  .error-title {
    font-size: 22px;
  }
  .error-message {
    font-size: 14px;
  }
}
</style>
