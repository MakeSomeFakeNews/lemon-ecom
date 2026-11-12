<template>
  <div class="filters">
    <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
    <StoreSelect v-model:value="storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" class="filter" />
    <a-input v-model:value="keyword" allow-clear :placeholder="$t('ecomAnalytics.common.keywordPh')" class="filter" @press-enter="load" />
    <a-checkbox v-model:checked="onlyReturned">{{ $t('ecomAnalytics.product.onlyReturned') }}</a-checkbox>
    <a-button type="primary" :loading="loading" @click="load">
      <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
    </a-button>
    <a-button v-perm="'ecom:productAnalysis:export'" :disabled="!rows.length" @click="doExport">
      <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
    </a-button>
  </div>
  <div class="sub-text tip">{{ $t('ecomAnalytics.product.returnTip') }}</div>

  <div v-if="result" class="overview">
    <div class="summary">
      <div class="stat">
        <div class="sub-text">{{ $t('ecomAnalytics.product.soldQty') }}</div>
        <div class="stat-value">{{ qty(result.soldQty) }}</div>
      </div>
      <div class="stat">
        <div class="sub-text">{{ $t('ecomAnalytics.product.returnQty') }}</div>
        <div class="stat-value">{{ qty(result.returnQty) }}</div>
      </div>
      <div class="stat stat-warn">
        <div class="sub-text">{{ $t('ecomAnalytics.product.returnRate') }}</div>
        <div class="stat-value">{{ percent(result.returnRatePercent, 2) }}</div>
      </div>
    </div>
    <a-card v-if="result.reasons.length" :bordered="false" size="small" :title="$t('ecomAnalytics.product.reasonChartTitle')" class="chart-card">
      <EChart :option="chartOption" height="220px" />
    </a-card>
  </div>

  <AnalyticsTable :columns="columns" :rows="rows" :loading="loading" :row-key="(r) => `${r.storeId}|${r.sellerSku}`">
    <template #cell="{ column, record }">
      <template v-if="column.key === 'returnRatePercent'">
        <span :class="{ warn: Number(record.returnRatePercent) >= 10 }">{{ percent(record.returnRatePercent, 2) }}</span>
      </template>
      <template v-else-if="column.key === 'topReason'">
        <DictTag v-if="record.topReason" type="ecom_return_reason" :value="record.topReason" />
        <span v-else class="sub-text">-</span>
      </template>
      <template v-else-if="column.key === 'reasons'">{{ reasonText(record) }}</template>
    </template>
  </AnalyticsTable>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { percent } from '@/components/charts/charts'
import { productReturnRate } from '@/api/ecom/analytics'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { nz, qty } from '@/utils/number'
import { t } from '@/locales'
import AnalyticsTable from './AnalyticsTable.vue'
import { col, exportTable, lastDays, materialCol, rangeParams, rangePresets, skuCol, storeCol } from './analytics'

/** 退货率排行：期间内下单订单的退回件数 ÷ 销量（退货退款单不含已关闭的），按原因分布 */
const dict = useDict('ecom_return_reason')
const presets = rangePresets()
const range = ref(lastDays(90))
const storeId = ref()
const keyword = ref('')
const onlyReturned = ref(true)
const result = ref(null)
const loading = ref(false)
const rows = computed(() => (result.value?.rows || []).filter((r) => !onlyReturned.value || nz(r.returnQty) > 0))

const reasonLabel = (v) => (v ? findDict(dict.ecom_return_reason, v)?.label || v : t('ecomAnalytics.product.noReason'))
const reasonText = (r) => Object.entries(r.reasons || {}).map(([k, v]) => `${reasonLabel(k)} ${qty(v)}`).join('; ') || '-'

const columns = [
  storeCol(),
  skuCol(),
  materialCol(),
  col('soldQty', t('ecomAnalytics.product.soldQty'), 'qty'),
  col('returnQty', t('ecomAnalytics.product.returnQty'), 'qty'),
  col('returnRatePercent', t('ecomAnalytics.product.returnRate'), 'percent', { custom: true }),
  col('refund', t('ecomAnalytics.product.refund'), 'money'),
  col('topReason', t('ecomAnalytics.product.topReason'), 'dict', { dict: 'ecom_return_reason', width: 130, custom: true }),
  col('reasons', t('ecomAnalytics.product.reasons'), 'text', { width: 240, custom: true, exportValue: (r) => reasonText(r) })
]

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await productReturnRate({ ...rangeParams(range.value), storeId: storeId.value || undefined, keyword: keyword.value?.trim() || undefined })
    if (current === seq) result.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}

const chartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      data: (result.value?.reasons || []).map((r) => ({ name: reasonLabel(r.reason), value: nz(r.qty) }))
    }
  ]
}))

async function doExport() {
  await exportTable(`${t('ecomAnalytics.product.tabReturns')}_${range.value[0]}_${range.value[1]}`, columns, rows.value)
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
.tip {
  margin: 8px 0 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.overview {
  margin-bottom: 16px;
}
.summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.stat-value {
  margin: 4px 0;
  font-size: 18px;
  font-weight: 600;
}
.stat-warn .stat-value,
.warn {
  color: var(--status-critical, #cf1322);
}
.chart-card {
  border: 1px solid var(--app-border);
}
</style>
