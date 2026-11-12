<template>
  <div class="filters">
    <span>
      {{ $t('ecomAnalytics.product.slowDaysPrefix') }}
      <a-input-number v-model:value="days" :min="1" :max="365" :precision="0" class="days" />
      {{ $t('ecomAnalytics.product.slowDaysSuffix') }}
    </span>
    <StoreSelect v-model:value="storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" class="filter" />
    <a-input v-model:value="keyword" allow-clear :placeholder="$t('ecomAnalytics.common.materialKeywordPh')" class="filter" @press-enter="load" />
    <a-button type="primary" :loading="loading" @click="load">
      <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
    </a-button>
    <a-button v-perm="'ecom:productAnalysis:export'" :disabled="!rows.length" @click="doExport">
      <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
    </a-button>
  </div>
  <div class="sub-text tip">{{ $t('ecomAnalytics.product.slowTip') }}</div>

  <div v-if="result" class="summary">
    <div class="stat">
      <div class="sub-text">{{ $t('ecomAnalytics.product.movingRate') }}</div>
      <div class="stat-value">{{ percent(result.movingRatePercent, 1) }}</div>
      <div class="sub-text">{{ $t('ecomAnalytics.product.movingDetail', { moving: result.movingSkus, active: result.activeSkus, days: result.days }) }}</div>
    </div>
    <div class="stat">
      <div class="sub-text">{{ $t('ecomAnalytics.product.slowCount') }}</div>
      <div class="stat-value">{{ num(result.slowCount) }}</div>
    </div>
    <div class="stat stat-warn">
      <div class="sub-text">{{ $t('ecomAnalytics.product.slowAmount') }}</div>
      <div class="stat-value">{{ money(result.slowAmount) }}</div>
    </div>
  </div>

  <a-card v-if="rows.length" :bordered="false" size="small" :title="$t('ecomAnalytics.product.ageChartTitle')" class="chart-card">
    <EChart :option="chartOption" height="220px" />
  </a-card>

  <AnalyticsTable :columns="columns" :rows="rows" :loading="loading" row-key="materialId">
    <template #cell="{ column, record }">
      <template v-if="column.key === 'lastSaleDate'">
        <span v-if="record.lastSaleDate">{{ record.lastSaleDate }}</span>
        <span v-else class="sub-text">{{ $t('ecomAnalytics.product.neverSold') }}</span>
        <div v-if="record.idleDays != null" class="sub-text">{{ $t('ecomAnalytics.product.idleDays', { n: record.idleDays }) }}</div>
      </template>
      <template v-else-if="column.key === 'age'">
        <div>{{ $t('ecomAnalytics.product.oldestAge', { n: record.oldestAgeDays ?? '-' }) }}</div>
        <div class="sub-text">{{ $t('ecomAnalytics.product.avgAge', { n: qty(record.avgAgeDays, 1) }) }}</div>
      </template>
    </template>
  </AnalyticsTable>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { percent } from '@/components/charts/charts'
import { productSlow } from '@/api/ecom/analytics'
import { money, num, nz, qty } from '@/utils/number'
import { t } from '@/locales'
import AnalyticsTable from './AnalyticsTable.vue'
import { col, exportTable, materialCol } from './analytics'

/** 滞销与库龄：有库存但最近 N 天没有销量的物料（全部仓库库存 × 移动加权成本），库龄分段；在售 SKU 的动销率 */
const AGE_KEYS = ['age0to30', 'age31to90', 'age91to180', 'age181to365', 'ageOver365']
const days = ref(30)
const storeId = ref()
const keyword = ref('')
const result = ref(null)
const loading = ref(false)
const rows = computed(() => result.value?.rows || [])

const columns = [
  materialCol(220),
  col('materialSpec', t('ecomAnalytics.common.spec'), 'text', { width: 120, ellipsis: true }),
  col('skuCount', t('ecomAnalytics.product.skuCountCol'), 'int'),
  col('stockQty', t('ecomAnalytics.common.stockQty'), 'qty'),
  col('unitCost', t('ecomAnalytics.product.unitCost'), 'money'),
  col('stockAmount', t('ecomAnalytics.product.stockAmount'), 'money'),
  col('lastSaleDate', t('ecomAnalytics.product.lastSale'), 'text', { width: 130, custom: true }),
  col('age', t('ecomAnalytics.product.age'), 'text', { width: 140, custom: true, exportValue: (r) => r.oldestAgeDays }),
  ...AGE_KEYS.map((k) => col(k, t(`ecomAnalytics.product.${k}`), 'qty'))
]

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await productSlow({ days: days.value || 30, storeId: storeId.value || undefined, keyword: keyword.value?.trim() || undefined })
    if (current === seq) result.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}

/** 各库龄段的滞销数量 */
const chartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 16, right: 16, top: 16, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: AGE_KEYS.map((k) => t(`ecomAnalytics.product.${k}`)) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', barMaxWidth: 40, data: AGE_KEYS.map((k) => rows.value.reduce((s, r) => s + nz(r[k]), 0)) }]
}))

async function doExport() {
  await exportTable(`${t('ecomAnalytics.product.tabSlow')}_${days.value}`, columns, rows.value)
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 200px;
}
.days {
  width: 80px;
  margin: 0 4px;
}
.tip {
  margin: 8px 0 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.stat-warn .stat-value {
  color: var(--status-warning, #d46b08);
}
.stat-value {
  margin: 4px 0;
  font-size: 18px;
  font-weight: 600;
}
.chart-card {
  margin-bottom: 16px;
  border: 1px solid var(--app-border);
}
</style>
