<template>
  <svg v-if="points.length > 1" :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="none" class="sparkline" :style="{ height: height + 'px' }">
    <path v-if="area" :d="areaPath" :fill="color" fill-opacity="0.12" />
    <polyline :points="linePoints" fill="none" :stroke="color" stroke-width="1.5" vector-effect="non-scaling-stroke" stroke-linejoin="round" />
    <circle v-if="lastPoint" :cx="lastPoint[0]" :cy="lastPoint[1]" r="2" :fill="color" vector-effect="non-scaling-stroke" />
  </svg>
  <div v-else class="sparkline-empty" :style="{ height: height + 'px' }" />
</template>

<script setup>
import { computed } from 'vue'

/**
 * 迷你趋势图（SVG 折线，可带面积）：用于指标卡、表格单元格。
 * values 中的 null 跳过；invert 为 true 时数值越小越靠上（如排名）
 */
const props = defineProps({
  values: { type: Array, default: () => [] },
  color: { type: String, default: '#1677ff' },
  height: { type: Number, default: 28 },
  area: { type: Boolean, default: true },
  invert: Boolean
})
const W = 100
const H = 30
const points = computed(() => {
  const list = props.values.map((v, i) => [i, v === null || v === undefined || v === '' ? null : Number(v)]).filter((p) => p[1] !== null && !Number.isNaN(p[1]))
  if (list.length < 2) return []
  const xs = list.map((p) => p[0])
  const ys = list.map((p) => p[1])
  const [x0, x1] = [Math.min(...xs), Math.max(...xs)]
  const [y0, y1] = [Math.min(...ys), Math.max(...ys)]
  const sx = (x) => ((x - x0) / (x1 - x0 || 1)) * W
  const sy = (y) => {
    const r = y1 === y0 ? 0.5 : (y - y0) / (y1 - y0)
    return 2 + (props.invert ? r : 1 - r) * (H - 4)
  }
  return list.map(([x, y]) => [sx(x), sy(y)])
})
const linePoints = computed(() => points.value.map((p) => p.join(',')).join(' '))
const areaPath = computed(() => {
  const p = points.value
  if (!p.length) return ''
  return `M${p[0][0]},${H} L${p.map((q) => q.join(',')).join(' L')} L${p[p.length - 1][0]},${H} Z`
})
const lastPoint = computed(() => points.value[points.value.length - 1])
</script>

<style scoped>
.sparkline {
  display: block;
  width: 100%;
  overflow: visible;
}
.sparkline-empty {
  width: 100%;
}
</style>
