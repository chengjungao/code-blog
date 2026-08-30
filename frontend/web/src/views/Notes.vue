<template>
  <div class="notes-page">
    <header class="page-header">
      <span class="page-kicker">Technical Notes</span>
      <h1 class="page-title">技术笔记</h1>
      <p class="page-lead">
        搜索、AI、架构、工程实践——写下来才真正属于自己的。这里有踩坑记录、系统设计和读书笔记。
      </p>
    </header>

    <section class="notes-layout section">
      <div>
        <div class="note-grid" v-if="blogs.length">
          <article class="note-card" v-for="blog in blogs" :key="blog.blogId">
            <router-link :to="blogLink(blog)" class="note-cover">
              <img v-if="blog.blogCoverImage" :src="blog.blogCoverImage" :alt="blog.blogTitle" />
              <div v-else class="cover-placeholder">{{ getInitial(blog.blogTitle) }}</div>
            </router-link>
            <div class="note-body">
              <router-link :to="'/category/' + blog.blogCategoryName + '/1'" class="note-category">
                {{ blog.blogCategoryName || '技术笔记' }}
              </router-link>
              <h2>
                <router-link :to="blogLink(blog)">{{ blog.blogTitle }}</router-link>
              </h2>
            </div>
          </article>
        </div>
        <div v-else class="empty-state">暂无技术笔记</div>

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

      <aside class="notes-sidebar">
        <section class="side-panel" v-if="newBlogs.length">
          <h3>最新发布</h3>
          <router-link v-for="b in newBlogs" :key="b.blogId" :to="blogLink(b)">{{ b.blogTitle }}</router-link>
        </section>
        <section class="side-panel" v-if="hotBlogs.length">
          <h3>高频阅读</h3>
          <router-link v-for="b in hotBlogs" :key="b.blogId" :to="blogLink(b)">{{ b.blogTitle }}</router-link>
        </section>
        <section class="side-panel" v-if="hotTags.length">
          <h3>标签</h3>
          <div class="tag-cloud">
            <router-link v-for="t in hotTags" :key="t.tagName" :to="'/tag/' + t.tagName + '/1'" class="tag-pill">
              {{ t.tagName }}({{ t.tagCount }})
            </router-link>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchIndex, blogLink } from '../api/blog'
import { setPageMeta } from '../utils/seo'

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

const getInitial = (title = '') => title.trim().slice(0, 1) || 'N'

const goPage = (p) => {
  router.push(p === 1 ? '/notes' : '/notes/page/' + p)
}

const loadData = async (page) => {
  setPageMeta({
    title: '技术笔记',
    description: '搜索、AI、架构、工程实践——写下来才真正属于自己的。踩坑记录、系统设计和读书笔记。',
    url: window.location.origin + window.location.pathname
  })
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
    console.error('加载技术笔记失败', e)
  }
}

watch(
  () => route.params.pageNum,
  val => loadData(val ? parseInt(val) : 1),
  { immediate: true }
)
</script>

<style scoped>
.notes-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 26px;
  align-items: start;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.note-card {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
}

.note-cover {
  display: block;
  height: 178px;
  overflow: hidden;
  background: var(--color-surface-strong);
}

.note-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s;
}

.note-card:hover .note-cover img {
  transform: scale(1.04);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text);
  font-size: 42px;
  font-weight: 850;
}

.note-body {
  padding: 18px;
}

.note-category {
  color: var(--color-warm);
  font-size: 12px;
  font-weight: 850;
}

.note-body h2 {
  margin-top: 9px;
  font-size: 18px;
  line-height: 1.38;
}

.note-body h2 a {
  color: var(--color-text);
}

.note-body h2 a:hover {
  color: var(--color-accent);
}

.notes-sidebar {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 92px;
}

.side-panel {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
}

.side-panel h3 {
  color: var(--color-text);
  font-size: 15px;
}

.side-panel a {
  color: var(--color-subtle);
  font-size: 13px;
  line-height: 1.45;
}

.side-panel a:hover {
  color: var(--color-accent);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-state {
  padding: 42px;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  color: var(--color-muted);
  background: var(--color-surface);
}

@media (max-width: 1200px) {
  .note-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 920px) {
  .notes-layout {
    grid-template-columns: 1fr;
  }

  .notes-sidebar {
    position: static;
  }
}

@media (max-width: 640px) {
  .note-grid {
    grid-template-columns: 1fr;
  }
}
</style>
