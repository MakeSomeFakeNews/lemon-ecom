<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.localStatus')" name="localStatus">
          <a-select v-model:value="query.localStatus" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_local_status)" />
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
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:order:import'" @click="importModal.open()">
          <template #icon><UploadOutlined /></template>{{ $t('ecomOrder.order.import') }}
        </a-button>
        <a-button v-perm="'ecom:order:outbound'" type="primary" :disabled="!selectedKeys.length" @click="outboundModal.open(selectedKeys)">
          <template #icon><ExportOutlined /></template>{{ $t('ecomOrder.order.batchOutbound') }}
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
        :row-selection="selection"
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
            <a-tooltip v-if="record.localStatus === EcomLocalStatus.EXCEPTION && record.exception" :title="record.exception">
              <ExclamationCircleOutlined class="exception-icon" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'fulfillment'">
            <DictTag type="ecom_fulfillment" :value="record.fulfillment" />
          </template>
          <template v-else-if="column.key === 'shipBy'">
            <span :class="{ 'overdue-text': isOverdue(record) }">{{ record.shipBy || '-' }}</span>
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

    <OrderDetail ref="detail" @changed="load" @outbound="(o) => outboundModal.open([o.id])" @ship="(o) => shipModal.open(o)" @edit="(o) => editDrawer.open(o)" @return="toReturn" />
    <OutboundModal ref="outboundModal" @done="afterChange" />
    <ShipModal ref="shipModal" @done="afterChange" />
    <OrderEditDrawer ref="editDrawer" @success="afterChange" />
    <OrderImportModal ref="importModal" @success="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { CarOutlined, ExclamationCircleOutlined, ExportOutlined, ReloadOutlined, RollbackOutlined, UploadOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import OrderDetail from './OrderDetail.vue'
import OutboundModal from './OutboundModal.vue'
import ShipModal from './ShipModal.vue'
import OrderEditDrawer from './OrderEditDrawer.vue'
import OrderImportModal from './OrderImportModal.vue'
import { cancelEcomOrder, pageEcomOrder, rematchEcomOrder } from '@/api/ecom/order'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { EcomLocalStatus } from '@/constants/enums'
import { baseMoney, canCancel, canEdit, canOutbound, canRematch, canReturn, canShip, fcMoney, isOverdue } from './order'

/**
 * 平台订单：导入 / 同步的订单按处理状态流转：待处理 → 出库（生成销售出库单）→ 发货（填写物流、回传平台）；
 * SKU 未匹配等为异常，处理后重新匹配、出库；未出库的可取消，出库后走退货退款
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ecom_platform', 'ecom_order_status', 'ecom_local_status', 'ecom_fulfillment')

const columns = [
  { title: t('ecomOrder.common.platformOrderNo'), key: 'platformOrderNo', dataIndex: 'platformOrderNo', width: 190 },
  { title: t('ecomOrder.common.store'), dataIndex: 'storeName', width: 150 },
  { title: t('ecomOrder.common.platform'), key: 'platform', dataIndex: 'platform', width: 100, dict: 'ecom_platform' },
  { title: t('ecomOrder.order.orderTime'), dataIndex: 'orderTime', width: 170 },
  {
    title: t('ecomOrder.order.buyer'),
    key: 'buyer',
    width: 160,
    exportColumns: [
      { title: t('ecomOrder.order.buyer'), dataIndex: 'buyerName' },
      { title: t('ecomOrder.order.country'), dataIndex: 'shipCountry' }
    ]
  },
  { title: t('ecomOrder.order.itemCount'), dataIndex: 'itemCount', width: 90, align: 'right' },
  {
    title: t('ecomOrder.order.paid'),
    key: 'paid',
    width: 150,
    align: 'right',
    exportColumns: [
      { title: t('ecomOrder.common.currency'), dataIndex: 'currency' },
      { title: t('ecomOrder.order.paid'), dataIndex: 'totalAmount' },
      { title: t('ecomOrder.order.paidBase'), dataIndex: 'totalBase' }
    ]
  },
  { title: t('ecomOrder.order.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_order_status' },
  {
    title: t('ecomOrder.order.localStatus'),
    key: 'localStatus',
    dataIndex: 'localStatus',
    width: 110,
    dict: 'ecom_local_status',
    exportColumns: [
      { title: t('ecomOrder.order.localStatus'), dataIndex: 'localStatus', dict: 'ecom_local_status' },
      { title: t('ecomOrder.order.exception'), dataIndex: 'exception' }
    ]
  },
  { title: t('ecomOrder.order.fulfillment'), key: 'fulfillment', dataIndex: 'fulfillment', width: 110, dict: 'ecom_fulfillment' },
  { title: t('ecomOrder.order.shipBy'), key: 'shipBy', dataIndex: 'shipBy', width: 170 },
  {
    title: t('ecomOrder.common.trackingNo'),
    key: 'trackingNo',
    width: 170,
    exportColumns: [
      { title: t('ecomOrder.common.carrier'), dataIndex: 'carrier' },
      { title: t('ecomOrder.common.trackingNo'), dataIndex: 'trackingNo' }
    ]
  },
  { title: t('ecomOrder.order.profit'), key: 'profit', dataIndex: 'profit', width: 120, align: 'right' },
  // 查看、编辑、出库 / 发货（按状态互斥）、更多
  actionColumn(4, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-order')
const showSearch = ref(true)

const emptyQuery = () => ({
  storeId: undefined,
  platform: undefined,
  status: undefined,
  localStatus: undefined,
  fulfillment: undefined,
  keyword: undefined,
  country: undefined,
  range: undefined,
  overdue: false
})
const query = reactive(emptyQuery())
// 从看板、首页待办跳转过来时带上处理状态 / 只看逾期（?localStatus=PENDING、?overdue=1）
const route = useRoute()
if (route.query.localStatus) query.localStatus = String(route.query.localStatus)
if (route.query.overdue === '1' || route.query.overdue === 'true') query.overdue = true
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageEcomOrder, query, {
  params: ({ range, overdue, country, ...rest }) => ({
    ...rest,
    country: country?.trim().toUpperCase() || undefined,
    beginDate: range?.[0],
    endDate: range?.[1],
    overdue: overdue || undefined
  })
})
/** 只有待处理 / 异常的订单可以勾选批量出库 */
const selection = computed(() => ({ ...rowSelection, getCheckboxProps: (r) => ({ disabled: !canOutbound(r) }) }))

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const detail = ref()
const outboundModal = ref()
const shipModal = ref()
const editDrawer = ref()
const importModal = ref()

const openDetail = (record) => detail.value.open(record.id)
/** 出库、发货、修改后：刷新列表和打开中的详情，清掉已不能出库的勾选 */
function afterChange() {
  load()
  detail.value?.reload()
  selectedKeys.value = []
}
useRouteOpen({ id: (id) => detail.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(load)

const rematch = useAction(rematchEcomOrder, { successKey: 'ecomOrder.order.rematched', reload: afterChange })
const cancel = useAction(cancelEcomOrder, { successKey: 'ecomOrder.order.cancelled', reload: afterChange })
const toReturn = (record) => router.push({ path: '/ecom/return', query: { orderId: record.id } })

const rowActions = (record) => [
  { type: 'view', onClick: () => openDetail(record) },
  { type: 'edit', perm: 'ecom:order:edit', show: canEdit(record), onClick: () => editDrawer.value.open(record) },
  {
    key: 'outbound',
    label: t('ecomOrder.order.outbound'),
    icon: ExportOutlined,
    perm: 'ecom:order:outbound',
    show: canOutbound(record),
    onClick: () => outboundModal.value.open([record.id])
  },
  { key: 'ship', label: t('ecomOrder.order.ship'), icon: CarOutlined, perm: 'ecom:order:ship', show: canShip(record), onClick: () => shipModal.value.open(record) },
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
.exception-icon {
  margin-left: 4px;
  color: var(--ant-color-error, #ff4d4f);
  cursor: help;
}
.overdue-text {
  color: var(--ant-color-error, #ff4d4f);
  font-weight: 500;
}
.profit-negative {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
