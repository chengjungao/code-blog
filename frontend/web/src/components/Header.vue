<template>
  <header class="site-header">
    <div class="header-inner">
      <router-link to="/" class="logo">
        <img v-if="config.websiteLogo" :src="config.websiteLogo" class="logo-img" alt="logo" />
        <span v-else class="logo-text">🗡️ 代码江湖</span>
      </router-link>
      <nav class="nav-links">
        <router-link to="/">主页</router-link>
        <router-link to="/link">友链</router-link>
        <router-link to="/about">关于</router-link>
        <a href="https://github.com/chengjungao" target="_blank">GitHub</a>
      </nav>
      <div class="search-box">
        <input v-model="keyword" type="text" placeholder="搜索文章..." @keyup.enter="doSearch" />
        <button @click="doSearch">🔍</button>
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
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 100%);
  border-bottom: 1px solid #d7e8d4;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.logo {
  display: flex;
  align-items: center;
  text-decoration: none;
  flex-shrink: 0;
}
.logo-img {
  height: 36px;
  border-radius: 6px;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 1px;
}
.nav-links {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.nav-links a {
  padding: 6px 14px;
  border-radius: 8px;
  color: #546e5a;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}
.nav-links a:hover,
.nav-links a.router-link-active {
  background: rgba(102, 187, 106, 0.12);
  color: var(--color-primary);
}
.search-box {
  margin-left: auto;
  display: flex;
  gap: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #c8e6c9;
  background: #fff;
}
.search-box input {
  border: none;
  outline: none;
  padding: 7px 14px;
  font-size: 14px;
  width: 180px;
  background: transparent;
}
.search-box button {
  border: none;
  background: var(--color-primary);
  color: #fff;
  padding: 7px 12px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}
.search-box button:hover { background: var(--color-accent); }

@media (max-width: 768px) {
  .header-inner { gap: 12px; }
  .nav-links { display: none; }
  .search-box input { width: 120px; }
}
</style>
