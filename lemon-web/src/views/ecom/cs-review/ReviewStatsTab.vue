<template>
  <div class="stats-body">
    <div class="filters">
      <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomCs.common.allStores')" class="filter" />
      <a-input v-model:value="query.sellerSku" allow-clear :placeholder="$t('ecomCs.review.sellerSku')" class="filter" @press-enter="load" />
      <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
      <a-button type="primary" :loading="loading" @click="load">
        <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
      </a-button>
    </div>

    <a-row :gutter="[16, 16]" class="cards">
      <a-col v-for="c in cards" :key="c.key" :xs="12" :sm="8" :lg="4">
        <div class="card">
          <div class="label">{{ c.label }}</div>
          <div class="value" :class="c.cls">{{ c.value }}</div>
        </div>
      </a-col>
    </a-row>

    <div class="section-title">{{ $t('ecomCs.stats.trend') }}</div>
    <EChart :option="trendOption" height="300px" />

    <div class="section-title">{{ $t('ecomCs.stats.bySku') }}</div>
    <a-table row-key="key" size="small" :columns="skuColumns" :data-source="skuRows" :loading="loading" :pagination="skuPagination" :scroll="{ x: 900 }" @change="(p) => applyPageChange(skuPagination, p)">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sellerSku'">
          <div>{{ record.sellerSku || $t('ecomCs.stats.noSku') }}</div>
          <div v-if="record.title" class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
        </template>
        <template v-else-if="column.key === 'avgRating'">
          <a-rate :value="Number(record.avgRating) || 0" disabled allow-half class="rate" />
          <span class="avg">{{ record.avgRating ?? '-' }}</span>
        </template>
        <template v-else-if="column.key === 'lowRatePercent'">
          <span :class="{ danger: Number(record.lowRatePercent) >= 20 }">{{ pct(record.lowRatePercent) }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { applyPageChange, lastDays, localPagination, rangeParams, rangePresets } from '@/components/ecom/ecomReport'
import { csReviewSkuStats, csReviewSummary, csReviewTrend } from '@/api/ecom/cs'

/**
 * 评价统计（默认最近 30 天）：概况（评价数、平均分、差评数与差评率、待处理、已回复）、按天的评价数与平均分趋势、按 SKU 的平均分与差评率（差评率 ≥ 20% 标红）
 */
const { t } = useI18n()
const presets = rangePresets()
const query = reactive({ storeId: undefined, sellerSku: '', range: lastDays(30) })
const loading = ref(false)
const summary = ref({})
const trend = ref([])
const skuRows = ref([])
const skuPagination = reactive(localPagination(10))

const pct = (v) => (v == null ? '-' : `${Number(v).toFixed(2)}%`)
const cards = computed(() => [
  { key: 'total', label: t('ecomCs.stats.total'), value: summary.value.total ?? 0 },
  { key: 'avg', label: t('ecomCs.stats.avgRating'), value: summary.value.avgRating ?? '-' },
  { key: 'low', label: t('ecomCs.stats.lowCount'), value: summary.value.lowCount ?? 0, cls: summary.value.lowCount ? 'danger' : '' },
  { key: 'lowRate', label: t('ecomCs.stats.lowRate'), value: pct(summary.value.lowRatePercent), cls: Number(summary.value.lowRatePercent) >= 20 ? 'danger' : '' },
  { key: 'pending', label: t('ecomCs.stats.pending'), value: summary.value.pendingCount ?? 0, cls: summary.value.pendingCount ? 'warn' : '' },
  { key: 'replied', label: t('ecomCs.stats.replied'), value: summary.value.repliedCount ?? 0 }
])

const skuColumns = [
  { title: t('ecomCs.common.store'), dataIndex: 'storeName', width: 140 },
  { title: t('ecomCs.review.sku'), key: 'sellerSku', width: 240 },
  { title: t('ecomCs.stats.reviewCount'), dataIndex: 'reviewCount', width: 100, align: 'right' },
  { title: t('ecomCs.stats.avgRating'), key: 'avgRating', width: 200 },
  { title: t('ecomCs.stats.lowCount'), dataIndex: 'lowCount', width: 90, align: 'right' },
  { title: t('ecomCs.stats.lowRate'), key: 'lowRatePercent', width: 100, align: 'right' },
  { title: t('ecomCs.stats.lastDate'), dataIndex: 'lastDate', width: 120 }
]

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: trend.value.map((r) => dayjs(r.date).format('MM-DD')) },
  yAxis: [
    { type: 'value', minInterval: 1 },
    { type: 'value', min: 0, max: 5, interval: 1, splitLine: { show: false } }
  ],
  series: [
    { name: t('ecomCs.stats.reviewCount'), type: 'bar', barMaxWidth: 18, itemStyle: { color: '#91caff' }, data: trend.value.map((r) => r.reviewCount) },
    { name: t('ecomCs.stats.lowCount'), type: 'bar', barMaxWidth: 18, itemStyle: { color: '#ff7875' }, data: trend.value.map((r) => r.lowCount) },
    { name: t('ecomCs.stats.avgRating'), type: 'line', yAxisIndex: 1, smooth: true, connectNulls: true, itemStyle: { color: '#faad14' }, data: trend.value.map((r) => r.avgRating) }
  ]
}))

async function load() {
  const params = { storeId: query.storeId || undefined, sellerSku: query.sellerSku?.trim() || undefined, ...rangeParams(query.range) }
  loading.value = true
  try {
    const [s, tr, sku] = await Promise.all([csReviewSummary(params), csReviewTrend(params), csReviewSkuStats(params)])
    summary.value = s || {}
    trend.value = tr || []
    skuRows.value = (sku || []).map((r) => ({ ...r, key: `${r.storeId}-${r.sellerSku}` }))
    skuPagination.current = 1
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stats-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 16px;
}
.filter {
  width: 200px;
}
.cards {
  margin-bottom: 8px;
}
.card {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-bg);
}
.label {
  color: var(--app-text-3);
  font-size: 12px;
}
.value {
  font-size: 22px;
  font-weight: 600;
  margin-top: 4px;
}
.danger {
  color: #ff4d4f;
}
.warn {
  color: #fa8c16;
}
.section-title {
  font-weight: 600;
  margin: 16px 0 8px;
}
.rate {
  font-size: 13px;
}
.avg {
  margin-left: 6px;
}
.ellipsis {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
