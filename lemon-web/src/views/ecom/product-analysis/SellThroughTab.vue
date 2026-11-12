<template>
  <div class="filters">
    <a-radio-group v-model:value="view" button-style="solid" @change="load">
      <a-radio-button value="sellThrough">{{ $t('ecomAnalytics.product.viewSellThrough') }}</a-radio-button>
      <a-radio-button value="stockout">{{ $t('ecomAnalytics.product.viewStockout') }}</a-radio-button>
      <a-radio-button value="newProducts">{{ $t('ecomAnalytics.product.viewNew') }}</a-radio-button>
    </a-radio-group>
    <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
    <StoreSelect v-model:value="storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" class="filter" />
    <a-input v-model:value="keyword" allow-clear :placeholder="keywordPh" class="filter" @press-enter="load" />
    <a-button type="primary" :loading="loading" @click="load">
      <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
    </a-button>
    <a-button v-perm="'ecom:productAnalysis:export'" :disabled="!rows.length" @click="doExport">
      <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
    </a-button>
  </div>
  <div class="sub-text tip">{{ $t(`ecomAnalytics.product.${view}Tip`) }}</div>

  <AnalyticsTable :columns="columns" :rows="rows" :loading="loading" :row-key="rowKey">
    <template #cell="{ column, record }">
      <template v-if="column.key === 'stockoutDays'">
        <span :class="{ warn: record.stockoutDays > 0 }">{{ record.stockoutDays ?? '-' }}</span>
      </template>
      <template v-else-if="column.key === 'sellThroughPercent'">
        <a-progress
          v-if="record.sellThroughPercent != null"
          :percent="Math.min(100, Number(record.sellThroughPercent))"
          :format="() => percent(record.sellThroughPercent, 1)"
          size="small"
        />
        <span v-else class="sub-text">-</span>
      </template>
    </template>
  </AnalyticsTable>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { percent } from '@/components/charts/charts'
import { productNew, productSellThrough, productStockout } from '@/api/ecom/analytics'
import { t } from '@/locales'
import AnalyticsTable from './AnalyticsTable.vue'
import { col, exportTable, lastDays, materialCol, rangeParams, rangePresets, skuCol, storeCol } from './analytics'

/**
 * 售罄与周转：物料的售罄率（期间销量 ÷ (期初 + 期间入库)）、库存周转天数、可售天数；SKU 的断货天数（每日快照）；
 * 新品表现（首单日起 30 天）
 */
const presets = rangePresets()
const view = ref('sellThrough')
const range = ref(lastDays(30))
const storeId = ref()
const keyword = ref('')
const rows = ref([])
const loading = ref(false)

const VIEW_TITLES = {
  sellThrough: 'ecomAnalytics.product.viewSellThrough',
  stockout: 'ecomAnalytics.product.viewStockout',
  newProducts: 'ecomAnalytics.product.viewNew'
}
const API = { sellThrough: productSellThrough, stockout: productStockout, newProducts: productNew }
const COLUMNS = {
  sellThrough: [
    materialCol(220),
    col('skuCount', t('ecomAnalytics.product.skuCountCol'), 'int'),
    col('openingQty', t('ecomAnalytics.product.openingQty'), 'qty'),
    col('inboundQty', t('ecomAnalytics.product.inboundQty'), 'qty'),
    col('salesQty', t('ecomAnalytics.product.salesQty'), 'qty'),
    col('closingQty', t('ecomAnalytics.product.closingQty'), 'qty'),
    col('sellThroughPercent', t('ecomAnalytics.product.sellThrough'), 'percent', { width: 150, custom: true }),
    col('dailySales', t('ecomAnalytics.common.dailySales'), 'qty'),
    col('turnoverDays', t('ecomAnalytics.product.turnoverDays'), 'days'),
    col('stockQty', t('ecomAnalytics.common.stockQty'), 'qty'),
    col('coverDays', t('ecomAnalytics.product.coverDays'), 'days'),
    col('stockAmount', t('ecomAnalytics.product.stockAmount'), 'money')
  ],
  stockout: [
    storeCol(),
    skuCol(),
    col('fulfillment', t('ecomAnalytics.common.fulfillment'), 'dict', { dict: 'ecom_fulfillment' }),
    materialCol(),
    col('snapshotDays', t('ecomAnalytics.product.snapshotDays'), 'int'),
    col('stockoutDays', t('ecomAnalytics.product.stockoutDays'), 'int', { custom: true }),
    col('stockoutPercent', t('ecomAnalytics.product.stockoutPercent'), 'percent'),
    col('lastStockoutDate', t('ecomAnalytics.product.lastStockout'), 'text', { width: 120 })
  ],
  newProducts: [
    storeCol(),
    skuCol(),
    materialCol(),
    col('firstOrderDate', t('ecomAnalytics.product.firstOrderDate'), 'text', { width: 120 }),
    col('observedDays', t('ecomAnalytics.product.observedDays'), 'int'),
    col('orders30', t('ecomAnalytics.product.orders30'), 'int'),
    col('qty30', t('ecomAnalytics.product.qty30'), 'qty'),
    col('sales30', t('ecomAnalytics.product.sales30'), 'money'),
    col('dailyQty', t('ecomAnalytics.common.dailySales'), 'qty'),
    col('stockoutDays30', t('ecomAnalytics.product.stockoutDays30'), 'int')
  ]
}
const columns = computed(() => COLUMNS[view.value])
const rowKey = (r) => (view.value === 'sellThrough' ? String(r.materialId) : view.value === 'stockout' ? String(r.listingId) : `${r.storeId}|${r.sellerSku}`)
const keywordPh = computed(() => (view.value === 'sellThrough' ? t('ecomAnalytics.common.materialKeywordPh') : t('ecomAnalytics.common.keywordPh')))

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await API[view.value]({ ...rangeParams(range.value), storeId: storeId.value || undefined, keyword: keyword.value?.trim() || undefined })
    if (current === seq) rows.value = res || []
  } finally {
    if (current === seq) loading.value = false
  }
}

async function doExport() {
  await exportTable(`${t(VIEW_TITLES[view.value])}_${range.value[0]}_${range.value[1]}`, columns.value, rows.value)
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
.warn {
  color: var(--status-critical, #cf1322);
  font-weight: 600;
}
</style>
