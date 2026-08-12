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
    <a v-show="showBackTop" class="back-top" href="#" aria-label="回到顶部" @click.prevent="scrollToTop">↑</a>
    <AssistantChat />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { fetchConfig } from './api/blog'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import AssistantChat from './components/AssistantChat.vue'

const siteConfig = ref({})
const showBackTop = ref(false)

const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

const handleScroll = () => {
  showBackTop.value = window.scrollY > 360
}

onMounted(async () => {
  try {
    const res = await fetchConfig()
    siteConfig.value = res.data || {}
    document.title = (siteConfig.value.websiteName || '拾光集') + ' — 程军高'
  } catch (e) {
    console.error('加载配置失败', e)
  }
  window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
