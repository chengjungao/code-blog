<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-col" v-if="config.footerAbout">
        <h4>📌 关于</h4>
        <p>{{ config.footerAbout }}</p>
      </div>
      <div class="footer-col" v-if="config.footerICP">
        <h4>📄 备案</h4>
        <p>{{ config.footerICP }}</p>
      </div>
      <div class="footer-col" v-if="config.footerCopyRight">
        <h4>©️ 版权</h4>
        <p>{{ config.footerCopyRight }}</p>
      </div>
      <div class="footer-col" v-if="config.footerPoweredBy">
        <h4>⚡ Powered By</h4>
        <p>
          <a v-if="config.footerPoweredByURL" :href="config.footerPoweredByURL" target="_blank">
            {{ config.footerPoweredBy }}
          </a>
          <span v-else>{{ config.footerPoweredBy }}</span>
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup>
defineProps({ config: { type: Object, default: () => ({}) } })
</script>

<style scoped>
.site-footer {
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  border-top: 1px solid #d7e8d4;
  margin-top: 48px;
}
.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
}
.footer-col h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 8px;
}
.footer-col p {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>
