<template>
  <div class="tab-body">
    <div class="filters">
      <a-select v-model:value="query.eventType" allow-clear :placeholder="$t('ecomFba.ledger.eventType')" :options="EVENT_TYPES.map((v) => ({ value: v, label: v }))" class="w180" @change="search" />
      <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" @change="search" />
      <a-input-search v-model:value="query.keyword" :placeholder="$t('ecomFba.ledger.keywordPh')" allow-clear class="w240" @search="search" />
    </div>
    <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
      <span class="sub-text toolbar-tip">{{ $t('ecomFba.ledger.reasonTip') }}</span>
    </TableToolbar>
    <a-table
      row-key="id"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'quantity'">
          <span :class="Number(record.quantity) < 0 ? 'neg' : 'pos'">{{ n0(record.quantity) }}</span>
        </template>
        <template v-else-if="column.key === 'reason'">
          <a-tag v-if="record.reason" :color="REASON_COLORS[record.reason]" :bordered="false">{{ record.reason }}</a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, watch } from 'vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { pageLedger } from '@/api/ecom/fba'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { t } from '@/locales'
import { n0 } from '../fba-health/fba'
import { qty } from '@/utils/number'

/** 库存流水（库存分类账明细报告） */
const props = defineProps({ storeId: Number })
/** 亚马逊库存分类账的事件类型（报告原值） */
const EVENT_TYPES = ['Receipts', 'Shipments', 'CustomerReturns', 'VendorReturns', 'WhseTransfers', 'Adjustments']
const REASON_COLORS = { M: 'red', 5: 'red', F: 'green', E: 'volcano', 6: 'volcano', 7: 'volcano' }

const columns = [
  { title: t('ecomFba.ledger.eventDate'), dataIndex: 'eventDate', width: 110 },
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('ecomFba.common.sku'), dataIndex: 'sellerSku', width: 160 },
  { title: t('ecomFba.common.fnsku'), dataIndex: 'fnsku', width: 120 },
  { title: t('ecomFba.ledger.eventType'), dataIndex: 'eventType', width: 130 },
  { title: t('ecomFba.ledger.referenceId'), dataIndex: 'referenceId', width: 170, ellipsis: true },
  { title: t('ecomFba.ledger.quantity'), key: 'quantity', dataIndex: 'quantity', width: 80, align: 'right' },
  { title: t('ecomFba.ledger.fc'), dataIndex: 'fulfillmentCenter', width: 90 },
  { title: t('ecomFba.ledger.disposition'), dataIndex: 'disposition', width: 130 },
  { title: t('ecomFba.ledger.reason'), key: 'reason', dataIndex: 'reason', width: 70 },
  { title: t('ecomFba.ledger.unreconciled'), dataIndex: 'unreconciledQty', width: 80, align: 'right', customRender: ({ text }) => qty(text) }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-ledger')
const query = reactive({ eventType: undefined, range: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageLedger, query, {
  params: (p) => ({ storeId: props.storeId || undefined, eventType: p.eventType, keyword: p.keyword, beginDate: p.range?.[0], endDate: p.range?.[1] })
})
onMounted(load)
watch(() => props.storeId, () => search())
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.w180 {
  width: 180px;
}
.w240 {
  width: 240px;
  max-width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.toolbar-tip {
  align-self: center;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.pos {
  color: var(--status-success, #389e0d);
}
</style>
