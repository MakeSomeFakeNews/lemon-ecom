<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.pkg.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.channel')" name="channelId">
          <ChannelSelect v-model:value="query.channelId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.pkg.createDate')" name="range" data-wide>
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" size="small" class="status-tabs" @change="onTab">
        <a-tab-pane v-for="tb in PACKAGE_TABS" :key="tb.key">
          <template #tab>
            {{ $t(tb.label) }}<span v-if="tb.key !== 'all'" class="tab-count">{{ counts[tb.key] || 0 }}</span>
          </template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-if="tab !== EcomPackageStatus.SHIPPED && tab !== EcomPackageStatus.CANCELLED" v-perm="'ecom:package:label'" type="primary" :disabled="!selectedKeys.length" @click="labelModal.open(selectedKeys)">
          <template #icon><ThunderboltOutlined /></template>{{ $t('ecomOrder.pkg.label') }}
        </a-button>
        <a-button v-perm="'ecom:package:label'" :disabled="!selectedKeys.length" :loading="printing" @click="printLabels(selectedKeys)">
          <template #icon><PrinterOutlined /></template>{{ $t('ecomOrder.pkg.printLabels') }}
        </a-button>
        <a-button v-perm="'ecom:package:label'" :disabled="!selectedKeys.length" @click="printPickList(selectedKeys)">
          <template #icon><UnorderedListOutlined /></template>{{ $t('ecomOrder.pkg.printPickList') }}
        </a-button>
        <a-button v-if="tab === EcomPackageStatus.LABELED || tab === 'all'" v-perm="'ecom:package:ship'" :disabled="!selectedKeys.length" @click="shipSelected">
          <template #icon><CarOutlined /></template>{{ $t('ecomOrder.pkg.ship') }}
        </a-button>
        <a-button v-perm="'ecom:package:ship'" @click="router.push('/ecom/scan-ship')">
          <template #icon><ScanOutlined /></template>{{ $t('ecomOrder.scan.title') }}
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
          <template v-if="column.key === 'packageNo'">
            <a @click="detail.open(record.id)">{{ record.packageNo }}</a>
          </template>
          <template v-else-if="column.key === 'receiver'">
            <div>{{ record.shipName || '-' }}</div>
            <div class="sub-text">{{ record.shipCountry }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_package_status" :value="record.status" />
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
              <ExclamationCircleOutlined class="exception-icon" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'tracking'">
            <div>{{ record.trackingNo || '-' }}</div>
            <div v-if="record.carrierName" class="sub-text">{{ record.carrierName }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'weight'">{{ record.weight != null ? Number(record.weight) : '-' }}</template>
          <template v-else-if="column.key === 'shippingCost'">{{ record.shippingCost != null ? `¥${money(record.shippingCost)}` : '-' }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <PackageDetail
      ref="detail"
      @label="(p) => labelModal.open([p.id])"
      @tracking="(p) => trackingModal.openTracking(p)"
      @edit="(p) => trackingModal.openEdit(p)"
      @view-label="(p) => viewLabel(p)"
      @refetch="(p) => refetch(p.id)"
      @ship="(p) => shipIds([p.id])"
      @cancel="(p) => cancelOne(p)"
    />
    <LabelModal ref="labelModal" @done="afterChange" />
    <TrackingModal ref="trackingModal" @done="afterChange" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Modal } from 'ant-design-vue'
import {
  CarOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  FilePdfOutlined,
  NumberOutlined,
  PrinterOutlined,
  ReloadOutlined,
  ScanOutlined,
  ThunderboltOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import PackageDetail from './PackageDetail.vue'
import LabelModal from './LabelModal.vue'
import TrackingModal from './TrackingModal.vue'
import {
  cancelEcomPackage,
  ecomPackageLabel,
  ecomPackageLabels,
  ecomPackageStatusCounts,
  pageEcomPackage,
  refetchEcomPackageLabel,
  shipEcomPackages
} from '@/api/ecom/package'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { EcomPackageStatus } from '@/constants/enums'
import { money, qty } from '@/utils/number'
import { showPdf } from '@/utils/print'
import { showBatchResult } from '../order/order'
import { PACKAGE_RESULT_KEYS, PACKAGE_TABS, canCancelPackage, canEditPackage, canLabel, canRefetch, canShipPackage, canTracking, printPickList } from './package'

/**
 * 包裹打单：审核通过的订单生成包裹（合并 / 拆分在平台订单中操作）。待打单 → 批量申请运单号（渠道对接了物流商时在线下单并保存面单，
 * 否则手工填写运单号）→ 待发货 → 批量打印面单（合并为一个 PDF）、打印拣货单（按库位汇总）→ 扫描或批量发货（生成销售出库、回传平台）
 */
const { t } = useI18n()
const router = useRouter()

const columns = [
  { title: t('ecomOrder.pkg.packageNo'), key: 'packageNo', dataIndex: 'packageNo', width: 170 },
  { title: t('ecomOrder.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  {
    title: t('ecomOrder.pkg.receiver'),
    key: 'receiver',
    width: 140,
    exportColumns: [
      { title: t('ecomOrder.pkg.receiver'), dataIndex: 'shipName' },
      { title: t('ecomOrder.order.country'), dataIndex: 'shipCountry' }
    ]
  },
  { title: t('ecomOrder.pkg.orders'), dataIndex: 'orderCount', width: 70, align: 'right' },
  { title: t('ecomOrder.pkg.totalQty'), key: 'totalQty', dataIndex: 'totalQty', width: 70, align: 'right' },
  { title: t('ecomOrder.common.warehouse'), dataIndex: 'warehouseName', width: 130, ellipsis: true },
  { title: t('ecomOrder.common.channel'), dataIndex: 'channelName', width: 140, ellipsis: true },
  {
    title: t('ecomOrder.common.trackingNo'),
    key: 'tracking',
    width: 180,
    exportColumns: [
      { title: t('ecomOrder.pkg.carrierName'), dataIndex: 'carrierName' },
      { title: t('ecomOrder.common.trackingNo'), dataIndex: 'trackingNo' }
    ]
  },
  { title: t('ecomOrder.pkg.weight'), key: 'weight', dataIndex: 'weight', width: 90, align: 'right' },
  { title: t('ecomOrder.pkg.shippingCost'), key: 'shippingCost', dataIndex: 'shippingCost', width: 100, align: 'right' },
  { title: t('ecomOrder.pkg.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_package_status' },
  { title: t('ecomOrder.pkg.printCount'), dataIndex: 'printCount', width: 80, align: 'right' },
  { title: t('ecomOrder.pkg.labelTime'), dataIndex: 'labelTime', width: 160 },
  { title: t('ecomOrder.pkg.shipTime'), dataIndex: 'shipTime', width: 160 },
  actionColumn(4, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-package')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, storeId: undefined, warehouseId: undefined, channelId: undefined, range: undefined })
const query = reactive(emptyQuery())
const tab = ref(EcomPackageStatus.WAIT_LABEL)
const baseParams = ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageEcomPackage, query, {
  params: (q) => ({ ...baseParams(q), status: tab.value === 'all' ? undefined : tab.value })
})

const counts = ref({})
async function loadCounts() {
  counts.value = (await ecomPackageStatusCounts(baseParams({ ...query }))) || {}
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

const detail = ref()
const labelModal = ref()
const trackingModal = ref()

function afterChange() {
  reload()
  detail.value?.reload()
  selectedKeys.value = []
}
// 从订单详情点包裹号进入：?keyword= 查到包裹（标签页切到全部）
useRouteOpen({
  id: (id) => detail.value.open(id),
  keyword: (kw) => {
    query.keyword = kw
    tab.value = 'all'
  },
  reload: () => search()
})
onMounted(reload)

const printing = ref(false)
async function printLabels(ids) {
  printing.value = true
  try {
    showPdf(await ecomPackageLabels(ids), t('ecomOrder.pkg.printLabels'))
    reload()
  } finally {
    printing.value = false
  }
}
async function viewLabel(p) {
  showPdf(await ecomPackageLabel(p.id), p.packageNo)
}

async function shipIds(ids) {
  showBatchResult(await shipEcomPackages(ids), ids.length, PACKAGE_RESULT_KEYS)
  afterChange()
}
function shipSelected() {
  const ids = [...selectedKeys.value]
  Modal.confirm({ title: t('ecomOrder.pkg.shipConfirm', { n: ids.length }), onOk: () => shipIds(ids) })
}

const refetch = useAction(refetchEcomPackageLabel, { successKey: 'ecomOrder.pkg.refetched', reload: afterChange })
const cancel = useAction(cancelEcomPackage, { successKey: 'ecomOrder.pkg.cancelled', reload: afterChange })
const cancelOne = (p) =>
  Modal.confirm({ title: t('ecomOrder.pkg.cancelConfirm', { no: p.packageNo }), content: t('ecomOrder.pkg.cancelContent'), okType: 'danger', onOk: () => cancel(p.id) })

const rowActions = (record) => [
  { type: 'view', onClick: () => detail.value.open(record.id) },
  { key: 'label', label: t('ecomOrder.pkg.label'), icon: ThunderboltOutlined, perm: 'ecom:package:label', show: canLabel(record), onClick: () => labelModal.value.open([record.id]) },
  { key: 'viewLabel', label: t('ecomOrder.pkg.viewLabel'), icon: FilePdfOutlined, perm: 'ecom:package:list', show: !!record.hasLabel, onClick: () => viewLabel(record) },
  { key: 'ship', label: t('ecomOrder.pkg.ship'), icon: CarOutlined, perm: 'ecom:package:ship', show: canShipPackage(record), more: true, onClick: () => shipIds([record.id]) },
  { key: 'tracking', label: t('ecomOrder.pkg.tracking'), icon: NumberOutlined, perm: 'ecom:package:label', show: canTracking(record), more: true, onClick: () => trackingModal.value.openTracking(record) },
  { key: 'edit', label: t('ecomOrder.pkg.edit'), icon: EditOutlined, perm: 'ecom:package:label', show: canEditPackage(record), more: true, onClick: () => trackingModal.value.openEdit(record) },
  { key: 'refetch', label: t('ecomOrder.pkg.refetch'), icon: ReloadOutlined, perm: 'ecom:package:label', show: canRefetch(record), more: true, onClick: () => refetch(record.id) },
  {
    type: 'cancel',
    label: t('ecomOrder.pkg.cancel'),
    perm: 'ecom:package:label',
    show: canCancelPackage(record),
    more: true,
    danger: true,
    confirm: { title: t('ecomOrder.pkg.cancelConfirm', { no: record.packageNo }), description: t('ecomOrder.pkg.cancelContent'), okType: 'danger' },
    onClick: () => cancel(record.id)
  }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
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
  color: var(--ant-color-warning, #faad14);
  cursor: help;
}
</style>
