<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.inbound.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <OwhFilters :model="query" field="account" @change="search" />
        <OwhFilters :model="query" field="warehouse" @change="search" />
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" size="small" class="status-tabs" @change="onTab">
        <a-tab-pane v-for="tb in TABS" :key="tb.key">
          <template #tab>
            {{ tb.label }}<span class="tab-count">{{ tb.key === 'all' ? allCount : counts[tb.key] || 0 }}</span>
          </template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:owhInbound:add'" type="primary" @click="createRef.open()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOwh.inbound.create') }}
        </a-button>
        <template v-if="selectedKeys.length">
          <a-button v-perm="'ecom:owhInbound:sync'" :loading="syncing" @click="syncIds(selectedKeys)">
            <template #icon><SyncOutlined /></template>{{ $t('ecomOwh.inbound.sync') }}
          </a-button>
          <span v-if="selectedKeys.length" class="selected-tip">
            {{ $t('common.table.selected', { n: selectedKeys.length }) }} <a @click="selectedKeys = []">{{ $t('common.action.clear') }}</a>
          </span>
        </template>
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
          <template v-if="column.key === 'inboundNo'">
            <a @click="detailRef.open(record.id)">{{ record.inboundNo }}</a>
          </template>
          <template v-else-if="column.key === 'source'">
            <DictTag type="ecom_owh_inbound_source" :value="record.sourceType" />
            <div class="sub-text">{{ record.sourceNo }}</div>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <div>{{ record.warehouseName }}</div>
            <div class="sub-text">{{ record.accountName }} · {{ record.owhCode }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <span>{{ qty(record.receivedQty) }} / {{ qty(record.totalQty) }}</span>
            <a-progress :percent="percent(record)" size="small" :show-info="false" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_owh_inbound_status" :value="record.status" />
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
              <ExclamationCircleOutlined class="error-icon" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'erpReceived'">
            <a-tag :bordered="false" :color="record.erpReceived === 1 ? 'green' : 'default'">
              {{ record.erpReceived === 1 ? $t('ecomOwh.inbound.erpReceived') : $t('ecomOwh.inbound.erpTransit') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <InboundCreateModal ref="createRef" @created="(id) => (reload(), detailRef.open(id))" />
    <InboundDetail ref="detailRef" @push="(d) => push(d)" @sync="(d) => syncIds([d.id])" @cancel="(d) => cancelOne(d)" @void="(d) => voidOne(d)" @saved="reload" />
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
import InboundCreateModal from './InboundCreateModal.vue'
import InboundDetail from './InboundDetail.vue'
import { cancelOwhInbound, owhInboundStatusCounts, pageOwhInbound, pushOwhInbound, syncOwhInbound, voidOwhInbound } from '@/api/ecom/owh'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { OwhInboundStatus } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { qty } from '@/utils/number'
import { showBatchResult } from '../order/order'
import { OWH_RESULT_KEYS, canSyncInbound, isInboundDraft, isInboundPushedOnly, useOwhCaps } from '../owh-outbound/owh'

/**
 * 海外仓入库单（头程到海外仓的 ASN）：从头程货件 / 在途调拨单生成 → 修改装箱 → 推送 → 同步收货数量与状态 →
 * 收货完成后 ERP 签收来源单据（在途转可用）
 */
const { t } = useI18n()
const dict = useDict('ecom_owh_inbound_status')
const STATUS_TABS = [OwhInboundStatus.DRAFT, OwhInboundStatus.PUSHED, OwhInboundStatus.RECEIVING, OwhInboundStatus.RECEIVED, OwhInboundStatus.CANCELLED]
const TABS = computed(() => [{ key: 'all', label: t('common.status.all') }, ...STATUS_TABS.map((s) => ({ key: s, label: findDict(dict.ecom_owh_inbound_status, s)?.label || s }))])

const columns = [
  { title: t('ecomOwh.inbound.inboundNo'), key: 'inboundNo', dataIndex: 'inboundNo', width: 170 },
  { title: t('ecomOwh.inbound.source'), key: 'source', width: 150, exportColumns: [{ title: t('ecomOwh.inbound.sourceType'), dataIndex: 'sourceType', dict: 'ecom_owh_inbound_source' }, { title: t('ecomOwh.inbound.sourceNo'), dataIndex: 'sourceNo' }] },
  { title: t('ecomOwh.common.warehouse'), key: 'warehouse', width: 200, ellipsis: true, exportValue: (r) => `${r.warehouseName} ${r.owhCode || ''}` },
  { title: t('ecomOwh.inbound.owhOrderNo'), dataIndex: 'owhOrderNo', width: 150, ellipsis: true },
  { title: t('ecomOwh.inbound.boxCount'), dataIndex: 'boxCount', width: 80, align: 'right' },
  { title: t('ecomOwh.inbound.qtyReceived'), key: 'qty', width: 140, exportValue: (r) => `${r.receivedQty}/${r.totalQty}` },
  { title: t('ecomOwh.inbound.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_owh_inbound_status' },
  { title: t('ecomOwh.inbound.erp'), key: 'erpReceived', width: 100, exportValue: (r) => (r.erpReceived === 1 ? t('ecomOwh.inbound.erpReceived') : t('ecomOwh.inbound.erpTransit')) },
  { title: t('ecomOwh.inbound.trackingNo'), dataIndex: 'trackingNo', width: 160, ellipsis: true },
  { title: t('ecomOwh.inbound.etaDate'), dataIndex: 'etaDate', width: 110 },
  { title: t('ecomOwh.inbound.pushTime'), dataIndex: 'pushTime', width: 160 },
  { title: t('ecomOwh.inbound.receiveTime'), dataIndex: 'receiveTime', width: 160 },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-owh-inbound')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, accountId: undefined, warehouseId: undefined })
const query = reactive(emptyQuery())
const tab = ref('all')
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageOwhInbound, query, {
  params: (q) => ({ ...q, status: tab.value === 'all' ? undefined : tab.value })
})
const counts = ref({})
const allCount = computed(() => STATUS_TABS.reduce((n, s) => n + Number(counts.value[s] || 0), 0))
async function loadCounts() {
  counts.value = (await owhInboundStatusCounts({ ...query })) || {}
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

const percent = (r) => (Number(r.totalQty) > 0 ? Math.min(100, Math.round((Number(r.receivedQty) * 100) / Number(r.totalQty))) : 0)

const createRef = ref()
const detailRef = ref()
function afterChange() {
  selectedKeys.value = []
  reload()
  detailRef.value?.reload()
}

const push = useAction((d) => pushOwhInbound(d.id), {
  confirm: (d) => ({ title: t('ecomOwh.inbound.pushConfirm', { no: d.inboundNo }) }),
  successKey: 'ecomOwh.inbound.pushed',
  reload: () => afterChange()
})
const cancelOne = useAction((d) => cancelOwhInbound(d.id), {
  confirm: (d) => ({ title: t('ecomOwh.inbound.cancelConfirm', { no: d.inboundNo }), okType: 'danger' }),
  successKey: 'ecomOwh.inbound.cancelled',
  reload: () => afterChange()
})
const hasCap = useOwhCaps()
const voidOne = useAction((d) => voidOwhInbound(d.id), {
  confirm: (d) => ({ title: t('ecomOwh.common.voidConfirm', { no: d.inboundNo }), content: t('ecomOwh.common.voidContent'), okType: 'danger' }),
  successKey: 'ecomOwh.common.voided',
  reload: () => afterChange()
})
const syncing = ref(false)
async function syncIds(ids) {
  syncing.value = true
  try {
    const res = await syncOwhInbound([...ids])
    // 失败结果的键为入库单 ID，换成单号显示
    const named = Object.fromEntries(Object.entries(res || {}).map(([id, msg]) => [list.value.find((r) => String(r.id) === id)?.inboundNo || id, msg]))
    showBatchResult(named, ids.length, OWH_RESULT_KEYS)
    afterChange()
  } finally {
    syncing.value = false
  }
}

const rowActions = (record) => [
  { type: 'view', onClick: () => detailRef.value.open(record.id) },
  { key: 'push', label: t('ecomOwh.inbound.push'), icon: CloudUploadOutlined, perm: 'ecom:owhInbound:push', show: isInboundDraft(record), onClick: () => push(record) },
  { key: 'sync', label: t('ecomOwh.inbound.sync'), icon: SyncOutlined, perm: 'ecom:owhInbound:sync', show: canSyncInbound(record), onClick: () => syncIds([record.id]) },
  { key: 'cancel', label: t('ecomOwh.inbound.cancel'), icon: CloseCircleOutlined, perm: 'ecom:owhInbound:cancel', more: true, danger: true,
    show: isInboundDraft(record) || (isInboundPushedOnly(record) && hasCap(record.provider, 'CANCEL_INBOUND')), onClick: () => cancelOne(record) },
  { key: 'void', label: t('ecomOwh.common.markVoided'), icon: StopOutlined, perm: 'ecom:owhInbound:cancel', more: true, danger: true,
    show: isInboundPushedOnly(record) && !hasCap(record.provider, 'CANCEL_INBOUND'), onClick: () => voidOne(record) }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.selected-tip {
  align-self: center;
  color: var(--app-text-3);
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
