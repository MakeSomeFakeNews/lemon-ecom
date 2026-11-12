<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.order.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" @update:value="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.orderDate')" name="range" data-wide>
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.platform')" name="platform">
          <a-select v-model:value="query.platform" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_platform)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_order_status)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.fulfillment')" name="fulfillment">
          <a-select v-model:value="query.fulfillment" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_fulfillment)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.country')" name="country">
          <a-input v-model:value="query.country" :placeholder="$t('ecomOrder.order.countryPh')" allow-clear :maxlength="8" @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrd.order.tag')" name="tagId">
          <a-select v-model:value="query.tagId" :placeholder="$t('common.placeholder.all')" allow-clear :options="tagOptions" option-filter-prop="label" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrd.order.orderType')" name="orderType">
          <a-select v-model:value="query.orderType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_order_type)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrd.order.addressInvalid')" name="addressInvalid">
          <a-switch v-model:checked="query.addressInvalid" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.overdueOnly')" name="overdue">
          <a-switch v-model:checked="query.overdue" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.ops.urgencySort')" name="urgency">
          <a-switch v-model:checked="query.urgency" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="status-tabs" @change="onTab">
        <a-tab-pane v-for="tb in ORDER_TABS" :key="tb.key">
          <template #tab>
            {{ $t(tb.label) }}<span class="tab-count" :class="`tab-count-${tb.key}`">{{ tabCount(tb) }}</span>
          </template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <BatchActionBar v-if="selectedKeys.length" :count="selectedKeys.length" :text="$t('ecomOrdUi.batch.selected', { n: selectedKeys.length })" :actions="batchActions" @clear="selectedKeys = []" />
        <template v-else>
          <a-button v-if="tab === 'all' || tab === 'pending'" v-perm="'ecom:order:add'" type="primary" @click="manualDrawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('ecomOrd.manual.add') }}
          </a-button>
          <a-button v-if="tab === 'all' || tab === 'pending'" v-perm="'ecom:order:import'" @click="importModal.open()">
            <template #icon><UploadOutlined /></template>{{ $t('ecomOrder.order.import') }}
          </a-button>
          <a-button v-if="tab === 'toLabel' || tab === 'toShip'" v-perm="'ecom:package:list'" type="primary" @click="router.push('/ecom/package')">
            <template #icon><PrinterOutlined /></template>{{ $t('ecomOrder.ops.toPackages') }}
          </a-button>
          <span v-if="batchActions.length" class="batch-hint">{{ $t('ecomOrdUi.batch.hint') }}</span>
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
        :locale="{ emptyText: emptyText }"
        class="order-table"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <OrderListCell v-if="CELL_KEYS.includes(column.key)" :column-key="column.key" :record="record" @open="openDetail(record)" />
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <OrderDetail
      ref="detail"
      @changed="afterChange"
      @outbound="(o) => outboundModal.open([o.id])"
      @ship="(o) => shipModal.open(o)"
      @edit="(o) => editDrawer.open(o)"
      @return="toReturn"
      @hold="(o) => holdModal.open([o.id])"
      @split="(o) => splitModal.open(o)"
    />
    <OutboundModal ref="outboundModal" @done="afterChange" />
    <ShipModal ref="shipModal" @done="afterChange" />
    <OrderEditDrawer ref="editDrawer" @success="afterChange" />
    <OrderImportModal ref="importModal" @success="reload" />
    <HoldModal ref="holdModal" @done="afterChange" />
    <SplitModal ref="splitModal" @done="afterChange" />
    <ManualOrderDrawer ref="manualDrawer" @success="(id) => { reload(); id && detail.open(id) }" />
    <TagModal ref="tagModal" @done="afterChange" />
    <AssignModal ref="assignModal" @done="afterChange" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  AuditOutlined,
  CarOutlined,
  ExportOutlined,
  PlusOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PrinterOutlined,
  ReloadOutlined,
  RollbackOutlined,
  ScissorOutlined,
  SyncOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import OrderDetail from './OrderDetail.vue'
import OutboundModal from './OutboundModal.vue'
import ShipModal from './ShipModal.vue'
import OrderEditDrawer from './OrderEditDrawer.vue'
import OrderImportModal from './OrderImportModal.vue'
import HoldModal from './HoldModal.vue'
import SplitModal from './SplitModal.vue'
import ManualOrderDrawer from './ManualOrderDrawer.vue'
import TagModal from './TagModal.vue'
import AssignModal from './AssignModal.vue'
import OrderListCell from './OrderListCell.vue'
import BatchActionBar from '@/components/ecom/BatchActionBar.vue'
import { useOrderBatch } from './useOrderBatch'
import { orderTagOptions } from '@/api/ecom/orderEnhance'
import { orderColumns } from './columns'
import { auditEcomOrders, cancelEcomOrder, ecomOrderStatusCounts, pageEcomOrder, recheckEcomOrders, rematchEcomOrder, unholdEcomOrders } from '@/api/ecom/order'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import {
  ORDER_RESULT_KEYS,
  ORDER_TABS,
  canAudit,
  canCancel,
  canEdit,
  canHold,
  canOutbound,
  canRecheck,
  canRematch,
  canReturn,
  canShip,
  canSplit,
  canUnhold,
  showBatchResult,
  tabOfStatus
} from './order'

/**
 * 平台订单（作业台）：按处理状态分标签页——待审核（审单规则 / 人工审核）→ 待打单（已生成包裹，到「包裹打单」申请运单号）→ 待发货 → 已发货；
 * 缺货的补货后重新检查库存，异常（SKU 未匹配）与挂起的单独一页。勾选订单后出现批量操作条，批量操作逐单处理，有失败的弹窗列出原因。
 * 平台配送订单、不走包裹的简单情况仍可直接出库、发货
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ecom_platform', 'ecom_order_status', 'ecom_local_status', 'ecom_fulfillment', 'ecom_order_type')
/** 紧凑显示多项信息的列（OrderListCell） */
const CELL_KEYS = ['order', 'items', 'buyer', 'paid', 'profit', 'localStatus', 'ship']
const tagOptions = ref([])
orderTagOptions().then((list) => (tagOptions.value = (list || []).map((x) => ({ value: x.id, label: x.name }))))

const columns = orderColumns()
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-order-v2')
const showSearch = ref(true)

const emptyQuery = () => ({
  storeId: undefined,
  platform: undefined,
  status: undefined,
  fulfillment: undefined,
  keyword: undefined,
  country: undefined,
  range: undefined,
  overdue: false,
  urgency: false,
  tagId: undefined,
  orderType: undefined,
  addressInvalid: false
})
const query = reactive(emptyQuery())
// 从看板、首页待办跳转过来时带上处理状态 / 只看逾期（?localStatus=PENDING、?overdue=1）
const route = useRoute()
const tab = ref(route.query.localStatus ? tabOfStatus(String(route.query.localStatus)) : 'pending')
if (route.query.overdue === '1' || route.query.overdue === 'true') query.overdue = true

const baseParams = ({ range, overdue, country, urgency, addressInvalid, ...rest }) => ({
  ...rest,
  addressInvalid: addressInvalid || undefined,
  country: country?.trim().toUpperCase() || undefined,
  beginDate: range?.[0],
  endDate: range?.[1],
  overdue: overdue || undefined,
  sort: urgency ? 'urgency' : undefined
})
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageEcomOrder, query, {
  params: (q) => {
    const statuses = ORDER_TABS.find((x) => x.key === tab.value)?.statuses || []
    return { ...baseParams(q), localStatuses: statuses.length ? statuses.join(',') : undefined }
  }
})

const counts = ref({})
async function loadCounts() {
  counts.value = (await ecomOrderStatusCounts(baseParams({ ...query }))) || {}
}
const tabCount = (tb) =>
  tb.statuses.length ? tb.statuses.reduce((sum, s) => sum + (counts.value[s] || 0), 0) : Object.values(counts.value).reduce((sum, n) => sum + (n || 0), 0)

const hasFilter = computed(() => Object.entries(query).some(([k, v]) => !['overdue', 'urgency', 'addressInvalid'].includes(k) && v != null && v !== '') || query.overdue || query.addressInvalid)
const emptyText = computed(() => (hasFilter.value ? t('ecomOrdUi.list.emptyFiltered') : t('ecomOrdUi.list.emptyTab')))

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

const detail = ref()
const outboundModal = ref()
const shipModal = ref()
const editDrawer = ref()
const importModal = ref()
const holdModal = ref()
const splitModal = ref()
const manualDrawer = ref()
const tagModal = ref()
const assignModal = ref()

const openDetail = (record) => detail.value.open(record.id)
/** 处理后：刷新列表、数量和打开中的详情，清掉勾选 */
function afterChange() {
  reload()
  detail.value?.reload()
  selectedKeys.value = []
}
useRouteOpen({ id: (id) => detail.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(reload)

const { batchActions } = useOrderBatch({
  selectedKeys,
  tab,
  afterChange,
  modals: { hold: holdModal, outbound: outboundModal, tag: tagModal, assign: assignModal }
})

const rematch = useAction(rematchEcomOrder, { successKey: 'ecomOrder.order.rematched', reload: afterChange })
const cancel = useAction(cancelEcomOrder, { successKey: 'ecomOrder.order.cancelled', reload: afterChange })
const auditOne = async (id) => {
  showBatchResult(await auditEcomOrders([id]), 1, ORDER_RESULT_KEYS)
  afterChange()
}
const recheckOne = async (id) => {
  showBatchResult(await recheckEcomOrders([id]), 1, ORDER_RESULT_KEYS)
  afterChange()
}
const unholdOne = useAction((id) => unholdEcomOrders([id]), { successKey: 'ecomOrder.ops.unheld', reload: afterChange })
const toReturn = (record) => router.push({ path: '/ecom/return', query: { orderId: record.id } })

const rowActions = (record) => [
  { type: 'view', onClick: () => openDetail(record) },
  { type: 'edit', perm: 'ecom:order:edit', show: canEdit(record), onClick: () => editDrawer.value.open(record) },
  { key: 'audit', label: t('ecomOrder.ops.audit'), icon: AuditOutlined, perm: 'ecom:order:audit', show: canAudit(record), onClick: () => auditOne(record.id) },
  { key: 'recheck', label: t('ecomOrder.ops.recheck'), icon: SyncOutlined, perm: 'ecom:order:audit', show: canRecheck(record), onClick: () => recheckOne(record.id) },
  { key: 'ship', label: t('ecomOrder.order.ship'), icon: CarOutlined, perm: 'ecom:order:ship', show: canShip(record), onClick: () => shipModal.value.open(record) },
  { key: 'outbound', label: t('ecomOrder.order.outbound'), icon: ExportOutlined, perm: 'ecom:order:outbound', show: canOutbound(record), more: true, onClick: () => outboundModal.value.open([record.id]) },
  { key: 'split', label: t('ecomOrder.ops.split'), icon: ScissorOutlined, perm: 'ecom:order:merge', show: canSplit(record), more: true, onClick: () => splitModal.value.open(record) },
  { key: 'hold', label: t('ecomOrder.ops.hold'), icon: PauseCircleOutlined, perm: 'ecom:order:audit', show: canHold(record), more: true, onClick: () => holdModal.value.open([record.id]) },
  { key: 'unhold', label: t('ecomOrder.ops.unhold'), icon: PlayCircleOutlined, perm: 'ecom:order:audit', show: canUnhold(record), more: true, onClick: () => unholdOne(record.id) },
  {
    key: 'rematch',
    label: t('ecomOrder.order.rematch'),
    icon: ReloadOutlined,
    perm: 'ecom:order:outbound',
    show: canRematch(record),
    more: true,
    confirm: t('ecomOrder.order.rematchConfirm', { no: record.platformOrderNo }),
    onClick: () => rematch(record.id)
  },
  { key: 'return', label: t('ecomOrder.order.return'), icon: RollbackOutlined, perm: 'ecom:return:add', show: canReturn(record), more: true, onClick: () => toReturn(record) },
  {
    type: 'cancel',
    label: t('ecomOrder.order.cancel'),
    perm: 'ecom:order:cancel',
    show: canCancel(record),
    more: true,
    danger: true,
    confirm: { title: t('ecomOrder.order.cancelConfirm', { no: record.platformOrderNo }), description: t('ecomOrder.order.cancelContent'), okType: 'danger' },
    onClick: () => cancel(record.id)
  }
]
</script>

<style scoped>
.status-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}
.status-tabs {
  margin-top: -12px;
}
.tab-count {
  display: inline-block;
  min-width: 20px;
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 10px;
  background: var(--app-hover);
  color: var(--app-text-2);
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}
.status-tabs :deep(.ant-tabs-tab-active) .tab-count {
  background: var(--app-primary-bg, #e6f4ff);
  color: var(--app-primary, #1677ff);
}
.tab-count-abnormal,
.tab-count-outOfStock {
  background: rgba(255, 77, 79, 0.1);
  color: var(--ant-color-error, #ff4d4f);
}
.batch-hint {
  align-self: center;
  color: var(--app-text-3);
  font-size: 12px;
}
.order-table :deep(.ant-table-tbody > tr > td) {
  vertical-align: top;
}
.order-table :deep(.ant-table-tbody > tr > td.ant-table-selection-column),
.order-table :deep(.ant-table-tbody > tr > td.ant-table-cell-fix-right) {
  vertical-align: middle;
}
</style>
