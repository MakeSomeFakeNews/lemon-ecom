<template>
  <div class="tab-body">
    <div class="filters">
      <a-select v-model:value="query.matched" allow-clear :placeholder="$t('ecomFba.reimb.matched')" :options="matchedOptions" class="w140" @change="search" />
      <a-input-search v-model:value="query.keyword" :placeholder="$t('ecomFba.reimb.keywordPh')" allow-clear class="w260" @search="search" />
    </div>
    <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load" />
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
        <template v-if="column.key === 'amountTotal'">{{ money(record.amountTotal) }} <span class="sub-text">{{ record.currency }}</span></template>
        <template v-else-if="column.key === 'amountBase'">{{ money(record.amountBase) }}</template>
        <template v-else-if="column.key === 'claimNo'">
          <a-tag v-if="record.claimNo" color="green" :bordered="false">{{ record.claimNo }}</a-tag>
          <a-tag v-else :bordered="false">{{ $t('ecomFba.reimb.matchedNo') }}</a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, watch } from 'vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { pageReimbursement } from '@/api/ecom/fba'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/number'
import { t } from '@/locales'

/** 赔偿记录（赔偿报告），显示自动匹配到的索赔；ref 调用 reload() */
const props = defineProps({ storeId: Number })
const matchedOptions = [
  { value: 'true', label: t('ecomFba.reimb.matchedYes') },
  { value: 'false', label: t('ecomFba.reimb.matchedNo') }
]
const columns = [
  { title: t('ecomFba.reimb.approvalDate'), dataIndex: 'approvalDate', width: 110 },
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('ecomFba.reimb.reimbursementId'), dataIndex: 'reimbursementId', width: 140 },
  { title: t('ecomFba.reimb.caseId'), dataIndex: 'caseId', width: 130 },
  { title: t('ecomFba.reimb.orderId'), dataIndex: 'amazonOrderId', width: 170 },
  { title: t('ecomFba.reimb.reason'), dataIndex: 'reason', width: 150 },
  { title: t('ecomFba.common.sku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomFba.common.fnsku'), dataIndex: 'fnsku', width: 120 },
  { title: t('ecomFba.reimb.amountPerUnit'), dataIndex: 'amountPerUnit', width: 90, align: 'right' },
  { title: t('ecomFba.reimb.amountTotal'), key: 'amountTotal', dataIndex: 'amountTotal', width: 120, align: 'right' },
  { title: t('ecomFba.reimb.qtyCash'), dataIndex: 'qtyCash', width: 90, align: 'right' },
  { title: t('ecomFba.reimb.qtyInventory'), dataIndex: 'qtyInventory', width: 110, align: 'right' },
  { title: t('ecomFba.reimb.amountBase'), key: 'amountBase', dataIndex: 'amountBase', width: 110, align: 'right' },
  { title: t('ecomFba.reimb.claimNo'), key: 'claimNo', dataIndex: 'claimNo', width: 150 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-reimbursement')
const query = reactive({ matched: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageReimbursement, query, {
  params: (p) => ({ storeId: props.storeId || undefined, matched: p.matched, keyword: p.keyword })
})
onMounted(load)
watch(() => props.storeId, () => search())

defineExpose({ reload: () => load() })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.w140 {
  width: 140px;
}
.w260 {
  width: 260px;
  max-width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
