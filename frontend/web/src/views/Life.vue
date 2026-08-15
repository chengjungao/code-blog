<template>
  <div class="life-page">
    <header class="page-header">
      <span class="page-kicker">Life Notes</span>
      <h1 class="page-title">生活杂记</h1>
      <p class="page-lead">
        这里记录做菜、读书和日常观察。它会让网站不只是一份技术档案，也保留真实生活里的判断、趣味和节奏。
      </p>
    </header>

    <section class="section life-layout">
      <div class="life-column" v-for="col in columns" :key="col.name">
        <div class="section-head">
          <div>
            <span class="page-kicker">{{ col.kicker }}</span>
            <h2>{{ col.title }}</h2>
          </div>
        </div>

        <template v-if="col.blogs.length">
          <router-link
            v-for="blog in col.blogs"
            :key="blog.blogId"
            :to="'/notes/' + blog.blogId"
            class="life-card"
            :class="col.type"
          >
            <span>{{ blog.createTime || '' }}</span>
            <h3>{{ blog.blogTitle }}</h3>
          </router-link>
        </template>
        <div v-else class="life-empty">暂无{{ col.title }}</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchCategoryBlogs } from '../api/blog'

// 生活杂记对应的分类（与后台 tb_blog_category 中的分类名一致）
const columns = ref([
  { name: '做菜笔记', kicker: 'Cooking', title: '做菜分享', type: 'cooking', blogs: [] },
  { name: '读书心得', kicker: 'Reading', title: '读书笔记', type: 'reading', blogs: [] }
])

onMounted(async () => {
  await Promise.all(columns.value.map(async (col) => {
    try {
      const res = await fetchCategoryBlogs(col.name, 1)
      const d = res.data || {}
      const bp = d.blogPage || {}
      col.blogs = bp.list || []
    } catch (e) {
      console.error('加载' + col.title + '失败', e)
      col.blogs = []
    }
  }))
})
</script>

<style scoped>
.life-layout {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 28px;
}

.life-column {
  display: grid;
  gap: 16px;
  align-content: start;
}

.life-column .section-head {
  margin-bottom: 2px;
}

.life-card {
  display: block;
  min-height: 110px;
  padding: 22px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.life-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.life-card.cooking {
  border-top: 4px solid var(--color-warm);
}

.life-card.reading {
  border-top: 4px solid var(--color-accent);
}

.life-card span {
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
}

.life-card h3 {
  margin-top: 12px;
  color: var(--color-text);
  font-size: 21px;
  line-height: 1.4;
}

.life-empty {
  padding: 32px;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  color: var(--color-muted);
  text-align: center;
  background: var(--color-surface);
}

@media (max-width: 760px) {
  .life-layout {
    grid-template-columns: 1fr;
  }
}
</style>
