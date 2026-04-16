<template>
  <div class="blog-list-page">
    <div class="list-header">
      <h2>{{ listTitle }} <span v-if="keyword" class="keyword">: {{ keyword }}</span></h2>
    </div>

    <div class="blog-grid" v-if="blogs.length">
      <article class="blog-card" v-for="blog in blogs" :key="blog.blogId">
        <router-link :to="'/blog/' + blog.blogId" class="card-cover">
          <img v-if="blog.blogCoverImage" :src="blog.blogCoverImage" :alt="blog.blogTitle" />
          <div v-else class="cover-placeholder">🗡️</div>
        </router-link>
        <div class="card-body">
          <div class="card-category">
            <router-link :to="'/category/' + blog.blogCategoryName + '/1'" class="category-link">
              <img v-if="blog.blogCategoryIcon" :src="blog.blogCategoryIcon" class="category-icon" />
              <span>{{ blog.blogCategoryName }}</span>
            </router-link>
          </div>
          <h3 class="card-title">
            <router-link :to="'/blog/' + blog.blogId">{{ blog.blogTitle }}</router-link>
          </h3>
        </div>
      </article>
    </div>
    <div v-else class="empty-state">暂无相关文章</div>

    <!-- 侧边栏 -->
    <aside class="list-sidebar">
      <div class="sidebar-section">
        <h4>🔥 点击最多</h4>
        <ul class="sidebar-list">
          <li v-for="b in hotBlogs" :key="b.blogId">
            <router-link :to="'/blog/' + b.blogId">{{ b.blogTitle }}</router-link>
          </li>
        </ul>
      </div>
      <div class="sidebar-section">
        <h4>🆕 最新发布</h4>
        <ul class="sidebar-list">
          <li v-for="b in newBlogs" :key="b.blogId">
            <router-link :to="'/blog/' + b.blogId">{{ b.blogTitle }}</router-link>
          </li>
        </ul>
      </div>
      <div class="sidebar-section">
        <h4>🏷️ 标签</h4>
        <div class="tag-cloud">
          <router-link v-for="t in hotTags" :key="t.tagName" :to="'/tag/' + t.tagName + '/1'" class="tag-item">
            {{ t.tagName }}({{ t.tagCount }})
          </router-link>
        </div>
      </div>
    </aside>

    <!-- 分页 -->
    <ul class="pagination" v-if="totalPage > 1">
      <li :class="{ disabled: currPage <= 1 }">
        <a v-if="currPage > 1" href="#" @click.prevent="goPage(currPage - 1)">«</a>
        <span v-else>«</span>
      </li>
      <li v-for="p in pageNumbers" :key="p" :class="{ active: p === currPage }">
        <a v-if="p !== currPage" href="#" @click.prevent="goPage(p)">{{ p }}</a>
        <span v-else>{{ p }}</span>
      </li>
      <li :class="{ disabled: currPage >= totalPage }">
        <a v-if="currPage < totalPage" href="#" @click.prevent="goPage(currPage + 1)">»</a>
        <span v-else>»</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCategoryBlogs, fetchTagBlogs, fetchSearchBlogs } from '../api/blog'

const route = useRoute()
const router = useRouter()

const blogs = ref([])
const hotBlogs = ref([])
const newBlogs = ref([])
const hotTags = ref([])
const currPage = ref(1)
const totalPage = ref(1)
const keyword = ref('')

const listTitle = computed(() => {
  if (route.name === 'Category') return '分类文章'
  if (route.name === 'Tag') return '标签文章'
  if (route.name === 'Search') return '搜索结果'
  return '文章列表'
})

const pageNumbers = computed(() => {
  const pages = []
  const start = Math.max(1, currPage.value - 2)
  const end = Math.min(totalPage.value, currPage.value + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const goPage = (p) => {
  const name = route.params.name || route.params.keyword
  const type = route.name === 'Category' ? 'category' : route.name === 'Tag' ? 'tag' : 'search'
  router.push(`/${type}/${name}/${p}`)
}

const loadData = async () => {
  const name = route.params.name || route.params.keyword
  const page = parseInt(route.params.page) || 1
  currPage.value = page
  keyword.value = name
  try {
    let res
    if (route.name === 'Category') res = await fetchCategoryBlogs(name, page)
    else if (route.name === 'Tag') res = await fetchTagBlogs(name, page)
    else res = await fetchSearchBlogs(name, page)
    const d = res.data || {}
    const bp = d.blogPage || {}
    blogs.value = bp.list || []
    totalPage.value = bp.totalPage || 1
    hotBlogs.value = d.hotBlogs || []
    newBlogs.value = d.newBlogs || []
    hotTags.value = d.hotTags || []
  } catch (e) { console.error(e) }
}

watch(() => route.params.name, () => loadData())
watch(() => route.params.page, () => loadData())
onMounted(() => loadData())
</script>

<style scoped>
.blog-list-page {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
}
.list-header {
  grid-column: 1 / -1;
}
.list-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
}
.keyword { color: var(--color-primary); font-weight: 400; }

.blog-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.blog-card {
  background: var(--color-card);
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--color-border);
  transition: transform 0.25s, box-shadow 0.25s;
}
.blog-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
.card-cover {
  display: block;
  height: 140px;
  overflow: hidden;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}
.blog-card:hover .card-cover img { transform: scale(1.05); }
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  font-size: 32px;
}
.card-body { padding: 14px; }
.card-category { margin-bottom: 6px; }
.category-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: #f1f8e9;
  padding: 2px 8px;
  border-radius: 20px;
}
.category-icon { height: 12px; width: 12px; }
.card-title { font-size: 14px; font-weight: 600; margin: 0; line-height: 1.4; }
.card-title a { color: var(--color-text); }
.card-title a:hover { color: var(--color-primary); }

.list-sidebar {
  grid-column: 2;
  grid-row: 2 / 4;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sidebar-section {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 16px;
  border: 1px solid var(--color-border);
}
.sidebar-section h4 { font-size: 14px; font-weight: 600; margin-bottom: 10px; }
.sidebar-list { list-style: none; padding: 0; }
.sidebar-list li { margin-bottom: 6px; }
.sidebar-list a { font-size: 13px; color: var(--color-text-secondary); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sidebar-list a:hover { color: var(--color-primary); }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 4px; }
.tag-item { font-size: 11px; padding: 2px 8px; background: #f1f8e9; border-radius: 20px; color: var(--color-text-secondary); }
.tag-item:hover { background: #c8e6c9; color: var(--color-primary); }

.empty-state { text-align: center; padding: 60px; color: var(--color-text-secondary); grid-column: 1; }

@media (max-width: 900px) {
  .blog-list-page { grid-template-columns: 1fr; }
  .blog-grid { grid-template-columns: repeat(2, 1fr); }
  .list-sidebar { grid-column: 1; grid-row: auto; }
}
</style>
