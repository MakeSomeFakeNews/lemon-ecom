<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_purchase_plan_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomPurchase.plan.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:purchasePlan:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomPurchase.plan.add') }}
        </a-button>
        <a-button v-perm="'ecom:replenish:list'" @click="router.push('/ecom/replenish')">
          <template #icon><AlertOutlined /></template>{{ $t('ecomPurchase.plan.fromReplenish') }}
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
          <template v-else-if="column.key === 'totalQty' || column.key === 'orderedQty' || column.key === 'arrivedQty'">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="progressOf(record)" size="small" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_purchase_plan_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <PlanForm ref="form" @success="afterChange" />
    <PlanDetail ref="detail" @changed="load" @edit="(r) => form.openEdit(r)" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AlertOutlined, PlusOutlined, ShoppingCartOutlined, StopOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import PlanForm from './PlanForm.vue'
import PlanDetail from './PlanDetail.vue'
import {
  approvePurchasePlan,
  closePurchasePlan,
  deletePurchasePlan,
  generatePurchaseOrders,
  pagePurchasePlan,
  unapprovePurchasePlan
} from '@/api/ecom/purchasePlan'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { qty } from '@/utils/number'
import { canClose, canGenerate, canUnapprove, isDraft, progressOf } from './plan'

/**
 * 采购计划：由补货建议（本地仓采购）勾选生成或手工新建。草稿 → 审核 → 生成采购订单（按供应商拆单，草稿）→ 采购订单入库后显示已到货
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ecom_purchase_plan_status')

const columns = [
  { title: t('ecomPurchase.plan.docNo'), key: 'docNo', dataIndex: 'docNo', width: 170 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_purchase_plan_status' },
  { title: t('ecomPurchase.plan.planDate'), dataIndex: 'planDate', width: 110 },
  { title: t('ecomPurchase.plan.warehouse'), dataIndex: 'warehouseName', width: 140 },
  { title: t('ecomPurchase.plan.itemCount'), dataIndex: 'itemCount', width: 80, align: 'right' },
  { title: t('ecomPurchase.plan.totalQty'), key: 'totalQty', dataIndex: 'totalQty', width: 110, align: 'right' },
  { title: t('ecomPurchase.plan.orderedQty'), key: 'orderedQty', dataIndex: 'orderedQty', width: 110, align: 'right' },
  { title: t('ecomPurchase.plan.arrivedQty'), key: 'arrivedQty', dataIndex: 'arrivedQty', width: 110, align: 'right' },
  { title: t('ecomPurchase.plan.progress'), key: 'progress', width: 150, exportValue: (r) => `${progressOf(r)}%` },
  { title: t('ecomPurchase.plan.createBy'), dataIndex: 'createByName', width: 110 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  // 查看、编辑 / 生成采购订单、审核 / 删除 / 更多
  actionColumn(4, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-purchase-plan')
const showSearch = ref(true)

const emptyQuery = () => ({ status: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePurchasePlan, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const form = ref()
const detail = ref()
const openDetail = (record) => detail.value.open(record.id)
function afterChange() {
  load()
  detail.value?.reload()
}
useRouteOpen({ id: (id) => detail.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(load)

const approve = useAction(approvePurchasePlan, { successKey: 'ecomPurchase.plan.approved', reload: afterChange })
const unapprove = useAction(unapprovePurchasePlan, { successKey: 'ecomPurchase.plan.unapproved', reload: afterChange })
const close = useAction(closePurchasePlan, { successKey: 'ecomPurchase.plan.closed', reload: afterChange })
const remove = useAction(deletePurchasePlan, { successKey: 'common.msg.deleteSuccess', reload: afterChange })
const generate = useAction(generatePurchaseOrders, {
  successKey: (nos) => t('ecomPurchase.plan.generated', { nos: (nos || []).join(', ') }),
  reload: afterChange
})

const rowActions = (record) => [
  { type: 'view', onClick: () => openDetail(record) },
  { type: 'edit', perm: 'ecom:purchasePlan:edit', show: isDraft(record), onClick: () => form.value.openEdit(record) },
  {
    type: 'approve',
    label: t('ecomPurchase.plan.approve'),
    perm: 'ecom:purchasePlan:approve',
    show: isDraft(record),
    confirm: { title: t('ecomPurchase.plan.approveConfirm', { no: record.docNo }) },
    onClick: () => approve(record.id)
  },
  {
    key: 'generate',
    label: t('ecomPurchase.plan.generate'),
    icon: ShoppingCartOutlined,
    perm: 'ecom:purchasePlan:order',
    show: canGenerate(record),
    confirm: { title: t('ecomPurchase.plan.generateConfirm'), description: t('ecomPurchase.plan.generateContent') },
    onClick: () => generate(record.id)
  },
  {
    type: 'delete',
    perm: 'ecom:purchasePlan:remove',
    show: isDraft(record),
    confirm: { title: t('ecomPurchase.plan.deleteConfirm', { no: record.docNo }), okType: 'danger' },
    onClick: () => remove(record.id)
  },
  {
    type: 'unaudit',
    label: t('ecomPurchase.plan.unapprove'),
    perm: 'ecom:purchasePlan:approve',
    show: canUnapprove(record),
    more: true,
    confirm: { title: t('ecomPurchase.plan.unapproveConfirm', { no: record.docNo }) },
    onClick: () => unapprove(record.id)
  },
  {
    key: 'close',
    label: t('ecomPurchase.plan.close'),
    icon: StopOutlined,
    perm: 'ecom:purchasePlan:edit',
    show: canClose(record),
    more: true,
    danger: true,
    confirm: { title: t('ecomPurchase.plan.closeConfirm', { no: record.docNo }), okType: 'danger' },
    onClick: () => close(record.id)
  }
]
</script>
