<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.ret.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <OwhFilters :model="query" />
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" size="small" class="status-tabs" @change="onTab">
        <a-tab-pane v-for="tb in TABS" :key="tb.key">
          <template #tab>{{ tb.label }}<span v-if="tb.key !== 'all'" class="tab-count">{{ counts[tb.key] || 0 }}</span></template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:owhReturn:add'" type="primary" @click="createRef.open()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOwh.ret.create') }}
        </a-button>
        <a-button v-perm="'ecom:owhReturn:sync'" :disabled="!selectedKeys.length" :loading="syncing" @click="syncIds(selectedKeys)">
          <template #icon><SyncOutlined /></template>{{ $t('ecomOwh.ret.sync') }}
        </a-button>
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
          <template v-if="column.key === 'returnNo'">
            <a @click="detailRef.open(record.id)">{{ record.returnNo }}</a>
          </template>
          <template v-else-if="column.key === 'source'">
            <div>{{ record.ecomReturnNo }}</div>
            <div class="sub-text">{{ record.platformOrderNo }}</div>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <div>{{ record.warehouseName }}</div>
            <div class="sub-text">{{ record.accountName }} · {{ record.owhCode }}</div>
          </template>
          <template v-else-if="column.key === 'result'">{{ qty(record.goodQty) }} / {{ qty(record.badQty) }} / {{ qty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_owh_return_status" :value="record.status" />
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg"><ExclamationCircleOutlined class="error-icon" /></a-tooltip>
          </template>
          <template v-else-if="column.key === 'erpReceived'">
            <a-tag :bordered="false" :color="record.erpReceived === 1 ? 'green' : 'default'">
              {{ record.erpReceived === 1 ? $t('ecomOwh.ret.erpReceived') : $t('ecomOwh.ret.erpPending') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <ReturnCreateModal ref="createRef" @created="(id) => (reload(), detailRef.open(id))" />
    <ReturnDetail ref="detailRef" @push="(d) => push(d)" @sync="(d) => syncIds([d.id])" @cancel="(d) => cancelOne(d)" @void="(d) => voidOne(d)" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CloseCircleOutlined, CloudUploadOutlined, ExclamationCircleOutlined, PlusOutlined, StopOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import OwhFilters from '../owh-stock/OwhFilters.vue'
import ReturnCreateModal from './ReturnCreateModal.vue'
import ReturnDetail from './ReturnDetail.vue'
import { cancelOwhReturn, owhReturnStatusCounts, pageOwhReturn, pushOwhReturn, syncOwhReturn, voidOwhReturn } from '@/api/ecom/owh'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { OwhReturnStatus } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { qty } from '@/utils/number'
import { showBatchResult } from '../order/order'
import { OWH_RESULT_KEYS, canSyncReturn, isReturnDraft, isReturnPushed, useOwhCaps } from '../owh-outbound/owh'

/**
 * 海外仓退件：退回仓是海外仓的退货退款单 → 生成退件单 → 推送 → 同步收货质检结果 → ERP 退货入库（不良品转不良品仓或报废）
 */
const { t } = useI18n()
const dict = useDict('ecom_owh_return_status')
const STATUS_TABS = [OwhReturnStatus.DRAFT, OwhReturnStatus.PUSHED, OwhReturnStatus.RECEIVED, OwhReturnStatus.CANCELLED]
const TABS = computed(() => [
  ...STATUS_TABS.map((s) => ({ key: s, label: findDict(dict.ecom_owh_return_status, s)?.label || s })),
  { key: 'all', label: t('common.status.all') }
])

const columns = [
  { title: t('ecomOwh.ret.returnNo'), key: 'returnNo', dataIndex: 'returnNo', width: 170 },
  { title: t('ecomOwh.ret.source'), key: 'source', width: 170, exportValue: (r) => `${r.ecomReturnNo} ${r.platformOrderNo || ''}` },
  { title: t('ecomOwh.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('ecomOwh.common.warehouse'), key: 'warehouse', width: 180, exportValue: (r) => `${r.warehouseName} ${r.owhCode || ''}` },
  { title: t('ecomOwh.ret.owhOrderNo'), dataIndex: 'owhOrderNo', width: 150 },
  { title: t('ecomOwh.ret.trackingNo'), dataIndex: 'trackingNo', width: 150 },
  { title: t('ecomOwh.ret.resultCol'), key: 'result', width: 140, exportValue: (r) => `${r.goodQty}/${r.badQty}/${r.totalQty}` },
  { title: t('ecomOwh.ret.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_owh_return_status' },
  { title: t('ecomOwh.ret.erp'), key: 'erpReceived', width: 100, exportValue: (r) => (r.erpReceived === 1 ? t('ecomOwh.ret.erpReceived') : t('ecomOwh.ret.erpPending')) },
  { title: t('ecomOwh.ret.receiveTime'), dataIndex: 'receiveTime', width: 160 },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-owh-return')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, accountId: undefined, warehouseId: undefined })
const query = reactive(emptyQuery())
const tab = ref('all')
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageOwhReturn, query, {
  params: (q) => ({ ...q, status: tab.value === 'all' ? undefined : tab.value })
})
const counts = ref({})
async function loadCounts() {
  counts.value = (await owhReturnStatusCounts({ ...query })) || {}
}
function reload() {
  load()
  loadCounts()
}
function search() {
  doSearch()
  loadCounts()
}
function onTab() {
  selectedKeys.value = []
  doSearch()
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
useRouteOpen({ id: (id) => detailRef.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(reload)

const createRef = ref()
const detailRef = ref()
function afterChange() {
  selectedKeys.value = []
  reload()
  detailRef.value?.reload()
}
const push = useAction((d) => pushOwhReturn(d.id), {
  confirm: (d) => ({ title: t('ecomOwh.ret.pushConfirm', { no: d.returnNo }) }),
  successKey: 'ecomOwh.ret.pushed',
  reload: () => afterChange()
})
const cancelOne = useAction((d) => cancelOwhReturn(d.id), {
  confirm: (d) => ({ title: t('ecomOwh.ret.cancelConfirm', { no: d.returnNo }), okType: 'danger' }),
  successKey: 'ecomOwh.ret.cancelled',
  reload: () => afterChange()
})
const hasCap = useOwhCaps()
const voidOne = useAction((d) => voidOwhReturn(d.id), {
  confirm: (d) => ({ title: t('ecomOwh.common.voidConfirm', { no: d.returnNo }), content: t('ecomOwh.common.voidContent'), okType: 'danger' }),
  successKey: 'ecomOwh.common.voided',
  reload: () => afterChange()
})
const syncing = ref(false)
async function syncIds(ids) {
  syncing.value = true
  try {
    const res = await syncOwhReturn([...ids])
    const named = Object.fromEntries(Object.entries(res || {}).map(([id, msg]) => [list.value.find((r) => String(r.id) === id)?.returnNo || id, msg]))
    showBatchResult(named, ids.length, OWH_RESULT_KEYS)
    afterChange()
  } finally {
    syncing.value = false
  }
}

const rowActions = (record) => [
  { type: 'view', onClick: () => detailRef.value.open(record.id) },
  { key: 'push', label: t('ecomOwh.ret.push'), icon: CloudUploadOutlined, perm: 'ecom:owhReturn:push', show: isReturnDraft(record), onClick: () => push(record) },
  { key: 'sync', label: t('ecomOwh.ret.sync'), icon: SyncOutlined, perm: 'ecom:owhReturn:sync', show: canSyncReturn(record), onClick: () => syncIds([record.id]) },
  { key: 'cancel', label: t('ecomOwh.ret.cancel'), icon: CloseCircleOutlined, perm: 'ecom:owhReturn:cancel', more: true, danger: true,
    show: isReturnDraft(record) || (isReturnPushed(record) && hasCap(record.provider, 'CANCEL_RETURN')), onClick: () => cancelOne(record) },
  { key: 'void', label: t('ecomOwh.common.markVoided'), icon: StopOutlined, perm: 'ecom:owhReturn:cancel', more: true, danger: true,
    show: isReturnPushed(record) && !hasCap(record.provider, 'CANCEL_RETURN'), onClick: () => voidOne(record) }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.status-tabs {
  margin-top: -8px;
}
.status-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.tab-count {
  margin-left: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
.error-icon {
  margin-left: 4px;
  color: var(--ant-color-warning, #faad14);
  cursor: help;
}
</style>
