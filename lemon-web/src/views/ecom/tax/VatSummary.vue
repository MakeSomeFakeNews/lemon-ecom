<template>
  <a-card :bordered="false" class="page-card">
    <div class="filters">
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
      <StoreSelect v-model:value="storeId" :placeholder="$t('ecomReport.common.allStores')" class="filter" />
      <a-button type="primary" :loading="loading" @click="load">
        <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
      </a-button>
      <a-button :disabled="!rows.length" :loading="exporting" @click="doExport">
        <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
      </a-button>
    </div>
    <div class="sub-text tip">{{ $t('ecomProfit.tax.vatTip') }}</div>
    <a-table
      :row-key="(r) => `${r.country}|${r.period}|${r.taxType}|${r.facilitator}`"
      size="middle"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX }"
      @change="(p) => applyPageChange(pagination, p)"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'taxType'">
          <DictTag v-if="record.taxType" type="ecom_tax_type" :value="record.taxType" />
          <span v-else class="sub-text">{{ $t('ecomProfit.tax.inputOnly') }}</span>
        </template>
        <template v-else-if="column.key === 'facilitator'">
          <a-tag v-if="record.facilitator === 1" color="purple">{{ $t('ecomProfit.tax.facilitatorYes') }}</a-tag>
          <a-tag v-else-if="record.taxType" color="blue">{{ $t('ecomProfit.tax.facilitatorNo') }}</a-tag>
        </template>
        <template v-else-if="column.kind === 'fc'">
          <template v-if="record[column.key] != null">{{ money(record[column.key]) }} <span class="sub-text">{{ record.currency }}</span></template>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.kind === 'money'">
          <span :class="{ neg: nz(record[column.key]) < 0, strong: column.key === 'payable' }">{{ money(record[column.key]) }}</span>
        </template>
      </template>
      <template v-if="rows.length" #summary>
        <a-table-summary fixed>
          <a-table-summary-row class="total-row">
            <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
              <template v-if="i === 0">{{ $t('ecomReport.common.total') }}</template>
              <template v-else-if="c.kind === 'money'">{{ money(total[c.key]) }}</template>
              <template v-else-if="c.key === 'orders'">{{ total.orders }}</template>
            </a-table-summary-cell>
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
  </a-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { applyPageChange, exportRows, localPagination, rangeParams, rangePresets, sumFields, thisMonth } from '@/components/ecom/ecomReport'
import { vatSummary } from '@/api/ecom/tax'
import { sumWidth } from '@/composables/useColumns'
import { money, nz } from '@/utils/number'
import { t } from '@/locales'

/**
 * VAT 汇总：按收件国家 × 月份列出应申报销售额（不含税、扣除退款）、销项税、平台代扣，以及头程货件的关税、进口增值税（按店铺站点、发运月份）；
 * 应缴 = 销项税 − 进口增值税（本位币）。原币金额只在同一行只有一个币种时显示
 */
const presets = rangePresets()
const range = ref(thisMonth())
const storeId = ref()
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive(localPagination(20))

const fc = (key, title) => ({ title, key, dataIndex: key, kind: 'fc', width: 140, align: 'right' })
const base = (key, title) => ({ title, key, dataIndex: key, kind: 'money', width: 130, align: 'right' })
const columns = [
  { title: t('ecomProfit.tax.country'), key: 'country', dataIndex: 'country', width: 90 },
  { title: t('ecomProfit.col.month'), key: 'month', dataIndex: 'month', width: 90 },
  { title: t('ecomProfit.tax.taxType'), key: 'taxType', dataIndex: 'taxType', width: 150, dict: 'ecom_tax_type' },
  {
    title: t('ecomProfit.tax.facilitator'),
    key: 'facilitator',
    width: 120,
    exportValue: (r) => (r.facilitator === 1 ? t('ecomProfit.tax.facilitatorYes') : r.taxType ? t('ecomProfit.tax.facilitatorNo') : '')
  },
  { title: t('ecomReport.common.orders'), key: 'orders', dataIndex: 'orders', kind: 'int', width: 80, align: 'right' },
  { title: t('ecomReport.common.currency'), key: 'currency', dataIndex: 'currency', width: 70 },
  fc('taxableFc', t('ecomProfit.tax.taxableFc')),
  fc('vatFc', t('ecomProfit.tax.vatFc')),
  fc('withheldFc', t('ecomProfit.tax.withheldFc')),
  base('taxableBase', t('ecomProfit.tax.taxableBase')),
  base('vatBase', t('ecomProfit.tax.vatBase')),
  base('withheldBase', t('ecomProfit.tax.withheldBase')),
  base('duty', t('ecomProfit.tax.duty')),
  base('importVat', t('ecomProfit.tax.importVat')),
  base('payable', t('ecomProfit.tax.payable'))
]
const scrollX = sumWidth(columns)
const total = computed(() => sumFields(rows.value, [...columns.filter((c) => c.kind === 'money').map((c) => c.key), 'orders']))

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await vatSummary({ ...rangeParams(range.value), storeId: storeId.value || undefined })
    if (current !== seq) return
    rows.value = res || []
    pagination.current = 1
  } finally {
    if (current === seq) loading.value = false
  }
}

async function doExport() {
  exporting.value = true
  try {
    const cols = columns.map((c) => (c.kind === 'fc' ? { ...c, kind: 'money' } : c))
    await exportRows(`${t('ecomProfit.tax.vatSummary')}_${range.value[0]}_${range.value[1]}`, cols, rows.value, { ...total.value, country: t('ecomReport.common.total') })
  } finally {
    exporting.value = false
  }
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
  width: 220px;
}
.tip {
  margin: 8px 0 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.strong {
  font-weight: 600;
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
