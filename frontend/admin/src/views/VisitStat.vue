<template>
  <div class="visit-stat">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :xs="12" :sm="5">
        <div class="stat-card">
          <div class="stat-value">{{ formatNumber(totalPv) }}</div>
          <div class="stat-label">总浏览量 PV</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="5">
        <div class="stat-card">
          <div class="stat-value">{{ formatNumber(totalUv) }}</div>
          <div class="stat-label">总访客数 UV</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="14">
        <div class="stat-card range-card">
          <span class="stat-label" style="margin-right: 12px;">时间范围</span>
          <el-radio-group v-model="days" @change="fetchData">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="14">近 14 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
      </el-col>
    </el-row>

    <!-- PV 趋势 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>浏览量趋势（PV）</span>
          <span class="card-hint">每 5 分钟聚合一次</span>
        </div>
      </template>
      <BarChart v-if="pvData.length" :data="pvData" color="#409eff" />
      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-card>

    <!-- UV 趋势 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>独立访客趋势（UV）</span>
          <span class="card-hint">按 IP 去重</span>
        </div>
      </template>
      <BarChart v-if="uvData.length" :data="uvData" color="#67c23a" />
      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-card>

    <!-- TOP 页面 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>热门页面 TOP 10</span>
          <span class="card-hint">按 PV 排序</span>
        </div>
      </template>
      <div v-if="topPages.length" class="top-list">
        <div class="top-item" v-for="(p, i) in topPages" :key="p.pagepath">
          <span class="top-rank" :class="{ 'top-3': i < 3 }">{{ i + 1 }}</span>
          <span class="top-path" :title="p.pagepath">{{ p.pagepath }}</span>
          <div class="top-bar-wrap">
            <div class="top-bar" :style="{ width: barWidth(p.pv) }"></div>
          </div>
          <span class="top-pv">{{ formatNumber(p.pv) }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { getStatOverview } from '../api/admin'

const days = ref(14)
const totalPv = ref(0)
const totalUv = ref(0)
const daily = ref([])
const topPages = ref([])

// 纯 SVG 柱状图组件（不引入图表库）
const BarChart = {
  name: 'BarChart',
  props: {
    data: { type: Array, default: () => [] },
    color: { type: String, default: '#409eff' }
  },
  setup(props) {
    const W = 760
    const H = 240
    const topPad = 26
    const bottomPad = 34

    const max = computed(() => Math.max(1, ...props.data.map(d => d.value)))

    const slotW = computed(() => W / props.data.length)
    const barW = computed(() => Math.min(48, slotW.value * 0.6))
    const areaH = computed(() => H - topPad - bottomPad)

    const x = (i) => i * slotW.value + (slotW.value - barW.value) / 2
    const barH = (v) => Math.max(2, (v / max.value) * areaH.value)
    const y = (v) => topPad + (areaH.value - barH(v))

    return { W, H, topPad, max, x, y, barH, barW }
  },
  render() {
    const { W, H, max } = this
    const gridLines = []
    for (let i = 0; i <= 4; i++) {
      const gy = this.topPad + (i * (H - this.topPad - 34)) / 4
      gridLines.push(h('line', {
        x1: 0, x2: W, y1: gy, y2: gy,
        class: 'grid-line', key: 'g' + i
      }))
    }
    return h('svg', { viewBox: `0 0 ${W} ${H}`, class: 'bar-chart', preserveAspectRatio: 'xMidYMid meet' }, [
      ...gridLines,
      h('text', { x: W - 4, y: 12, class: 'max-label' }, `max ${max}`),
      ...this.data.map((d, i) => {
        const bw = this.barW
        const bh = this.barH(d.value)
        const bx = this.x(i)
        const by = this.y(d.value)
        return h('g', { key: i }, [
          h('rect', { x: bx, y: by, width: bw, height: bh, rx: 3, fill: this.color }),
          h('title', {}, `${d.label}：${d.value}`),
          h('text', { x: bx + bw / 2, y: by - 6, class: 'bar-value', 'text-anchor': 'middle' }, d.value),
          h('text', { x: bx + bw / 2, y: H - 12, class: 'bar-label', 'text-anchor': 'middle' }, d.label)
        ])
      })
    ])
  }
}

const pvData = computed(() => daily.value.map(d => ({ label: shortDate(d.statdate), value: d.pv })))
const uvData = computed(() => daily.value.map(d => ({ label: shortDate(d.statdate), value: d.uv })))

const shortDate = (s) => String(s || '').slice(5)

const formatNumber = (n) => {
  const v = Number(n || 0)
  return v >= 10000 ? (v / 10000).toFixed(1) + 'w' : String(v)
}

const maxPv = computed(() => Math.max(1, ...topPages.value.map(p => p.pv)))

const barWidth = (pv) => {
  return Math.max(2, (pv / maxPv.value) * 100) + '%'
}

const fetchData = async () => {
  try {
    const res = await getStatOverview(days.value)
    const d = res.data || {}
    const rawDaily = d.daily || []
    daily.value = rawDaily.map(item => ({
      statdate: String(item.statdate || item.statDate || ''),
      pv: Number(item.pv || 0),
      uv: Number(item.uv || 0)
    }))
    topPages.value = (d.topPages || []).map(item => ({
      pagepath: String(item.pagepath || item.pagePath || ''),
      pv: Number(item.pv || 0),
      uv: Number(item.uv || 0)
    }))
    const total = d.total || {}
    totalPv.value = Number(total.pv || 0)
    totalUv.value = Number(total.uv || 0)
  } catch (e) {
    console.error('获取访问统计失败', e)
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.visit-stat { padding: 0; }

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  border: 1px solid #e8f5e9;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-value { font-size: 28px; font-weight: 800; color: #2e7d32; }
.stat-label { font-size: 13px; color: #8aa093; margin-top: 4px; }
.range-card { flex-direction: row; align-items: center; }

.chart-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-hint { font-size: 12px; color: #a3b8ab; font-weight: 400; }

.bar-chart { width: 100%; height: auto; display: block; }
.grid-line { stroke: #eef2ef; stroke-width: 1; }
.max-label { font-size: 11px; fill: #b8c8bd; text-anchor: end; }
.bar-value { font-size: 11px; fill: #6b7f73; }
.bar-label { font-size: 11px; fill: #9aaca0; }

.top-list { display: flex; flex-direction: column; gap: 10px; }
.top-item { display: flex; align-items: center; gap: 12px; }
.top-rank {
  width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; color: #8aa093; background: #f0f4f1;
}
.top-rank.top-3 { color: #fff; background: #67c23a; }
.top-path {
  width: 180px; flex-shrink: 0; font-size: 13px; color: #4b5f53;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.top-bar-wrap { flex: 1; height: 14px; background: #f0f4f1; border-radius: 7px; overflow: hidden; }
.top-bar { height: 100%; background: linear-gradient(90deg, #67c23a, #409eff); border-radius: 7px; transition: width 0.4s; }
.top-pv { width: 70px; text-align: right; flex-shrink: 0; font-size: 12px; color: #6b7f73; font-weight: 600; }

@media (max-width: 640px) {
  .top-path { width: 100px; }
}
</style>
