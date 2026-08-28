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
      <div class="footer-qr">
        <div class="qr-item">
          <img src="/qrcode-gh.jpg" alt="微信公众号" />
          <span>公众号</span>
        </div>
        <div class="qr-item">
          <img src="/qrcode-mp.png" alt="小程序" />
          <span>小程序</span>
        </div>
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
  grid-template-columns: 1.2fr auto auto 1fr;
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

.footer-qr {
  display: flex;
  gap: 14px;
}

.qr-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.qr-item img {
  width: 84px;
  height: 84px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  padding: 4px;
  object-fit: contain;
}

.qr-item span {
  font-size: 12px;
  color: var(--color-muted);
}

.footer-meta {
  text-align: right;
}

@media (max-width: 1200px) {
  .footer-inner {
    grid-template-columns: 1fr auto 1fr;
  }
  .footer-qr {
    grid-column: 1 / -1;
    justify-content: center;
    order: 4;
    padding-top: 8px;
    border-top: 1px dashed var(--color-border);
  }
}

@media (max-width: 760px) {
  .footer-inner {
    grid-template-columns: 1fr;
  }

  .footer-qr {
    grid-column: 1;
    justify-content: center;
    order: 0;
    border-top: none;
    padding-top: 0;
  }

  .footer-meta {
    text-align: left;
  }
}
</style>
