<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.return.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" @update:value="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.return.returnType')" name="returnType">
          <a-select v-model:value="query.returnType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_return_type)" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="query.status" class="status-tabs" @change="search">
        <a-tab-pane v-for="tb in tabs" :key="tb.key">
          <template #tab>{{ tb.label }}<span v-if="counts[tb.key] != null" class="tab-count">{{ counts[tb.key] }}</span></template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:return:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOrder.return.addTitle') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        :locale="{ emptyText: $t('ecomOrdUi.ret.empty') }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a class="doc-no" @click="openDetail(record)">{{ record.docNo }}</a>
            <div class="sub-text ellipsis" :title="record.storeName">{{ record.storeName }}</div>
          </template>
          <template v-else-if="column.key === 'order'">
            <a v-if="record.orderId" @click="router.push({ path: '/ecom/order', query: { id: record.orderId } })">{{ record.platformOrderNo }}</a>
            <span v-else>{{ record.platformOrderNo || '-' }}</span>
            <div class="sub-text ellipsis" :title="record.buyerName">{{ record.buyerName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <div class="ellipsis" :title="record.warehouseName">{{ record.warehouseName || '-' }}</div>
            <div v-if="record.tradeDocNo" class="sub-text ellipsis" :title="record.tradeDocNo">{{ record.tradeDocNo }}</div>
          </template>
          <template v-else-if="column.key === 'returnType'">
            <DictTag type="ecom_return_type" :value="record.returnType" />
          </template>
          <template v-else-if="column.key === 'reason'">
            <DictTag v-if="record.reason" type="ecom_return_reason" :value="record.reason" />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'refundAmount'">
            <span class="nowrap">{{ fcMoney(record.currency, record.refundAmount) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_return_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <ReturnForm ref="form" @success="reload" />
    <ReturnDetail ref="detail" @changed="reload" @receive="openReceive" />
    <DateConfirmModal ref="dateModal" :label="$t('ecomOrder.return.receiveDate')" @done="afterChange" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ImportOutlined, PayCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import DateConfirmModal from '@/components/ecom/DateConfirmModal.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import ReturnForm from './ReturnForm.vue'
import ReturnDetail from './ReturnDetail.vue'
import { closeEcomReturn, deleteEcomReturn, ecomReturnStatusCounts, pageEcomReturn, receiveEcomReturn, refundEcomReturn } from '@/api/ecom/return'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { fcMoney } from '../order/order'
import { canClose, canDelete, canReceive, canRefund } from './return'

/**
 * 退货退款：针对平台订单登记仅退款 / 退货退款。退货退款：待处理 → 退货入库（生成销售退货单）→ 登记退款；仅退款直接登记退款。
 * 从订单详情 / 列表的「退货退款」跳转过来时（?orderId=）直接打开新增表单并带入订单
 */
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const dict = useDict('ecom_return_status', 'ecom_return_type', 'ecom_return_reason')

const columns = [
  {
    title: t('ecomOrder.common.docNo'),
    key: 'docNo',
    dataIndex: 'docNo',
    width: 170,
    fixed: 'left',
    exportColumns: [
      { title: t('ecomOrder.common.docNo'), dataIndex: 'docNo' },
      { title: t('ecomOrder.common.store'), dataIndex: 'storeName' }
    ]
  },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_return_status' },
  {
    title: t('ecomOrdUi.ret.order'),
    key: 'order',
    width: 200,
    exportColumns: [
      { title: t('ecomOrder.common.platformOrderNo'), dataIndex: 'platformOrderNo' },
      { title: t('ecomOrder.return.buyer'), dataIndex: 'buyerName' }
    ]
  },
  { title: t('ecomOrder.return.returnType'), key: 'returnType', dataIndex: 'returnType', width: 110, dict: 'ecom_return_type' },
  { title: t('ecomOrder.return.reason'), key: 'reason', dataIndex: 'reason', width: 120, dict: 'ecom_return_reason' },
  {
    title: t('ecomOrder.return.refundAmount'),
    key: 'refundAmount',
    width: 140,
    align: 'right',
    exportColumns: [
      { title: t('ecomOrder.common.currency'), dataIndex: 'currency' },
      { title: t('ecomOrder.return.refundAmount'), dataIndex: 'refundAmount' }
    ]
  },
  {
    title: t('ecomOrdUi.ret.warehouse'),
    key: 'warehouse',
    width: 180,
    exportColumns: [
      { title: t('ecomOrder.return.warehouse'), dataIndex: 'warehouseName' },
      { title: t('ecomOrder.return.tradeDoc'), dataIndex: 'tradeDocNo' }
    ]
  },
  { title: t('ecomOrder.return.refundTime'), dataIndex: 'refundTime', width: 170 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  // 查看、退货入库 / 登记退款（按状态互斥）、删除、更多（关闭）
  actionColumn(4, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-return')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, returnType: undefined, keyword: undefined })
const query = reactive({ ...emptyQuery(), status: '' })
// 从首页待办跳转过来时带上状态（?status=PENDING）
if (route.query.status) query.status = String(route.query.status)
const { list, loading, size, pagination, load, search: doSearch, onTableChange, exportAll } = useTable(pageEcomReturn, query, {
  params: (q) => ({ ...q, status: q.status || undefined })
})

/** 状态标签页（「全部」的 key 为空字符串），数量按当前筛选条件分状态汇总（「全部」为合计） */
const tabs = computed(() => [{ key: '', label: t('ecomOrdUi.ret.all') }, ...(dict.ecom_return_status || []).map((d) => ({ key: d.value, label: d.label }))])
const counts = ref({})
async function loadCounts() {
  const byStatus = (await ecomReturnStatusCounts({ storeId: query.storeId, returnType: query.returnType, keyword: query.keyword || undefined })) || {}
  const all = Object.values(byStatus).reduce((sum, n) => sum + (n || 0), 0)
  counts.value = { '': all, ...Object.fromEntries((dict.ecom_return_status || []).map((d) => [d.value, byStatus[d.value] || 0])) }
}
watch(() => dict.ecom_return_status?.length, (n) => n && loadCounts(), { immediate: true })
function search() {
  doSearch()
  loadCounts()
}
function reload() {
  load()
  loadCounts()
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const form = ref()
const detail = ref()
const dateModal = ref()
const openDetail = (record) => detail.value.open(record.id)
function afterChange() {
  reload()
  detail.value?.reload()
}
useRouteOpen({ id: (id) => detail.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })

onMounted(() => {
  load()
  // 从订单页「退货退款」跳转：带入订单打开新增表单
  const orderId = Number(route.query.orderId)
  if (orderId) {
    form.value.openCreate({ orderId })
    router.replace({ query: { ...route.query, orderId: undefined } })
  }
})

/** 退货入库：选择入库日期后调接口 */
function openReceive(record) {
  dateModal.value.open({
    title: t('ecomOrder.return.receiveTitle', { no: record.docNo }),
    content: t('ecomOrder.return.receiveContent'),
    action: async (date) => {
      await receiveEcomReturn(record.id, date)
      message.success(t('ecomOrder.return.received'))
    }
  })
}
const refund = useAction(refundEcomReturn, { successKey: 'ecomOrder.return.refunded', reload: afterChange })
const close = useAction(closeEcomReturn, { successKey: 'ecomOrder.return.closed', reload: afterChange })
const remove = useAction(deleteEcomReturn, { successKey: 'common.msg.deleteSuccess', reload: afterChange })

const rowActions = (record) => [
  { type: 'view', onClick: () => openDetail(record) },
  { key: 'receive', label: t('ecomOrder.return.receive'), icon: ImportOutlined, perm: 'ecom:return:receive', show: canReceive(record), onClick: () => openReceive(record) },
  {
    key: 'refund',
    label: t('ecomOrder.return.refund'),
    icon: PayCircleOutlined,
    perm: 'ecom:return:refund',
    show: canRefund(record),
    confirm: { title: t('ecomOrder.return.refundConfirm', { no: record.docNo }), description: t('ecomOrder.return.refundContent', { amount: fcMoney(record.currency, record.refundAmount) }) },
    onClick: () => refund(record.id)
  },
  {
    type: 'delete',
    perm: 'ecom:return:remove',
    show: canDelete(record),
    confirm: { title: t('ecomOrder.return.deleteConfirm', { no: record.docNo }), okType: 'danger' },
    onClick: () => remove(record.id)
  },
  {
    type: 'cancel',
    label: t('ecomOrder.return.close'),
    perm: 'ecom:return:add',
    show: canClose(record),
    more: true,
    confirm: t('ecomOrder.return.closeConfirm', { no: record.docNo }),
    onClick: () => close(record.id)
  }
]
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.doc-no {
  font-weight: 500;
}
.status-tabs {
  margin-top: -12px;
}
.status-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
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
</style>
