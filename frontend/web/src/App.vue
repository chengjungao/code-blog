<template>
  <div id="app">
    <Header :config="siteConfig" />
    <main class="site-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :config="siteConfig" />
        </transition>
      </router-view>
    </main>
    <Footer :config="siteConfig" />
    <a v-show="showBackTop" class="back-top" href="#" @click.prevent="scrollToTop">↑</a>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchConfig } from './api/blog'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'

const siteConfig = ref({})
const showBackTop = ref(false)

const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

const handleScroll = () => {
  showBackTop.value = window.scrollY > 300
}

onMounted(async () => {
  try {
    const res = await fetchConfig()
    siteConfig.value = res.data || {}
    document.title = (siteConfig.value.websiteName || '代码江湖') + ' - 博客'
  } catch (e) {
    console.error('加载配置失败', e)
  }
  window.addEventListener('scroll', handleScroll)
})
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
