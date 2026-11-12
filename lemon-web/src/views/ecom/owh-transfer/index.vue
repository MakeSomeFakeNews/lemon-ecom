<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.tr.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.tr.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" size="small" class="status-tabs" @change="onTab">
        <a-tab-pane v-for="tb in TABS" :key="tb.key">
          <template #tab>{{ tb.label }}<span v-if="tb.key !== 'all'" class="tab-count">{{ counts[tb.key] || 0 }}</span></template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:owhTransfer:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOwh.tr.add') }}
        </a-button>
        <a-button v-perm="'ecom:owhTransfer:sync'" :disabled="!selectedKeys.length" :loading="syncing" @click="syncIds(selectedKeys)">
          <template #icon><SyncOutlined /></template>{{ $t('ecomOwh.tr.sync') }}
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
          <template v-if="column.key === 'transferNo'">
            <a @click="detailRef.open(record.id)">{{ record.transferNo }}</a>
          </template>
          <template v-else-if="column.key === 'route'">
            <div>{{ record.fromWarehouseName }} → {{ record.toWarehouseName }}</div>
            <div class="sub-text">{{ record.fromOwhCode || $t('ecomOwh.tr.local') }} → {{ record.toOwhCode || $t('ecomOwh.tr.local') }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_owh_transfer_status" :value="record.status" />
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg"><ExclamationCircleOutlined class="error-icon" /></a-tooltip>
          </template>
          <template v-else-if="column.key === 'inbound'">
            <template v-if="record.inboundNo">
              <div>{{ record.inboundNo }}</div>
              <DictTag type="ecom_owh_inbound_status" :value="record.inboundStatus" />
            </template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <TransferForm ref="formRef" @success="afterChange" />
    <ReceiveModal ref="receiveRef" @done="afterChange" />
    <TransferDetail
      ref="detailRef"
      @edit="(d) => formRef.openEdit(d)"
      @submit="(d) => submit(d)"
      @sync="(d) => syncIds([d.id])"
      @receive="(d) => receive(d)"
      @cancel="(d) => cancelOne(d)"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CheckOutlined, CloseCircleOutlined, ExclamationCircleOutlined, PlusOutlined, SendOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import TransferForm from './TransferForm.vue'
import TransferDetail from './TransferDetail.vue'
import ReceiveModal from './ReceiveModal.vue'
import { cancelOwhTransfer, owhTransferStatusCounts, pageOwhTransfer, submitOwhTransfer, syncOwhTransfer } from '@/api/ecom/owh'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { OwhTransferStatus } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { qty } from '@/utils/number'
import { showBatchResult } from '../order/order'
import { OWH_RESULT_KEYS, canCancelTransfer, canReceiveTransfer, canSyncTransfer } from '../owh-outbound/owh'

/**
 * 海外仓调拨：海外仓之间、海外仓 ↔ 本地仓。提交 → 调出仓处理中（调出海外仓推送调拨出库单）→ 出库后 ERP 在途调拨单（调入海外仓生成入库单）
 * → 收货（海外仓入库单收货完成 / 本地仓确认收货）→ 已收货
 */
const { t } = useI18n()
const dict = useDict('ecom_owh_transfer_status')
const STATUS_TABS = [OwhTransferStatus.DRAFT, OwhTransferStatus.OUT_PUSHED, OwhTransferStatus.IN_TRANSIT, OwhTransferStatus.RECEIVED, OwhTransferStatus.CANCELLED]
const TABS = computed(() => [
  ...STATUS_TABS.map((s) => ({ key: s, label: findDict(dict.ecom_owh_transfer_status, s)?.label || s })),
  { key: 'all', label: t('common.status.all') }
])

const columns = [
  { title: t('ecomOwh.tr.transferNo'), key: 'transferNo', dataIndex: 'transferNo', width: 170 },
  { title: t('ecomOwh.tr.route'), key: 'route', width: 260, exportValue: (r) => `${r.fromWarehouseName} -> ${r.toWarehouseName}` },
  { title: t('ecomOwh.tr.qty'), key: 'totalQty', dataIndex: 'totalQty', width: 80, align: 'right' },
  { title: t('ecomOwh.tr.status'), key: 'status', dataIndex: 'status', width: 120, dict: 'ecom_owh_transfer_status' },
  { title: t('ecomOwh.tr.owhOutNo'), dataIndex: 'owhOutNo', width: 150 },
  { title: t('ecomOwh.tr.transferDoc'), dataIndex: 'transferDocNo', width: 150 },
  { title: t('ecomOwh.tr.inbound'), key: 'inbound', width: 150, exportValue: (r) => r.inboundNo || '' },
  { title: t('ecomOwh.tr.trackingNo'), dataIndex: 'trackingNo', width: 150 },
  { title: t('ecomOwh.tr.shipTime'), dataIndex: 'shipTime', width: 160 },
  { title: t('ecomOwh.tr.receiveTime'), dataIndex: 'receiveTime', width: 160 },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-owh-transfer')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, warehouseId: undefined })
const query = reactive(emptyQuery())
const tab = ref('all')
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageOwhTransfer, query, {
  params: (q) => ({ ...q, status: tab.value === 'all' ? undefined : tab.value })
})
const counts = ref({})
async function loadCounts() {
  counts.value = (await owhTransferStatusCounts({ ...query })) || {}
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

const formRef = ref()
const detailRef = ref()
function afterChange() {
  selectedKeys.value = []
  reload()
  detailRef.value?.reload()
}
const submit = useAction((d) => submitOwhTransfer(d.id), {
  confirm: (d) => ({ title: t('ecomOwh.tr.submitConfirm', { no: d.transferNo }) }),
  successKey: 'ecomOwh.tr.submitted',
  reload: () => afterChange()
})
const receiveRef = ref()
const receive = (d) => receiveRef.value.open(d)
async function cancelOne(d) {
  const st = await cancelOwhTransfer(d.id)
  message.success(st === OwhTransferStatus.CANCELLED ? t('ecomOwh.tr.cancelled') : t('ecomOwh.tr.intercepting'))
  afterChange()
}
const syncing = ref(false)
async function syncIds(ids) {
  syncing.value = true
  try {
    const res = await syncOwhTransfer([...ids])
    const named = Object.fromEntries(Object.entries(res || {}).map(([id, msg]) => [list.value.find((r) => String(r.id) === id)?.transferNo || id, msg]))
    showBatchResult(named, ids.length, OWH_RESULT_KEYS)
    afterChange()
  } finally {
    syncing.value = false
  }
}

const rowActions = (record) => [
  { type: 'view', onClick: () => detailRef.value.open(record.id) },
  { key: 'submit', label: t('ecomOwh.tr.submit'), icon: SendOutlined, perm: 'ecom:owhTransfer:push', show: record.status === OwhTransferStatus.DRAFT, onClick: () => submit(record) },
  { key: 'sync', label: t('ecomOwh.tr.sync'), icon: SyncOutlined, perm: 'ecom:owhTransfer:sync', show: canSyncTransfer(record), onClick: () => syncIds([record.id]) },
  { key: 'receive', label: t('ecomOwh.tr.receive'), icon: CheckOutlined, perm: 'ecom:owhTransfer:receive', show: canReceiveTransfer(record), onClick: () => receive(record) },
  {
    key: 'cancel',
    label: t('ecomOwh.tr.cancel'),
    icon: CloseCircleOutlined,
    perm: 'ecom:owhTransfer:cancel',
    show: canCancelTransfer(record),
    more: true,
    danger: true,
    confirm: { title: t('ecomOwh.tr.cancelConfirm', { no: record.transferNo }), okType: 'danger' },
    onClick: () => cancelOne(record)
  }
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
