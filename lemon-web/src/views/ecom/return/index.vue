<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_return_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.return.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
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
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
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

    <ReturnForm ref="form" @success="load" />
    <ReturnDetail ref="detail" @changed="load" @receive="openReceive" />
    <DateConfirmModal ref="dateModal" :label="$t('ecomOrder.return.receiveDate')" @done="afterChange" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
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
import { closeEcomReturn, deleteEcomReturn, pageEcomReturn, receiveEcomReturn, refundEcomReturn } from '@/api/ecom/return'
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
  { title: t('ecomOrder.common.docNo'), key: 'docNo', dataIndex: 'docNo', width: 170 },
  { title: t('ecomOrder.common.store'), dataIndex: 'storeName', width: 150 },
  { title: t('ecomOrder.common.platformOrderNo'), dataIndex: 'platformOrderNo', width: 190 },
  { title: t('ecomOrder.return.buyer'), dataIndex: 'buyerName', width: 130 },
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
  { title: t('ecomOrder.return.warehouse'), dataIndex: 'warehouseName', width: 140 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_return_status' },
  { title: t('ecomOrder.return.tradeDoc'), dataIndex: 'tradeDocNo', width: 170 },
  { title: t('ecomOrder.return.refundTime'), dataIndex: 'refundTime', width: 170 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  // 查看、退货入库 / 登记退款（按状态互斥）、删除、更多（关闭）
  actionColumn(4, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-return')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, status: undefined, keyword: undefined })
const query = reactive(emptyQuery())
// 从首页待办跳转过来时带上状态（?status=PENDING）
if (route.query.status) query.status = String(route.query.status)
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageEcomReturn, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const form = ref()
const detail = ref()
const dateModal = ref()
const openDetail = (record) => detail.value.open(record.id)
function afterChange() {
  load()
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
</style>
