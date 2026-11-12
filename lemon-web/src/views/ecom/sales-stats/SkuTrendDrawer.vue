<template>
  <a-drawer :open="open" :title="title" width="min(960px, 100vw)" destroy-on-close @close="open = false">
    <div class="filters">
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
      <span v-if="data" class="sub-text">
        {{ $t('ecomAnalytics.sales.trendSummary', { qty: qty(data.totalQty), amount: money(data.totalAmount), stockout: data.stockoutDays, days: data.snapshotDays }) }}
      </span>
    </div>
    <a-spin :spinning="loading">
      <div v-if="data" class="info">
        <span>{{ data.storeName }}</span>
        <DictTag type="ecom_fulfillment" :value="data.fulfillment" />
        <span v-if="data.materialCode" class="sub-text">{{ data.materialCode }} {{ data.materialName }}</span>
      </div>
      <EChart v-if="data" :option="chartOption" height="380px" />
      <div class="sub-text">{{ $t('ecomAnalytics.sales.trendTip') }}</div>
    </a-spin>
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import EChart from '@/components/charts/EChart.vue'
import DictTag from '@/components/DictTag.vue'
import { skuTrend } from '@/api/ecom/analytics'
import { money, nz, qty } from '@/utils/number'
import { t } from '@/locales'
import { lastDays, rangeParams, rangePresets } from '../product-analysis/analytics'

/**
 * SKU 销量走势：每天的件数（柱）与销售额，叠加每日库存快照（平台仓 / 本地仓 / 可售，右轴），断货日（可售 ≤ 0）用红点标出。
 * open(listingId, range) 打开
 */
const presets = rangePresets()
const open = ref(false)
const loading = ref(false)
const listingId = ref()
const range = ref(lastDays(90))
const data = ref(null)

const title = computed(() => (data.value ? `${t('ecomAnalytics.sales.trendTitle')} · ${data.value.sellerSku}` : t('ecomAnalytics.sales.trendTitle')))

async function load() {
  loading.value = true
  try {
    data.value = await skuTrend({ listingId: listingId.value, ...rangeParams(range.value) })
  } finally {
    loading.value = false
  }
}

function show(id, initialRange) {
  listingId.value = id
  // 走势至少看 30 天
  range.value = initialRange && initialRange[0] < lastDays(30)[0] ? [...initialRange] : lastDays(90)
  data.value = null
  open.value = true
  load()
}

const chartOption = computed(() => {
  const days = data.value.days
  const stock = (k) => days.map((d) => (d[k] == null ? null : nz(d[k])))
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 16, right: 16, top: 40, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: days.map((d) => d.date.slice(5)) },
    yAxis: [{ type: 'value', name: t('ecomAnalytics.common.qty') }, { type: 'value', name: t('ecomAnalytics.sales.stock') }],
    series: [
      { name: t('ecomAnalytics.common.qty'), type: 'bar', barMaxWidth: 16, data: days.map((d) => nz(d.qty)) },
      { name: t('ecomAnalytics.sales.sellable'), type: 'line', yAxisIndex: 1, step: 'end', showSymbol: false, connectNulls: false, data: stock('sellableQty') },
      { name: t('ecomAnalytics.sales.platformStock'), type: 'line', yAxisIndex: 1, showSymbol: false, lineStyle: { type: 'dashed' }, data: stock('platformQty') },
      { name: t('ecomAnalytics.sales.localStock'), type: 'line', yAxisIndex: 1, showSymbol: false, lineStyle: { type: 'dotted' }, data: stock('localQty') },
      {
        name: t('ecomAnalytics.sales.stockoutDay'),
        type: 'scatter',
        yAxisIndex: 1,
        symbolSize: 9,
        itemStyle: { color: '#f5222d' },
        data: days.map((d) => (d.stockout ? 0 : null))
      }
    ]
  }
})

defineExpose({ open: show })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  margin-bottom: 12px;
}
.info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
