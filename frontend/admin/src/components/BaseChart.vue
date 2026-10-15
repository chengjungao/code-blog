<template>
  <div ref="el" class="base-chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
/**
 * ECharts 包装组件（按需引入）。
 *
 * 只注册实际用到的图表类型与组件，避免把 1MB 的 echarts 全量打进 admin 产物。
 * 用 ResizeObserver 而不是 window.resize：卡片在栅格里换行/侧边栏收起时
 * 窗口尺寸并不变，靠 window resize 会漏掉这类容器变化，图表就被拉伸变形。
 */
import { ref, shallowRef, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkLineComponent,
  MarkAreaComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkLineComponent,
  MarkAreaComponent,
  CanvasRenderer
])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 300 }
})

const el = ref(null)
const chart = shallowRef(null)
let ro = null

function resize() {
  if (chart.value) {
    chart.value.resize()
  }
}

onMounted(() => {
  chart.value = echarts.init(el.value)
  chart.value.setOption(props.option, true)
  if (typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(resize)
    ro.observe(el.value)
  } else {
    window.addEventListener('resize', resize)
  }
})

onBeforeUnmount(() => {
  if (ro) {
    ro.disconnect()
    ro = null
  }
  window.removeEventListener('resize', resize)
  if (chart.value) {
    chart.value.dispose()
    chart.value = null
  }
})

// notMerge=true：切换图形类型时结构会变（折线 <-> 环形），
// 合并模式会留下上一份 option 的残留系列，必须整体替换。
watch(
  () => props.option,
  (opt) => {
    if (chart.value && opt) {
      chart.value.setOption(opt, true)
    }
  },
  { deep: true }
)
</script>

<style scoped>
.base-chart {
  width: 100%;
}
</style>
