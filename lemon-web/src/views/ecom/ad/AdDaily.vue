<template>
  <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
    <a-button v-perm="'ecom:ad:import'" type="primary" @click="importOpen = true">
      <template #icon><ImportOutlined /></template>{{ $t('ecomReport.ad.import') }}
    </a-button>
    <a-button v-if="selectedKeys.length" v-perm="'ecom:ad:remove'" danger @click="removeSelected">
      <template #icon><DeleteOutlined /></template>{{ $t('ecomReport.ad.deleteN', { n: selectedKeys.length }) }}
    </a-button>
    <a-tooltip :title="$t('ecomReport.ad.importTip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
  </TableToolbar>
  <a-table
    row-key="id"
    :size="size"
    :columns="visibleColumns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    :row-selection="rowSelection"
    :scroll="{ x: scrollX + 32, y: '100%' }"
    @change="onTableChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.kind === 'money'">{{ money(record[column.key]) }}</template>
      <template v-else-if="column.kind === 'int'">{{ num(record[column.key]) }}</template>
      <template v-else-if="column.key === 'action'">
        <TableActions :actions="rowActions(record)" />
      </template>
    </template>
  </a-table>
  <ImportModal
    v-model:open="importOpen"
    :title="$t('ecomReport.ad.importTitle')"
    :columns="importColumns"
    :sample="importSample"
    :importer="importAds"
    @success="load"
  />
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { DeleteOutlined, ImportOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import ImportModal from '@/components/ImportModal.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { deleteAd, importAds, pageAd } from '@/api/ecom/ad'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { money, num } from '@/utils/number'
import { t } from '@/locales'

/**
 * 广告日报明细页签：分页列表（GET /ecom/ad）、导入广告报告、删除。
 * 根节点为工具栏与表格（没有外层 div），表格才能占满页签的剩余高度
 */
const props = defineProps({
  params: { type: Object, required: true },
  active: Boolean
})

const columns = [
  { title: t('ecomReport.ad.adDate'), key: 'adDate', dataIndex: 'adDate', width: 110 },
  { title: t('ecomReport.common.store'), key: 'storeName', dataIndex: 'storeName', width: 150, ellipsis: true },
  { title: t('ecomReport.common.campaign'), key: 'campaign', dataIndex: 'campaign', width: 200, ellipsis: true },
  { title: t('ecomReport.common.sellerSku'), key: 'sellerSku', dataIndex: 'sellerSku', width: 140 },
  { title: t('ecomReport.common.platformItemId'), key: 'platformItemId', dataIndex: 'platformItemId', width: 140 },
  { title: t('ecomReport.ad.impressions'), key: 'impressions', dataIndex: 'impressions', kind: 'int', width: 100, align: 'right' },
  { title: t('ecomReport.ad.clicks'), key: 'clicks', dataIndex: 'clicks', kind: 'int', width: 90, align: 'right' },
  { title: t('ecomReport.ad.spend'), key: 'spend', dataIndex: 'spend', kind: 'money', width: 110, align: 'right' },
  { title: t('ecomReport.common.currency'), key: 'currency', dataIndex: 'currency', width: 80 },
  { title: t('ecomReport.ad.adSales'), key: 'adSales', dataIndex: 'adSales', kind: 'money', width: 120, align: 'right' },
  { title: t('ecomReport.ad.adOrders'), key: 'adOrders', dataIndex: 'adOrders', kind: 'int', width: 100, align: 'right' },
  actionColumn(1, [t('common.action.delete')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-ad-daily')

const query = reactive({ storeId: undefined, beginDate: undefined, endDate: undefined, keyword: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageAd, query)

/** 页签可见且条件变化时按新条件查询 */
let loaded = null
watch(
  () => [props.params, props.active],
  () => {
    if (props.active && loaded !== props.params) {
      loaded = props.params
      Object.assign(query, { storeId: undefined, beginDate: undefined, endDate: undefined, keyword: undefined }, props.params)
      search()
    }
  },
  { immediate: true }
)

const remove = useAction(deleteAd, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const removeSelected = useAction(() => deleteAd(selectedKeys.value), {
  confirm: () => t('ecomReport.ad.confirmDeleteN', { n: selectedKeys.value.length }),
  successKey: 'common.msg.deleteSuccess',
  reload: () => {
    selectedKeys.value = []
    return load()
  }
})

const rowActions = (record) => [
  { type: 'delete', perm: 'ecom:ad:remove', confirm: t('ecomReport.ad.confirmDelete'), onClick: () => remove(record.id) }
]

// ---------------------------------------------------------------- 导入（列与后端 AdImportRow 一致）
const importOpen = ref(false)
const importColumns = [
  { title: t('ecomReport.ad.storeCode'), key: 'storeCode', required: true, width: 12 },
  { title: t('ecomReport.ad.adDate'), key: 'adDate', required: true, type: 'date', width: 14 },
  { title: t('ecomReport.common.campaign'), key: 'campaign', width: 24 },
  { title: t('ecomReport.common.sellerSku'), key: 'sellerSku', width: 16 },
  { title: t('ecomReport.common.platformItemId'), key: 'platformItemId', width: 16 },
  { title: t('ecomReport.ad.impressions'), key: 'impressions', type: 'int', width: 10 },
  { title: t('ecomReport.ad.clicks'), key: 'clicks', type: 'int', width: 10 },
  { title: t('ecomReport.ad.spend'), key: 'spend', required: true, type: 'number', width: 12 },
  { title: t('ecomReport.common.currency'), key: 'currency', width: 8 },
  { title: t('ecomReport.ad.adSales'), key: 'adSales', type: 'number', width: 12 },
  { title: t('ecomReport.ad.adOrders'), key: 'adOrders', type: 'int', width: 10 }
]
const importSample = {
  storeCode: 'US01',
  adDate: '2026-10-01',
  campaign: t('ecomReport.ad.sampleCampaign'),
  sellerSku: 'SKU-001',
  platformItemId: 'B000000001',
  impressions: '1200',
  clicks: '36',
  spend: '18.50',
  currency: 'USD',
  adSales: '129.90',
  adOrders: '5'
}
</script>

<style scoped>
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
</style>
