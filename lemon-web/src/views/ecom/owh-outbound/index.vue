<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.outbound.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <OwhFilters :model="query" field="account" @change="search" />
        <OwhFilters :model="query" field="warehouse" @change="search" />
        <a-form-item :label="$t('ecomOwh.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" @change="search" />
        </a-form-item>
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
        <template v-if="selectedKeys.length">
          <a-button v-if="tab === PENDING" v-perm="'ecom:owhOutbound:push'" type="primary" @click="pushSelected">
            <template #icon><CloudUploadOutlined /></template>{{ $t('ecomOwh.outbound.push') }}
          </a-button>
          <a-button v-if="tab === OwhOutboundStatus.FAILED" v-perm="'ecom:owhOutbound:push'" type="primary" @click="repushSelected">
            <template #icon><RedoOutlined /></template>{{ $t('ecomOwh.outbound.repush') }}
          </a-button>
          <a-button v-if="tab !== PENDING" v-perm="'ecom:owhOutbound:sync'" :loading="syncing" @click="syncIds(selectedKeys)">
            <template #icon><SyncOutlined /></template>{{ $t('ecomOwh.outbound.sync') }}
          </a-button>
          <span class="selected-tip">
            {{ $t('common.table.selected', { n: selectedKeys.length }) }} <a @click="selectedKeys = []">{{ $t('common.action.clear') }}</a>
          </span>
        </template>
        <span v-else-if="tab === PENDING && list.length" class="selected-tip">{{ $t('ecomOwh.misc.batchHint') }}</span>
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
          <template v-if="column.key === 'outboundNo'">
            <a @click="detailRef.open(record.id)">{{ record.outboundNo }}</a>
          </template>
          <template v-else-if="column.key === 'packageNo'">
            <a @click="router.push({ path: '/ecom/package', query: { keyword: record.packageNo } })">{{ record.packageNo }}</a>
          </template>
          <template v-else-if="column.key === 'receiver'">
            <div>{{ record.shipName || '-' }}</div>
            <div class="sub-text">{{ record.shipCountry }}</div>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <div>{{ record.warehouseName }}</div>
            <div v-if="record.accountName" class="sub-text">{{ record.accountName }} · {{ record.owhCode }}</div>
          </template>
          <template v-else-if="column.key === 'product'">
            <div>{{ record.productName || record.productCode || '-' }}</div>
            <div v-if="record.productName" class="sub-text">{{ record.productCode }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_owh_outbound_status" :value="record.status" />
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
              <ExclamationCircleOutlined class="error-icon" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'pendingError'">
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
              <a-tag color="red" :bordered="false">{{ $t('ecomOwh.outbound.lastFailed') }}</a-tag>
            </a-tooltip>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'tracking'">
            <div>{{ record.trackingNo || '-' }}</div>
            <div v-if="record.carrierName" class="sub-text">{{ record.carrierName }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'feeBase'">{{ record.feeBase != null ? `${symMoney(record.feeBase)}` : '-' }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="tab === PENDING ? pendingActions(record) : rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <OutboundDetail ref="detailRef" @sync="(d) => syncIds([d.id])" @repush="(d) => pushRef.open([d.packageId], [d.accountId])" @cancel="(d) => cancelRef.open(d)" />
    <PushModal ref="pushRef" @done="afterChange" />
    <CancelModal ref="cancelRef" @done="afterChange" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { CloseCircleOutlined, CloudUploadOutlined, ExclamationCircleOutlined, RedoOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import OwhFilters from '../owh-stock/OwhFilters.vue'
import OutboundDetail from './OutboundDetail.vue'
import PushModal from './PushModal.vue'
import CancelModal from './CancelModal.vue'
import { owhAllMappings, owhOutboundStatusCounts, pageOwhOutbound, pageOwhPending, syncOwhOutbound } from '@/api/ecom/owh'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { OwhOutboundStatus } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { money, qty, symMoney } from '@/utils/number'
import { showBatchResult } from '../order/order'
import { OWH_RESULT_KEYS, canCancelOutbound, canRepush, canSyncOutbound } from './owh'

/**
 * 海外仓出库（一件代发）：待推送（发货仓是海外仓、待打单、没有有效出库单的包裹）→ 推送海外仓 → 已推送 →（同步）已出库，
 * 拿到运单号后包裹自动发货、回传平台 → 已发货；推送失败显示原因、可重推；已推送的可取消 / 截单
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ecom_owh_outbound_status')
const PENDING = 'PENDING'
const STATUS_TABS = [OwhOutboundStatus.FAILED, OwhOutboundStatus.PUSHED, OwhOutboundStatus.INTERCEPTING, OwhOutboundStatus.OUTBOUND, OwhOutboundStatus.SHIPPED, OwhOutboundStatus.CANCELLED]
const TABS = computed(() => [
  { key: PENDING, label: t('ecomOwh.outbound.tabPending') },
  ...STATUS_TABS.map((s) => ({ key: s, label: findDict(dict.ecom_owh_outbound_status, s)?.label || s })),
  { key: 'all', label: t('common.status.all') }
])

const pendingColumns = [
  { title: t('ecomOwh.outbound.packageNo'), key: 'packageNo', dataIndex: 'packageNo', width: 170 },
  { title: t('ecomOwh.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('ecomOwh.outbound.orderNos'), dataIndex: 'orderNos', width: 180, ellipsis: true },
  { title: t('ecomOwh.outbound.receiver'), key: 'receiver', width: 150, ellipsis: true, exportValue: (r) => `${r.shipName || ''} ${r.shipCountry || ''}` },
  { title: t('ecomOwh.common.warehouse'), dataIndex: 'warehouseName', width: 170, ellipsis: true },
  { title: t('ecomOwh.outbound.channel'), dataIndex: 'channelName', width: 140, ellipsis: true },
  { title: t('ecomOwh.outbound.totalQty'), key: 'totalQty', dataIndex: 'totalQty', width: 80, align: 'right' },
  { title: t('ecomOwh.outbound.pushResult'), key: 'pendingError', width: 130 },
  { title: t('ecomOwh.outbound.createTime'), dataIndex: 'createTime', width: 160 },
  actionColumn(1, [t('ecomOwh.outbound.push')])
]
const outboundColumns = [
  { title: t('ecomOwh.outbound.outboundNo'), key: 'outboundNo', dataIndex: 'outboundNo', width: 170 },
  { title: t('ecomOwh.outbound.packageNo'), key: 'packageNo', dataIndex: 'packageNo', width: 160 },
  { title: t('ecomOwh.outbound.orderNos'), dataIndex: 'orderNos', width: 170, ellipsis: true },
  { title: t('ecomOwh.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('ecomOwh.common.warehouse'), key: 'warehouse', width: 190, ellipsis: true, exportValue: (r) => `${r.warehouseName} ${r.owhCode || ''}` },
  { title: t('ecomOwh.outbound.product'), key: 'product', width: 150, exportValue: (r) => r.productCode },
  { title: t('ecomOwh.outbound.owhOrderNo'), dataIndex: 'owhOrderNo', width: 160, ellipsis: true },
  { title: t('ecomOwh.outbound.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_owh_outbound_status' },
  {
    title: t('ecomOwh.outbound.trackingNo'),
    key: 'tracking',
    width: 190,
    ellipsis: true,
    exportColumns: [
      { title: t('ecomOwh.outbound.carrier'), dataIndex: 'carrierName' },
      { title: t('ecomOwh.outbound.trackingNo'), dataIndex: 'trackingNo' }
    ]
  },
  { title: t('ecomOwh.outbound.feeBase'), key: 'feeBase', dataIndex: 'feeBase', width: 110, align: 'right' },
  { title: t('ecomOwh.outbound.pushTime'), dataIndex: 'pushTime', width: 160 },
  { title: t('ecomOwh.outbound.shipTime'), dataIndex: 'shipTime', width: 160 },
  actionColumn(3)
]

const tab = ref(PENDING)
// 待推送（包裹）与出库单两套列，各自记住列设置
const pendingCols = useColumns(pendingColumns, 'ecom-owh-pending')
const outboundCols = useColumns(outboundColumns, 'ecom-owh-outbound')
const cols = () => (tab.value === PENDING ? pendingCols : outboundCols)
const columns = computed(() => (tab.value === PENDING ? pendingColumns : outboundColumns))
const checked = computed({ get: () => cols().checked.value, set: (v) => (cols().checked.value = v) })
const visibleColumns = computed(() => cols().visibleColumns.value)
const scrollX = computed(() => cols().scrollX.value)
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, accountId: undefined, warehouseId: undefined, storeId: undefined })
const query = reactive(emptyQuery())
const fetcher = (params) => (tab.value === PENDING ? pageOwhPending(params) : pageOwhOutbound(params))
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(fetcher, query, {
  params: (q) => ({ ...q, status: tab.value === PENDING || tab.value === 'all' ? undefined : tab.value })
})

const counts = ref({})
const allCount = computed(() => STATUS_TABS.reduce((n, s) => n + Number(counts.value[s] || 0), 0))
async function loadCounts() {
  counts.value = (await owhOutboundStatusCounts({ ...query })) || {}
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
  list.value = []
  doSearch()
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
useRouteOpen({ id: (id) => detailRef.value.open(id), keyword: (kw) => ((query.keyword = kw), (tab.value = 'all')), reload: () => search() })
onMounted(async () => {
  mappings.value = (await owhAllMappings()) || []
  reload()
})

const detailRef = ref()
const pushRef = ref()
const cancelRef = ref()
const mappings = ref([])
const accountOf = (warehouseId) => mappings.value.find((m) => m.warehouseId === warehouseId)?.accountId

function afterChange() {
  selectedKeys.value = []
  reload()
  detailRef.value?.reload()
}

function pushSelected() {
  const rows = list.value.filter((r) => selectedKeys.value.includes(r.id))
  pushRef.value.open(rows.map((r) => r.id), rows.map((r) => accountOf(r.warehouseId)))
}
function repushSelected() {
  const rows = list.value.filter((r) => selectedKeys.value.includes(r.id) && canRepush(r))
  pushRef.value.open(rows.map((r) => r.packageId), rows.map((r) => r.accountId))
}

const syncing = ref(false)
async function syncIds(ids) {
  syncing.value = true
  try {
    showBatchResult(await syncOwhOutbound([...ids]), ids.length, OWH_RESULT_KEYS)
    afterChange()
  } finally {
    syncing.value = false
  }
}

const pendingActions = (record) => [
  { key: 'push', label: t('ecomOwh.outbound.push'), icon: CloudUploadOutlined, perm: 'ecom:owhOutbound:push', onClick: () => pushRef.value.open([record.id], [accountOf(record.warehouseId)]) }
]
const rowActions = (record) => [
  { type: 'view', onClick: () => detailRef.value.open(record.id) },
  { key: 'sync', label: t('ecomOwh.outbound.sync'), icon: SyncOutlined, perm: 'ecom:owhOutbound:sync', show: canSyncOutbound(record), onClick: () => syncIds([record.id]) },
  { key: 'repush', label: t('ecomOwh.outbound.repush'), icon: RedoOutlined, perm: 'ecom:owhOutbound:push', show: canRepush(record), onClick: () => pushRef.value.open([record.packageId], [record.accountId]) },
  { key: 'cancel', label: t('ecomOwh.outbound.cancel'), icon: CloseCircleOutlined, perm: 'ecom:owhOutbound:cancel', show: canCancelOutbound(record), more: true, danger: true, onClick: () => cancelRef.value.open(record) }
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
