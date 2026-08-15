<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-copy">
        <span class="page-kicker">搜索架构 · AI 工程 · 生活笔记</span>
        <h1>{{ profile.name }}，{{ profile.title }}</h1>
        <p>{{ profile.summary }}</p>
        <ul class="pill-list hero-focus">
          <li v-for="item in profile.focus" :key="item">{{ item }}</li>
        </ul>
        <div class="button-row">
          <router-link class="btn primary" to="/portfolio">查看作品集</router-link>
          <router-link class="btn" to="/notes">阅读技术笔记</router-link>
        </div>
      </div>

      <div class="hero-visual" aria-label="AI 搜索系统能力图">
        <img :src="heroImage" alt="搜索系统架构抽象图" />
        <div class="pipeline">
          <span>Query</span>
          <span>Recall</span>
          <span>Rerank</span>
          <span>Answer</span>
        </div>
        <div class="visual-caption">
          <strong>AI Search Stack</strong>
          <p>从查询到答案，每一环都拆开揉碎了聊。</p>
        </div>
      </div>
      <div class="hero-contact">
        <a :href="'mailto:' + profile.email">{{ profile.email }}</a>
        <span>{{ profile.location }}</span>
      </div>
    </section>

    <section class="stats-bar">
      <div class="stat-item" v-for="stat in profile.stats" :key="stat.label">
        <strong>{{ stat.value }}</strong>
        <span>{{ stat.label }}</span>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <span class="page-kicker">Capability Matrix</span>
          <h2>能力矩阵</h2>
        </div>
        <p>不堆技能清单。按解决的问题分组，每个方向都带着项目证据和踩坑记录。</p>
      </div>

      <div class="capability-grid">
        <article class="capability-card" v-for="capability in profile.capabilities" :key="capability.group">
          <div class="capability-meta">
            <span>{{ capability.group }}</span>
            <small>{{ capability.level }}</small>
          </div>
          <p>{{ capability.description }}</p>
          <ul>
            <li v-for="item in capability.items" :key="item">{{ item }}</li>
          </ul>
        </article>
      </div>
    </section>

    <section class="section about-section">
      <div>
        <span class="page-kicker">About</span>
        <h2>个人简介</h2>
      </div>
      <div class="about-copy">
        <p>
          我叫程军高，11 年后端开发和架构老兵，6 年带团队。在某电商公司搭了快十年的搜索平台——从 Solr 引擎、索引系统到召回、排序、Query 理解，一路迭代到双塔召回和 AI Search，日均扛着 1500 万 PV。手里有 PMP，也当了几年 Scrum Master。
        </p>
        <p>
          近一年把重心压到 Agent 和 MCP 平台上。从给某电商公司做 AI 购物助手入坑，到双塔召回、Gemma 微调、LangGraph 编排、MCP 协议落地，越扎越深。
        </p>
        <p>
          这个网站是我的数字花园。技术笔记追求系统化、能复用；生活板块留点松弛感，记记做菜和读书。
        </p>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <span class="page-kicker">Selected Work</span>
          <h2>精选作品</h2>
        </div>
        <router-link class="btn" to="/portfolio">全部作品</router-link>
      </div>
      <div class="featured-grid">
        <article class="work-card" v-for="item in featuredWorks" :key="item.title">
          <div class="work-top">
            <span>{{ item.type }}</span>
            <small>{{ item.status }}</small>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
        </article>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <span class="page-kicker">Latest Notes</span>
          <h2>最新技术笔记</h2>
        </div>
        <router-link class="btn" to="/notes">进入笔记</router-link>
      </div>
      <div class="note-list" v-if="latestNotes.length">
        <router-link class="note-row" v-for="note in latestNotes" :key="note.blogId" :to="'/notes/' + note.blogId">
          <span>{{ note.blogCategoryName || '技术笔记' }}</span>
          <strong>{{ note.blogTitle }}</strong>
        </router-link>
      </div>
      <div v-else class="empty-state">笔记正在整理中，很快上线。</div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchIndex } from '../api/blog'
import { setPageMeta } from '../utils/seo'
import heroImage from '../assets/hero.png'
import { portfolioItems, profile } from '../content/profile'

const latestNotes = ref([])
const featuredWorks = computed(() => portfolioItems.slice(0, 3))

onMounted(async () => {
  setPageMeta({
    title: '',
    description: '拾光集 — 程军高的个人空间，11 年搜索架构与 AI 工程实践，记录搜索引擎架构、AI 搜索、Agent 与 MCP 平台的技术笔记，也写写做菜和读书。'
  })
  try {
    const res = await fetchIndex(1)
    latestNotes.value = (res.data?.newBlogs || res.data?.blogPage?.list || []).slice(0, 5)
  } catch (e) {
    console.error('加载最新技术笔记失败', e)
  }
})
</script>

<style scoped>
.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(380px, 0.9fr);
  gap: 56px;
  align-items: center;
  min-height: 600px;
}

.hero-copy h1 {
  margin-top: 12px;
  max-width: 780px;
  font-size: 58px;
  line-height: 1.08;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 740px;
  margin-top: 22px;
  color: var(--color-subtle);
  font-size: 18px;
}

.hero-focus {
  margin-top: 22px;
}

.button-row {
  margin-top: 30px;
}

.hero-visual {
  position: relative;
  min-height: 460px;
  padding: 32px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(238, 244, 241, 0.96)),
    repeating-linear-gradient(90deg, transparent, transparent 36px, rgba(15, 118, 110, 0.05) 37px);
  overflow: hidden;
}

.hero-visual img {
  position: absolute;
  right: 28px;
  top: 20px;
  width: 260px;
  opacity: 0.88;
}

.pipeline {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
  max-width: 260px;
  margin-top: 42px;
}

.pipeline span {
  display: flex;
  align-items: center;
  min-height: 48px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text);
  font-weight: 800;
  box-shadow: var(--shadow-sm);
}

.pipeline span:nth-child(2) {
  margin-left: 36px;
}

.pipeline span:nth-child(3) {
  margin-left: 72px;
}

.pipeline span:nth-child(4) {
  margin-left: 108px;
  background: var(--color-text);
  color: #fff;
}

.visual-caption {
  position: absolute;
  left: 28px;
  right: 28px;
  bottom: 26px;
  z-index: 1;
  padding-top: 18px;
  border-top: 1px solid var(--color-border);
}

.visual-caption strong {
  color: var(--color-text);
  font-size: 15px;
}

.visual-caption p {
  margin-top: 6px;
  color: var(--color-subtle);
  font-size: 13px;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.capability-card {
  min-height: 288px;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
}

.capability-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.capability-meta span {
  color: var(--color-text);
  font-size: 18px;
  font-weight: 850;
}

.capability-meta small {
  color: var(--color-warm);
  font-size: 12px;
  font-weight: 800;
}

.capability-card p {
  margin-top: 16px;
  color: var(--color-subtle);
  font-size: 14px;
}

.capability-card ul {
  margin-top: 18px;
  padding-left: 18px;
  color: var(--color-text);
  font-size: 13px;
}

.capability-card li + li {
  margin-top: 7px;
}

.hero-contact {
  margin-top: 18px;
  display: flex;
  gap: 20px;
  align-items: center;
  font-size: 14px;
}

.hero-contact a {
  color: var(--color-accent);
  font-weight: 600;
}

.hero-contact span {
  color: var(--color-subtle);
}

.stats-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  padding: 28px 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: center;
  border-right: 1px solid var(--color-border);
}

.stat-item:last-child {
  border-right: none;
}

.stat-item strong {
  font-size: 28px;
  color: var(--color-text);
  font-weight: 850;
}

.stat-item span {
  font-size: 13px;
  color: var(--color-subtle);
}

.about-section {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 56px;
  padding: 40px 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.about-section h2 {
  margin-top: 8px;
  font-size: 28px;
}

.about-copy {
  display: grid;
  gap: 16px;
  color: var(--color-subtle);
  font-size: 16px;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.work-card {
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
}

.work-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
}

.work-card h3 {
  margin-top: 18px;
  color: var(--color-text);
  font-size: 19px;
}

.work-card p {
  margin-top: 10px;
  color: var(--color-subtle);
  font-size: 14px;
}

.note-list {
  display: grid;
  border-top: 1px solid var(--color-border);
}

.note-row {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 18px;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}

.note-row span {
  color: var(--color-muted);
  font-size: 13px;
}

.note-row strong {
  font-size: 16px;
}

.note-row:hover strong {
  color: var(--color-accent);
}

.empty-state {
  padding: 34px;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  color: var(--color-muted);
  background: var(--color-surface);
}

@media (max-width: 1200px) {
  .hero-section,
  .about-section {
    grid-template-columns: 1fr;
  }

  .hero-section {
    min-height: 0;
  }

  .capability-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .featured-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stats-bar {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-item:nth-child(2) {
    border-right: none;
  }

  .stat-item:nth-child(1),
  .stat-item:nth-child(2) {
    border-bottom: 1px solid var(--color-border);
    padding-bottom: 20px;
  }
}

@media (max-width: 640px) {
  .hero-copy h1 {
    font-size: 38px;
  }

  .hero-visual {
    min-height: 360px;
    padding: 20px;
  }

  .hero-visual img {
    width: 180px;
  }

  .pipeline span:nth-child(n) {
    margin-left: 0;
  }

  .capability-grid,
  .featured-grid {
    grid-template-columns: 1fr;
  }

  .stats-bar {
    grid-template-columns: 1fr;
  }

  .stat-item {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
    padding: 16px 0;
  }

  .stat-item:last-child {
    border-bottom: none;
  }

  .note-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
