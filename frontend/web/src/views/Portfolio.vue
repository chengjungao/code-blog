<template>
  <div class="portfolio-page">
    <header class="page-header">
      <span class="page-kicker">Portfolio</span>
      <h1 class="page-title">作品集</h1>
      <p class="page-lead">
        从电商搜索平台到 AI 搜索系统，以下是我近年主导和参与的核心项目，覆盖搜索架构、AI 应用、数据服务平台和广告系统。
      </p>
    </header>

    <section class="portfolio-grid section">
      <article class="portfolio-card" v-for="item in portfolioItems" :key="item.title">
        <div class="portfolio-head">
          <span>{{ item.type }}</span>
          <small>{{ item.status }}</small>
        </div>
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <ul class="pill-list">
          <li v-for="tag in item.tags" :key="tag">{{ tag }}</li>
        </ul>
        <div class="highlight-list">
          <strong>可展示内容</strong>
          <ul>
            <li v-for="highlight in item.highlights" :key="highlight">{{ highlight }}</li>
          </ul>
        </div>
        <a v-if="item.link" :href="item.link" target="_blank" rel="noopener" class="portfolio-link">查看项目 →</a>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { portfolioItems } from '../content/profile'
import { setPageMeta } from '../utils/seo'

onMounted(() => {
  setPageMeta({
    title: '作品集',
    description: '从电商搜索平台到 AI 搜索系统，程军高近年主导和参与的核心项目，覆盖搜索架构、AI 应用、数据服务平台和广告系统。'
  })
})
</script>

<style scoped>
.portfolio-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.portfolio-card {
  display: flex;
  flex-direction: column;
  min-height: 360px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
}

.portfolio-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 850;
}

.portfolio-head small {
  color: var(--color-warm);
}

.portfolio-card h2 {
  margin-top: 18px;
  color: var(--color-text);
  font-size: 24px;
  line-height: 1.25;
}

.portfolio-card p {
  margin-top: 12px;
  color: var(--color-subtle);
  font-size: 15px;
}

.portfolio-card .pill-list {
  margin-top: 18px;
}

.highlight-list {
  margin-top: auto;
  padding-top: 22px;
}

.highlight-list strong {
  color: var(--color-text);
  font-size: 13px;
}

.highlight-list ul {
  margin-top: 10px;
  padding-left: 18px;
  color: var(--color-subtle);
  font-size: 14px;
}

.highlight-list li + li {
  margin-top: 6px;
}

.portfolio-link {
  display: inline-block;
  margin-top: 16px;
  color: var(--color-accent);
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  transition: opacity 0.2s;
}

.portfolio-link:hover {
  opacity: 0.75;
}

@media (max-width: 1200px) {
  .portfolio-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .portfolio-grid {
    grid-template-columns: 1fr;
  }
}
</style>
