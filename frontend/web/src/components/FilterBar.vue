<template>
  <div class="filter-bar">
    <div class="filter-row">
      <span class="filter-label">类目</span>
      <div class="chips">
        <router-link :to="allLink" class="chip" :class="{ on: !activeCategory }">
          全部
        </router-link>
        <router-link
          v-for="c in catChips"
          :key="c.name"
          :to="categoryLink(c.name)"
          class="chip"
          :class="{ on: activeCategory === c.name }"
        >
          {{ c.name }}<em v-if="c.count !== null">{{ c.count }}</em>
        </router-link>
      </div>
    </div>

    <!-- 二级联动：只有选定类目才出标签，避免一次铺满全部标签 -->
    <div class="filter-row" v-if="activeCategory && tags.length">
      <span class="filter-label">标签</span>
      <div class="chips">
        <router-link
          v-for="t in tags"
          :key="t.tagName"
          :to="tagLink(t.tagName)"
          class="chip tag"
          :class="{ on: activeTag === t.tagName }"
          :title="activeTag === t.tagName ? '再点一次取消该标签' : t.tagName"
        >
          <i>#</i>{{ t.tagName }}<em>{{ t.tagCount }}</em>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 类目 chips（后端已按作用域过滤：技术笔记侧不含生活类分类）
  categories: { type: Array, default: () => [] },
  // 标签 chips（后端按当前类目作用域下发；未选类目时为空）
  tags: { type: Array, default: () => [] },
  activeCategory: { type: String, default: '' },
  activeTag: { type: String, default: '' },
  // 「全部」指向：技术笔记列表
  allLink: { type: String, default: '/notes' }
})

/**
 * 类目 chips。生活类页面后端不下发技术类目，但当前类目仍需可见（否则无高亮、也回不去），
 * 故当前类目不在列表里时临时补一个不带计数的 chip。
 */
const catChips = computed(() => {
  const list = props.categories.map(c => ({ name: c.categoryName, count: c.categoryCount }))
  if (props.activeCategory && !list.some(c => c.name === props.activeCategory)) {
    list.push({ name: props.activeCategory, count: null })
  }
  return list
})

/**
 * 类目链接：切换类目时清掉标签。
 * 标签列表是按类目联动的，跨类目保留上一个标签只会得到无法解释的空结果，
 * 所以「换类目 = 重置标签」这条规则要在链接上落地，而不是靠页面逻辑兜底。
 */
const categoryLink = name => '/category/' + encodeURIComponent(name) + '/1'

/**
 * 标签链接：标签是类目内的收窄条件，必须把当前类目一起带进 URL，
 * 否则会退回全站标签视图，看起来就是「类目和标签没有同时生效」。
 * 再点一次已选中的标签 = 取消该标签，回到纯类目视图。
 */
const tagLink = name => {
  const cat = props.activeCategory
  if (!cat) return '/tag/' + encodeURIComponent(name) + '/1'
  const base = '/category/' + encodeURIComponent(cat) + '/1'
  return props.activeTag === name ? base : base + '?tag=' + encodeURIComponent(name)
}
</script>

<style scoped>
.filter-bar {
  display: grid;
  gap: 12px;
  margin-bottom: 24px;
  padding: 14px 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.filter-row {
  display: grid;
  grid-template-columns: 36px 1fr;
  gap: 12px;
  align-items: start;
}

/* 标签行与类目行之间用虚线分隔，视觉上分出主次 */
.filter-row + .filter-row {
  padding-top: 12px;
  border-top: 1px dashed var(--color-border);
}

.filter-label {
  padding-top: 5px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 4px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  color: var(--color-subtle);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.chip em {
  color: var(--color-muted);
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}

.chip:hover {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

/* 选中态：柔和底色 + 底部色线，不用大面积实心块压住页面 */
.chip.on {
  background: var(--color-accent-soft);
  color: var(--color-accent);
  box-shadow: inset 0 -2px 0 var(--color-accent);
}

.chip.on em {
  color: var(--color-accent);
  opacity: 0.7;
}

/* 标签比类目轻一档：字号更小、字重更低，且带 # 前缀 */
.chip.tag {
  font-size: 12.5px;
  font-weight: 600;
}

.chip.tag i {
  color: var(--color-muted);
  font-style: normal;
  font-weight: 700;
}

.chip.tag.on i {
  color: var(--color-accent);
  opacity: 0.7;
}

@media (max-width: 640px) {
  .filter-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }
  .filter-label {
    padding-top: 0;
  }
}
</style>
