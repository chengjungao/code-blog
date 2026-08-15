<template>
  <div class="categories-page">
    <header class="page-header">
      <span class="page-kicker">Topics</span>
      <h1 class="page-title">笔记分类</h1>
      <p class="page-lead">按主题浏览技术笔记，也可以通过标签追踪具体知识点。</p>
    </header>

    <div class="categories-grid" v-if="categories?.length">
      <router-link v-for="cat in categories" :key="cat.categoryId" :to="'/category/' + cat.categoryName + '/1'" class="cat-card">
        <div class="cat-icon-wrap">
          <img v-if="cat.categoryIcon" :src="cat.categoryIcon" class="cat-icon" />
          <span v-else class="cat-icon-placeholder">{{ cat.categoryName.slice(0, 1) }}</span>
        </div>
        <h3>{{ cat.categoryName }}</h3>
      </router-link>
    </div>
    <div v-else class="empty-state">暂无分类</div>

    <div class="tag-section" v-if="hotTags?.length">
      <h3>标签云</h3>
      <div class="tag-cloud">
        <router-link v-for="t in hotTags" :key="t.tagName" :to="'/tag/' + t.tagName + '/1'" class="tag-item">
          {{ t.tagName }} ({{ t.tagCount }})
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchCategories } from '../api/blog'
import { setPageMeta } from '../utils/seo'

const categories = ref([])
const hotTags = ref([])

onMounted(async () => {
  setPageMeta({
    title: '笔记分类',
    description: '按主题浏览程军高的技术笔记，也可通过标签追踪具体知识点。'
  })
  try {
    const res = await fetchCategories()
    const d = res.data || {}
    categories.value = d.categories || []
    hotTags.value = d.hotTags || []
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  margin-top: 42px;
  margin-bottom: 40px;
}
.cat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 24px 16px;
  text-align: center;
  transition: transform 0.25s, box-shadow 0.25s;
}
.cat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  color: var(--color-text);
}
.cat-icon-wrap { margin-bottom: 12px; }
.cat-icon { width: 40px; height: 40px; border-radius: 8px; }
.cat-icon-placeholder {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--color-surface-strong);
  color: var(--color-text);
  font-size: 20px;
  font-weight: 850;
}
.cat-card h3 { font-size: 14px; font-weight: 600; color: var(--color-text); margin: 0; }

.tag-section { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius); padding: 24px; }
.tag-section h3 { font-size: 18px; margin-bottom: 16px; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-item {
  padding: 6px 16px;
  background: var(--color-surface-strong);
  border-radius: 20px;
  font-size: 13px;
  color: var(--color-text-secondary);
  transition: all 0.2s;
}
.tag-item:hover { background: var(--color-accent-soft); color: var(--color-primary); }

.empty-state { text-align: center; padding: 60px; color: var(--color-text-secondary); }
</style>
