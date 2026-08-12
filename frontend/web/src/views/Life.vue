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
      <div class="life-column">
        <div class="section-head">
          <div>
            <span class="page-kicker">Cooking</span>
            <h2>做菜分享</h2>
          </div>
        </div>
        <article class="life-card cooking" v-for="entry in lifeEntries.cooking" :key="entry.title">
          <span>{{ entry.meta }}</span>
          <h3>{{ entry.title }}</h3>
          <p>{{ entry.note }}</p>
        </article>
      </div>

      <div class="life-column">
        <div class="section-head">
          <div>
            <span class="page-kicker">Reading</span>
            <h2>读书笔记</h2>
          </div>
        </div>
        <article class="life-card reading" v-for="entry in lifeEntries.books" :key="entry.title">
          <span>{{ entry.meta }}</span>
          <h3>{{ entry.title }}</h3>
          <p>{{ entry.note }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { lifeEntries } from '../content/profile'
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
  min-height: 164px;
  padding: 22px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
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
}

.life-card p {
  margin-top: 10px;
  color: var(--color-subtle);
  font-size: 15px;
}

@media (max-width: 760px) {
  .life-layout {
    grid-template-columns: 1fr;
  }
}
</style>
