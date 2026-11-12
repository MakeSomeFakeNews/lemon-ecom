<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_inbound_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.inbound.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:inbound:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOrder.inbound.addTitle') }}
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
          <template v-else-if="column.key === 'trackingNo'">
            <div>{{ record.trackingNo || '-' }}</div>
            <div v-if="record.carrier" class="sub-text">{{ record.carrier }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'cost'">
            <div class="nowrap">{{ baseMoney(totalCost(record)) }}</div>
            <div class="sub-text nowrap">{{ money(record.freight) }} / {{ money(record.duty) }} / {{ money(record.otherCost) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_inbound_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <InboundForm ref="form" @success="afterChange" />
    <InboundDetail ref="detail" @changed="load" @edit="(r) => form.openEdit(r)" @costs="(r) => form.openCosts(r)" @receive="openReceive" />
    <DateConfirmModal ref="dateModal" :label="$t('ecomOrder.inbound.receiveDate')" @done="afterChange" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DollarOutlined, ImportOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import DateConfirmModal from '@/components/ecom/DateConfirmModal.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import InboundForm from './InboundForm.vue'
import InboundDetail from './InboundDetail.vue'
import { cancelEcomInbound, deleteEcomInbound, pageEcomInbound, receiveEcomInbound, submitEcomInbound } from '@/api/ecom/inbound'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { baseMoney } from '../order/order'
import { canCancel, canDelete, canEditCosts, canReceive, isDraft, totalCost } from './inbound'

/**
 * 头程货件：本地仓 → 平台仓（FBA 等）的调拨。草稿 → 提交发运（配置了审批流程时先审批，生成在途调拨单）→ 签收（调拨收货，平台仓库存增加，
 * 头程运费 / 关税 / 其他费用按数量分摊为单件头程成本）。已发运 / 已签收后可补录物流与费用
 */
const { t } = useI18n()
const dict = useDict('ecom_inbound_status')

const columns = [
  { title: t('ecomOrder.common.docNo'), key: 'docNo', dataIndex: 'docNo', width: 170 },
  { title: t('ecomOrder.common.store'), dataIndex: 'storeName', width: 150 },
  { title: t('ecomOrder.inbound.shipmentId'), dataIndex: 'shipmentId', width: 150 },
  { title: t('ecomOrder.inbound.fromWarehouse'), dataIndex: 'fromWarehouseName', width: 140 },
  { title: t('ecomOrder.inbound.toWarehouse'), dataIndex: 'toWarehouseName', width: 150 },
  { title: t('ecomOrder.common.channel'), dataIndex: 'channelName', width: 140 },
  {
    title: t('ecomOrder.common.trackingNo'),
    key: 'trackingNo',
    width: 170,
    exportColumns: [
      { title: t('ecomOrder.common.carrier'), dataIndex: 'carrier' },
      { title: t('ecomOrder.common.trackingNo'), dataIndex: 'trackingNo' }
    ]
  },
  { title: t('ecomOrder.inbound.shipDate'), dataIndex: 'shipDate', width: 120 },
  { title: t('ecomOrder.inbound.etaDate'), dataIndex: 'etaDate', width: 120 },
  { title: t('ecomOrder.inbound.receiveDate'), dataIndex: 'receiveDate', width: 120 },
  { title: t('ecomOrder.inbound.boxCount'), dataIndex: 'boxCount', width: 80, align: 'right' },
  { title: t('ecomOrder.inbound.totalQty'), key: 'totalQty', dataIndex: 'totalQty', width: 110, align: 'right' },
  {
    title: t('ecomOrder.inbound.totalCost'),
    key: 'cost',
    width: 170,
    align: 'right',
    exportColumns: [
      { title: t('ecomOrder.inbound.freight'), dataIndex: 'freight' },
      { title: t('ecomOrder.inbound.duty'), dataIndex: 'duty' },
      { title: t('ecomOrder.inbound.otherCost'), dataIndex: 'otherCost' },
      { title: t('ecomOrder.inbound.totalCost'), exportValue: totalCost }
    ]
  },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_inbound_status' },
  { title: t('ecomOrder.inbound.transferDoc'), dataIndex: 'transferDocNo', width: 170 },
  { title: t('ecomOrder.common.createBy'), dataIndex: 'createByName', width: 120 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  // 查看、编辑、提交发运 / 签收（按状态互斥）、删除 / 更多
  actionColumn(4, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-inbound')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, status: undefined, keyword: undefined })
const query = reactive(emptyQuery())
// 从看板、首页待办跳转过来时带上状态（?status=SHIPPED）
const route = useRoute()
if (route.query.status) query.status = String(route.query.status)
if (route.query.keyword) query.keyword = String(route.query.keyword)
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageEcomInbound, query)

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
onMounted(load)

/** 提交发运：返回 true 表示进入审批 */
const submit = useAction(submitEcomInbound, {
  successKey: (res) => (res ? t('ecomOrder.inbound.submitted') : t('ecomOrder.inbound.shipped')),
  reload: afterChange
})
const cancel = useAction(cancelEcomInbound, { successKey: 'ecomOrder.inbound.cancelled', reload: afterChange })
const remove = useAction(deleteEcomInbound, { successKey: 'common.msg.deleteSuccess', reload: afterChange })

function openReceive(record) {
  dateModal.value.open({
    title: t('ecomOrder.inbound.receiveTitle', { no: record.docNo }),
    content: t('ecomOrder.inbound.receiveContent'),
    action: async (date) => {
      await receiveEcomInbound(record.id, date)
      message.success(t('ecomOrder.inbound.received'))
    }
  })
}

const rowActions = (record) => [
  { type: 'view', onClick: () => openDetail(record) },
  { type: 'edit', perm: 'ecom:inbound:edit', show: isDraft(record), onClick: () => form.value.openEdit(record) },
  {
    type: 'submit',
    label: t('ecomOrder.inbound.submit'),
    perm: 'ecom:inbound:ship',
    show: isDraft(record),
    confirm: { title: t('ecomOrder.inbound.submitConfirm', { no: record.docNo }), description: t('ecomOrder.inbound.submitContent') },
    onClick: () => submit(record.id)
  },
  { key: 'receive', label: t('ecomOrder.inbound.receive'), icon: ImportOutlined, perm: 'ecom:inbound:receive', show: canReceive(record), onClick: () => openReceive(record) },
  {
    type: 'delete',
    perm: 'ecom:inbound:remove',
    show: canDelete(record),
    confirm: { title: t('ecomOrder.inbound.deleteConfirm', { no: record.docNo }), okType: 'danger' },
    onClick: () => remove(record.id)
  },
  { key: 'costs', label: t('ecomOrder.inbound.costs'), icon: DollarOutlined, perm: 'ecom:inbound:edit', show: canEditCosts(record), more: true, onClick: () => form.value.openCosts(record) },
  {
    type: 'cancel',
    label: t('ecomOrder.inbound.cancel'),
    perm: 'ecom:inbound:edit',
    show: canCancel(record),
    more: true,
    danger: true,
    confirm: { title: t('ecomOrder.inbound.cancelConfirm', { no: record.docNo }), okType: 'danger' },
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
</style>
