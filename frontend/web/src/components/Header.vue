<template>
  <header class="site-header">
    <div class="header-inner">
      <router-link to="/" class="brand" aria-label="拾光集">
        <span class="brand-mark">拾光</span>
        <span class="brand-copy">
          <strong>{{ config.websiteName || '拾光集' }}</strong>
          <small>Search · AI · Life</small>
        </span>
      </router-link>

      <nav class="nav-links" aria-label="主导航">
        <router-link to="/">主页</router-link>
        <router-link to="/portfolio">作品集</router-link>
        <router-link to="/notes">技术笔记</router-link>
        <router-link to="/life">生活杂记</router-link>
        <router-link to="/message">留言板</router-link>
      </nav>

      <div class="search-box">
        <input v-model="keyword" type="text" placeholder="搜索技术笔记" @keyup.enter="doSearch" />
        <button type="button" aria-label="搜索" @click="doSearch">⌕</button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

defineProps({ config: { type: Object, default: () => ({}) } })

const router = useRouter()
const keyword = ref('')

const doSearch = () => {
  const kw = keyword.value.trim()
  if (kw) router.push(`/search/${kw}/1`)
}
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(252, 253, 250, 0.92);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(18px);
}

.header-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 24px;
  min-height: 68px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--color-text);
  flex-shrink: 0;
}

.brand-mark {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--color-text);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-copy strong {
  font-size: 16px;
  font-weight: 800;
}

.brand-copy small {
  margin-top: 3px;
  color: var(--color-muted);
  font-size: 11px;
}

.nav-links {
  display: flex;
  gap: 2px;
}

.nav-links a {
  padding: 8px 13px;
  border-radius: 8px;
  color: var(--color-subtle);
  font-size: 14px;
  font-weight: 650;
}

.nav-links a:hover,
.nav-links a.router-link-active {
  background: var(--color-surface-strong);
  color: var(--color-text);
}

.search-box {
  margin-left: auto;
  display: flex;
  align-items: center;
  min-width: 220px;
  height: 38px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.search-box input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  padding: 0 12px;
  color: var(--color-text);
  font-size: 14px;
  background: transparent;
}

.search-box button {
  width: 38px;
  height: 38px;
  border: 0;
  border-left: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
  font-size: 20px;
}

.search-box button:hover {
  background: var(--color-accent-soft);
}

@media (max-width: 860px) {
  .header-inner {
    padding: 12px 16px;
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .nav-links {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .nav-links a {
    white-space: nowrap;
  }

  .search-box {
    width: 100%;
    min-width: 0;
    margin-left: 0;
  }
}
</style>
