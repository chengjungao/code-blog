<template>
  <div class="blog-list-page">
    <div class="list-header">
      <h2>
        {{ listTitle }}
        <span v-if="keyword" class="keyword">: {{ keyword }}</span>
        <span v-if="headingTag" class="keyword">#{{ headingTag }}</span>
      </h2>
    </div>

    <!-- 主列整体包一层：FilterBar 不能直接作为两列网格的子项，否则会被塞进侧栏列，网格列数错乱 -->
    <div class="list-main">
      <FilterBar
        :categories="categories"
        :tags="hotTags"
        :active-category="activeCategory"
        :active-tag="activeTag"
        all-link="/notes"
      />

      <div class="blog-grid" v-if="blogs.length">
        <article class="blog-card" v-for="blog in blogs" :key="blog.blogId">
          <router-link :to="blogLink(blog)" class="card-cover">
            <img v-if="blog.blogCoverImage" :src="blog.blogCoverImage" :alt="blog.blogTitle" />
            <div v-else class="cover-placeholder">{{ getInitial(blog.blogTitle) }}</div>
          </router-link>
          <div class="card-body">
            <div class="card-category">
              <router-link :to="'/category/' + blog.blogCategoryName + '/1'" class="category-link">
                <span>{{ blog.blogCategoryName }}</span>
              </router-link>
            </div>
            <h3 class="card-title">
              <router-link :to="blogLink(blog)">{{ blog.blogTitle }}</router-link>
            </h3>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">暂无相关笔记</div>

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

    <!-- 侧边栏 -->
    <aside class="list-sidebar">
      <div class="sidebar-section">
        <h4>高频阅读</h4>
        <ul class="sidebar-list">
          <li v-for="b in hotBlogs" :key="b.blogId">
            <router-link :to="blogLink(b)">{{ b.blogTitle }}</router-link>
          </li>
        </ul>
      </div>
      <div class="sidebar-section">
        <h4>最新发布</h4>
        <ul class="sidebar-list">
          <li v-for="b in newBlogs" :key="b.blogId">
            <router-link :to="blogLink(b)">{{ b.blogTitle }}</router-link>
          </li>
        </ul>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCategoryBlogs, fetchTagBlogs, fetchSearchBlogs, blogLink } from '../api/blog'
import { setPageMeta, setJsonLd, removeJsonLd } from '../utils/seo'
import FilterBar from '../components/FilterBar.vue'

const route = useRoute()
const router = useRouter()

const blogs = ref([])
const hotBlogs = ref([])
const newBlogs = ref([])
const hotTags = ref([])
const categories = ref([])
const currPage = ref(1)
const totalPage = ref(1)
const keyword = ref('')

// 当前选中的筛选项（用于 FilterBar 高亮）
// 类目优先取后端回传：标签页会反查出该标签归属的类目，靠路由参数推不出来
const serverActiveCategory = ref('')
const activeCategory = computed(() =>
  serverActiveCategory.value || (route.name === 'Category' ? route.params.name || '' : '')
)
// 标签有两个来源：标签页走路径参数，类目页内的收窄走 ?tag= 查询参数（类目 ∩ 标签）
const activeTag = computed(() =>
  route.name === 'Tag' ? route.params.name || '' : route.query.tag || ''
)
// 标题里额外的标签后缀（标签页本身就以标签为标题，不重复）
const headingTag = computed(() => (route.name === 'Category' ? activeTag.value : ''))

const listTitle = computed(() => {
  if (route.name === 'Category') return '分类笔记'
  if (route.name === 'Tag') return '标签笔记'
  if (route.name === 'Search') return '搜索结果'
  return '技术笔记'
})

const getInitial = (title = '') => title.trim().slice(0, 1) || 'N'

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
  // 翻页要带上类目内的标签收窄条件，否则翻到第 2 页标签就丢了
  const query = route.name === 'Category' && activeTag.value ? { tag: activeTag.value } : undefined
  router.push({ path: `/${type}/${name}/${p}`, query })
}

const loadData = async () => {
  const name = route.params.name || route.params.keyword
  const page = parseInt(route.params.page) || 1
  currPage.value = page
  keyword.value = name
  // 切路由时先清掉，避免上一页的类目高亮残留（类目页有路由参数兜底，不会闪）
  serverActiveCategory.value = ''
  
  const pageUrl = window.location.origin + window.location.pathname
  setPageMeta({
    title: `${listTitle.value}${name ? '：' + name : ''}${headingTag.value ? ' #' + headingTag.value : ''}`,
    description: `程军高关于「${name || '技术笔记'}」的笔记列表。`,
    url: pageUrl
  })
  
  // 注入 BreadcrumbList 结构化数据
  const typeLabel = route.name === 'Category' ? '分类' : route.name === 'Tag' ? '标签' : '搜索'
  setJsonLd('BreadcrumbList', {
    itemListElement: [
      { '@type': 'ListItem', position: 1, name: '首页', item: 'https://www.chengjungao.cn/' },
      { '@type': 'ListItem', position: 2, name: typeLabel, item: window.location.origin + '/' + typeLabel.toLowerCase() },
      { '@type': 'ListItem', position: 3, name: name || typeLabel }
    ]
  })
  
  try {
    let res
    // 类目页把 ?tag= 一起交给后端，两个条件在同一条 SQL 里生效
    if (route.name === 'Category') res = await fetchCategoryBlogs(name, page, route.query.tag || '')
    else if (route.name === 'Tag') res = await fetchTagBlogs(name, page)
    else res = await fetchSearchBlogs(name, page)
    const d = res.data || {}
    const bp = d.blogPage || {}
    blogs.value = bp.list || []
    totalPage.value = bp.totalPage || 1
    hotBlogs.value = d.hotBlogs || []
    newBlogs.value = d.newBlogs || []
    hotTags.value = d.hotTags || []
    categories.value = d.categoryFilters || []
    serverActiveCategory.value = d.activeCategory || ''
  } catch (e) { console.error(e) }
}

// 类目名 / 页码 / 类目内的标签（?tag=）任一变化都要重新取数。
// 合成一个 watcher 而不是拆三个：切换类目时 name 和 tag 会同时变，拆开就会重复请求两次。
watch(
  () => [route.name, route.params.name, route.params.keyword, route.params.page, route.query.tag].join('|'),
  () => loadData()
)
onMounted(() => loadData())
onUnmounted(() => removeJsonLd('BreadcrumbList'))
</script>

<style scoped>
.blog-list-page {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
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

.list-main {
  grid-column: 1;
  min-width: 0;
}

/* 列数与 Notes.vue 的 .note-grid 保持一致，避免点类目后每行条数跳变 */
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
.card-title { font-size: 14px; font-weight: 600; margin: 0; line-height: 1.4; }
.card-title a { color: var(--color-text); }
.card-title a:hover { color: var(--color-primary); }

.list-sidebar {
  grid-column: 2;
  grid-row: 2;
  align-self: start;
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

.empty-state { text-align: center; padding: 60px; color: var(--color-text-secondary); }

@media (max-width: 1200px) {
  .blog-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 900px) {
  .blog-list-page { grid-template-columns: 1fr; }
  .blog-grid { grid-template-columns: repeat(2, 1fr); }
  .list-main { grid-column: 1; }
  .list-sidebar { grid-column: 1; grid-row: auto; }
}

@media (max-width: 640px) {
  .blog-grid { grid-template-columns: 1fr; }
}
</style>
