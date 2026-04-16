<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :xs="12" :sm="6" v-for="item in statCards" :key="item.key" style="margin-bottom: 16px;">
        <div class="stat-card" :style="{ '--accent': item.color }">
          <div class="stat-icon-wrap">
            <el-icon :size="26"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ stats[item.key] }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-card class="welcome-card" shadow="never">
          <div class="welcome-inner">
            <div class="welcome-text">
              <h3>⚔️ 欢迎回来，{{ username }}</h3>
              <p>欢迎使用代码江湖博客后台管理系统，在这里管理您的博客内容。</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboardData } from '../api/admin'

const username = ref(localStorage.getItem('username') || '大侠')

const statCards = [
  { key: 'blogCount', label: '文章总数', icon: 'Document', color: '#43a047' },
  { key: 'commentCount', label: '评论总数', icon: 'ChatDotRound', color: '#26a69a' },
  { key: 'categoryCount', label: '分类总数', icon: 'Folder', color: '#7cb342' },
  { key: 'tagCount', label: '标签总数', icon: 'PriceTag', color: '#8bc34a' },
  { key: 'linkCount', label: '友情链接', icon: 'Link', color: '#009688' }
]

const stats = ref({
  blogCount: 0,
  commentCount: 0,
  categoryCount: 0,
  tagCount: 0,
  linkCount: 0
})

const fetchData = async () => {
  try {
    const res = await getDashboardData()
    const d = res.data || {}
    stats.value = {
      blogCount: d.blogCount || 0,
      commentCount: d.commentCount || 0,
      categoryCount: d.categoryCount || 0,
      tagCount: d.tagCount || 0,
      linkCount: d.linkCount || 0
    }
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.dashboard { padding: 0; }

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
  border: 1px solid #e8f5e9;
  transition: transform 0.25s, box-shadow 0.25s;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.07);
}

.stat-icon-wrap {
  width: 52px; height: 52px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--accent) 12%, transparent);
  color: var(--accent);
  flex-shrink: 0;
}

.stat-body { flex: 1; }
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #2e3b2e;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #78909c;
  margin-top: 3px;
}

.welcome-card {
  border-radius: 12px;
  border: 1px solid #e8f5e9;
  margin-top: 16px;
}
.welcome-inner {
  padding: 8px 0;
}
.welcome-text h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #2e7d32;
  font-weight: 600;
}
.welcome-text p {
  margin: 0;
  color: #78909c;
  font-size: 14px;
  line-height: 1.6;
}
</style>
