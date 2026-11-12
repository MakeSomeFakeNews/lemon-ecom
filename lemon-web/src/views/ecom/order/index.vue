<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.orderDate')" name="range" data-wide>
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.order.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.platform')" name="platform">
          <a-select v-model:value="query.platform" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_order_status)" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.fulfillment')" name="fulfillment">
          <a-select v-model:value="query.fulfillment" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_fulfillment)" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.country')" name="country">
          <a-input v-model:value="query.country" :placeholder="$t('ecomOrder.order.countryPh')" allow-clear :maxlength="8" @press-enter="search" />
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
      <a-tabs v-model:active-key="tab" size="small" class="status-tabs" @change="onTab">
        <a-tab-pane v-for="tb in ORDER_TABS" :key="tb.key">
          <template #tab>
            {{ $t(tb.label) }}<span v-if="tb.key !== 'all'" class="tab-count">{{ tabCount(tb) }}</span>
          </template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-if="tab === 'all' || tab === 'pending'" v-perm="'ecom:order:import'" @click="importModal.open()">
          <template #icon><UploadOutlined /></template>{{ $t('ecomOrder.order.import') }}
        </a-button>
        <template v-for="a in batchActions" :key="a.key">
          <a-button v-perm="a.perm" :type="a.primary ? 'primary' : 'default'" :disabled="!selectedKeys.length" @click="a.run">
            <template #icon><component :is="a.icon" /></template>{{ a.label }}
          </a-button>
        </template>
        <a-button v-if="tab === 'toLabel' || tab === 'toShip'" v-perm="'ecom:package:list'" @click="router.push('/ecom/package')">
          <template #icon><PrinterOutlined /></template>{{ $t('ecomOrder.ops.toPackages') }}
        </a-button>
        <span v-if="selectedKeys.length" class="selected-tip">
          {{ $t('common.table.selected', { n: selectedKeys.length }) }} <a @click="selectedKeys = []">{{ $t('common.action.clear') }}</a>
        </span>
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
          <template v-if="column.key === 'platformOrderNo'">
            <a @click="openDetail(record)">{{ record.platformOrderNo }}</a>
          </template>
          <template v-else-if="column.key === 'platform'">
            <DictTag type="ecom_platform" :value="record.platform" />
          </template>
          <template v-else-if="column.key === 'buyer'">
            <div>{{ record.buyerName || record.shipName || '-' }}</div>
            <div class="sub-text">{{ record.shipCountry }}</div>
          </template>
          <template v-else-if="column.key === 'paid'">
            <div class="nowrap">{{ fcMoney(record.currency, record.totalAmount) }}</div>
            <div class="sub-text nowrap">{{ baseMoney(record.totalBase) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_order_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'localStatus'">
            <DictTag type="ecom_local_status" :value="record.localStatus" />
            <a-tooltip v-if="record.localStatus === EcomLocalStatus.ON_HOLD ? record.holdReason : record.exception" :title="record.localStatus === EcomLocalStatus.ON_HOLD ? record.holdReason : record.exception">
              <ExclamationCircleOutlined class="exception-icon" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'fulfillment'">
            <DictTag type="ecom_fulfillment" :value="record.fulfillment" />
          </template>
          <template v-else-if="column.key === 'shipBy'">
            <span :class="{ 'overdue-text': isOverdue(record) }">{{ record.shipBy || '-' }}</span>
            <a-tag v-if="dueHours(record)" color="warning" :bordered="false" class="due-tag">{{ $t('ecomOrder.ops.dueIn', { h: dueHours(record) }) }}</a-tag>
            <div v-if="record.planShipTime" class="sub-text">{{ $t('ecomOrder.ops.planShip', { time: record.planShipTime.slice(5, 16) }) }}</div>
          </template>
          <template v-else-if="column.key === 'trackingNo'">
            <div>{{ record.trackingNo || '-' }}</div>
            <div v-if="record.carrier" class="sub-text">{{ record.carrier }}</div>
          </template>
          <template v-else-if="column.key === 'profit'">
            <span v-if="record.profit != null" :class="{ 'profit-negative': Number(record.profit) < 0 }">{{ baseMoney(record.profit) }}</span>
            <span v-else>-</span>
          </template>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import {
  AuditOutlined,
  CarOutlined,
  ExclamationCircleOutlined,
  ExportOutlined,
  FilterOutlined,
  MergeCellsOutlined,
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
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import OrderDetail from './OrderDetail.vue'
import OutboundModal from './OutboundModal.vue'
import ShipModal from './ShipModal.vue'
import OrderEditDrawer from './OrderEditDrawer.vue'
import OrderImportModal from './OrderImportModal.vue'
import HoldModal from './HoldModal.vue'
import SplitModal from './SplitModal.vue'
import { orderColumns } from './columns'
import {
  applyEcomOrderRules,
  auditEcomOrders,
  cancelEcomOrder,
  ecomOrderStatusCounts,
  mergeEcomOrders,
  pageEcomOrder,
  recheckEcomOrders,
  rematchEcomOrder,
  unholdEcomOrders
} from '@/api/ecom/order'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { EcomLocalStatus } from '@/constants/enums'
import {
  ORDER_RESULT_KEYS,
  ORDER_TABS,
  baseMoney,
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
  dueHours,
  fcMoney,
  isOverdue,
  showBatchResult,
  tabOfStatus
} from './order'

/**
 * 平台订单（作业台）：按处理状态分标签页——待审核（审单规则 / 人工审核）→ 待打单（已生成包裹，到「包裹打单」申请运单号）→ 待发货 → 已发货；
 * 缺货的补货后重新检查库存，异常（SKU 未匹配）与挂起的单独一页。批量操作逐单处理，有失败的弹窗列出原因。
 * 平台配送订单、不走包裹的简单情况仍可直接出库、发货
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ecom_platform', 'ecom_order_status', 'ecom_local_status', 'ecom_fulfillment')

const columns = orderColumns()
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-order')
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
  urgency: false
})
const query = reactive(emptyQuery())
// 从看板、首页待办跳转过来时带上处理状态 / 只看逾期（?localStatus=PENDING、?overdue=1）
const route = useRoute()
const tab = ref(route.query.localStatus ? tabOfStatus(String(route.query.localStatus)) : 'pending')
if (route.query.overdue === '1' || route.query.overdue === 'true') query.overdue = true

const baseParams = ({ range, overdue, country, urgency, ...rest }) => ({
  ...rest,
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
const tabCount = (tb) => tb.statuses.reduce((sum, s) => sum + (counts.value[s] || 0), 0)

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

const openDetail = (record) => detail.value.open(record.id)
/** 处理后：刷新列表、数量和打开中的详情，清掉勾选 */
function afterChange() {
  reload()
  detail.value?.reload()
  selectedKeys.value = []
}
useRouteOpen({ id: (id) => detail.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(reload)

/** 批量接口：确认 → 调用 → 列出失败 → 刷新 */
function runBatch(api, confirmTitle, content) {
  const ids = [...selectedKeys.value]
  const exec = async () => {
    showBatchResult(await api(ids), ids.length, ORDER_RESULT_KEYS)
    afterChange()
  }
  if (!confirmTitle) return exec()
  Modal.confirm({ title: confirmTitle, content, onOk: exec })
}

const BATCH = {
  audit: () => ({
    key: 'audit', label: t('ecomOrder.ops.batchAudit'), icon: AuditOutlined, perm: 'ecom:order:audit', primary: true,
    run: () => runBatch(auditEcomOrders, t('ecomOrder.ops.auditConfirm', { n: selectedKeys.value.length }), t('ecomOrder.ops.auditHint'))
  }),
  rules: () => ({ key: 'rules', label: t('ecomOrder.ops.applyRules'), icon: FilterOutlined, perm: 'ecom:order:audit', run: () => runBatch(applyEcomOrderRules) }),
  recheck: () => ({ key: 'recheck', label: t('ecomOrder.ops.recheck'), icon: SyncOutlined, perm: 'ecom:order:audit', primary: true, run: () => runBatch(recheckEcomOrders) }),
  hold: () => ({ key: 'hold', label: t('ecomOrder.ops.hold'), icon: PauseCircleOutlined, perm: 'ecom:order:audit', run: () => holdModal.value.open(selectedKeys.value) }),
  unhold: () => ({
    key: 'unhold', label: t('ecomOrder.ops.unhold'), icon: PlayCircleOutlined, perm: 'ecom:order:audit',
    run: () => runBatch(unholdEcomOrders, t('ecomOrder.ops.unholdConfirm', { n: selectedKeys.value.length }))
  }),
  merge: () => ({
    key: 'merge', label: t('ecomOrder.ops.merge'), icon: MergeCellsOutlined, perm: 'ecom:order:merge',
    run: () => Modal.confirm({
      title: t('ecomOrder.ops.mergeConfirm', { n: selectedKeys.value.length }),
      content: t('ecomOrder.ops.mergeHint'),
      onOk: async () => {
        await mergeEcomOrders(selectedKeys.value)
        message.success(t('ecomOrder.ops.merged'))
        afterChange()
      }
    })
  }),
  outbound: () => ({ key: 'outbound', label: t('ecomOrder.order.batchOutbound'), icon: ExportOutlined, perm: 'ecom:order:outbound', run: () => outboundModal.value.open(selectedKeys.value) })
}

/** 各标签页的批量操作 */
const batchActions = computed(() => {
  const keys = {
    pending: ['audit', 'rules', 'hold', 'outbound'],
    outOfStock: ['recheck', 'hold', 'outbound'],
    toLabel: ['merge', 'recheck', 'hold'],
    toShip: ['hold'],
    shipped: [],
    abnormal: ['unhold', 'rules', 'audit'],
    all: ['outbound']
  }[tab.value]
  return keys.map((k) => BATCH[k]())
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.nowrap {
  white-space: nowrap;
}
.selected-tip {
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
.exception-icon {
  margin-left: 4px;
  color: var(--ant-color-error, #ff4d4f);
  cursor: help;
}
.due-tag {
  margin-left: 4px;
}
.overdue-text {
  color: var(--ant-color-error, #ff4d4f);
  font-weight: 500;
}
.profit-negative {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
