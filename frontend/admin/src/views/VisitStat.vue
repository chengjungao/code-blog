<template>
  <div v-loading="loading" class="visit-stat">
    <!-- 顶部：时间范围 -->
    <div class="toolbar">
      <el-radio-group v-model="days" @change="reload">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="14">近 14 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
      </el-radio-group>
      <span v-if="report.generatedAt" class="gen-at">统计时间 {{ report.generatedAt }}</span>
      <el-button size="small" :loading="loading" @click="reload">刷新</el-button>
    </div>

    <!-- 数据健康预警：只在真的出问题时出现，避免天天喊狼来了 -->
    <el-alert
      v-if="healthAlert"
      :title="healthAlert"
      type="warning"
      show-icon
      :closable="false"
      class="alert-row"
    />

    <!-- 概览卡片 -->
    <el-row :gutter="12" class="card-row">
      <el-col v-for="c in summaryCards" :key="c.label" :xs="12" :sm="8" :md="4">
        <div class="stat-card">
          <div class="stat-value">{{ c.value }}</div>
          <div class="stat-label">{{ c.label }}</div>
          <div v-if="c.hint" class="stat-hint">{{ c.hint }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 访问趋势 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>访问趋势</span>
          <div class="ctrl">
            <el-radio-group v-model="gran" size="small">
              <el-radio-button value="day">按天</el-radio-button>
              <el-radio-button value="hour">按小时</el-radio-button>
            </el-radio-group>
            <!-- 按小时看的是「某一天内部的 24 小时」，所以必须选日期；不选就是跨天叠加，与按天对不上 -->
            <el-select
              v-if="gran === 'hour'"
              v-model="hourDate"
              size="small"
              class="hour-date"
              placeholder="选择日期"
            >
              <el-option v-for="d in hourlyDays" :key="d.date" :label="d.date" :value="d.date" />
            </el-select>
            <el-radio-group v-model="trendType" size="small">
              <el-radio-button value="line">折线</el-radio-button>
              <el-radio-button value="area">面积</el-radio-button>
              <el-radio-button value="bar">柱状</el-radio-button>
            </el-radio-group>
            <span v-if="dbDays" class="card-hint">{{ dbDays }} 天为 DB 汇总口径（明细上线前，已剔除每日预渲染批次；UV 仅为下界）</span>
          </div>
        </div>
      </template>
      <BaseChart v-if="trendData.length" :option="trendOption" :height="300" />
      <el-empty v-else :description="gran === 'hour' ? '该维度暂无分时明细（只有走明细的日期才有小时数据）' : '暂无数据'" :image-size="80" />
      <div v-if="gran === 'hour' && hourDay" class="hour-note">
        {{ hourDay.date }} 当日合计 <b>PV {{ hourDay.pv }}</b> · <b>UV {{ hourDay.uv }}</b>
        ，与「按天」的点位同一口径；下方 24 个点的小时 UV 之和会偏大，因为同一访客跨小时会各计一次。
      </div>
    </el-card>

    <el-row :gutter="12">
      <!-- 时段分布 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>时段分布（区间内同小时累计）</span>
              <el-radio-group v-model="hourType" size="small">
                <el-radio-button value="bar">柱状</el-radio-button>
                <el-radio-button value="line">折线</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <BaseChart v-if="hourly.length" :option="hourOption" :height="260" />
          <el-empty v-else description="暂无数据" :image-size="70" />
        </el-card>
      </el-col>

      <!-- 来源构成 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>访问来源</span>
              <el-radio-group v-model="sourceType" size="small">
                <el-radio-button value="pie">环形</el-radio-button>
                <el-radio-button value="bar">柱状</el-radio-button>
                <el-radio-button value="line">折线</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <BaseChart v-if="sources.length" :option="sourceOption" :height="260" />
          <el-empty v-else description="暂无数据" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>

    <!-- TOP 页面 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>热门页面 TOP {{ pages.length }}</span>
          <el-radio-group v-model="pageType" size="small">
            <el-radio-button value="bar">横向柱状</el-radio-button>
            <el-radio-button value="pie">环形</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-if="pages.length">
        <BaseChart :option="pageOption" :height="Math.max(240, pages.length * 22 + 40)" />
        <el-table :data="pages" size="small" class="page-table">
          <el-table-column label="#" type="index" width="46" />
          <el-table-column label="页面" prop="path" min-width="220" show-overflow-tooltip />
          <el-table-column label="PV" width="90" align="right">
            <template #default="{ row }">{{ row.pv }}</template>
          </el-table-column>
          <el-table-column label="占比" min-width="150">
            <template #default="{ row }">
              <el-progress :percentage="pctOf(row.pv)" :stroke-width="8" :show-text="false" />
            </template>
          </el-table-column>
          <el-table-column label="UV" width="80" align="right" />
          <el-table-column label="落地次数" width="90" align="right" prop="entries" />
          <el-table-column label="平均停留" width="100" align="right">
            <template #default="{ row }">{{ fmtDur(row.avgDuration) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-card>

    <el-row :gutter="12">
      <!-- 终端画像 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>终端画像</span>
              <div class="ctrl">
                <el-radio-group v-model="profileMetric" size="small">
                  <el-radio-button value="devices">设备</el-radio-button>
                  <el-radio-button value="browsers">浏览器</el-radio-button>
                  <el-radio-button value="systems">系统</el-radio-button>
                </el-radio-group>
                <el-radio-group v-model="profileType" size="small">
                  <el-radio-button value="pie">环形</el-radio-button>
                  <el-radio-button value="bar">柱状</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <BaseChart v-if="profileList.length" :option="profileOption" :height="260" />
          <el-empty v-else description="暂无数据" :image-size="70" />
        </el-card>
      </el-col>

      <!-- 停留时长 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>停留时长分布</span>
              <el-radio-group v-model="durType" size="small">
                <el-radio-button value="bar">柱状</el-radio-button>
                <el-radio-button value="line">折线</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <BaseChart v-if="durations.length" :option="durOption" :height="260" />
          <el-empty v-else description="暂无数据" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 爬虫与自动化：与人的口径完全隔离，只做对照，不混进 PV -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>爬虫与自动化</span>
          <div class="ctrl">
            <el-radio-group v-model="botMetric" size="small">
              <el-radio-button value="names">爬虫标识</el-radio-button>
              <el-radio-button value="kinds">类别</el-radio-button>
              <el-radio-button value="pages">被抓页面</el-radio-button>
              <el-radio-button value="trend">按天趋势</el-radio-button>
            </el-radio-group>
            <el-radio-group v-model="botType" size="small">
              <el-radio-button value="bar">柱状</el-radio-button>
              <el-radio-button value="pie">环形</el-radio-button>
              <el-radio-button value="line">折线</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div class="bot-summary">
        <div class="bot-num">{{ bots.pv || 0 }}<span class="bot-unit">PV</span></div>
        <div class="bot-meta">
          <span>占可见流量 <b>{{ bots.ratio || 0 }}%</b></span>
          <span>{{ (bots.names || []).length }} 种爬虫</span>
          <span>{{ bots.uv || 0 }} 个来源 IP</span>
        </div>
      </div>
      <BaseChart v-if="botList.length" :option="botOption" :height="260" />
      <el-empty v-else description="区间内没有捕捉到爬虫" :image-size="70" />
      <el-table
        v-if="(bots.names || []).length"
        :data="bots.names.slice(0, 10)"
        size="small"
        class="page-table"
      >
        <el-table-column label="爬虫标识" prop="name" min-width="180" />
        <el-table-column label="类别" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ botKindLabel(row.kind) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="PV" width="90" align="right" prop="value" />
        <el-table-column label="占比" min-width="170">
          <template #default="{ row }">
            <el-progress :percentage="row.pct" :stroke-width="8" />
          </template>
        </el-table-column>
      </el-table>
      <div class="bot-note">
        只统计<strong>会执行 JS 的爬虫</strong>（Googlebot、Bingbot、无头浏览器等）——它们会真的跑起埋点，
        所以能被这里看见。不跑 JS 的爬虫（Baiduspider、扫描器、图片抓取）根本不会请求本接口，
        这部分只在 CDN / nginx 访问日志里可见。因此这里的数字是<strong>可见爬虫的下限</strong>，
        不能直接等同于 CDN 账单上的差额。
      </div>
    </el-card>

    <!-- 数据健康 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>数据健康</span>
          <span class="card-hint">
            埋点经服务端有界内存队列 → 单消费者线程写文件；队列满会丢弃并计数
          </span>
        </div>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="今日实时">
          {{ realtime.pv || 0 }} PV / {{ realtime.uv || 0 }} UV
        </el-descriptions-item>
        <el-descriptions-item label="队列积压">
          {{ queue.queueSize || 0 }} / {{ queue.queueCapacity || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="累计接收">{{ queue.received || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已写文件">{{ queue.written || 0 }}</el-descriptions-item>
        <el-descriptions-item label="队列丢弃">
          <span :class="{ bad: (queue.dropped || 0) > 0 }">{{ queue.dropped || 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="非法路径">
          {{ queue.rejected || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="爬虫过滤">{{ health.bot || 0 }}</el-descriptions-item>
        <el-descriptions-item label="内网过滤">{{ health.internal || 0 }}</el-descriptions-item>
        <el-descriptions-item label="消费者线程">
          <el-tag :type="queue.consumerAlive ? 'success' : 'danger'" size="small">
            {{ queue.consumerAlive ? '运行中' : '已停止' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近写入">
          {{ queue.lastWrite || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="明细口径天数">{{ health.logDays || 0 }}</el-descriptions-item>
        <el-descriptions-item label="回退 DB 天数">{{ health.dbFallbackDays || 0 }}</el-descriptions-item>
        <el-descriptions-item label="IP 记录方式">
          {{ queue.ipMode === 'mask' ? '网段脱敏' : '完整 IP' }}
        </el-descriptions-item>
        <el-descriptions-item label="保留天数">{{ queue.retainDays || 0 }}</el-descriptions-item>
        <el-descriptions-item label="明细目录" :span="2">{{ queue.logDir }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="files.length" class="files">
        <span class="card-hint">明细文件：</span>
        <el-tag v-for="f in files.slice(0, 12)" :key="f" size="small" type="info" class="file-tag">
          {{ f }}
        </el-tag>
        <span v-if="files.length > 12" class="card-hint">…共 {{ files.length }} 个</span>
      </div>
      <div v-if="queue.lastError" class="err">最近异常：{{ queue.lastError }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import BaseChart from '../components/BaseChart.vue'
import { getStatReport } from '../api/admin'

const PALETTE = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#f97316', '#64748b']

const days = ref(14)
const loading = ref(false)
const report = ref({})

// 各卡片的展示形态
const gran = ref('day')
const trendType = ref('line')
const hourType = ref('bar')
const sourceType = ref('pie')
const pageType = ref('bar')
const profileMetric = ref('devices')
const profileType = ref('pie')
const durType = ref('bar')
const botMetric = ref('names')
const botType = ref('bar')

/** 后端输出类别 code，展示名统一在前端映射，改文案不用动 Java */
const BOT_KINDS = {
  search: '搜索引擎',
  ai: 'AI 抓取',
  seo: 'SEO/分析',
  monitor: '可用性监控',
  social: '社交预览',
  script: '脚本/客户端',
  automation: '无头浏览器',
  scanner: '安全扫描',
  empty: '空 UA',
  other: '其他'
}

const trend = computed(() => report.value.trend || [])
const hourly = computed(() => report.value.hourly || [])
/** 单日分时：只含有明细的日期，每项带 hours[24] 与当日合计 */
const hourlyDays = computed(() => report.value.hourlyDays || [])
const hourDate = ref('')
const hourDay = computed(
  () => hourlyDays.value.find((d) => d.date === hourDate.value) || hourlyDays.value[hourlyDays.value.length - 1] || null
)
const hourSeries = computed(() => hourDay.value?.hours || [])
// 默认停在最新一天；换统计区间后若原选中日不在新列表里也回落到最新
watch(
  hourlyDays,
  (list) => {
    if (!list.length) {
      hourDate.value = ''
      return
    }
    if (!list.some((d) => d.date === hourDate.value)) hourDate.value = list[list.length - 1].date
  },
  { immediate: true }
)
const pages = computed(() => report.value.pages || [])
const sources = computed(() => report.value.sources || [])
const durations = computed(() => report.value.durations || [])
const summary = computed(() => report.value.summary || {})
const bots = computed(() => report.value.bots || {})
const health = computed(() => report.value.health || {})
const queue = computed(() => report.value.queue || {})
const realtime = computed(() => report.value.realtime || {})
const files = computed(() => report.value.files || [])

const dbDays = computed(() => trend.value.filter((t) => t.src === 'db').length)

// ---------- 格式化 ----------
function fmtNum(n) {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w'
  return String(v)
}
function fmtDur(s) {
  const v = Math.round(Number(s) || 0)
  if (v <= 0) return '—'
  if (v < 60) return v + ' 秒'
  const m = Math.floor(v / 60)
  if (m < 60) return v % 60 ? `${m} 分 ${v % 60} 秒` : `${m} 分`
  return `${Math.floor(m / 60)} 小时 ${m % 60} 分`
}
const shortDay = (s) => String(s || '').slice(5)

const maxPv = computed(() => Math.max(1, ...pages.value.map((p) => p.pv)))
const pctOf = (pv) => Math.round((Number(pv || 0) / maxPv.value) * 1000) / 10

// ---------- 概览卡片 ----------
const summaryCards = computed(() => [
  { label: '浏览量 PV', value: fmtNum(summary.value.pv),
    hint: summary.value.pvLog == null
      ? '不含爬虫与内网'
      : `明细 ${fmtNum(summary.value.pvLog)} + DB 汇总 ${fmtNum(Number(summary.value.pv || 0) - Number(summary.value.pvLog || 0))}` },
  { label: '独立访客 UV', value: fmtNum(summary.value.uv), hint: '按 IP 去重' },
  { label: '会话数', value: fmtNum(summary.value.sessions), hint: '30 分钟无操作即新会话' },
  { label: '平均停留', value: fmtDur(summary.value.avgDuration), hint: `${summary.value.durationSamples || 0} 次采样` },
  { label: '跳出率', value: (summary.value.bounceRate || 0) + '%', hint: `人均 ${summary.value.pagesPerSession || 0} 页` },
  { label: '今日实时', value: fmtNum(realtime.value.pv), hint: `UV ${realtime.value.uv || 0}` }
])

const healthAlert = computed(() => {
  if (queue.value.enabled === false) return '埋点落盘已关闭（page-stat.enabled=false），统计不会产生新数据'
  if (queue.value.consumerAlive === false) return '写盘消费者线程已停止，队列正在积压，请检查服务日志'
  if ((queue.value.dropped || 0) > 0) return `队列已丢弃 ${queue.value.dropped} 条埋点（队列满）。数据不完整，建议排查磁盘写入`
  if (health.value.truncated) return '区间内明细事件超过处理上限，报表已截断，请缩小时间范围'
  if (queue.value.lastError) return '写盘链路最近出现过异常，详见「数据健康」'
  return ''
})

// ---------- ECharts option 构造 ----------
function cartesian(cats, seriesList, opts = {}) {
  return {
    color: PALETTE,
    grid: { left: 8, right: 16, top: seriesList.length > 1 ? 34 : 16, bottom: 6, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: opts.pointer || 'shadow' } },
    legend: seriesList.length > 1
      ? {
          top: 0,
          right: 0,
          icon: 'roundRect',
          itemWidth: 10,
          itemHeight: 10,
          textStyle: { fontSize: 11, color: '#64748b' }
        }
      : { show: false },
    xAxis: {
      type: 'category',
      data: cats,
      boundaryGap: opts.boundaryGap !== false,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 11, hideOverlap: true }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    series: seriesList
  }
}

function mkSeries(name, values, kind, color) {
  if (kind === 'bar') {
    return {
      name,
      type: 'bar',
      barMaxWidth: 22,
      itemStyle: { color, borderRadius: [3, 3, 0, 0] },
      data: values
    }
  }
  return {
    name,
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 5,
    lineStyle: { width: 2, color },
    itemStyle: { color },
    areaStyle: kind === 'area' ? { color, opacity: 0.14 } : undefined,
    data: values
  }
}

function pieOption(list) {
  return {
    color: PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c}（{d}%）' },
    legend: {
      type: 'scroll',
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { fontSize: 11, color: '#64748b' }
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 600 } },
        data: list.map((d) => ({ name: d.name, value: d.value }))
      }
    ]
  }
}

/** 统一的分类型图表：环形 / 柱状 / 折线 三选一 */
function distOption(list, kind) {
  if (kind === 'pie') return pieOption(list)
  const cats = list.map((d) => d.name)
  const vals = list.map((d) => d.value)
  if (kind === 'line') {
    return cartesian(cats, [mkSeries('数量', vals, 'area', PALETTE[0])], { boundaryGap: false })
  }
  return cartesian(cats, [mkSeries('数量', vals, 'bar', PALETTE[0])])
}

// 访问趋势：按天 = 每天一点；按小时 = 选中那一天内部的 24 点（不是跨天叠加）
const trendData = computed(() => (gran.value === 'day' ? trend.value : hourSeries.value))
const trendOption = computed(() => {
  const isHour = gran.value === 'hour'
  const cats = trendData.value.map((d) => (isHour ? d.name : shortDay(d.date)))
  const pv = trendData.value.map((d) => d.pv)
  const uv = trendData.value.map((d) => d.uv)
  return cartesian(
    cats,
    [
      mkSeries('PV', pv, trendType.value, PALETTE[0]),
      mkSeries('UV', uv, trendType.value === 'bar' ? 'bar' : 'line', PALETTE[1])
    ],
    { boundaryGap: trendType.value === 'bar', pointer: trendType.value === 'bar' ? 'shadow' : 'line' }
  )
})

const hourOption = computed(() => {
  const cats = hourly.value.map((d) => d.name)
  return cartesian(cats, [
    mkSeries('PV', hourly.value.map((d) => d.pv), hourType.value, PALETTE[0]),
    mkSeries('UV', hourly.value.map((d) => d.uv), hourType.value, PALETTE[1])
  ])
})

const sourceOption = computed(() => distOption(sources.value, sourceType.value))
const profileList = computed(() => report.value[profileMetric.value] || [])
const profileOption = computed(() => distOption(profileList.value, profileType.value))
const durOption = computed(() => distOption(durations.value, durType.value))

// 爬虫：names / kinds / pages 都是 {name,value,pct} 同形结构，trend 需转成同样的形状
const botKindLabel = (k) => BOT_KINDS[k] || k || '其他'
const botList = computed(() => {
  const b = bots.value
  if (botMetric.value === 'trend') {
    return (b.trend || []).map((t) => ({ name: shortDay(t.date), value: t.pv }))
  }
  const list = b[botMetric.value] || []
  if (botMetric.value === 'kinds') {
    return list.slice(0, 12).map((d) => ({ ...d, name: botKindLabel(d.name) }))
  }
  return list.slice(0, 12)
})
const botOption = computed(() => distOption(botList.value, botType.value))

const pageOption = computed(() => {
  if (pageType.value === 'pie') {
    return pieOption(pages.value.slice(0, 12).map((p) => ({ name: p.path, value: p.pv })))
  }
  // 横向柱状：y 轴类目从下往上排，所以反转一次让 PV 最高的在最上面
  const list = pages.value.slice(0, 15)
  const cats = list.map((p) => p.path).reverse()
  const vals = list.map((p) => p.pv).reverse()
  return {
    color: PALETTE,
    grid: { left: 8, right: 48, top: 10, bottom: 6, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: cats,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#4b5563', fontSize: 11, width: 180, overflow: 'truncate' }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 14,
        itemStyle: { color: PALETTE[0], borderRadius: [0, 3, 3, 0] },
        label: { show: true, position: 'right', fontSize: 10, color: '#6b7280' },
        data: vals
      }
    ]
  }
})

// ---------- 数据加载 ----------
async function reload() {
  loading.value = true
  try {
    const res = await getStatReport(days.value)
    report.value = res.data || {}
  } catch (e) {
    console.error('获取访问报表失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.visit-stat {
  padding: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.gen-at {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.alert-row {
  margin-bottom: 12px;
}

.card-row {
  margin-bottom: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 18px;
  border: 1px solid #e8f5e9;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  height: 100%;
  margin-bottom: 12px;
}
.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #2e7d32;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #8aa093;
  margin-top: 4px;
}
.stat-hint {
  font-size: 11px;
  color: #b6c6bb;
  margin-top: 2px;
}

.chart-card {
  margin-bottom: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.card-hint {
  font-size: 12px;
  color: #a3b8ab;
  font-weight: 400;
}
.ctrl {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
/* 按小时必须选定某一天，否则 14 天会被压成 24 桶 */
.hour-date {
  width: 128px;
}
.hour-note {
  margin-top: 6px;
  font-size: 12px;
  color: #8a9a90;
  line-height: 1.6;
}
.hour-note b {
  color: #0f766e;
}

.page-table {
  margin-top: 8px;
}

/* 爬虫卡片：用琥珀色与「人的数据」的绿色系区分，避免看串 */
.bot-summary {
  display: flex;
  align-items: baseline;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.bot-num {
  font-size: 26px;
  font-weight: 800;
  color: #b45309;
  line-height: 1;
}
.bot-unit {
  font-size: 12px;
  font-weight: 600;
  color: #d97706;
  margin-left: 4px;
}
.bot-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #94a3b8;
}
.bot-meta b {
  color: #b45309;
}
.bot-note {
  margin-top: 10px;
  padding: 8px 12px;
  font-size: 12px;
  line-height: 1.75;
  color: #78716c;
  background: #fffbeb;
  border-left: 3px solid #fcd34d;
  border-radius: 0 6px 6px 0;
}
.bot-note strong {
  color: #b45309;
}

.files {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.file-tag {
  font-family: Consolas, Monaco, monospace;
}
.err {
  margin-top: 8px;
  font-size: 12px;
  color: #f56c6c;
  word-break: break-all;
}
.bad {
  color: #f56c6c;
  font-weight: 700;
}
</style>
