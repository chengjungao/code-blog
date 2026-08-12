<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div>
        <strong>{{ config.websiteName || '拾光集' }}</strong>
        <p>{{ config.footerAbout || '程军高的个人空间。写搜索架构和 AI，也写做菜和读书。' }}</p>
      </div>
      <div class="footer-links">
        <router-link to="/portfolio">作品集</router-link>
        <router-link to="/notes">技术笔记</router-link>
        <router-link to="/life">生活杂记</router-link>
        <router-link to="/message">留言板</router-link>
        <a href="https://github.com/chengjungao" target="_blank" rel="noreferrer">GitHub</a>
      </div>
      <div class="footer-meta">
        <p v-if="config.footerICP">{{ config.footerICP }}</p>
        <p>{{ config.footerCopyRight || '© 拾光集 · 程军高' }}</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
defineProps({ config: { type: Object, default: () => ({}) } })
</script>

<style scoped>
.site-footer {
  margin-top: 72px;
  border-top: 1px solid var(--color-border);
  background: #f5f1e8;
}

.footer-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 34px 24px;
  display: grid;
  grid-template-columns: 1.2fr auto 1fr;
  gap: 28px;
  align-items: start;
}

.footer-inner strong {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text);
  font-size: 16px;
}

.footer-inner p {
  margin: 0;
  color: var(--color-subtle);
  font-size: 13px;
  line-height: 1.7;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
}

.footer-links a {
  color: var(--color-subtle);
  font-size: 13px;
  font-weight: 650;
}

.footer-links a:hover {
  color: var(--color-text);
}

.footer-meta {
  text-align: right;
}

@media (max-width: 760px) {
  .footer-inner {
    grid-template-columns: 1fr;
  }

  .footer-meta {
    text-align: left;
  }
}
</style>
