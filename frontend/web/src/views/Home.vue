<template>
  <div class="home-layout">
    <div class="home-main">
      <!-- 博客卡片网格 -->
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
      <div v-else class="empty-state">暂无文章</div>

      <!-- 分页 -->
      <ul class="pagination" v-if="totalPage > 1">
        <li :class="{ disabled: currPage <= 1 }">
          <a v-if="currPage > 1" :href="'/page/' + (currPage - 1)" @click.prevent="goPage(currPage - 1)">«</a>
          <span v-else>«</span>
        </li>
        <li v-for="p in pageNumbers" :key="p" :class="{ active: p === currPage }">
          <a v-if="p !== currPage" href="#" @click.prevent="goPage(p)">{{ p }}</a>
          <span v-else>{{ p }}</span>
        </li>
        <li :class="{ disabled: currPage >= totalPage }">
          <a v-if="currPage < totalPage" :href="'/page/' + (currPage + 1)" @click.prevent="goPage(currPage + 1)">»</a>
          <span v-else>»</span>
        </li>
      </ul>
    </div>

    <!-- 侧边栏 -->
    <aside class="home-sidebar">
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
        <h4>🏷️ 标签栏</h4>
        <div class="tag-cloud">
          <router-link v-for="t in hotTags" :key="t.tagName" :to="'/tag/' + t.tagName + '/1'" class="tag-item">
            {{ t.tagName }}({{ t.tagCount }})
          </router-link>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchIndex } from '../api/blog'

const route = useRoute()
const router = useRouter()

const blogs = ref([])
const hotBlogs = ref([])
const newBlogs = ref([])
const hotTags = ref([])
const currPage = ref(1)
const totalPage = ref(1)

const pageNumbers = computed(() => {
  const pages = []
  const start = Math.max(1, currPage.value - 2)
  const end = Math.min(totalPage.value, currPage.value + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const goPage = (p) => {
  if (p === 1) router.push('/')
  else router.push('/page/' + p)
}

const loadData = async (page) => {
  try {
    const res = await fetchIndex(page)
    const d = res.data || {}
    const bp = d.blogPage || {}
    blogs.value = bp.list || []
    currPage.value = bp.currPage || 1
    totalPage.value = bp.totalPage || 1
    hotBlogs.value = d.hotBlogs || []
    newBlogs.value = d.newBlogs || []
    hotTags.value = d.hotTags || []
  } catch (e) {
    console.error('加载失败', e)
  }
}

watch(() => route.params.pageNum, (val) => {
  loadData(val ? parseInt(val) : 1)
}, { immediate: true })

onMounted(() => {
  if (!route.params.pageNum) loadData(1)
})
</script>

<style scoped>
.home-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
}
.blog-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
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
  height: 180px;
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
  font-size: 40px;
}
.card-body { padding: 16px; }
.card-category { margin-bottom: 8px; }
.category-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: #f1f8e9;
  padding: 3px 10px;
  border-radius: 20px;
}
.category-icon { height: 14px; width: 14px; }
.card-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  margin: 0;
}
.card-title a {
  color: var(--color-text);
}
.card-title a:hover { color: var(--color-primary); }

.home-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.sidebar-section {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 20px;
  border: 1px solid var(--color-border);
}
.sidebar-section h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--color-text);
}
.sidebar-list {
  list-style: none;
  padding: 0;
}
.sidebar-list li {
  margin-bottom: 8px;
}
.sidebar-list a {
  font-size: 14px;
  color: var(--color-text-secondary);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sidebar-list a:hover { color: var(--color-primary); }

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag-item {
  font-size: 12px;
  padding: 3px 10px;
  background: #f1f8e9;
  border-radius: 20px;
  color: var(--color-text-secondary);
}
.tag-item:hover { background: #c8e6c9; color: var(--color-primary); }

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
  font-size: 15px;
}

@media (max-width: 900px) {
  .home-layout { grid-template-columns: 1fr; }
  .blog-grid { grid-template-columns: 1fr; }
}
</style>
