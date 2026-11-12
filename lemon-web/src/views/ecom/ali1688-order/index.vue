<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_1688_order_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomDev.order.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:ali1688Order:create'" :disabled="!selectedKeys.length" @click="pay(selectedKeys)">
          <template #icon><PayCircleOutlined /></template>{{ $t('ecomDev.order.payBatch') }}
        </a-button>
        <a-button v-perm="'ecom:ali1688Order:sync'" :loading="syncing" @click="syncMany(selectedKeys)">
          <template #icon><SyncOutlined /></template>{{ selectedKeys.length ? $t('ecomDev.order.syncSelected') : $t('ecomDev.order.syncOpen') }}
        </a-button>
        <a-button v-perm="'ecom:ali1688Order:create'" type="primary" @click="createModal.open()">
          <template #icon><ShoppingCartOutlined /></template>{{ $t('ecomDev.order.createFromPo') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'aliOrderId'">
            <a @click="detail.open(record.id)">{{ record.aliOrderId }}</a>
            <div class="sub-text">{{ record.aliCreateTime || record.createTime }}</div>
          </template>
          <template v-else-if="column.key === 'po'">
            <a v-if="record.scmOrderId && can('scm:purchase:list')" @click="router.push({ path: '/scm/purchase', query: { id: record.scmOrderId } })">{{ record.scmOrderNo }}</a>
            <span v-else>{{ record.scmOrderNo || '-' }}</span>
            <div class="sub-text">{{ record.supplierName || '' }}</div>
          </template>
          <template v-else-if="column.key === 'seller'">{{ record.sellerName || record.sellerLoginId || '-' }}</template>
          <template v-else-if="column.key === 'amount'">
            <b>¥ {{ money(record.totalAmount) }}</b>
            <div class="sub-text">{{ $t('ecomDev.order.amountText', { product: money(record.productAmount), fee: money(record.shippingFee) }) }}</div>
          </template>
          <template v-else-if="column.key === 'logistics'">
            <template v-if="record.logisticsNo">{{ record.logisticsCompany || '' }}<div class="sub-text">{{ record.logisticsNo }}</div></template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_1688_order_status" :value="record.status" />
            <div v-if="record.syncMessage" class="err" :title="record.syncMessage">{{ $t('ecomDev.order.syncError') }}</div>
          </template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <OrderDetail ref="detail" @changed="load" />
    <Ali1688OrderModal ref="createModal" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PayCircleOutlined, ShoppingCartOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import Ali1688OrderModal from '@/components/ecom/dev/Ali1688OrderModal.vue'
import OrderDetail from './OrderDetail.vue'
import { ali1688PayUrl, cancelAli1688Order, pageAli1688Order, syncAli1688Order, syncAli1688Orders } from '@/api/ecom/dev'
import { Ali1688OrderStatus } from '@/constants/enums'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'

/**
 * 1688 采购单：采购订单一键下单到 1688 生成（按卖家拆单），同步交易状态、实付金额（含运费）、物流单号与轨迹（定时任务每小时同步），
 * 待付款的可批量打开 1688 收银台付款或取消
 */
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const can = (p) => userStore.hasPerm(p)
const dict = useDict('ecom_1688_order_status')

const columns = [
  { title: t('ecomDev.order.aliOrderId'), key: 'aliOrderId', dataIndex: 'aliOrderId', width: 200 },
  { title: t('ecomDev.order.po'), key: 'po', dataIndex: 'scmOrderNo', width: 180 },
  { title: t('ecomDev.order.seller'), key: 'seller', dataIndex: 'sellerName', width: 200 },
  { title: t('ecomDev.order.itemCount'), dataIndex: 'itemCount', width: 70, align: 'right' },
  { title: t('ecomDev.order.totalQty'), dataIndex: 'totalQty', width: 90, align: 'right' },
  { title: t('ecomDev.order.totalAmount'), key: 'amount', dataIndex: 'totalAmount', width: 230, align: 'right' },
  { title: t('ecomDev.order.logistics'), key: 'logistics', dataIndex: 'logisticsNo', width: 170 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_1688_order_status' },
  { title: t('ecomDev.order.syncTime'), dataIndex: 'syncTime', width: 165 },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-ali1688-order')
const showSearch = ref(true)
const emptyQuery = () => ({ status: undefined, keyword: undefined })
const query = reactive(emptyQuery())
if (route.query.status) query.status = String(route.query.status)
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageAli1688Order, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const detail = ref()
const createModal = ref()
useRouteOpen({ id: (id) => detail.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(load)

const syncing = ref(false)
async function syncMany(ids) {
  syncing.value = true
  try {
    const r = await syncAli1688Orders(ids)
    const text = t('ecomDev.order.syncResult', { total: r.total, success: r.success, changed: r.changed, failed: r.failed })
    r.failed ? message.warning(text) : message.success(text)
    load()
  } finally {
    syncing.value = false
  }
}
async function pay(ids) {
  const url = await ali1688PayUrl([].concat(ids))
  window.open(url, '_blank')
}
function cancel(r) {
  Modal.confirm({
    title: t('ecomDev.order.cancelConfirm', { no: r.aliOrderId }),
    okType: 'danger',
    async onOk() {
      await cancelAli1688Order(r.id)
      message.success(t('ecomDev.order.cancelled'))
      load()
    }
  })
}
async function syncOne(r) {
  await syncAli1688Order(r.id)
  message.success(t('ecomDev.order.synced'))
  load()
}

const rowActions = (r) => [
  { type: 'view', onClick: () => detail.value.open(r.id) },
  { key: 'sync', label: t('ecomDev.order.sync'), icon: SyncOutlined, perm: 'ecom:ali1688Order:sync', onClick: () => syncOne(r) },
  { key: 'pay', label: t('ecomDev.order.pay'), icon: PayCircleOutlined, perm: 'ecom:ali1688Order:create', show: r.status === Ali1688OrderStatus.WAIT_BUYER_PAY, more: true, onClick: () => pay([r.id]) },
  { type: 'cancel', label: t('ecomDev.order.cancel'), perm: 'ecom:ali1688Order:cancel', show: r.status === Ali1688OrderStatus.WAIT_BUYER_PAY, more: true, danger: true, onClick: () => cancel(r) }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.err {
  color: #cf1322;
  font-size: 12px;
}
</style>
