<template>
  <div class="links-page">
    <div class="links-section" v-if="links[0]?.length">
      <h3>🤝 友链</h3>
      <div class="links-grid">
        <a v-for="link in links[0]" :key="link.linkId" :href="link.linkUrl" target="_blank" class="link-card">
          <div class="link-name">{{ link.linkName }}</div>
          <div class="link-desc">{{ link.linkDescription }}</div>
        </a>
      </div>
    </div>

    <div class="links-section" v-if="links[1]?.length">
      <h3>⭐ 推荐网站</h3>
      <div class="links-grid">
        <a v-for="link in links[1]" :key="link.linkId" :href="link.linkUrl" target="_blank" class="link-card">
          <div class="link-name">{{ link.linkName }}</div>
          <div class="link-desc">{{ link.linkDescription }}</div>
        </a>
      </div>
    </div>

    <div class="links-section" v-if="links[2]?.length">
      <h3>🌐 个人链接</h3>
      <div class="links-grid">
        <a v-for="link in links[2]" :key="link.linkId" :href="link.linkUrl" target="_blank" class="link-card">
          <div class="link-name">{{ link.linkName }}</div>
          <div class="link-desc">{{ link.linkDescription }}</div>
        </a>
      </div>
    </div>

    <div v-if="!hasLinks" class="empty-state">暂无友情链接</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchLinks } from '../api/blog'

const links = ref({})

const hasLinks = computed(() => {
  return (links.value[0]?.length) || (links.value[1]?.length) || (links.value[2]?.length)
})

onMounted(async () => {
  try {
    const res = await fetchLinks()
    links.value = res.data || {}
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.links-page { padding-top: 12px; }
.links-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 24px;
  margin-bottom: 20px;
}
.links-section h3 {
  font-size: 18px;
  margin-bottom: 16px;
  color: var(--color-text);
}
.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}
.link-card {
  display: block;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  transition: all 0.2s;
}
.link-card:hover {
  border-color: var(--color-primary-light);
  box-shadow: var(--shadow-sm);
  color: var(--color-text);
}
.link-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}
.link-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.empty-state { text-align: center; padding: 60px; color: var(--color-text-secondary); }
</style>
